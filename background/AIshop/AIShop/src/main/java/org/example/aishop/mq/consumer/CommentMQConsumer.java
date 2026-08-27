package org.example.aishop.mq.consumer;

import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.product.Comment;
import org.example.aishop.mapper.product.CommentMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mq.message.CommentMQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 评论 MQ 消费者
 * 异步更新商品评分、清除缓存，推送评论提醒
 */
@Component
public class CommentMQConsumer {
    private static final Logger log = LoggerFactory.getLogger(CommentMQConsumer.class);

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消费评论创建消息，异步更新评分、清除缓存
     */
    @RabbitListener(queues = MQConstant.COMMENT_QUEUE)
    public void onCommentCreate(CommentMQMessage msg) {
        if (msg == null || msg.getCommentId() == null) return;
        try {
            // 更新商品评分
            List<Comment> commentList = commentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Comment>()
                            .eq(Comment::getProductId, msg.getProductId())
                            .eq(Comment::getDelFlag, 0));
            int totalScore = commentList.stream().mapToInt(Comment::getScore).sum();
            double avgScore = commentList.isEmpty() ? 0 : (double) totalScore / commentList.size();
            int count = commentList.size();

            // 缓存商品评分统计
            String ratingKey = RedisConstant.commentRatingKey(msg.getProductId());
            stringRedisTemplate.opsForValue().set(ratingKey,
                    String.format("{\"avgScore\":%.1f,\"count\":%d}", avgScore, count),
                    RedisConstant.COMMENT_RATING_TTL_SECONDS, TimeUnit.SECONDS);

            // 清除评论分页缓存
            stringRedisTemplate.delete(stringRedisTemplate.keys(
                    RedisConstant.COMMENT_PAGE_PREFIX + msg.getProductId() + ":*"));

            log.info("评论 " + msg.getCommentId() + " 异步处理完成，商品 " + msg.getProductId() + " 评分更新为 " + avgScore + "/" + count);
        } catch (Exception e) {
            log.error("评论异步处理失败 commentId=" + msg.getCommentId(), e);
        }
    }
}