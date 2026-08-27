package org.example.aishop.ai.config;

import org.example.aishop.ai.storage.postgres.PgVectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 向量数据库启动初始化器
 *
 * 应用启动时自动：
 * 1. 初始化 pgvector 扩展和表结构（CREATE EXTENSION + CREATE TABLE IF NOT EXISTS）
 * 2. 检查向量库是否为空，如果为空则自动从 MySQL 全量重建
 *
 * 关闭自动重建：设置 pgvector.auto-rebuild=false
 * 关闭整个 pgvector（服务器内存不足时）：设置 pgvector.enabled=false
 */
@Component
@ConditionalOnProperty(name = "pgvector.enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PgVectorInitializer.class);

    @Autowired
    private PgVectorStoreService pgVectorStoreService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 向量数据库初始化开始 ==========");

        try {
            // 1. 初始化表结构（幂等，已有则跳过）
            pgVectorStoreService.initVectorStore();

            // 2. 检查是否需要重建
            int count = pgVectorStoreService.count();
            if (count == 0) {
                log.info("向量库为空，自动从 MySQL 全量重建...");
                int indexed = pgVectorStoreService.rebuildAll();
                log.info("========== 向量库自动重建完成，共索引 {} 个商品 ==========", indexed);
            } else {
                log.info("向量库已有 {} 条数据，跳过自动重建", count);
                log.info("========== 向量数据库初始化完成（已有数据） ==========");
            }
        } catch (Exception e) {
            log.error("向量数据库初始化失败，RAG 功能将不可用。请检查 PostgreSQL 连接和 pgvector 扩展", e);
            log.info("========== 向量数据库初始化失败 ==========");
        }
    }
}