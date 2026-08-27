package org.example.aishop.service.coupon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.CouponDTO;
import org.example.aishop.dto.CouponRecordVO;
import org.example.aishop.entity.coupon.Coupon;
import org.example.aishop.entity.user.User;
import org.example.aishop.entity.coupon.UserCoupon;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.coupon.CouponMapper;
import org.example.aishop.mapper.coupon.UserCouponMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.mq.producer.CouponMqProducer;
import org.example.aishop.service.coupon.CouponService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CouponMqProducer couponMqProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("claimCouponScript")
    private RedisScript<Long> claimCouponScript;

    @Override
    public Page<CouponDTO> pageCoupons(Integer current, Integer size, String name, Integer status) {
        Page<Coupon> page = new Page<>(current, size);
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null && !name.isEmpty(), Coupon::getName, name);
        wrapper.eq(status != null, Coupon::getStatus, status);
        wrapper.orderByDesc(Coupon::getId);
        super.page(page, wrapper);

        List<Coupon> records = page.getRecords();
        Set<Long> couponIds = records.stream().map(Coupon::getId).collect(Collectors.toSet());
        Map<Long, Long> claimedCount = couponIds.isEmpty() ? new HashMap<>() :
                userCouponMapper.selectList(
                                new LambdaQueryWrapper<UserCoupon>().in(UserCoupon::getCouponId, couponIds))
                        .stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        List<CouponDTO> dtoList = records.stream().map(c -> {
            CouponDTO dto = new CouponDTO();
            BeanUtils.copyProperties(c, dto);
            long claimed = claimedCount.getOrDefault(c.getId(), 0L);
            dto.setRemain((int) (c.getStock() - claimed));
            return dto;
        }).collect(Collectors.toList());

        Page<CouponDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public CouponDTO getCouponById(Long id) {
        Coupon coupon = super.getById(id);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        CouponDTO dto = new CouponDTO();
        BeanUtils.copyProperties(coupon, dto);
        long claimed = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, id));
        dto.setRemain((int) (coupon.getStock() - claimed));
        return dto;
    }

    @Override
    public void addCoupon(Coupon coupon) {
        if (coupon.getType() == null) coupon.setType(1);
        if (coupon.getStatus() == null) coupon.setStatus(1);
        super.save(coupon);

        // 初始化 Redis 优惠券库存缓存
        try {
            String stockKey = RedisConstant.couponStockKey(coupon.getId());
            stringRedisTemplate.opsForValue().set(stockKey,
                    String.valueOf(coupon.getStock()));
        } catch (Exception ignored) {}

        // 投递延时消息：优惠券到期自动置为过期
        if (coupon.getEndTime() != null) {
            long delayMs = coupon.getEndTime().getTime() - System.currentTimeMillis();
            if (delayMs > 0) {
                couponMqProducer.sendDelayExpire(coupon.getId(), delayMs);
            }
        }
    }

    @Override
    public void updateCoupon(Coupon coupon) {
        Coupon exist = super.getById(coupon.getId());
        if (exist == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        if (coupon.getStock() != null && coupon.getStock() < exist.getStock()) {
            long claimed = userCouponMapper.selectCount(
                    new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, coupon.getId()));
            if (claimed > coupon.getStock()) {
                throw new BusinessException(400, "已有 " + claimed + " 名用户领取，总发放数量不能低于 " + claimed);
            }
        }
        super.updateById(coupon);
    }

    @Override
    public void deleteCoupon(Long id) {
        Coupon exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        long claimed = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, id));
        if (claimed > 0) {
            throw new BusinessException(400, "该优惠券已有 " + claimed + " 名用户领取，无法删除");
        }
        super.removeById(id);
    }

    @Override
    public void toggleStatus(Long id) {
        Coupon coupon = super.getById(id);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        coupon.setStatus(coupon.getStatus() == 1 ? 0 : 1);
        super.updateById(coupon);
    }

    @Override
    public Page<CouponRecordVO> pageRecords(Integer current, Integer size, Long couponId) {
        Page<UserCoupon> page = new Page<>(current, size);
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(couponId != null, UserCoupon::getCouponId, couponId);
        wrapper.orderByDesc(UserCoupon::getCreateTime);
        userCouponMapper.selectPage(page, wrapper);
        List<UserCoupon> records = page.getRecords();

        // 批量查询用户和券信息
        Set<Long> userIds = records.stream().map(UserCoupon::getUserId).collect(Collectors.toSet());
        Set<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());

        Map<Long, String> usernameMap = userIds.isEmpty() ? new HashMap<>() :
                userMapper.selectBatchIds(userIds)
                        .stream().collect(Collectors.toMap(User::getId, User::getUsername));
        Map<Long, String> couponNameMap = couponIds.isEmpty() ? new HashMap<>() :
                super.listByIds(couponIds)
                        .stream().collect(Collectors.toMap(Coupon::getId, Coupon::getName));

        List<CouponRecordVO> voList = records.stream().map(uc -> {
            CouponRecordVO vo = new CouponRecordVO();
            BeanUtils.copyProperties(uc, vo);
            vo.setUsername(usernameMap.getOrDefault(uc.getUserId(), "未知"));
            vo.setCouponName(couponNameMap.getOrDefault(uc.getCouponId(), "未知"));
            vo.setStatusText(uc.getStatus() == 0 ? "未使用" : uc.getStatus() == 1 ? "已使用" : "已过期");
            return vo;
        }).collect(Collectors.toList());

        Page<CouponRecordVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public void claimCoupon(Long userId, Long couponId) {
        // 校验优惠券是否存在且有效
        Coupon coupon = super.getById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new BusinessException(400, "优惠券已下架");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().before(new Date())) {
            throw new BusinessException(400, "优惠券已过期");
        }

        String stockKey = RedisConstant.couponStockKey(couponId);
        String userSetKey = RedisConstant.couponUserSetKey(couponId);

        // 如果 Redis 库存 key 不存在，从数据库初始化（兼容 SQL 直接导入的数据）
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            long claimed = userCouponMapper.selectCount(
                    new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, couponId));
            int remain = (int) (coupon.getStock() - claimed);
            if (remain <= 0) {
                throw new BusinessException(400, "优惠券已被领完");
            }
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(remain));
        }

        // Lua 原子操作：检查库存 + 防重领 + 扣减
        Long result = stringRedisTemplate.execute(claimCouponScript,
                Arrays.asList(stockKey, userSetKey),
                String.valueOf(userId));

        if (result == null || result == -1) {
            throw new BusinessException(400, "优惠券已被领完");
        }
        if (result == -2) {
            throw new BusinessException(400, "您已领取过该优惠券");
        }

        // Redis 扣减成功后，写 MySQL
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setCreateTime(new Date());
        userCouponMapper.insert(uc);

        // 异步推送领券通知
        try {
            couponMqProducer.sendClaimNotify(userId);
        } catch (Exception ignored) {}
    }

    @Override
    public Page<CouponDTO> pageAvailable(Integer current, Integer size, Long userId) {
        // 查询状态为1（启用）且未过期的优惠券
        Page<Coupon> page = new Page<>(current, size);
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1);
        wrapper.gt(Coupon::getEndTime, new Date());
        wrapper.orderByDesc(Coupon::getId);
        super.page(page, wrapper);

        List<Coupon> records = page.getRecords();
        Set<Long> couponIds = records.stream().map(Coupon::getId).collect(Collectors.toSet());

        // 查询已领取数量
        Map<Long, Long> claimedCount = couponIds.isEmpty() ? new HashMap<>() :
                userCouponMapper.selectList(
                                new LambdaQueryWrapper<UserCoupon>().in(UserCoupon::getCouponId, couponIds))
                        .stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        // 查询当前用户已领取的优惠券ID集合
        final Set<Long> userClaimedIds;
        if (userId != null && !couponIds.isEmpty()) {
            userClaimedIds = userCouponMapper.selectList(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .in(UserCoupon::getCouponId, couponIds))
                    .stream().map(UserCoupon::getCouponId)
                    .collect(Collectors.toSet());
        } else {
            userClaimedIds = Collections.emptySet();
        }

        List<CouponDTO> dtoList = records.stream().map(c -> {
            CouponDTO dto = new CouponDTO();
            BeanUtils.copyProperties(c, dto);
            long claimed = claimedCount.getOrDefault(c.getId(), 0L);
            dto.setRemain((int) (c.getStock() - claimed));
            dto.setClaimed(userClaimedIds.contains(c.getId()));
            return dto;
        }).collect(Collectors.toList());

        Page<CouponDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public Page<CouponRecordVO> pageMyCoupons(Long userId, Integer current, Integer size, Integer status) {
        Page<UserCoupon> page = new Page<>(current, size);
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreateTime);
        userCouponMapper.selectPage(page, wrapper);
        List<UserCoupon> records = page.getRecords();

        Set<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = couponIds.isEmpty() ? new HashMap<>() :
                super.listByIds(couponIds)
                        .stream().collect(Collectors.toMap(Coupon::getId, c -> c));

        List<CouponRecordVO> voList = records.stream().map(uc -> {
            CouponRecordVO vo = new CouponRecordVO();
            BeanUtils.copyProperties(uc, vo);
            Coupon coupon = couponMap.get(uc.getCouponId());
            if (coupon != null) {
                vo.setCouponName(coupon.getName());
                vo.setType(coupon.getType());
                vo.setMinPrice(coupon.getMinPrice());
                vo.setDiscount(coupon.getDiscount());
                vo.setStartTime(coupon.getStartTime());
                vo.setEndTime(coupon.getEndTime());
            } else {
                vo.setCouponName("未知");
            }
            vo.setStatusText(uc.getStatus() == 0 ? "未使用" : uc.getStatus() == 1 ? "已使用" : "已过期");
            return vo;
        }).collect(Collectors.toList());

        Page<CouponRecordVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public List<CouponDTO> listMyAvailable(Long userId) {
        // 查询用户未使用且未过期的优惠券
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0));

        if (userCoupons.isEmpty()) return Collections.emptyList();

        Set<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        List<Coupon> coupons = super.listByIds(couponIds);

        Date now = new Date();
        return coupons.stream()
                .filter(c -> c.getStatus() == 1 && c.getEndTime() != null && c.getEndTime().after(now))
                .map(c -> {
                    CouponDTO dto = new CouponDTO();
                    BeanUtils.copyProperties(c, dto);
                    return dto;
                }).collect(Collectors.toList());
    }
}