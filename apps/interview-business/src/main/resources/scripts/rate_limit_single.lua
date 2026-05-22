-- rate_limit_single.lua
-- 滑动时间窗口限流脚本（单 Key 版本）
--
-- KEYS[1] = Redis Key（包含限流维度标识）
-- ARGV[1] = 当前时间戳（毫秒）
-- ARGV[2] = 时间窗口大小（毫秒）
-- ARGV[3] = 最大允许请求次数
--
-- 返回值：
--   0 = 允许通过
--   1 = 超出限制

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local expire = math.ceil(window / 1000) + 1

-- 移除窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 统计窗口内的请求数
local count = redis.call('ZCARD', key)

if count < limit then
    -- 未超限：记录本次请求时间戳（以 now 作为 member 和 score）
    redis.call('ZADD', key, now, now .. '-' .. math.random(1, 1000000))
    redis.call('EXPIRE', key, expire)
    return 0
else
    -- 已超限：拒绝
    return 1
end
