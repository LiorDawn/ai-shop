package org.example.aishop.service.user.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.LoginVO;
import org.example.aishop.dto.RegisterDTO;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.user.Role;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.entity.user.User;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.user.RoleMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.service.user.AuthService;
import org.example.aishop.service.user.SmsService;
import org.example.aishop.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private SmsService smsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void sendSmsCode(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(400, "手机号不能为空");
        }
        smsService.sendCode(phone);
    }

    @Override
    public LoginVO loginByPassword(String phone, String password) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(400, "手机号不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(400, "密码不能为空");
        }

        // 根据手机号查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(400, "该手机号未注册");
        }

        // 校验密码（BCrypt）
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        // 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(400, "账号已被禁用");
        }

        return buildLoginVO(user);
    }

    @Override
    public LoginVO loginBySms(String phone, String code) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(400, "手机号不能为空");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "验证码不能为空");
        }

        // 校验验证码
        if (!smsService.validateCode(phone, code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 查找用户，不存在则自动注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            // 自动注册新用户
            user = new User();
            user.setPhone(phone);
            user.setUsername("用户" + phone.substring(phone.length() - 4));
            user.setRoleId(3L); // 普通用户角色
            user.setStatus(1);
            userMapper.insert(user);
        }

        // 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(400, "账号已被禁用");
        }

        return buildLoginVO(user);
    }

    @Override
    public LoginVO registerUser(RegisterDTO dto) {
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new BusinessException(400, "手机号不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (!dto.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        // 检查手机号是否已注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "该手机号已注册");
        }

        // 构建用户
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setUsername(StringUtils.hasText(dto.getUsername())
                ? dto.getUsername()
                : "用户" + dto.getPhone().substring(dto.getPhone().length() - 4));
        user.setRoleId(3L); // 普通用户角色
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    @Override
    public void sendCode(String account, Integer type) {
        smsService.sendCode(account, type);
    }

    @Override
    public LoginVO loginByPassword(String account, String password, Integer type) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(400, "账号不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(400, "密码不能为空");
        }

        // 根据账号类型查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (type == 1) {
            wrapper.eq(User::getPhone, account);
        } else if (type == 2) {
            wrapper.eq(User::getEmail, account);
        } else {
            throw new BusinessException(400, "账号类型不正确");
        }

        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(400, "该账号未注册");
        }

        // 校验密码（BCrypt）
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        // 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(400, "账号已被禁用");
        }

        return buildLoginVO(user);
    }

    @Override
    public LoginVO registerByCode(String account, String code, String password, Integer type) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(400, "账号不能为空");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "验证码不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (password.length() < 6 || password.length() > 16) {
            throw new BusinessException(400, "密码长度为6~16位");
        }

        // 校验验证码
        if (!smsService.validateCode(account, code, type)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 校验账号格式并检查是否已注册
        if (type == 1) {
            if (!account.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(400, "手机号格式不正确");
            }
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, account);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(400, "该手机号已注册");
            }
        } else if (type == 2) {
            if (!account.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                throw new BusinessException(400, "邮箱格式不正确");
            }
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, account);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(400, "该邮箱已注册");
            }
        } else {
            throw new BusinessException(400, "注册类型不正确");
        }

        // 构建用户
        User user = new User();
        if (type == 1) {
            user.setPhone(account);
            user.setUsername("用户" + account.substring(account.length() - 4));
        } else {
            user.setEmail(account);
            String emailPrefix = account.contains("@") ? account.substring(0, account.indexOf("@")) : account;
            user.setUsername(emailPrefix);
        }
        user.setPassword(BCrypt.hashpw(password));
        user.setRoleId(3L); // 普通用户角色
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    @Override
    public void resetPassword(String account, String code, String newPwd, Integer type) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(400, "账号不能为空");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "验证码不能为空");
        }
        if (!StringUtils.hasText(newPwd)) {
            throw new BusinessException(400, "新密码不能为空");
        }
        if (newPwd.length() < 6 || newPwd.length() > 16) {
            throw new BusinessException(400, "密码长度为6~16位");
        }

        // 校验验证码
        if (!smsService.validateCode(account, code, type)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (type == 1) {
            wrapper.eq(User::getPhone, account);
        } else if (type == 2) {
            wrapper.eq(User::getEmail, account);
        } else {
            throw new BusinessException(400, "账号类型不正确");
        }

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(400, "该账号未注册");
        }

        // 更新密码
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                .set(User::getPassword, BCrypt.hashpw(newPwd));
        userMapper.update(null, updateWrapper);

        // 删除该用户所有 Redis 登录 token，强制下线
        try {
            stringRedisTemplate.delete(RedisConstant.userTokenKey(user.getId()));
            stringRedisTemplate.delete(RedisConstant.userInfoKey(user.getId()));
        } catch (Exception ignored) {}
    }

    /**
     * 构建登录成功返回值
     */
    private LoginVO buildLoginVO(User user) {
        // 查询角色信息
        Role role = null;
        if (user.getRoleId() != null) {
            role = roleMapper.selectById(user.getRoleId());
        }

        String roleCode = role != null ? role.getCode() : "USER";

        // 生成JWT
        String token = jwtUtil.createToken(String.valueOf(user.getId()), roleCode);

        // 构建UserDTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        if (role != null) {
            userDTO.setRoleName(role.getName());
            userDTO.setRoleCode(role.getCode());
        }

        // 缓存到 Redis：userTokenKey → token 字符串，userInfoKey → 用户信息
        try {
            // 商家用户：校验是否存在合法的商户入驻记录
            if ("MERCHANT".equals(roleCode)) {
                LambdaQueryWrapper<Merchant> mw = new LambdaQueryWrapper<>();
                mw.eq(Merchant::getUserId, user.getId());
                Merchant merchant = merchantMapper.selectOne(mw, false);
                if (merchant != null && merchant.getAuditStatus() != null && merchant.getAuditStatus() == 1) {
                    LambdaQueryWrapper<Shop> sw = new LambdaQueryWrapper<>();
                    sw.eq(Shop::getMerchantId, merchant.getId());
                    Shop shop = shopMapper.selectOne(sw, false);
                    if (shop != null) {
                        userDTO.setShopId(shop.getId());
                    }
                } else {
                    // 角色是 MERCHANT 但没有合法入驻记录 → 自动降级为普通用户
                    Role customerRole = roleMapper.selectOne(
                            new LambdaQueryWrapper<Role>().eq(Role::getCode, "CUSTOMER"));
                    if (customerRole != null) {
                        user.setRoleId(customerRole.getId());
                        userMapper.updateById(user);
                        roleCode = "CUSTOMER";
                        userDTO.setRoleCode("CUSTOMER");
                        if (customerRole.getName() != null) {
                            userDTO.setRoleName(customerRole.getName());
                        }
                        // 重新生成 JWT（角色已变更）
                        token = jwtUtil.createToken(String.valueOf(user.getId()), roleCode);
                    }
                }
            }

            String json = objectMapper.writeValueAsString(userDTO);
            stringRedisTemplate.opsForValue().set(RedisConstant.userTokenKey(userDTO.getId()), token,
                    RedisConstant.USER_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(RedisConstant.userInfoKey(userDTO.getId()), json,
                    RedisConstant.USER_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        return new LoginVO(token, userDTO);
    }
}