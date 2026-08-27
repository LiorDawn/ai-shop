package org.example.aishop.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * PostgreSQL 向量数据库数据源配置
 *
 * 用于 pgvector 向量检索，与主业务 MySQL 数据源分离。
 * 启动时自动创建 aishop_rag 数据库（不存在则创建）。
 *
 * 可通过 pgvector.enabled=false 关闭 pgvector（服务器内存不足时使用）。
 */
@Configuration
@ConditionalOnProperty(name = "pgvector.enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorConfig.class);

    @Value("${pgvector.datasource.url}")
    private String url;

    @Value("${pgvector.datasource.username}")
    private String username;

    @Value("${pgvector.datasource.password}")
    private String password;

    @Value("${pgvector.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean(name = "pgVectorDataSource")
    public DataSource pgVectorDataSource() {
        // 自动创建数据库（不存在则创建）
        ensureDatabaseExists();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.addDataSourceProperty("prepareThreshold", "0");
        return new HikariDataSource(config);
    }

    /**
     * 自动创建向量数据库（如果不存在）
     *
     * 先连接到默认的 postgres 库，执行 CREATE DATABASE，
     * 再连接到目标库 aishop_rag。
     */
    private void ensureDatabaseExists() {
        // 从 URL 中提取目标数据库名和基础连接地址
        // 格式: jdbc:postgresql://host:port/aishop_rag
        String dbName = url.substring(url.lastIndexOf('/') + 1);
        String baseUrl = url.substring(0, url.lastIndexOf('/')) + "/postgres";

        try {
            Class.forName(driverClassName);
            try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
                 Statement stmt = conn.createStatement()) {
                // 创建数据库（幂等，已存在则跳过）
                stmt.execute("CREATE DATABASE " + dbName);
                log.info("向量数据库 {} 创建成功", dbName);
            }
        } catch (Exception e) {
            // 数据库已存在时会报错，忽略即可
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.info("向量数据库 {} 已存在，跳过创建", dbName);
            } else {
                log.warn("向量数据库自动创建失败（可能已存在或权限不足）: {}", e.getMessage());
            }
        }
    }
}