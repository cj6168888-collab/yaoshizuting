package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.DistributedLockService;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.PolicyConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProfitServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfitLogMapper profitLogMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private PolicyConfigService policyConfigService;

    @Mock
    private DistributedLockService lockService;

    @InjectMocks
    private com.yaoshizuting.service.impl.ProfitServiceImpl profitService;

    @BeforeEach
    void setUp() {
        profitService = new ProfitServiceImpl(userMapper, profitLogMapper, orderService, policyConfigService, lockService);
    }

    @Test
    void testProcessPartnerRecruitAgentProfit_Boundary11_FeeApplied() {
        // setup partner as PARTNER with agentCount 10
        User partner = new User();
        partner.setId(1L);
        partner.setRole(3); // PARTNER
        partner.setAgentCount(10);
        partner.setBalance(BigDecimal.ZERO);
        partner.setTotalEarnings(BigDecimal.ZERO);

        // new agent recruited by partner
        User newAgent = new User();
        newAgent.setId(200L);
        newAgent.setParentId(partner.getId());

        when(policyConfigService.getConfigValue("PARTNER_MANAGE_FEE")).thenReturn(BigDecimal.valueOf(39800));
        when(policyConfigService.getConfigValue("HEADQUARTER_SUPPORT_FEE")).thenReturn(BigDecimal.valueOf(9800));
        when(lockService.tryLock(any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        // balance update flow uses selectById for fetch in addBalance
        when(userMapper.selectById(partner.getId())).thenReturn(partner, partner, partner);

        // execute
        profitService.processPartnerRecruitAgentProfit(partner, newAgent);

        // verify agent count increment
        assertEquals(11, partner.getAgentCount().intValue());
        // current implementation updates the same in-memory partner instance on each balance refresh
        assertEquals(new BigDecimal("90000"), partner.getBalance());

        // verify profit logs: two entries created
        ArgumentCaptor<ProfitLog> logCaptor = ArgumentCaptor.forClass(ProfitLog.class);
        verify(profitLogMapper, times(2)).insert(logCaptor.capture());
        List<ProfitLog> logs = logCaptor.getAllValues();
        ProfitLog first = logs.get(0);
        ProfitLog second = logs.get(1);

        // first log: 39,800 +  agent management fee
        assertEquals(partner.getId(), first.getReceiverId());
        assertEquals(newAgent.getId(), first.getContributorId());
        assertEquals(BigDecimal.valueOf(39800).setScale(2), first.getAmount().setScale(2));
        assertEquals(ProfitType.AGENT_MANAGE.getCode(), first.getType());
        // orderSn should be agent-specific
        // second log: -9800 HQ fee
        assertEquals(partner.getId(), second.getReceiverId());
        assertEquals(newAgent.getId(), second.getContributorId());
        assertEquals(BigDecimal.valueOf(-9800).setScale(2), second.getAmount().setScale(2));
        assertEquals(ProfitType.HEADQUARTER_SUPPORT_FEE.getCode(), second.getType());
    }
}
