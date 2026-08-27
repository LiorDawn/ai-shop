package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.*;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.entity.merchant.ShopFollow;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Collect;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.product.ProductImage;
import org.example.aishop.entity.user.Address;
import org.example.aishop.entity.user.User;
import org.example.aishop.mapper.merchant.ShopFollowMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.CollectMapper;
import org.example.aishop.mapper.product.ProductImageMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.service.product.CollectService;
import org.example.aishop.service.user.AddressService;
import org.example.aishop.service.user.UserService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "用户个人中心", description = "个人信息、收货地址、收藏管理、统计数据")
@RestController
@RequestMapping("/profile")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CollectService collectService;

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ShopFollowMapper shopFollowMapper;

    @Autowired
    private OrderMapper orderMapper;

    // ===== 用户统计 =====

    /** 获取用户统计数据 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Long userId = UserHolder.getUserId();

        // 订单数
        LambdaQueryWrapper<Orders> ow = new LambdaQueryWrapper<>();
        ow.eq(Orders::getUserId, userId);
        long orderCount = orderMapper.selectCount(ow);

        // 待付款订单数
        ow.clear();
        ow.eq(Orders::getUserId, userId).eq(Orders::getOrderStatus, 0);
        long pendingPayCount = orderMapper.selectCount(ow);

        // 待发货
        ow.clear();
        ow.eq(Orders::getUserId, userId).eq(Orders::getOrderStatus, 1);
        long pendingShipCount = orderMapper.selectCount(ow);

        // 待收货
        ow.clear();
        ow.eq(Orders::getUserId, userId).eq(Orders::getOrderStatus, 2);
        long pendingReceiveCount = orderMapper.selectCount(ow);

        // 待评价
        ow.clear();
        ow.eq(Orders::getUserId, userId).eq(Orders::getOrderStatus, 3);
        long pendingReviewCount = orderMapper.selectCount(ow);

        // 收藏数
        LambdaQueryWrapper<Collect> cw = new LambdaQueryWrapper<>();
        cw.eq(Collect::getUserId, userId);
        long collectCount = collectMapper.selectCount(cw);

        // 关注店铺数
        LambdaQueryWrapper<ShopFollow> sfw = new LambdaQueryWrapper<>();
        sfw.eq(ShopFollow::getUserId, userId);
        long followShopCount = shopFollowMapper.selectCount(sfw);

        Map<String, Object> stats = new HashMap<>();
        stats.put("orderCount", orderCount);
        stats.put("pendingPayCount", pendingPayCount);
        stats.put("pendingShipCount", pendingShipCount);
        stats.put("pendingReceiveCount", pendingReceiveCount);
        stats.put("pendingReviewCount", pendingReviewCount);
        stats.put("collectCount", collectCount);
        stats.put("followShopCount", followShopCount);
        return Result.success("查询成功", stats);
    }

    // ===== 用户信息 =====

    @Operation(summary = "获取用户信息")
    @GetMapping
    public Result<UserDTO> getProfile() {
        Long userId = UserHolder.getUserId();
        UserDTO dto = userService.getUserById(userId);
        return Result.success("查询成功", dto);
    }

    /** 更新个人资料 */
    @PutMapping
    public Result<Void> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        Long userId = UserHolder.getUserId();
        User user = new User();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setGender(dto.getGender());
        user.setSignature(dto.getSignature());
        user.setAvatar(dto.getAvatar());
        userService.updateById(user);

        // 更新 ThreadLocal 中的用户信息
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser != null) {
            if (dto.getNickname() != null) currentUser.setNickname(dto.getNickname());
            if (dto.getGender() != null) currentUser.setGender(dto.getGender());
            if (dto.getSignature() != null) currentUser.setSignature(dto.getSignature());
            if (dto.getAvatar() != null) currentUser.setAvatar(dto.getAvatar());
            UserHolder.saveUser(currentUser);
        }

        return Result.success("保存成功");
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordChangeDTO dto) {
        if (!StringUtils.hasText(dto.getOldPassword())) {
            throw new BusinessException(400, "原密码不能为空");
        }
        if (!StringUtils.hasText(dto.getNewPassword())) {
            throw new BusinessException(400, "新密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmPassword())) {
            throw new BusinessException(400, "确认密码不能为空");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(400, "两次密码不一致");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new BusinessException(400, "密码长度不少于6位");
        }

        Long userId = UserHolder.getUserId();
        User user = userService.getById(userId);
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userService.updateById(update);

        return Result.success("密码修改成功");
    }

    // ===== 收货地址 =====

    @Operation(summary = "地址列表")
    @GetMapping("/addresses")
    public Result<List<Address>> listAddresses() {
        List<Address> list = addressService.listCurrentUserAddresses();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "新增地址")
    @PostMapping("/addresses")
    public Result<Void> addAddress(@RequestBody Address address) {
        addressService.addAddress(address);
        return Result.success("新增成功");
    }

    /** 修改地址 */
    @PutMapping("/addresses")
    public Result<Void> updateAddress(@RequestBody Address address) {
        addressService.updateAddress(address);
        return Result.success("修改成功");
    }

    /** 删除地址 */
    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return Result.success("删除成功");
    }

    /** 获取默认地址 */
    @GetMapping("/addresses/default")
    public Result<Address> getDefaultAddress() {
        Address addr = addressService.getDefaultAddress();
        return Result.success("查询成功", addr);
    }

    // ===== 我的收藏 =====

    @Operation(summary = "收藏商品列表", description = "分页查询收藏的商品")
    @GetMapping("/collects")
    public Result<Page<ProductDTO>> listCollects(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserHolder.getUserId();

        // 获取用户收藏的商品ID列表
        LambdaQueryWrapper<Collect> cw = new LambdaQueryWrapper<>();
        cw.eq(Collect::getUserId, userId).orderByDesc(Collect::getCreateTime);
        Page<Collect> collectPage = new Page<>(current, size);
        collectMapper.selectPage(collectPage, cw);
        List<Collect> collectList = collectPage.getRecords();

        if (collectList.isEmpty()) {
            Page<ProductDTO> emptyPage = new Page<>(current, size);
            emptyPage.setTotal(collectPage.getTotal());
            return Result.success("查询成功", emptyPage);
        }

        List<Long> productIds = collectList.stream().map(Collect::getProductId).collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);

        // 按收藏顺序排序
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<ProductDTO> dtoList = new ArrayList<>();
        for (Collect c : collectList) {
            Product p = productMap.get(c.getProductId());
            if (p != null) {
                dtoList.add(toSimpleProductDTO(p));
            }
        }

        Page<ProductDTO> resultPage = new Page<>(current, size);
        resultPage.setTotal(collectPage.getTotal());
        resultPage.setRecords(dtoList);
        return Result.success("查询成功", resultPage);
    }

    @Operation(summary = "收藏商品ID列表")
    @GetMapping("/collects/ids")
    public Result<List<Long>> collectIds() {
        List<Long> ids = collectService.listCollectedProductIds();
        return Result.success("查询成功", ids);
    }

    private ProductDTO toSimpleProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);
        if (product.getCreateTime() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dto.setCreateTime(sdf.format(product.getCreateTime()));
        }
        // 查询第一张轮播图作为展示图
        LambdaQueryWrapper<ProductImage> iw = new LambdaQueryWrapper<>();
        iw.eq(ProductImage::getProductId, product.getId());
        iw.orderByAsc(ProductImage::getSort).last("LIMIT 1");
        ProductImage img = productImageMapper.selectOne(iw, false);
        if (img != null) {
            dto.setImage(img.getImageUrl());
        }
        return dto;
    }
}