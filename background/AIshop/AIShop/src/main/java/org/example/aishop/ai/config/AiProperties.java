package org.example.aishop.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块统一配置属性
 *
 * 映射 application.yml 中 ai.* 配置项
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** Chat 配置 */
    private Chat chat = new Chat();

    /** RAG 配置 */
    private Rag rag = new Rag();

    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }

    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }

    // ==================== 内部类 ====================

    public static class Chat {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int maxTokens = 2000;
        private double temperature = 0.7;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
    }

    public static class Rag {
        private boolean enabled = true;
        private String embeddingModel = "text-embedding-v3";
        private int topK = 5;
        private double similarityThreshold = 0.6;
        private long knowledgeTtl = 3600;
        private long searchCacheTtl = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        public long getKnowledgeTtl() { return knowledgeTtl; }
        public void setKnowledgeTtl(long knowledgeTtl) { this.knowledgeTtl = knowledgeTtl; }
        public long getSearchCacheTtl() { return searchCacheTtl; }
        public void setSearchCacheTtl(long searchCacheTtl) { this.searchCacheTtl = searchCacheTtl; }
    }
}