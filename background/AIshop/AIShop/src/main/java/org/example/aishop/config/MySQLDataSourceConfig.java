package org.example.aishop.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * MySQL 主数据源配置（@Primary）
 *
 * 确保 MyBatis-Plus / JPA 等框架始终使用 MySQL 作为默认数据源，
 * 不会被 PostgreSQL（pgvector）数据源干扰。
 */
@Configuration
public class MySQLDataSourceConfig {

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new DruidDataSource();
    }
}