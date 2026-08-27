package org.example.aishop.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.mq.message.MerchantAuditMQMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 商家入驻审核后处理 MQ 消费者
 * 店铺创建和角色分配已同步完成，MQ 仅用于异步非关键任务
 */
@Component
public class MerchantAuditMQConsumer {

    @Autowired
    private ShopMapper shopMapper;

    @RabbitListener(queues = MQConstant.MERCHANT_AUDIT_QUEUE)
    public void onMerchantAudit(MerchantAuditMQMessage msg) {
        if (msg == null) return;

        try {
            if (msg.getAuditStatus() == 1) {
                handleApproved(msg);
            } else {
                handleRejected(msg);
            }
        } catch (Exception e) {
            // 异常不影响审核结果（审核状态已写入数据库）
        }
    }

    private void handleApproved(MerchantAuditMQMessage msg) {
        // 幂等处理：店铺可能已被 auditMerchant 同步创建
        Shop existingShop = shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, msg.getMerchantId()));
        if (existingShop == null) {
            Shop shop = new Shop();
            shop.setMerchantId(msg.getMerchantId());
            shop.setShopName(msg.getMerchantName());
            shop.setIntro(msg.getMerchantName() + "，欢迎光临！");
            shop.setStatus(1);
            shopMapper.insert(shop);
        }

        // TODO: 站内信推送、商家通知推送
    }

    private void handleRejected(MerchantAuditMQMessage msg) {
        // TODO: 站内信通知驳回原因
    }
}
