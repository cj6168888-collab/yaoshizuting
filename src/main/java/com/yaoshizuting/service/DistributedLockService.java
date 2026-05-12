package com.yaoshizuting.service;

public interface DistributedLockService {
    
    boolean tryLock(String lockKey, long waitTime, long leaseTime);
    
    void unlock(String lockKey);
}
