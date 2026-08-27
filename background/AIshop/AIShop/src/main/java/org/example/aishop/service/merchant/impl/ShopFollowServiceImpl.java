package org.example.aishop.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aishop.entity.merchant.ShopFollow;
import org.example.aishop.mapper.merchant.ShopFollowMapper;
import org.example.aishop.service.merchant.ShopFollowService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShopFollowServiceImpl implements ShopFollowService {

    @Autowired
    private ShopFollowMapper shopFollowMapper;

    @Override
    public void follow(Long shopId) {
        Long userId = UserHolder.getUserId();
        if (isFollowed(shopId)) return;
        ShopFollow sf = new ShopFollow();
        sf.setUserId(userId);
        sf.setShopId(shopId);
        shopFollowMapper.insert(sf);
    }

    @Override
    public void unfollow(Long shopId) {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<ShopFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopFollow::getUserId, userId)
               .eq(ShopFollow::getShopId, shopId);
        shopFollowMapper.delete(wrapper);
    }

    @Override
    public boolean isFollowed(Long shopId) {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<ShopFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopFollow::getUserId, userId)
               .eq(ShopFollow::getShopId, shopId);
        return shopFollowMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countFollowers(Long shopId) {
        LambdaQueryWrapper<ShopFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopFollow::getShopId, shopId);
        return shopFollowMapper.selectCount(wrapper);
    }
}