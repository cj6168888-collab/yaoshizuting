package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import com.yaoshizuting.testing.TestMode;
import com.yaoshizuting.testing.TestMode.Mode;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProfitServiceImplIdempotentMultiLogsTest {

    @BeforeEach
    void setMode() {
        TestMode.setMode(Mode.CONCURRENT);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfitLogMapper profitLogMapper;

    @Mock
    private PolicyConfigService policyConfigService;

    @Mock
    private com.yaoshizuting.service.OrderService orderService;

    @InjectMocks
    private ProfitService profitService = new com.yaoshizuting.service.impl.ProfitServiceImpl();

    @Test
    void testStoreJoinProfit_MultiLogs_Idempotence() {
        // Prepare hierarchy: Partner (1) -> Store A (2) -> New Store B (3)
        User partner = new User();
        partner.setId(1L);
        partner.setRole(UserRole.PARTNER.getCode());

        User a = new User();
        a.setId(2L);
        a.setRole(UserRole.STORE.getCode());
        a.setParentId(partner.getId());
        a.setTreePath("/0/1/2/");

        User b = new User();
        b.setId(3L);
        b.setRole(UserRole.STORE.getCode());
        b.setParentId(a.getId());
        b.setTreePath("/0/1/2/3/");

        when(userMapper.selectById(a.getId())).thenReturn(a);
        when(userMapper.selectById(partner.getId())).thenReturn(partner);
        when(userMapper.selectById(b.getId())).thenReturn(b);
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.valueOf(9000));
        when(policyConfigService.getConfigValue("REWARD_INDIRECT")).thenReturn(BigDecimal.valueOf(6000));

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-IDE-MULTI-LOG-001");
        order.setUserId(b.getId());
        order.setStatus(1);
        order.setOrderType(1);

        AtomicInteger callCounter = new AtomicInteger(0);
        when(profitLogMapper.selectByUniqueKey(anyString(), anyString(), anyLong())).thenAnswer(invocation -> {
            int i = callCounter.getAndIncrement();
            // First two calls: allow inserts (return null)
            // Subsequent calls: simulate duplicates (return a log)
            return i < 2 ? null : new ProfitLog();
        });

        // First execution should insert two logs (direct and indirect)
        profitService.processJoinStoreProfit(order);
        // Second execution should not insert any new logs due to idempotence
        profitService.processJoinStoreProfit(order);

        // Expect two inserts (one for DIRECT_STORE and one for INDIRECT_STORE) in the first run
        verify(profitLogMapper, times(2)).insert(Mockito.any());
    }
}
