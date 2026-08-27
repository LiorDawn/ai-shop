package org.example.aishop.ai.agent.springai;

import org.example.aishop.dto.ai.ChatRequestV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 第2层：范式层｜ReAct Think-Act 执行范式
 *
 * <h3>职责</h3>
 * 把通用的单步 step()，拆成 ReAct 标准范式：Think（思考）→ Act（行动）。
 *
 * <h3>核心设计</h3>
 * - 实现父类 step()，拆为固定顺序：先 think，后 act
 * - think() 和 act() 不提供实现，向下层移交
 * - act() 始终被调用，因为 think() 返回 false 时仍需 act() 做流式输出
 * - 上一轮已设置 shouldTerminate=true 时，跳过 think() 直接调 act()，避免冗余 LLM 调用
 *
 * <h3>抽象方法（第3层实现）</h3>
 * <pre>
 * think()：思考推理阶段，决定下一步要做什么
 * act()：执行行动阶段，执行思考得出的决策
 * shouldTerminate()：上一轮是否已决定终止
 * </pre>
 */
public abstract class ReActAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    // ================== 实现 step()：Think → Act ==================

    @Override
    protected final void step(Long userId, Long sessionId, ChatRequestV2 req,
                               SseEmitter emitter, boolean[] disconnected) {
        // 断连检测
        if (disconnected[0]) {
            agentState = AgentState.FINISHED;
            return;
        }

        // ★ fix: 上一轮已决定终止 → 跳过 think()，直接调 act() 做流式输出
        // 避免 needContinue=false 后，再花一次 LLM 调用去问 "stop"
        if (shouldTerminate()) {
            act(userId, sessionId, req, emitter, disconnected);
            return;
        }

        // Step 1: Think — 思考推理
        think(userId, sessionId, req, emitter, disconnected);

        // 断连再检测
        if (disconnected[0]) {
            agentState = AgentState.FINISHED;
            return;
        }

        // Step 2: Act — 执行行动
        // ★ fix: 去掉 if (shouldAct) 守卫，act() 始终被调用
        // think() 返回 false（stop/none）时，act() 内部判断 shouldTerminate 做流式输出
        act(userId, sessionId, req, emitter, disconnected);
    }

    // ================== 抽象方法（第3层实现） ==================

    /**
     * 思考推理阶段
     * @return true=需要执行行动，false=本轮终结
     */
    protected abstract boolean think(Long userId, Long sessionId, ChatRequestV2 req,
                                     SseEmitter emitter, boolean[] disconnected);

    /**
     * 执行行动阶段
     */
    protected abstract void act(Long userId, Long sessionId, ChatRequestV2 req,
                                 SseEmitter emitter, boolean[] disconnected);

    /**
     * 上一轮是否已决定终止
     * 第3层通过 shouldTerminate 字段实现
     */
    protected abstract boolean shouldTerminate();
}