package com.yaoshizuting.service.impl;

import com.yaoshizuting.service.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedissonDistributedLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new RedissonDistributedLockService(redissonClient);
    }

    @Test
    void testTryLock_Success() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        boolean result = lockService.tryLock("test-lock", 10, 30);
        
        assertTrue(result);
        verify(redissonClient).getLock("test-lock");
        verify(rLock).tryLock(10, 30, TimeUnit.SECONDS);
    }

    @Test
    void testTryLock_Failed() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        boolean result = lockService.tryLock("test-lock", 10, 30);
        
        assertFalse(result);
    }

    @Test
    void testTryLock_Interrupted() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException());

        boolean result = lockService.tryLock("test-lock", 10, 30);
        
        assertFalse(result);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void testUnlock_Success() {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        lockService.unlock("test-lock");
        
        verify(rLock).unlock();
    }

    @Test
    void testUnlock_NotHeldByCurrentThread() {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        lockService.unlock("test-lock");
        
        verify(rLock, never()).unlock();
    }

    @Test
    void testConcurrentLocks_DifferentKeys() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        boolean lock1 = lockService.tryLock("lock-1", 10, 30);
        boolean lock2 = lockService.tryLock("lock-2", 10, 30);
        
        assertTrue(lock1);
        assertTrue(lock2);
        verify(redissonClient, times(2)).getLock(anyString());
    }
}
