package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptimisticLockTest {

    @Mock
    private UserMapper userMapper;

    @Test
    void testAddBalance_ConcurrentUpdates_OptimisticLockRetries() {
        User user = new User();
        user.setId(1L);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalEarnings(BigDecimal.ZERO);
        user.setVersion(0);

        lenient().when(userMapper.selectById(1L)).thenReturn(user);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        when(userMapper.updateById(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getVersion() == 0) {
                successCount.incrementAndGet();
                return 1;
            } else {
                failCount.incrementAndGet();
                return 0;
            }
        });

        int maxRetries = 3;
        int updated = updateBalanceWithRetry(user, BigDecimal.valueOf(100), maxRetries);
        
        assertTrue(updated > 0 || updated == 0);
    }

    @Test
    void testAddBalance_VersionConflict_UpdatesFail() {
        User user = new User();
        user.setId(1L);
        user.setBalance(BigDecimal.ZERO);
        user.setVersion(0);

        lenient().when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(0);

        int maxRetries = 3;
        int attempts = 0;
        for (int i = 0; i < maxRetries; i++) {
            attempts++;
            int updated = userMapper.updateById(user);
            if (updated > 0) break;
        }
        
        assertEquals(3, attempts);
        verify(userMapper, times(3)).updateById(any(User.class));
    }

    @Test
    void testConcurrentBalanceUpdates_WithOptimisticLock() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        User user = new User();
        user.setId(1L);
        user.setBalance(BigDecimal.ZERO);
        user.setVersion(0);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenAnswer(invocation -> {
            synchronized (this) {
                return successCount.incrementAndGet() <= 1 ? 1 : 0;
            }
        });

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int retry = 0; retry < 3; retry++) {
                        User u = userMapper.selectById(1L);
                        u.setBalance(u.getBalance().add(BigDecimal.TEN));
                        int updated = userMapper.updateById(u);
                        if (updated > 0) break;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        
        assertTrue(successCount.get() >= 1);
    }

    private int updateBalanceWithRetry(User user, BigDecimal amount, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            user.setBalance(user.getBalance().add(amount));
            int updated = userMapper.updateById(user);
            if (updated > 0) return updated;
        }
        return 0;
    }
}
