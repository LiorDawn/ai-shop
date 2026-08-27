package org.example.aishop.service.order;

import org.example.aishop.dto.CartItemVO;
import org.example.aishop.dto.CartSettleVO;

import java.util.List;

public interface CartService {
    /** 加入购物车 */
    void add(Long productId, Long skuId, Integer num);

    /** 查询当前用户购物车列表 */
    List<CartItemVO> listCart();

    /** 修改商品数量 */
    void updateNum(Long cartId, Integer num);

    /** 选中/取消选中单个商品 */
    void toggleCheck(Long cartId, Integer checked);

    /** 全选/全不选 */
    void checkAll(Integer checked);

    /** 删除单个购物车项 */
    void delete(Long cartId);

    /** 批量删除 */
    void deleteBatch(List<Long> cartIds);

    /** 结算前置校验 */
    CartSettleVO settleCheck();
}