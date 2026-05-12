package com.yaoshizuting.testing;

import com.yaoshizuting.service.DistributedLockService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestDistributedLockConfig {

    private final Map<String, Object> redisStore = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public DistributedLockService distributedLockService() {
        return new DistributedLockService() {
            @Override
            public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
                return true;
            }

            @Override
            public void unlock(String lockKey) {
                // no-op for tests
            }
        };
    }

    @Bean
    @Primary
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RedisTemplate<String, Object> testRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
        when(valueOperations.increment(any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object current = redisStore.get(key);
            long next = current instanceof Number ? ((Number) current).longValue() + 1 : 1L;
            redisStore.put(key, next);
            return next;
        });
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (redisStore.containsKey(key)) {
                return false;
            }
            redisStore.put(key, invocation.getArgument(1));
            return true;
        });
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(any(), any());
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(any(), any(), anyLong());
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(any(), any(), anyLong(), any(TimeUnit.class));
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(any(), any(), any(Duration.class));
        when(redisTemplate.delete(org.mockito.ArgumentMatchers.<String>any()))
                .thenAnswer(invocation -> redisStore.remove(invocation.getArgument(0)) != null);

        return redisTemplate;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisServerCommands serverCommands = mock(RedisServerCommands.class);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.serverCommands()).thenReturn(serverCommands);
        doAnswer(invocation -> {
            redisStore.clear();
            return null;
        }).when(serverCommands).flushDb();

        return connectionFactory;
    }
}
