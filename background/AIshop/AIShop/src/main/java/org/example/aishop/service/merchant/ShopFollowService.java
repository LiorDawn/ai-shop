package org.example.aishop.service.merchant;

public interface ShopFollowService {

    /** 关注店铺 */
    void follow(Long shopId);

    /** 取消关注 */
    void unfollow(Long shopId);

    /** 是否已关注 */
    boolean isFollowed(Long shopId);

    /** 获取店铺关注数 */
    long countFollowers(Long shopId);
}