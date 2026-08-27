package org.example.aishop.ai.storage.postgres;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.ai4j.openai4j.OpenAiHttpException;
import org.example.aishop.entity.product.Product;
import org.example.aishop.mapper.product.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL pgvector 向量存储服务
 *
 * 职责：
 * 1. 管理 pgvector 扩展和向量表
 * 2. 商品向量化存储（Embedding → INSERT）
 * 3. 向量相似度检索（余弦相似度 → Top-K）
 * 4. 增量更新（商品上架/下架/编辑）
 *
 * 相比 V1 自建 Redis 向量存储的优势：
 * - pgvector 在数据库层面做向量检索，性能更好
 * - 支持索引加速（IVFFlat / HNSW）
 * - 事务支持，和业务数据在同一数据库
 * - 生产级稳定性
 */
@Service
@ConditionalOnProperty(name = "pgvector.enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreService.class);

    /** 向量维度（text-embedding-v3 输出 1024 维） */
    private static final int VECTOR_DIM = 1024;
    /** 向量表名 */
    private static final String TABLE_NAME = "product_embeddings";
    /** 默认 Top-K */
    private static final int DEFAULT_TOP_K = 5;
    /** 相似度阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.6;

    @Autowired
    @Qualifier("pgVectorDataSource")
    private DataSource dataSource;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ProductMapper productMapper;

    @Value("${ai.rag.top-k:5}")
    private int topK;

    @Value("${ai.rag.similarity-threshold:0.6}")
    private double similarityThreshold;

    /**
     * 初始化 pgvector 扩展和向量表
     * 首次部署时调用
     */
    public void initVectorStore() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. 启用 pgvector 扩展
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");

            // 2. 创建向量表
            String ddl = """
                    CREATE TABLE IF NOT EXISTS %s (
                        id          BIGSERIAL PRIMARY KEY,
                        product_id  BIGINT NOT NULL UNIQUE,
                        product_name VARCHAR(500) NOT NULL,
                        product_text TEXT NOT NULL,
                        embedding   vector(%d),
                        metadata    JSONB,
                        created_at  TIMESTAMP DEFAULT NOW(),
                        updated_at  TIMESTAMP DEFAULT NOW()
                    )
                    """.formatted(TABLE_NAME, VECTOR_DIM);
            stmt.execute(ddl);

            // 3. 创建索引（IVFFlat 适合百万级数据，HNSW 适合更高性能需求）
            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_product_embedding
                    ON %s USING ivfflat (embedding vector_cosine_ops)
                    WITH (lists = 100)
                    """.formatted(TABLE_NAME));

            log.info("pgvector 向量存储初始化完成");

        } catch (Exception e) {
            log.error("pgvector 初始化失败", e);
        }
    }

    /**
     * 存储商品向量
     */
    public void store(Long productId, String productName, String productText, float[] embedding) {
        String sql = """
                INSERT INTO %s (product_id, product_name, product_text, embedding)
                VALUES (?, ?, ?, ?::vector)
                ON CONFLICT (product_id) DO UPDATE SET
                    product_name = EXCLUDED.product_name,
                    product_text = EXCLUDED.product_text,
                    embedding = EXCLUDED.embedding,
                    updated_at = NOW()
                """.formatted(TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productId);
            ps.setString(2, productName);
            ps.setString(3, productText);
            ps.setString(4, formatVector(embedding));
            ps.executeUpdate();

            log.info("商品向量存储成功: productId={}", productId);

        } catch (Exception e) {
            log.error("商品向量存储失败: productId={}", productId, e);
        }
    }

    /**
     * 删除商品向量（商品下架时调用）
     */
    public void delete(Long productId) {
        String sql = "DELETE FROM %s WHERE product_id = ?".formatted(TABLE_NAME);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.executeUpdate();
            log.info("商品向量删除成功: productId={}", productId);
        } catch (Exception e) {
            log.error("商品向量删除失败: productId={}", productId, e);
        }
    }

    // ==================== 全量重建 ====================

    /**
     * 全量重建商品向量库
     *
     * 从 MySQL 读取所有上架商品 → 调用 Embedding API 向量化 → 写入 pgvector
     *
     * @return 成功索引的商品数量
     */
    public int rebuildAll() {
        // 1. 确保表结构存在
        initVectorStore();

        // 2. 从 MySQL 读取所有上架商品
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
        );

        if (products.isEmpty()) {
            log.warn("没有上架商品，向量库为空");
            return 0;
        }

        log.info("开始全量重建向量库，共 {} 个商品...", products.size());

        // 3. 清空旧数据
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE " + TABLE_NAME);
        } catch (Exception e) {
            log.error("清空向量表失败", e);
        }

        // 4. 逐个向量化并写入 pgvector
        int success = 0;
        int fail = 0;
        for (Product product : products) {
            try {
                String productText = buildProductText(product);
                float[] embedding = embeddingModel.embed(productText).content().vector();
                store(product.getId(), product.getName(), productText, embedding);
                success++;
                if (success % 50 == 0) {
                    log.info("向量库重建进度: {}/{}", success, products.size());
                }
            } catch (Exception e) {
                fail++;
                log.warn("商品向量化失败: productId={}, name={}", product.getId(), product.getName());
            }
        }

        log.info("向量库重建完成: 成功 {} 个, 失败 {} 个, 共 {} 个商品", success, fail, products.size());
        return success;
    }

    /**
     * 构建商品描述文本（用于 Embedding 向量化）
     */
    private String buildProductText(Product product) {
        StringBuilder sb = new StringBuilder();
        sb.append("商品名称：").append(product.getName());
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            sb.append("。商品描述：").append(product.getDescription());
        }
        if (product.getPrice() != null) {
            sb.append("。价格：").append(product.getPrice()).append("元");
        }
        if (product.getSales() != null && product.getSales() > 0) {
            sb.append("。销量：").append(product.getSales()).append("件");
        }
        return sb.toString();
    }

    // ==================== 语义检索 ====================

    /**
     * 语义检索 — 基于余弦相似度查找最相关的商品
     *
     * 完整流程：用户查询文本 → EmbeddingModel 向量化 → pgvector 余弦相似度检索 → Top-K 结果
     *
     * @param queryText 用户查询文本
     * @return RAG 上下文文本（格式化的商品信息），未找到返回 null
     */
    public String search(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return null;
        }
        try {
            // 1. 调用 Embedding API 将查询文本转为向量
            float[] queryVector = embeddingModel.embed(queryText).content().vector();
            // 2. 使用 pgvector 进行余弦相似度检索
            return searchWithVector(queryVector);
        } catch (OpenAiHttpException e) {
            log.error("RAG 语义检索失败 — Embedding 接口 HTTP 错误: code={}, message={}, queryText={}",
                    e.code(), e.getMessage(), queryText);
            return null;
        } catch (Exception e) {
            log.error("RAG 语义检索失败: queryText={}", queryText, e);
            return null;
        }
    }

    /**
     * 语义检索 — 使用已向量化的查询向量
     */
    public String searchWithVector(float[] queryVector) {
        String sql = """
                SELECT product_id, product_name, product_text,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM %s
                WHERE 1 - (embedding <=> ?::vector) >= ?
                ORDER BY similarity DESC
                LIMIT ?
                """.formatted(TABLE_NAME);

        StringBuilder result = new StringBuilder();
        result.append("【以下是商城相关商品信息，供你参考推荐给用户】\n");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, formatVector(queryVector));
            ps.setString(2, formatVector(queryVector));
            ps.setDouble(3, similarityThreshold);
            ps.setInt(4, topK);

            int idx = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idx++;
                    result.append(idx).append(". ");
                    result.append("商品名称：").append(rs.getString("product_name"));
                    result.append("，描述：").append(rs.getString("product_text"));
                    result.append("（相关度：").append(String.format("%.0f%%", rs.getDouble("similarity") * 100)).append("）");
                    result.append("\n");
                }
            }

            if (idx == 0) {
                return null;
            }

            result.append("请基于以上真实商品信息，友好地向用户推荐。");
            return result.toString();

        } catch (Exception e) {
            log.error("pgvector 向量检索失败", e);
            return null;
        }
    }

    /**
     * 获取向量总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * 格式化向量为 pgvector 识别的字符串格式: [0.1,0.2,0.3,...]
     */
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}