package org.example.aishop.ai.orchestration;

import org.example.aishop.ai.agent.springai.AiAgentProvider;
import org.example.aishop.entity.ai.AISession;
import org.example.aishop.ai.storage.mysql.MysqlPersistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.Executor;

/**
 * 智能购物助手 Agent — 继承 BaseAgent 模板方法，注入四层组件
 *
 * 实现 AiAgentProvider 接口，与 Spring AI 新模块平级共存。
 * 默认 Agent（ai.provider=langchain4j 或未配置时使用）。
 */
@Component
public class AIChatAgent extends BaseAgent implements AiAgentProvider {

    @Autowired
    private Memory memory;

    @Autowired
    private Planner planner;

    @Autowired
    private ToolExecutor toolExecutor;

    @Autowired
    private Summarizer summarizer;

    @Autowired
    @Qualifier("aiStreamTaskExecutor")
    private ThreadPoolTaskExecutor aiStreamTaskExecutor;

    @Autowired
    private MysqlPersistService mysqlPersistService;

    /** 当前请求的会话 ID（用于持久化） */
    private final ThreadLocal<Long> currentSessionId = new ThreadLocal<>();

    @Override
    protected Memory getMemory() {
        return memory;
    }

    @Override
    protected Planner getPlanner() {
        return planner;
    }

    @Override
    protected ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    @Override
    protected Summarizer getSummarizer() {
        return summarizer;
    }

    @Override
    protected Executor getExecutor() {
        return aiStreamTaskExecutor;
    }

    /**
     * 钩子：将 sessionId 注入到 StreamingSummarizer，使其持久化时能关联会话
     */
    @Override
    protected void summariesToSession(Long sessionId) {
        currentSessionId.set(sessionId);
    }

    /**
     * 钩子：创建新会话
     */
    @Override
    protected Long getOrCreateSession(Long userId, Long sessionId) {
        if (sessionId != null) return sessionId;

        AISession session = new AISession();
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setCreateTime(new Date());
        session.setLastTime(new Date());
        mysqlPersistService.insertSession(session);
        return session.getId();
    }
}