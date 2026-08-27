package org.example.aishop.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.product.Collect;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.product.CollectMapper;
import org.example.aishop.service.product.CollectService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect> implements CollectService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addCollect(Long productId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        // 先查数据库防重复
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getProductId, productId);
        if (super.count(wrapper) > 0) {
            throw new BusinessException(400, "已收藏该商品");
        }

        // 写入数据库
        Collect collect = new Collect();
        collect.setUserId(userId);
        collect.setProductId(productId);
        super.save(collect);

        // 同步到 Redis Set
        String key = RedisConstant.collectUserKey(userId);
        stringRedisTemplate.opsForSet().add(key, String.valueOf(productId));
        stringRedisTemplate.expire(key, RedisConstant.COLLECT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void removeCollect(Long productId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        // 删除数据库记录
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getProductId, productId);
        super.remove(wrapper);

        // 从 Redis Set 中移除
        stringRedisTemplate.opsForSet().remove(RedisConstant.collectUserKey(userId), String.valueOf(productId));
    }

    @Override
    public boolean isCollected(Long productId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) return false;

        // 先查 Redis Set
        String key = RedisConstant.collectUserKey(userId);
        Boolean exists = stringRedisTemplate.opsForSet().isMember(key, String.valueOf(productId));
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        // 缓存未命中，查数据库
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId).eq(Collect::getProductId, productId);
        boolean dbExists = super.count(wrapper) > 0;

        // 回填 Redis Set（懒加载）
        if (dbExists) {
            stringRedisTemplate.opsForSet().add(key, String.valueOf(productId));
            stringRedisTemplate.expire(key, RedisConstant.COLLECT_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return dbExists;
    }

    @Override
    public List<Long> listCollectedProductIds() {
        Long userId = UserHolder.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        // 先查 Redis Set
        String key = RedisConstant.collectUserKey(userId);
        java.util.Set<String> cachedIds = stringRedisTemplate.opsForSet().members(key);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return cachedIds.stream().map(Long::parseLong).collect(Collectors.toList());
        }

        // 缓存未命中，查数据库
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId);
        List<Long> ids = super.list(wrapper).stream()
                .map(Collect::getProductId)
                .collect(Collectors.toList());

        // 回填 Redis Set
        if (!ids.isEmpty()) {
            String[] strIds = ids.stream().map(String::valueOf).toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(key, strIds);
            stringRedisTemplate.expire(key, RedisConstant.COLLECT_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return ids;
    }

    @Override
    public int getCollectCount(Long productId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getProductId, productId);
        return (int) super.count(wrapper);
    }
}