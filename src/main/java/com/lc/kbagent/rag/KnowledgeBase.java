package com.lc.kbagent.rag;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 知识库：启动时加载 classpath:kb/*.md 文档，并按段落切分（Chunking）。
 *
 * <p>切分策略（RAG 的经典环节）：
 * 1. 按空行把文档切成段落；
 * 2. 过短段落（&lt;30 字）并入上一条，避免产生无效片段；
 * 3. 超长段落（&gt;400 字）在句号/换行处硬切，保证检索粒度和 token 成本可控。
 *
 * <p>注：kb 目录下为演示用示例数据（虚构的"星辰科技"），换成真实文档即可上线使用。
 */
@Component
public class KnowledgeBase {

    /** 知识库片段 */
    public record Chunk(String id, String docName, String content) {
    }

    private final List<Chunk> chunks;

    public KnowledgeBase() throws IOException {
        this.chunks = loadChunks();
    }

    /** 供测试或自定义知识库直接注入片段列表，跳过 classpath 加载 */
    public KnowledgeBase(List<Chunk> chunks) {
        this.chunks = chunks;
    }

    public List<Chunk> chunks() {
        return chunks;
    }

    private List<Chunk> loadChunks() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:kb/*.md");
        // 按文件名排序，保证加载顺序稳定
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        List<Chunk> list = new ArrayList<>();
        int seq = 0;
        for (Resource resource : resources) {
            String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String fileName = resource.getFilename();
            for (String chunkText : splitChunks(raw)) {
                list.add(new Chunk("c" + (seq++), fileName, chunkText));
            }
        }
        return list;
    }

    /** 段落切分 + 清洗 Markdown 符号 */
    private List<String> splitChunks(String content) {
        List<String> paragraphs = new ArrayList<>();
        for (String p : content.split("\\n\\s*\\n")) {
            String cleaned = clean(p);
            if (!cleaned.isBlank()) {
                paragraphs.add(cleaned);
            }
        }

        // 过短段落并入上一条
        List<String> merged = new ArrayList<>();
        for (String p : paragraphs) {
            if (!merged.isEmpty() && p.length() < 30) {
                merged.set(merged.size() - 1, merged.get(merged.size() - 1) + "\n" + p);
            } else {
                merged.add(p);
            }
        }

        // 超长段落硬切
        List<String> result = new ArrayList<>();
        for (String p : merged) {
            while (p.length() > 400) {
                int cutAt = Math.max(p.lastIndexOf('。', 380), p.lastIndexOf('\n', 380));
                int idx = cutAt > 200 ? cutAt + 1 : 380;
                result.add(p.substring(0, Math.min(idx, p.length())));
                p = p.substring(Math.min(idx, p.length()));
            }
            result.add(p);
        }
        return result;
    }

    /** 去掉 Markdown 标题符、列表符与加粗斜体等符号，得到干净文本 */
    private String clean(String paragraph) {
        return paragraph
                .replaceAll("(?m)^\\s*#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*[-*]\\s+", "")
                .replaceAll("[*_`>]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
