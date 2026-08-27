package org.example.aishop.mq.consumer;

import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.entity.order.AfterSale;
import org.example.aishop.mapper.order.AfterSaleMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mq.message.AfterSaleMQMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 售后 MQ 消费者
 * 异步处理：通知推送、数据更新、退货超时自动关闭
 */
@Component
public class AfterSaleMQConsumer {

    @Autowired
    private AfterSaleMapper afterSaleMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;

    /**
     * 监听售后通知队列（普通异步通知 + 数据更新）
     */
    @RabbitListener(queues = MQConstant.AFTERSALE_NOTIFY_QUEUE)
    public void onAfterSaleNotify(AfterSaleMQMessage msg) {
        if (msg == null || msg.getMsgType() == null) return;

        try {
            switch (msg.getMsgType()) {
                case "NOTIFY":
                    handleNotify(msg);
                    break;
                case "DATA_UPDATE":
                    handleDataUpdate(msg);
                    break;
            }
        } catch (Exception e) {
            // 通知失败不影响主流程
        }
    }

    /**
     * 监听售后超时处理队列（延时死信消息）
     */
    @RabbitListener(queues = MQConstant.AFTERSALE_TIMEOUT_QUEUE)
    public void onAfterSaleTimeout(AfterSaleMQMessage msg) {
        if (msg == null || !"DELAY_CLOSE".equals(msg.getMsgType())) return;

        try {
            AfterSale as = afterSaleMapper.selectById(msg.getAfterSaleId());
            if (as == null || as.getDelFlag() == 1) return;

            // 仅状态为"待退货(已通过退货退款审核)"且未填物流的单子才自动关闭
            if (as.getAuditStatus() != null && as.getAuditStatus() == 1
                    && as.getType() != null && as.getType() == 1
                    && (as.getExpressNo() == null || as.getExpressNo().isEmpty())) {
                // 超时自动关闭售后单
                as.setAuditStatus(4); // 已完成(关闭)
                as.setAuditRemark("超时未填写物流，系统自动关闭");
                as.setFinishTime(new Date());
                afterSaleMapper.updateById(as);

                // TODO: 恢复商品库存
            }
        } catch (Exception e) {
            // 超时处理失败不影响主流程
        }
    }

    // ========== 内部处理方法 ==========

    private void handleNotify(AfterSaleMQMessage msg) {
        // TODO: 站内信/推送通知商家有新的售后申请
        // msg.getNotifyContent() 包含通知内容
        // msg.getShopId() 商家店铺ID
    }

    private void handleDataUpdate(AfterSaleMQMessage msg) {
        // TODO: 售后完成后的异步数据更新
        // 1. 更新商品售后统计数据
        // 2. 推送完结通知给用户
    }
}