-- 库存回滚 Lua 脚本
-- KEYS[1] = SKU 库存 Key
-- KEYS[2] = 优惠券库存 Key（可选）
-- ARGV[1] = 回滚数量
-- ARGV[2] = 是否回滚优惠券 (0/1)

local skuKey = KEYS[1]
local couponKey = KEYS[2]
local num = tonumber(ARGV[1])
local rollbackCoupon = tonumber(ARGV[2])

redis.call('INCRBY', skuKey, num)

if rollbackCoupon == 1 and couponKey ~= '' then
    redis.call('INCR', couponKey)
end

return 0