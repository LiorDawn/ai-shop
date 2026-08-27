-- 原子扣减库存 Lua 脚本
-- KEYS[1] = SKU 库存 Key (stock:sku:{skuId})
-- KEYS[2] = 优惠券库存 Key (coupon:stock:{couponId})，无优惠券时传空字符串
-- ARGV[1] = 扣减数量
-- ARGV[2] = 是否使用优惠券 (0/1)
-- 返回: 0=成功, -1=SKU库存不足, -2=优惠券库存不足

local skuKey = KEYS[1]
local couponKey = KEYS[2]
local num = tonumber(ARGV[1])
local useCoupon = tonumber(ARGV[2])

-- ① 查询并扣减 SKU 库存
local skuStock = redis.call('GET', skuKey)
if not skuStock or tonumber(skuStock) < num then
    return -1
end
redis.call('DECRBY', skuKey, num)

-- ② 如果使用优惠券，查询并扣减优惠券库存
if useCoupon == 1 and couponKey ~= '' then
    local couponStock = redis.call('GET', couponKey)
    if not couponStock or tonumber(couponStock) < 1 then
        -- 回滚 SKU 库存
        redis.call('INCRBY', skuKey, num)
        return -2
    end
    redis.call('DECR', couponKey)
end

return 0