package org.example.aishop.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.ai.rag.ProductKnowledgeBase;
import org.example.aishop.ai.rag.RagService;
import org.example.aishop.ai.storage.postgres.PgVectorStoreService;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.product.Product;
import org.example.aishop.mapper.product.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG 知识库管理接口
 */
@Tag(name = "RAG 知识库管理", description = "商品知识库构建、向量搜索、RAG 状态查询")
@RestController
@RequestMapping("/admin/rag")
public class RagController {

    @Autowired
    private ProductKnowledgeBase knowledgeBase;

    @Autowired
    private RagService ragService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired(required = false)
    private PgVectorStoreService pgVectorStoreService;  // 关闭 pgvector 时为 null

    // ==================== Redis 向量库（V1，保留兼容） ====================

    @Operation(summary = "重建知识库", description = "重新生成所有上架商品的向量索引")
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild() {
        int count = knowledgeBase.rebuild();
        return Result.success(Map.of(
                "indexedCount", count,
                "message", count > 0 ? "知识库重建完成，共索引 " + count + " 个商品" : "知识库重建完成，无上架商品"
        ));
    }

    @Operation(summary = "语义搜索商品", description = "根据查询文本，返回语义最相关的商品列表")
    @GetMapping("/search")
    public Result<List<ProductKnowledgeBase.ProductSearchResult>> search(@RequestParam String query) {
        List<ProductKnowledgeBase.ProductSearchResult> results = knowledgeBase.search(query);
        return Result.success(results);
    }

    @Operation(summary = "知识库状态", description = "查看知识库当前状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        int size = knowledgeBase.getKnowledgeBaseSize();
        return Result.success(Map.of(
                "size", size,
                "hasData", size > 0,
                "message", size > 0 ? "知识库已就绪，共 " + size + " 个商品" : "知识库为空，请执行 /admin/rag/rebuild"
        ));
    }

    // ==================== 增量更新 ====================

    @Operation(summary = "新增商品向量", description = "单商品上架后增量添加向量")
    @PostMapping("/products/{id}")
    public Result<Map<String, Object>> addProduct(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        boolean ok = knowledgeBase.addProduct(product);
        return Result.success(Map.of("success", ok, "productId", id));
    }

    @Operation(summary = "更新商品向量", description = "商品编辑后增量更新向量")
    @PutMapping("/products/{id}")
    public Result<Map<String, Object>> updateProduct(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        boolean ok = knowledgeBase.updateProduct(product);
        return Result.success(Map.of("success", ok, "productId", id));
    }

    @Operation(summary = "删除商品向量", description = "商品下架/删除后移除向量")
    @DeleteMapping("/products/{id}")
    public Result<Map<String, Object>> removeProduct(@PathVariable Long id) {
        boolean ok = knowledgeBase.removeProduct(id);
        return Result.success(Map.of("success", ok, "productId", id));
    }

    // ==================== PostgreSQL pgvector 向量库（V2，AI 对话使用） ====================

    @Operation(summary = "重建 pgvector 向量库", description = "从 MySQL 全量读取商品 → Embedding 向量化 → 写入 pgvector，供 AI 对话 RAG 检索")
    @PostMapping("/pgvector/rebuild")
    public Result<Map<String, Object>> rebuildPgVector() {
        if (pgVectorStoreService == null) {
            return Result.fail("pgvector 未启用（pgvector.enabled=false）");
        }
        int count = pgVectorStoreService.rebuildAll();
        return Result.success(Map.of(
                "indexedCount", count,
                "message", count > 0 ? "pgvector 向量库重建完成，共索引 " + count + " 个商品" : "pgvector 向量库为空，无上架商品"
        ));
    }

    @Operation(summary = "pgvector 语义搜索", description = "测试 pgvector 向量检索，输入查询文本返回相关商品")
    @GetMapping("/pgvector/search")
    public Result<Map<String, Object>> searchPgVector(@RequestParam String query) {
        String result = pgVectorStoreService.search(query);
        if (result == null) {
            return Result.success(Map.of("found", false, "message", "未找到相关商品"));
        }
        return Result.success(Map.of("found", true, "context", result));
    }

    @Operation(summary = "pgvector 向量库状态", description = "查看 pgvector 向量库当前状态")
    @GetMapping("/pgvector/status")
    public Result<Map<String, Object>> statusPgVector() {
        if (pgVectorStoreService == null) {
            return Result.fail("pgvector 未启用（pgvector.enabled=false）");
        }
        int size = pgVectorStoreService.count();
        return Result.success(Map.of(
                "size", size,
                "hasData", size > 0,
                "ready", size > 0,
                "message", size > 0 ? "pgvector 向量库已就绪，共 " + size + " 个商品向量" : "pgvector 向量库为空，请先执行 POST /admin/rag/pgvector/rebuild"
        ));
    }
}