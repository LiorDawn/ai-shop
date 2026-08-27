package org.example.aishop.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.aishop.ai.config.AiProperties;
import org.example.aishop.entity.product.Product;
import org.example.aishop.mapper.product.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品知识库服务
 *
 * 基于 Redis 持久化向量存储 + LangChain4j EmbeddingModel，
 * 支持全量重建、增量更新、删除、高频查询缓存。
 */
@Slf4j
@Service
public class ProductKnowledgeBase {

    private static final String CACHE_PREFIX = "AISHOP:RAG:CACHE:";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private RedisBackedEmbeddingStore embeddingStore;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private StringRedisTemplate redis;

    // ==================== 全量重建 ====================

    /**
     * 重建全量商品知识库
     */
    public int rebuild() {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
        );

        if (products.isEmpty()) {
            log.warn("没有上架商品，知识库为空");
            return 0;
        }

        // 清空旧数据
        embeddingStore.removeAll();

        // 批量向量化并存入 Redis
        int success = 0;
        for (Product product : products) {
            if (storeProductVector(product)) {
                success++;
            }
        }

        // 清空检索缓存
        clearSearchCache();

        log.info("知识库构建完成，共索引 " + success + "/" + products.size() + " 个商品");
        return success;
    }

    // ==================== 增量更新 ====================

    /**
     * 新增单商品（上架时调用）
     */
    public boolean addProduct(Product product) {
        if (product == null || product.getId() == null) {
            return false;
        }
        boolean ok = storeProductVector(product);
        if (ok) {
            clearSearchCache();
            log.info("新增商品向量: productId=" + product.getId() + ", name=" + product.getName());
        }
        return ok;
    }

    /**
     * 更新单商品（编辑时调用）
     */
    public boolean updateProduct(Product product) {
        if (product == null || product.getId() == null) {
            return false;
        }
        // 删除旧的向量
        removeProduct(product.getId());
        // 如果是上架状态，新增向量
        if (Integer.valueOf(1).equals(product.getStatus())) {
            return addProduct(product);
        }
        return true;
    }

    /**
     * 删除单商品（下架/删除时调用）
     */
    public boolean removeProduct(Long productId) {
        if (productId == null) return false;
        String id = findEmbeddingId(productId);
        if (id != null) {
            embeddingStore.remove(id);
            clearSearchCache();
            log.info("删除商品向量: productId=" + productId);
            return true;
        }
        return false;
    }

    // ==================== 语义搜索（带缓存） ====================

    /**
     * 语义搜索商品
     *
     * 高频重复提问命中 Redis 缓存，避免重复向量化和检索
     */
    public List<ProductSearchResult> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // 1. 查缓存
        String cacheKey = buildCacheKey(query);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("RAG 检索命中缓存: query=" + query);
            return parseCachedResult(cached);
        }

        // 2. Embedding 向量化
        Embedding queryEmbedding = embedWithRetry(query);
        if (queryEmbedding == null) {
            log.warn("RAG 查询向量化失败: query=" + query);
            return List.of();
        }

        // 3. 向量检索
        dev.langchain4j.store.embedding.EmbeddingSearchRequest request =
                dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(aiProperties.getRag().getTopK())
                        .minScore(aiProperties.getRag().getSimilarityThreshold())
                        .build();

        dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> result =
                embeddingStore.search(request);

        List<ProductSearchResult> results = result.matches().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        // 4. 写入缓存（短 TTL）
        cacheResult(cacheKey, results);

        return results;
    }

    // ==================== 知识库状态 ====================

    public int getKnowledgeBaseSize() {
        return embeddingStore.count();
    }

    // ==================== 私有方法 ====================

    /** 向量化并存储单个商品 */
    private boolean storeProductVector(Product product) {
        try {
            TextSegment segment = toSegment(product);
            Embedding embedding = embedWithRetry(segment);
            if (embedding == null) {
                log.warn("商品向量化失败: productId=" + product.getId());
                return false;
            }
            // 使用 productId 作为 embedding ID，方便查找和删除
            embeddingStore.add(String.valueOf(product.getId()), embedding, segment);
            return true;
        } catch (Exception e) {
            log.error("存储商品向量异常: productId=" + product.getId(), e);
            return false;
        }
    }

    /** Embedding 重试调用（@Retryable 需在独立方法上） */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Embedding embedWithRetry(String text) {
        return embeddingModel.embed(text).content();
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Embedding embedWithRetry(TextSegment segment) {
        return embeddingModel.embed(segment).content();
    }

    /** 通过 productId 查找 embedding ID */
    private String findEmbeddingId(Long productId) {
        Set<String> ids = redis.opsForSet().members("AISHOP:RAG:V2:INDEX");
        if (ids == null) return null;
        String pid = String.valueOf(productId);
        return ids.stream().filter(pid::equals).findFirst().orElse(null);
    }

    /** 构建商品描述文本 */
    private TextSegment toSegment(Product product) {
        String text = buildProductText(product);
        Metadata metadata = new Metadata();
        metadata.put("productId", String.valueOf(product.getId()));
        metadata.put("name", product.getName() != null ? product.getName() : "");
        metadata.put("price", product.getPrice() != null ? product.getPrice().toString() : "0");
        metadata.put("description", product.getDescription() != null ? product.getDescription() : "");
        metadata.put("sales", product.getSales() != null ? String.valueOf(product.getSales()) : "0");
        return TextSegment.from(text, metadata);
    }

    private String buildProductText(Product product) {
        StringBuilder sb = new StringBuilder();
        sb.append("商品名称：").append(product.getName());
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            sb.append("。商品描述：").append(product.getDescription());
        }
        sb.append("。价格：").append(product.getPrice()).append("元");
        if (product.getSales() != null && product.getSales() > 0) {
            sb.append("。销量：").append(product.getSales()).append("件");
        }
        return sb.toString();
    }

    private ProductSearchResult toSearchResult(
            dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata meta = segment.metadata();
        ProductSearchResult r = new ProductSearchResult();
        r.setProductId(Long.valueOf(meta.getString("productId")));
        r.setName(meta.getString("name"));
        r.setPrice(meta.getString("price"));
        r.setDescription(meta.getString("description"));
        r.setSales(Integer.valueOf(meta.getString("sales")));
        r.setSimilarity(match.score());
        return r;
    }

    // ==================== 缓存 ====================

    private String buildCacheKey(String query) {
        return CACHE_PREFIX + Integer.toHexString(query.hashCode());
    }

    /** 缓存搜索结果（短 TTL，避免返回过时结果） */
    private void cacheResult(String key, List<ProductSearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (ProductSearchResult r : results) {
            sb.append(r.getProductId()).append(",")
                    .append(r.getName()).append(",")
                    .append(r.getPrice()).append(",")
                    .append(r.getDescription() != null ? r.getDescription().replace(",", "，") : "").append(",")
                    .append(r.getSales()).append(",")
                    .append(r.getSimilarity()).append(";");
        }
        redis.opsForValue().set(key, sb.toString(), 60, TimeUnit.SECONDS); // 1 分钟缓存
    }

    private List<ProductSearchResult> parseCachedResult(String cached) {
        return java.util.Arrays.stream(cached.split(";"))
                .filter(s -> !s.isEmpty())
                .map(line -> {
                    String[] parts = line.split(",", 6);
                    ProductSearchResult r = new ProductSearchResult();
                    r.setProductId(Long.valueOf(parts[0]));
                    r.setName(parts[1]);
                    r.setPrice(parts[2]);
                    r.setDescription(parts.length > 3 ? parts[3] : "");
                    r.setSales(parts.length > 4 ? Integer.valueOf(parts[4]) : 0);
                    r.setSimilarity(parts.length > 5 ? Double.parseDouble(parts[5]) : 0);
                    return r;
                })
                .collect(Collectors.toList());
    }

    /** 全量清空检索缓存（知识库变更时调用） */
    private void clearSearchCache() {
        Set<String> keys = redis.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    // ==================== 内部类 ====================

    @Data
    public static class ProductSearchResult {
        private Long productId;
        private String name;
        private String price;
        private String description;
        private Integer sales;
        private double similarity;
    }
}