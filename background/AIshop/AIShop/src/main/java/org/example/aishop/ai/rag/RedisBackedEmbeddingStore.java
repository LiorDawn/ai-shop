package org.example.aishop.ai.rag;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 持久化向量存储
 *
 * 替代 InMemoryEmbeddingStore，解决：
 * 1. 服务重启向量丢失 → Redis 持久化，跨重启/部署/容器不丢失
 * 2. 集群多实例不共享 → 所有实例读写同一 Redis，数据统一
 * 3. JVM 堆内存暴涨 → 向量驻留 Redis，不占用堆内存
 * 4. 可扩展 → 后续可无缝迁移到 Redis Stack / Milvus
 *
 * Key 结构：
 *   AISHOP:RAG:V2:INDEX        → Set, 所有 embedding ID
 *   AISHOP:RAG:V2:VEC:{id}     → String, vector JSON 数组
 *   AISHOP:RAG:V2:META:{id}    → Hash, text + metadata JSON
 */
public class RedisBackedEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(RedisBackedEmbeddingStore.class);

    private static final String KEY_PREFIX = "AISHOP:RAG:V2";
    private static final String INDEX_KEY = KEY_PREFIX + ":INDEX";
    private static final String VEC_KEY_PREFIX = KEY_PREFIX + ":VEC:";
    private static final String META_KEY_PREFIX = KEY_PREFIX + ":META:";

    private final StringRedisTemplate redis;
    private final long ttlSeconds;

    public RedisBackedEmbeddingStore(StringRedisTemplate redis, long ttlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
    }

    // ==================== add ====================

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        store(id, embedding, null);
        return id;
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        store(id, embedding, textSegment);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        store(id, embedding, null);
    }

    public void add(String id, Embedding embedding, TextSegment textSegment) {
        store(id, embedding, textSegment);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return embeddings.stream()
                .map(this::add)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        if (embeddings.size() != embedded.size()) {
            throw new IllegalArgumentException("embeddings.size() != embedded.size()");
        }
        List<String> ids = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            ids.add(add(embeddings.get(i), embedded.get(i)));
        }
        return ids;
    }

    // ==================== search ====================

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        float[] queryVec = request.queryEmbedding().vector();
        int maxResults = request.maxResults();
        double minScore = request.minScore();

        // 从 Redis 加载所有向量
        Set<String> ids = redis.opsForSet().members(INDEX_KEY);
        if (ids == null || ids.isEmpty()) {
            return new EmbeddingSearchResult<>(List.of());
        }

        // 批量读取向量（pipeline 加速）
        List<String> vecKeys = ids.stream()
                .map(id -> VEC_KEY_PREFIX + id)
                .collect(Collectors.toList());
        List<String> vecJsons = redis.opsForValue().multiGet(vecKeys);

        // 批量读取元数据
        List<String> metaKeys = ids.stream()
                .map(id -> META_KEY_PREFIX + id)
                .collect(Collectors.toList());
        List<String> metaJsons = redis.opsForValue().multiGet(metaKeys);

        // 计算相似度并排序
        List<ScoredMatch> scored = new ArrayList<>();
        List<String> idList = new ArrayList<>(ids);
        for (int i = 0; i < idList.size(); i++) {
            if (vecJsons.get(i) == null) continue;

            try {
                float[] vec = parseVector(vecJsons.get(i));
                double score = cosineSimilarity(queryVec, vec);
                if (score >= minScore) {
                    TextSegment segment = null;
                    if (metaJsons.get(i) != null) {
                        segment = parseTextSegment(metaJsons.get(i));
                    }
                    scored.add(new ScoredMatch(idList.get(i), score, vec, segment));
                }
            } catch (Exception e) {
                log.warn("解析向量失败, id=" + idList.get(i));
            }
        }

        // 按相似度降序，取 Top-K
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        if (scored.size() > maxResults) {
            scored = scored.subList(0, maxResults);
        }

        List<EmbeddingMatch<TextSegment>> matches = scored.stream()
                .map(s -> new EmbeddingMatch<>(
                        s.score,
                        s.id,
                        new Embedding(s.vector),
                        s.segment))
                .collect(Collectors.toList());

        return new EmbeddingSearchResult<>(matches);
    }

    // ==================== remove ====================

    @Override
    public void remove(String id) {
        redis.delete(VEC_KEY_PREFIX + id);
        redis.delete(META_KEY_PREFIX + id);
        redis.opsForSet().remove(INDEX_KEY, id);
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<String> vecKeys = ids.stream().map(id -> VEC_KEY_PREFIX + id).collect(Collectors.toList());
        List<String> metaKeys = ids.stream().map(id -> META_KEY_PREFIX + id).collect(Collectors.toList());
        redis.delete(vecKeys);
        redis.delete(metaKeys);
        redis.opsForSet().remove(INDEX_KEY, ids.toArray());
    }

    public void removeAll(Filter filter) {
        // 不支持按 Filter 批量删除，业务层自行管理
        log.warn("removeAll(Filter) 未实现，请使用 removeAll(Collection) 或 remove(String)");
    }

    public void removeAll() {
        Set<String> ids = redis.opsForSet().members(INDEX_KEY);
        if (ids != null && !ids.isEmpty()) {
            removeAll(ids);
        }
    }

    // ==================== 数量 ====================

    public int count() {
        Long size = redis.opsForSet().size(INDEX_KEY);
        return size == null ? 0 : size.intValue();
    }

    // ==================== 私有方法 ====================

    private void store(String id, Embedding embedding, TextSegment segment) {
        String vecKey = VEC_KEY_PREFIX + id;
        String metaKey = META_KEY_PREFIX + id;

        // 存储向量
        float[] vector = embedding.vector();
        List<Float> vecList = new ArrayList<>(vector.length);
        for (float v : vector) {
            vecList.add(v);
        }
        redis.opsForValue().set(vecKey, JSONUtil.toJsonStr(vecList), ttlSeconds, TimeUnit.SECONDS);

        // 存储元数据
        if (segment != null) {
            Map<String, String> metaMap = new LinkedHashMap<>();
            metaMap.put("text", segment.text());
            if (segment.metadata() != null) {
                Metadata meta = segment.metadata();
                for (String key : meta.toMap().keySet()) {
                    metaMap.put(key, meta.getString(key));
                }
            }
            redis.opsForValue().set(metaKey, JSONUtil.toJsonStr(metaMap), ttlSeconds, TimeUnit.SECONDS);
        }

        // 加入索引
        redis.opsForSet().add(INDEX_KEY, id);
        redis.expire(vecKey, ttlSeconds, TimeUnit.SECONDS);
        redis.expire(metaKey, ttlSeconds, TimeUnit.SECONDS);
        redis.expire(INDEX_KEY, ttlSeconds, TimeUnit.SECONDS);
    }

    private float[] parseVector(String json) {
        List<Object> list = JSONUtil.parseArray(json);
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = ((Number) list.get(i)).floatValue();
        }
        return result;
    }

    private TextSegment parseTextSegment(String json) {
        Map<String, Object> map = JSONUtil.parseObj(json);
        String text = (String) map.getOrDefault("text", "");
        Metadata metadata = new Metadata();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!"text".equals(entry.getKey()) && entry.getValue() != null) {
                metadata.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return TextSegment.from(text, metadata);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class ScoredMatch {
        final String id;
        final double score;
        final float[] vector;
        final TextSegment segment;

        ScoredMatch(String id, double score, float[] vector, TextSegment segment) {
            this.id = id;
            this.score = score;
            this.vector = vector;
            this.segment = segment;
        }
    }
}