-- 领取优惠券 Lua 脚本
-- KEYS[1] = 优惠券库存 Key (coupon:stock:{couponId})
-- KEYS[2] = 用户已领集合 Key (coupon:user:{couponId})
-- ARGV[1] = 用户ID
-- 返回: 0=成功, -1=库存不足, -2=已领取过

local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local userId = ARGV[1]

-- ① 检查是否已领取
local isMember = redis.call('SISMEMBER', userSetKey, userId)
if isMember == 1 then
    return -2
end

-- ② 检查并扣减库存
local stock = redis.call('GET', stockKey)
if not stock or tonumber(stock) < 1 then
    return -1
end
redis.call('DECR', stockKey)

-- ③ 记录用户已领取
redis.call('SADD', userSetKey, userId)

return 0