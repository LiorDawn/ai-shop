package org.example.aishop.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.service.ai.AIContentService;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.product.Comment;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.dto.CommentDTO;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.CommentMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mq.message.CommentMQMessage;
import org.example.aishop.mq.producer.CommentMqProducer;
import org.example.aishop.service.product.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentMqProducer commentMqProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AIContentService aiContentService;

    @Override
    public Page<CommentDTO> pageList(Integer current, Integer size,
                                     String productName, Integer score, Integer status,
                                     Date startTime, Date endTime) {
        Set<Long> productIds = null;
        if (productName != null && !productName.isEmpty()) {
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().like(Product::getName, productName));
            if (products.isEmpty()) {
                return new Page<>(current, size, 0);
            }
            productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        }

        Page<CommentDTO> page = new Page<>(current, size);
        return commentMapper.selectCommentPage(page, productIds, score, status, startTime, endTime, null, null, null);
    }

    @Override
    public CommentDTO getDetail(Long id) {
        CommentDTO dto = commentMapper.selectCommentDetail(id);
        if (dto == null) {
            throw new BusinessException(404, "评价不存在");
        }
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            dto.setImageList(Arrays.asList(dto.getImages().split(",")));
        }
        return dto;
    }

    @Override
    public void reply(Long id, String reply) {
        Comment comment = super.getById(id);
        if (comment == null || comment.getDelFlag() == 1) {
            throw new BusinessException(404, "评价不存在");
        }
        if (reply == null || reply.isEmpty()) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        comment.setReply(reply);
        comment.setReplyTime(new Date());
        super.updateById(comment);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        Comment comment = super.getById(id);
        if (comment == null || comment.getDelFlag() == 1) {
            throw new BusinessException(404, "评价不存在");
        }
        if (status != 1 && status != 2) {
            throw new BusinessException(400, "状态值不合法");
        }
        comment.setStatus(status);
        super.updateById(comment);
    }

    @Override
    public void deleteComment(Long id) {
        Comment comment = super.getById(id);
        if (comment == null || comment.getDelFlag() == 1) {
            throw new BusinessException(404, "评价不存在");
        }
        comment.setDelFlag(1);
        super.updateById(comment);
    }

    @Override
    public Page<CommentDTO> pageByProduct(Integer current, Integer size, Long productId) {
        String cacheKey = RedisConstant.commentPageKey(productId, current);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception ignored) {}

        Page<CommentDTO> page = new Page<>(current, size);
        Set<Long> productIds = new HashSet<>();
        productIds.add(productId);
        Page<CommentDTO> result = commentMapper.selectCommentPage(page, productIds, null, 1, null, null, null, null, null);

        try {
            stringRedisTemplate.opsForValue().set(cacheKey, "1",
                    RedisConstant.COMMENT_PAGE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        return result;
    }

    @Override
    public void addComment(Long orderId, Long productId, Long shopId, Long userId,
                           Integer score, String content, String images) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getOrderStatus() != 3) {
            throw new BusinessException(400, "订单未完成，无法评价");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(400, "无权评价该订单");
        }

        Long count = super.count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getOrderId, orderId)
                .eq(Comment::getProductId, productId)
                .eq(Comment::getUserId, userId)
                .eq(Comment::getDelFlag, 0));
        if (count > 0) {
            throw new BusinessException(400, "您已评价过该商品");
        }

        Comment comment = new Comment();
        comment.setOrderId(orderId);
        comment.setProductId(productId);
        comment.setShopId(shopId);
        comment.setUserId(userId);
        comment.setScore(score);
        comment.setContent(content);
        comment.setImages(images);
        comment.setStatus(1);
        comment.setDelFlag(0);

        // AI 智能审核（检查开关）
        if (content != null && !content.trim().isEmpty()) {
            String reviewEnabled = stringRedisTemplate.opsForValue().get("AISHOP:CONFIG:AI_REVIEW_ENABLED");
            if (!"false".equals(reviewEnabled)) {
                String reviewResult = aiContentService.reviewContent(content, "评论");
                if (reviewResult != null) {
                    comment.setStatus(0); // 审核不通过，待人工复审
                    System.err.println("AI 审核拦截评论: commentId=" + comment.getId() + ", reason=" + reviewResult);
                }
            }
        }

        comment.setCreateTime(new Date());
        super.save(comment);

        try {
            CommentMQMessage mqMsg = new CommentMQMessage();
            mqMsg.setCommentId(comment.getId());
            mqMsg.setProductId(productId);
            mqMsg.setShopId(shopId);
            mqMsg.setUserId(userId);
            mqMsg.setScore(score);
            mqMsg.setType("CREATE");
            commentMqProducer.sendCommentProcess(mqMsg);
        } catch (Exception ignored) {}
    }

    @Override
    public Page<CommentDTO> pageMyComments(Integer current, Integer size, Long userId) {
        Page<CommentDTO> page = new Page<>(current, size);
        return commentMapper.selectCommentPage(page, null, null, null, null, null, null, userId, null);
    }

    @Override
    public Page<CommentDTO> pageByShop(Integer current, Integer size, Long shopId,
                                       Integer score, String productName,
                                       Integer hasReply) {
        Set<Long> productIds = null;
        if (productName != null && !productName.isEmpty()) {
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().like(Product::getName, productName));
            if (!products.isEmpty()) {
                productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            }
        }

        Page<CommentDTO> page = new Page<>(current, size);
        return commentMapper.selectCommentPage(page, productIds, score, null, null, null, shopId, null, hasReply);
    }
}