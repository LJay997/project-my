package com.qq.ijay997.controller;

import com.qq.ijay997.service.RedissonLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RScript;
import org.redisson.api.RSemaphore;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/lock")
@RequiredArgsConstructor
public class LockController {

    private final RedissonLockService lockService;

    private static final AtomicInteger counter = new AtomicInteger(0);

    @GetMapping("/test/reentrant")
    public String testReentrantLock(@RequestParam(defaultValue = "test-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        boolean acquired = lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);
        if (acquired) {
            try {
                int count = counter.incrementAndGet();
                log.info("[{}] 获取锁成功，当前计数: {}", requestId, count);
                
                Thread.sleep(2000);
                
                count = counter.get();
                log.info("[{}] 执行业务完成，计数: {}", requestId, count);
                return String.format("请求[%s] 获取锁成功，计数: %d", requestId, count);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "请求被中断";
            } finally {
                lockService.unlock(lockKey);
                log.info("[{}] 释放锁", requestId);
            }
        } else {
            log.warn("[{}] 获取锁失败", requestId);
            return String.format("请求[%s] 获取锁失败，请稍后重试", requestId);
        }
    }

    @GetMapping("/test/fair")
    public String testFairLock(@RequestParam(defaultValue = "fair-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RLock fairLock = lockService.getFairLock(lockKey);
        try {
            if (fairLock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    int count = counter.incrementAndGet();
                    log.info("[{}] 公平锁获取成功，当前计数: {}", requestId, count);
                    Thread.sleep(1000);
                    return String.format("请求[%s] 公平锁获取成功，计数: %d", requestId, count);
                } finally {
                    fairLock.unlock();
                }
            }
            return String.format("请求[%s] 公平锁获取失败", requestId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "请求被中断";
        }
    }

    @GetMapping("/test/read")
    public String testReadLock(@RequestParam(defaultValue = "rw-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RReadWriteLock rwLock = lockService.getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        
        try {
            if (readLock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    log.info("[{}] 读锁获取成功", requestId);
                    Thread.sleep(1500);
                    return String.format("请求[%s] 读锁获取成功，可并发读取", requestId);
                } finally {
                    readLock.unlock();
                }
            }
            return String.format("请求[%s] 读锁获取失败", requestId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "请求被中断";
        }
    }

    @GetMapping("/test/write")
    public String testWriteLock(@RequestParam(defaultValue = "rw-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RReadWriteLock rwLock = lockService.getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        
        try {
            if (writeLock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    int count = counter.incrementAndGet();
                    log.info("[{}] 写锁获取成功，当前计数: {}", requestId, count);
                    Thread.sleep(2000);
                    return String.format("请求[%s] 写锁获取成功，计数: %d", requestId, count);
                } finally {
                    writeLock.unlock();
                }
            }
            return String.format("请求[%s] 写锁获取失败", requestId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "请求被中断";
        }
    }

    @GetMapping("/test/semaphore")
    public String testSemaphore(@RequestParam(defaultValue = "test-semaphore") String semaphoreKey,
                                @RequestParam(defaultValue = "3") int permits) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RSemaphore semaphore = lockService.getSemaphore(semaphoreKey);
        
        try {
            semaphore.trySetPermits(permits);
            
            if (semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                try {
                    log.info("[{}] 获取信号量成功，剩余许可: {}", requestId, semaphore.availablePermits());
                    Thread.sleep(2000);
                    return String.format("请求[%s] 信号量获取成功，剩余许可: %d", requestId, semaphore.availablePermits());
                } finally {
                    semaphore.release();
                    log.info("[{}] 释放信号量，剩余许可: {}", requestId, semaphore.availablePermits());
                }
            }
            return String.format("请求[%s] 信号量获取失败", requestId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "请求被中断";
        }
    }

    @GetMapping("/test/execute")
    public String testExecuteWithLock(@RequestParam(defaultValue = "execute-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String result = lockService.executeWithLock(lockKey, 10, TimeUnit.SECONDS, () -> {
            int count = counter.incrementAndGet();
            log.info("[{}] executeWithLock 执行中，计数: {}", requestId, count);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return String.format("请求[%s] 执行成功，计数: %d", requestId, count);
        });
        
        return result != null ? result : String.format("请求[%s] 获取锁失败", requestId);
    }

    @GetMapping("/info")
    public String getLockInfo(@RequestParam(defaultValue = "test-lock") String lockKey) {
        return lockService.getLockInfo(lockKey);
    }

    @GetMapping("/counter")
    public String getCounter() {
        return String.format("当前计数: %d", counter.get());
    }

    @PostMapping("/counter/reset")
    public String resetCounter() {
        counter.set(0);
        return "计数器已重置";
    }

    @PostMapping("/lua/execute")
    public String executeLuaScript(@RequestParam(defaultValue = "return 'Hello Lua'") String script) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 执行 Lua 脚本: {}", requestId, script);
        
        String result = lockService.evalStringScript(script, new String[]{});
        return String.format("请求[%s] 执行结果: %s", requestId, result);
    }

    @GetMapping("/lua/incr")
    public String luaIncr(@RequestParam(defaultValue = "lua-counter") String key,
                          @RequestParam(defaultValue = "1") int delta) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String luaScript = "local val = redis.call('INCRBY', KEYS[1], ARGV[1])\n" +
                          "redis.call('EXPIRE', KEYS[1], 60)\n" +
                          "return val";
        
        Long result = lockService.evalLongScript(luaScript, new String[]{key}, delta);
        log.info("[{}] Lua INCRBY 执行结果: {}", requestId, result);
        return String.format("请求[%s] 计数器[%s] 值: %d", requestId, key, result);
    }

    @GetMapping("/lua/getset")
    public String luaGetSet(@RequestParam(defaultValue = "lua-key") String key,
                           @RequestParam(defaultValue = "lua-value") String value) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String luaScript = "local oldVal = redis.call('GET', KEYS[1])\n" +
                          "redis.call('SET', KEYS[1], ARGV[1])\n" +
                          "return oldVal";
        
        String result = lockService.evalStringScript(luaScript, new String[]{key}, value);
        log.info("[{}] Lua GETSET 执行结果: 旧值={}, 新值={}", requestId, result, value);
        return String.format("请求[%s] 键[%s] 旧值: %s, 新值: %s", requestId, key, result, value);
    }

    @GetMapping("/lua/lock")
    public String luaDistributedLock(@RequestParam(defaultValue = "lua-lock") String lockKey,
                                     @RequestParam(defaultValue = "30") int expireSeconds) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String requestIdFull = UUID.randomUUID().toString();
        
        String luaScript = "if redis.call('SETNX', KEYS[1], ARGV[1]) == 1 then\n" +
                          "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
                          "    return 1\n" +
                          "else\n" +
                          "    return 0\n" +
                          "end";
        
        Long result = lockService.evalLongScript(luaScript, new String[]{lockKey}, requestIdFull, expireSeconds);
        
        if (result == 1) {
            log.info("[{}] Lua 分布式锁获取成功", requestId);
            return String.format("请求[%s] 获取锁成功，锁ID: %s", requestId, requestIdFull.substring(0, 8));
        } else {
            log.warn("[{}] Lua 分布式锁获取失败", requestId);
            return String.format("请求[%s] 获取锁失败，锁已被占用", requestId);
        }
    }

    @PostMapping("/lua/unlock")
    public String luaDistributedUnlock(@RequestParam(defaultValue = "lua-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String luaScript = "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
                          "    redis.call('DEL', KEYS[1])\n" +
                          "    return 1\n" +
                          "else\n" +
                          "    return 0\n" +
                          "end";
        
        Long result = lockService.evalLongScript(luaScript, new String[]{lockKey}, "test-request-id");
        
        if (result == 1) {
            log.info("[{}] Lua 分布式锁释放成功", requestId);
            return String.format("请求[%s] 释放锁成功", requestId);
        } else {
            log.warn("[{}] Lua 分布式锁释放失败", requestId);
            return String.format("请求[%s] 释放锁失败，锁不存在或不属于当前请求", requestId);
        }
    }

    @GetMapping("/lua/hash")
    public String luaHashOperation(@RequestParam(defaultValue = "lua-hash") String hashKey,
                                   @RequestParam(defaultValue = "field1") String field,
                                   @RequestParam(defaultValue = "value1") String value) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String luaScript = "redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])\n" +
                          "local val = redis.call('HGET', KEYS[1], ARGV[1])\n" +
                          "local len = redis.call('HLEN', KEYS[1])\n" +
                          "return val .. '|' .. len";
        
        String result = lockService.evalStringScript(luaScript, new String[]{hashKey}, field, value);
        String[] parts = result.split("\\|");
        
        log.info("[{}] Lua Hash 操作结果: 值={}, 字段数={}", requestId, parts[0], parts[1]);
        return String.format("请求[%s] Hash[%s] 字段[%s] 值: %s, 字段总数: %s", 
                requestId, hashKey, field, parts[0], parts[1]);
    }

    @GetMapping("/lua/multi")
    public String luaMultiOperation() {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String luaScript = "redis.call('SET', 'lua:key1', 'value1')\n" +
                          "redis.call('SET', 'lua:key2', 'value2')\n" +
                          "redis.call('SET', 'lua:key3', 'value3')\n" +
                          "local val1 = redis.call('GET', 'lua:key1')\n" +
                          "local val2 = redis.call('GET', 'lua:key2')\n" +
                          "return val1 .. ',' .. val2";
        
        String result = lockService.evalStringScript(luaScript, new String[]{});
        log.info("[{}] Lua 批量操作结果: {}", requestId, result);
        return String.format("请求[%s] 批量操作完成，结果: %s", requestId, result);
    }

    @GetMapping("/watchdog/acquire")
    public String acquireLockWithWatchdog(@RequestParam(defaultValue = "watchdog-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RLock lock = lockService.getLock(lockKey);
        lock.lock();
        
        long remainTime = lockService.getLockRemainingTime(lockKey);
        log.info("[{}] 获取锁成功（看门狗模式），剩余时间: {}ms", requestId, remainTime);
        return String.format("请求[%s] 获取锁成功（看门狗模式），剩余时间: %dms", requestId, remainTime);
    }

    @GetMapping("/watchdog/check")
    public String checkWatchdogStatus(@RequestParam(defaultValue = "watchdog-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String info = lockService.getLockWatchdogInfo(lockKey);
        log.info("[{}] 看门狗状态检查: {}", requestId, info);
        return String.format("请求[%s] 看门狗状态: %s", requestId, info);
    }

    @GetMapping("/watchdog/remaining")
    public String getLockRemainingTime(@RequestParam(defaultValue = "watchdog-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        long remainTime = lockService.getLockRemainingTime(lockKey);
        log.info("[{}] 锁剩余时间: {}ms", requestId, remainTime);
        return String.format("请求[%s] 锁[%s] 剩余时间: %dms", requestId, lockKey, remainTime);
    }

    @GetMapping("/watchdog/long-task")
    public String longTaskWithWatchdog(@RequestParam(defaultValue = "watchdog-lock") String lockKey,
                                       @RequestParam(defaultValue = "15") int seconds) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RLock lock = lockService.getLock(lockKey);
        lock.lock();
        
        try {
            log.info("[{}] 开始长时间任务，预计{}秒", requestId, seconds);
            
            for (int i = 0; i < seconds; i++) {
                Thread.sleep(1000);
                long remainTime = lockService.getLockRemainingTime(lockKey);
                log.info("[{}] 任务进行中 - {}s / {}s, 锁剩余时间: {}ms", 
                        requestId, i + 1, seconds, remainTime);
            }
            
            long finalRemainTime = lockService.getLockRemainingTime(lockKey);
            return String.format("请求[%s] 长时间任务完成，锁剩余时间: %dms", requestId, finalRemainTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "任务被中断";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[{}] 释放锁", requestId);
            }
        }
    }

    @PostMapping("/watchdog/release")
    public String releaseWatchdogLock(@RequestParam(defaultValue = "watchdog-lock") String lockKey) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        RLock lock = lockService.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("[{}] 释放锁成功", requestId);
            return String.format("请求[%s] 释放锁成功", requestId);
        } else {
            log.warn("[{}] 无法释放锁，当前线程未持有锁", requestId);
            return String.format("请求[%s] 无法释放锁，当前线程未持有锁", requestId);
        }
    }

    @GetMapping("/watchdog/debug")
    public String debugWatchdogRenewal(@RequestParam(defaultValue = "watchdog-debug-lock") String lockKey,
                                       @RequestParam(defaultValue = "20") int observeSeconds) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        StringBuilder result = new StringBuilder();
        
        RLock lock = lockService.getLock(lockKey);
        lock.lock();
        
        try {
            result.append(String.format("请求[%s] 开始观察看门狗续期，持续%d秒\n", requestId, observeSeconds));
            result.append("=".repeat(60)).append("\n");
            
            long lastRemainingTime = -1;
            
            for (int i = 0; i < observeSeconds; i++) {
                long currentRemaining = lockService.getLockRemainingTime(lockKey);
                String status = "";
                
                if (lastRemainingTime > 0 && currentRemaining > lastRemainingTime) {
                    status = " ← 看门狗续期！";
                } else if (lastRemainingTime > 0 && currentRemaining < lastRemainingTime) {
                    status = " ← 正常递减";
                }
                
                result.append(String.format("[第%d秒] 剩余时间: %dms%s\n", 
                        i + 1, currentRemaining, status));
                
                log.info("[{}] 第{}秒 - 锁剩余时间: {}ms{}", requestId, i + 1, currentRemaining, status);
                
                lastRemainingTime = currentRemaining;
                Thread.sleep(1000);
            }
            
            result.append("=".repeat(60)).append("\n");
            result.append(String.format("请求[%s] 观察结束\n", requestId));
            
            return result.toString();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "观察被中断";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[{}] 释放锁", requestId);
            }
        }
    }
}