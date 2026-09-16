package com.lc.kbagent.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TF-IDF 中文检索索引（RAG 检索环节，无第三方依赖）。
 *
 * <p>实现要点：
 * <ul>
 *   <li>分词：英文/数字按单词切分；中文按 2-gram（二元组）切分——不依赖 jieba 等外部分词器，
 *       对"年假""报销流程"这类词也能稳定命中，是中文轻量检索的常用折中方案；</li>
 *   <li>权重：TF（词频）× IDF（逆文档频率，平滑公式 ln(1+N/(1+df))）；</li>
 *   <li>相似度：查询向量与文档向量的余弦相似度。</li>
 * </ul>
 *
 * <p>适用规模：几百个片段内效果良好且零部署成本。
 * 生产环境文档量达到十万级时，可平滑替换为向量数据库（如 Milvus / ES）或向量化模型，接口保持兼容。
 */
@Component
public class TfidfIndex {

    /** 检索命中结果 */
    public record Hit(KnowledgeBase.Chunk chunk, double score) {
    }

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_]{1,}");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");

    private final List<KnowledgeBase.Chunk> chunks;
    private final Map<String, Double> idf;
    private final List<Map<String, Double>> vectors;
    private final List<Double> norms;

    public TfidfIndex(KnowledgeBase knowledgeBase) {
        this.chunks = knowledgeBase.chunks();

        // 1. 统计每个片段的词频与文档频率 df
        List<Map<String, Integer>> termFreqs = new ArrayList<>();
        Map<String, Integer> docFreq = new HashMap<>();
        for (KnowledgeBase.Chunk chunk : chunks) {
            Map<String, Integer> tf = tokenize(chunk.content());
            termFreqs.add(tf);
            for (String token : tf.keySet()) {
                docFreq.merge(token, 1, Integer::sum);
            }
        }

        // 2. 计算 IDF（平滑公式，避免 df 接近 N 时出现 0）
        int docCount = chunks.size();
        this.idf = new HashMap<>();
        docFreq.forEach((token, df) ->
                idf.put(token, Math.log(1.0 + (double) docCount / (1.0 + df))));

        // 3. 预计算每个片段的 TF-IDF 向量与模长（查询时只需算点积）
        this.vectors = new ArrayList<>();
        this.norms = new ArrayList<>();
        for (Map<String, Integer> tf : termFreqs) {
            Map<String, Double> vector = new HashMap<>();
            double sumSquares = 0.0;
            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                double weight = entry.getValue() * idf.getOrDefault(entry.getKey(), 0.0);
                vector.put(entry.getKey(), weight);
                sumSquares += weight * weight;
            }
            vectors.add(vector);
            norms.add(Math.sqrt(sumSquares));
        }
    }

    /**
     * 检索与 query 最相关的 topK 个片段。
     */
    public List<Hit> search(String query, int topK) {
        // 查询向量
        Map<String, Integer> queryTf = tokenize(query);
        Map<String, Double> queryVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            queryVector.put(entry.getKey(), entry.getValue() * idf.getOrDefault(entry.getKey(), 0.0));
        }
        double queryNorm = 0.0;
        for (double w : queryVector.values()) {
            queryNorm += w * w;
        }
        queryNorm = Math.sqrt(queryNorm);
        if (queryNorm == 0.0 || queryVector.isEmpty()) {
            return List.of();
        }

        // 余弦相似度 = 点积 / (|q| * |d|)
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double dot = 0.0;
            Map<String, Double> docVector = vectors.get(i);
            for (Map.Entry<String, Double> entry : queryVector.entrySet()) {
                Double docWeight = docVector.get(entry.getKey());
                if (docWeight != null) {
                    dot += entry.getValue() * docWeight;
                }
            }
            double docNorm = norms.get(i) == 0.0 ? 1.0 : norms.get(i);
            double similarity = dot / (queryNorm * docNorm);
            if (similarity > 0.02) {
                hits.add(new Hit(chunks.get(i), similarity));
            }
        }
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        return hits.subList(0, Math.min(topK, hits.size()));
    }

    /**
     * 分词：英文/数字单词 + 中文 2-gram。
     */
    static Map<String, Integer> tokenize(String text) {
        Map<String, Integer> tokens = new HashMap<>();
        String lower = text.toLowerCase();

        Matcher wordMatcher = WORD_PATTERN.matcher(lower);
        while (wordMatcher.find()) {
            tokens.merge(wordMatcher.group(), 1, Integer::sum);
        }

        Matcher chineseMatcher = CHINESE_PATTERN.matcher(lower);
        while (chineseMatcher.find()) {
            String segment = chineseMatcher.group();
            for (int i = 0; i + 1 < segment.length(); i++) {
                tokens.merge(segment.substring(i, i + 2), 1, Integer::sum);
            }
        }
        return tokens;
    }
}
