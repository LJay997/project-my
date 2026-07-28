package com.qq.ijay997.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonLockService {

    private final RedissonClient redissonClient;

    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    public RLock getFairLock(String lockKey) {
        return redissonClient.getFairLock(lockKey);
    }

    public RReadWriteLock getReadWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey);
    }

    public RSemaphore getSemaphore(String semaphoreKey) {
        return redissonClient.getSemaphore(semaphoreKey);
    }

    public RCountDownLatch getCountDownLatch(String latchKey) {
        return redissonClient.getCountDownLatch(latchKey);
    }

    public RLock getRedLock(String... lockKeys) {
        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = redissonClient.getLock(lockKeys[i]);
        }
        return redissonClient.getRedLock(locks);
    }

    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁时被中断", e);
            return false;
        }
    }

    public void lock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockKey);
        lock.lock(leaseTime, timeUnit);
    }

    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public boolean executeWithLock(String lockKey, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
        RLock lock = getLock(lockKey);
        try {
            if (lock.tryLock(leaseTime, timeUnit)) {
                try {
                    runnable.run();
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            log.warn("未能获取锁: {}", lockKey);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁时被中断", e);
            return false;
        }
    }

    public <T> T executeWithLock(String lockKey, long leaseTime, TimeUnit timeUnit, java.util.function.Supplier<T> supplier) {
        RLock lock = getLock(lockKey);
        try {
            if (lock.tryLock(leaseTime, timeUnit)) {
                try {
                    return supplier.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            log.warn("未能获取锁: {}", lockKey);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁时被中断", e);
            return null;
        }
    }

    public String getLockInfo(String lockKey) {
        RLock lock = getLock(lockKey);
        return String.format("锁[%s] - 已锁定: %s, 锁定次数: %d",
                lockKey, lock.isLocked(), lock.getHoldCount());
    }

    public <T> T evalScript(String luaScript, RScript.ReturnType returnType, java.util.List<Object> keys, Object... values) {
        return redissonClient.getScript().eval(RScript.Mode.READ_WRITE, luaScript, returnType, keys, values);
    }

    public <T> T evalScript(String luaScript, RScript.ReturnType returnType, java.util.List<Object> keys) {
        return redissonClient.getScript().eval(RScript.Mode.READ_WRITE, luaScript, returnType, keys);
    }

    public String evalStringScript(String luaScript, String[] keys, Object... values) {
        try {
            java.util.List<Object> keyList = java.util.Arrays.asList(keys);
            return redissonClient.getScript().eval(RScript.Mode.READ_WRITE, luaScript, RScript.ReturnType.VALUE, keyList, values).toString();
        } catch (Exception e) {
            log.error("执行 Lua 脚本失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    public Long evalLongScript(String luaScript, String[] keys, Object... values) {
        try {
            java.util.List<Object> keyList = java.util.Arrays.asList(keys);
            return redissonClient.getScript().eval(RScript.Mode.READ_WRITE, luaScript, RScript.ReturnType.INTEGER, keyList, values);
        } catch (Exception e) {
            log.error("执行 Lua 脚本失败", e);
            return -1L;
        }
    }

    public Boolean evalBooleanScript(String luaScript, String[] keys, Object... values) {
        try {
            java.util.List<Object> keyList = java.util.Arrays.asList(keys);
            return redissonClient.getScript().eval(RScript.Mode.READ_WRITE, luaScript, RScript.ReturnType.BOOLEAN, keyList, values);
        } catch (Exception e) {
            log.error("执行 Lua 脚本失败", e);
            return false;
        }
    }

    public long getLockRemainingTime(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.remainTimeToLive();
    }

    public String getLockWatchdogInfo(String lockKey) {
        RLock lock = getLock(lockKey);
        long remainTime = lock.remainTimeToLive();
        boolean isLocked = lock.isLocked();
        boolean isHeldByCurrentThread = lock.isHeldByCurrentThread();
        int holdCount = lock.getHoldCount();
        
        return String.format("锁[%s] - 已锁定: %s, 当前线程持有: %s, 持有次数: %d, 剩余时间: %dms",
                lockKey, isLocked, isHeldByCurrentThread, holdCount, remainTime);
    }
}