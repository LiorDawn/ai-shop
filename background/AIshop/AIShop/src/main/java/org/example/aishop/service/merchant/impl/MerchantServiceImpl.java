package org.example.aishop.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.MerchantDTO;
import org.example.aishop.dto.ShopDTO;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.user.Role;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.entity.user.User;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.user.RoleMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.mq.message.MerchantAuditMQMessage;
import org.example.aishop.service.merchant.MerchantService;
import org.example.aishop.mq.producer.MerchantMqProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MerchantMqProducer merchantMqProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void addMerchant(Merchant merchant) {
        if (!StringUtils.hasText(merchant.getMerchantName())) {
            throw new BusinessException(400, "商家名称不能为空");
        }
        if (merchant.getUserId() == null) {
            throw new BusinessException(400, "关联用户不能为空");
        }

        // 检查该用户是否已有入驻申请
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, merchant.getUserId());
        Merchant exist = super.getOne(wrapper, false);
        if (exist != null) {
            // 如果已被驳回，允许重新提交（更新资料）
            if (exist.getAuditStatus() != null && exist.getAuditStatus() == 2) {
                merchant.setId(exist.getId());
                merchant.setAuditStatus(0); // 重回待审核
                merchant.setStatus(0);
                super.updateById(merchant);
                return;
            }
            // 审核中或已通过，不允许重复提交
            throw new BusinessException(400, "您已提交过入驻申请，请勿重复提交");
        }

        // 检查用户角色，已是商家则不允许
        User user = userMapper.selectById(merchant.getUserId());
        if (user != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null && "MERCHANT".equals(role.getCode())) {
                throw new BusinessException(400, "您已是入驻商家，无需重复申请");
            }
        }

        merchant.setStatus(0);
        merchant.setAuditStatus(0); // 待审核
        super.save(merchant);
    }

    @Override
    public void updateMerchant(Merchant merchant) {
        if (merchant.getId() == null) {
            throw new BusinessException(400, "商家ID不能为空");
        }
        Merchant exist = super.getById(merchant.getId());
        if (exist == null) {
            throw new BusinessException(404, "商家不存在");
        }
        super.updateById(merchant);
    }

    @Override
    public void deleteMerchant(Long id) {
        Merchant exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "商家不存在");
        }
        // 同时删除关联的店铺
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getMerchantId, id);
        shopMapper.delete(shopWrapper);
        // 恢复用户角色为普通用户
        if (exist.getUserId() != null) {
            User user = userMapper.selectById(exist.getUserId());
            if (user != null) {
                Role customerRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getCode, "CUSTOMER"));
                if (customerRole != null) {
                    user.setRoleId(customerRole.getId());
                    userMapper.updateById(user);
                }
            }
        }
        // 刷新 Redis 缓存：将用户角色降为 CUSTOMER
        refreshUserCache(exist.getUserId(), null, "CUSTOMER");
        super.removeById(id);
    }

    @Override
    public void deleteBatchMerchants(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的商家");
        }
        for (Long id : ids) {
            deleteMerchant(id);
        }
    }

    @Override
    @Transactional
    public void auditMerchant(Long id, Integer auditStatus, String auditRemark) {
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new BusinessException(400, "审核状态不合法，1=通过 2=驳回");
        }
        if (auditRemark == null || auditRemark.trim().isEmpty()) {
            throw new BusinessException(400, "审核备注不能为空");
        }
        Merchant exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "商家不存在");
        }
        if (exist.getAuditStatus() != null && exist.getAuditStatus() == 1) {
            throw new BusinessException(400, "该申请已审核通过，请勿重复操作");
        }

        Merchant update = new Merchant();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(auditRemark);
        update.setAuditTime(new Date());

        if (auditStatus == 1) {
            // 审核通过：更新状态、创建店铺、更新角色、刷新缓存
            update.setStatus(1);

            // 1. 同步创建店铺
            Shop shop = new Shop();
            shop.setMerchantId(id);
            shop.setShopName(exist.getMerchantName());
            shop.setIntro(exist.getMerchantName() + "，欢迎光临！");
            shop.setStatus(1);
            shopMapper.insert(shop);
            Long shopId = shop.getId();

            // 2. 同步更新用户角色为 MERCHANT
            if (exist.getUserId() != null) {
                User user = userMapper.selectById(exist.getUserId());
                if (user != null) {
                    Role merchantRole = roleMapper.selectOne(
                            new LambdaQueryWrapper<Role>().eq(Role::getCode, "MERCHANT"));
                    if (merchantRole != null) {
                        user.setRoleId(merchantRole.getId());
                        userMapper.updateById(user);
                    }
                }
            }

            // 3. 同步刷新 Redis 缓存，用户立即获得 MERCHANT 角色 + shopId，无需等待
            refreshUserCache(exist.getUserId(), shopId, "MERCHANT");

            // 4. 发送 MQ 异步处理站内信通知等非关键任务
            MerchantAuditMQMessage mqMsg = new MerchantAuditMQMessage();
            mqMsg.setMerchantId(id);
            mqMsg.setUserId(exist.getUserId());
            mqMsg.setMerchantName(exist.getMerchantName());
            mqMsg.setAuditStatus(1);
            mqMsg.setAuditRemark(auditRemark);
            try {
                merchantMqProducer.sendAuditAfter(mqMsg);
            } catch (Exception ignored) {}
        } else {
            // 审核驳回
            update.setStatus(0);
        }

        super.updateById(update);
    }

    /**
     * 刷新用户在 Redis 中的缓存信息（角色和店铺ID立即生效）
     */
    private void refreshUserCache(Long userId, Long shopId, String roleCode) {
        if (userId == null) return;
        try {
            String key = RedisConstant.userInfoKey(userId);
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> userMap = objectMapper.readValue(json, java.util.Map.class);
                userMap.put("roleCode", roleCode);
                userMap.put("shopId", shopId);
                Role role = roleMapper.selectOne(
                        new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
                if (role != null) {
                    userMap.put("roleName", role.getName());
                }
                stringRedisTemplate.opsForValue().set(key,
                        objectMapper.writeValueAsString(userMap),
                        RedisConstant.USER_TOKEN_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            } else {
                // 缓存不存在时重新构建（防止因缓存过期导致角色丢失）
                User user = userMapper.selectById(userId);
                if (user != null) {
                    org.example.aishop.dto.UserDTO dto = new org.example.aishop.dto.UserDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setPhone(user.getPhone());
                    dto.setEmail(user.getEmail());
                    dto.setStatus(user.getStatus());
                    dto.setRoleId(user.getRoleId());
                    dto.setRoleCode(roleCode);
                    dto.setShopId(shopId);
                    Role role = roleMapper.selectOne(
                            new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
                    if (role != null) {
                        dto.setRoleName(role.getName());
                    }
                    stringRedisTemplate.opsForValue().set(key,
                            objectMapper.writeValueAsString(dto),
                            RedisConstant.USER_TOKEN_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            // 缓存刷新失败不影响主流程
        }
    }

    // ========== 用户端：查询我的入驻申请状态 ==========
    public MerchantDTO getMyApplication(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId);
        wrapper.orderByDesc(Merchant::getCreateTime);
        wrapper.last("LIMIT 1");
        Merchant merchant = super.getOne(wrapper, false);
        if (merchant == null) {
            return null;
        }
        return toMerchantDTO(merchant);
    }

    @Override
    public MerchantDTO getMerchantById(Long id) {
        Merchant merchant = super.getById(id);
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        return toMerchantDTO(merchant);
    }

    @Override
    public List<MerchantDTO> listMerchants() {
        List<Merchant> list = super.list();
        return list.stream().map(this::toMerchantDTO).collect(Collectors.toList());
    }

    @Override
    public Page<MerchantDTO> pageMerchants(Integer current, Integer size, String merchantName, Integer status) {
        Page<Merchant> page = new Page<>(current, size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(merchantName), Merchant::getMerchantName, merchantName);
        if (status != null) {
            wrapper.eq(Merchant::getAuditStatus, status);
        }
        wrapper.orderByDesc(Merchant::getCreateTime);
        super.page(page, wrapper);
        return (Page<MerchantDTO>) page.convert(this::toMerchantDTO);
    }

    @Override
    public MerchantDTO toMerchantDTO(Merchant merchant) {
        MerchantDTO dto = new MerchantDTO();
        BeanUtils.copyProperties(merchant, dto);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (merchant.getCreateTime() != null) {
            dto.setCreateTime(sdf.format(merchant.getCreateTime()));
        }
        if (merchant.getAuditTime() != null) {
            dto.setAuditTime(sdf.format(merchant.getAuditTime()));
        }

        // 查询用户名
        if (merchant.getUserId() != null) {
            User user = userMapper.selectById(merchant.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
            }
        }

        // 查询关联店铺
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getMerchantId, merchant.getId());
        Shop shop = shopMapper.selectOne(shopWrapper, false);
        if (shop != null) {
            ShopDTO shopDTO = new ShopDTO();
            BeanUtils.copyProperties(shop, shopDTO);
            shopDTO.setMerchantName(merchant.getMerchantName());
            dto.setShop(shopDTO);
        }

        return dto;
    }
}