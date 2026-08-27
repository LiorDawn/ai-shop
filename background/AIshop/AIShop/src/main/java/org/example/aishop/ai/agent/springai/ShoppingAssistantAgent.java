package org.example.aishop.ai.agent.springai;

import org.example.aishop.entity.ai.AISession;
import org.example.aishop.ai.storage.mysql.MysqlPersistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.Executor;

/**
 * 第4层：角色实例配置层｜具体业务 Agent
 *
 * <h3>职责</h3>
 * 只做配置，零业务逻辑。把上层全部现成能力装配成一个可用的业务 Agent 实例。
 *
 * <h3>只做 4 件配置</h3>
 * <pre>
 * 1. setName() — Agent 名称
 * 2. getSystemPrompt() — 角色系统提示词，定义 AI 身份、人设、业务规则
 * 3. getNextStepPrompt() — 下一步指令提示词，指导模型如何做决策
 * 4. maxSteps() — 覆盖最大步数
 * </pre>
 *
 * <h3>新增其他 Agent</h3>
 * 要新增客服 Agent、数据统计 Agent，只需再写一个类似本类的子类，
 * 改上面 4 项配置即可。think/act/run 等流程方法全部继承，不重写。
 */
@Component("springAiAgent")
@ConditionalOnProperty(name = "ai.provider", havingValue = "spring-ai", matchIfMissing = false)
public class ShoppingAssistantAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是智能购物助手「小智」，职责是帮助用户推荐商品、查询订单、管理购物车、处理售后。

            规则：
            1. 回复简洁友好，不超过 200 字
            2. 推荐商品时必须基于上面提供的真实商品信息，不得编造
            3. 如果用户需要操作（加购/删购/改数量），必须调用对应工具
            4. 操作完成后告知用户结果
            5. 不确定的事情诚实说不知道，建议联系客服
            6. 如果不需要再做任何操作，调用 stop 工具停止
            """;

    private static final String NEXT_STEP_PROMPT = """
            请根据用户需求，从可用工具列表中选择最合适的工具。
            输出格式：{"toolName":"工具名","parameters":{...},"needContinueTool":true/false}
            不需要工具时 toolName 填 "stop"。
            """;

    @Autowired
    @Qualifier("aiStreamTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private MysqlPersistService mysqlPersistService;

    // ================== 配置 1：Agent 名称 ==================

    private final String agentName = "小智";

    public String getName() { return agentName; }

    // ================== 配置 2：角色系统提示词 ==================

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    // ================== 配置 3：下一步指令提示词 ==================

    @Override
    protected String getNextStepPrompt() {
        return NEXT_STEP_PROMPT;
    }

    // ================== 配置 4：最大步数 ==================

    @Override
    protected int maxSteps() {
        return 5;  // 工具调用最多 5 轮
    }

    // ================== 基础设施 ==================

    @Override
    protected Executor getExecutor() {
        return executor;
    }

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