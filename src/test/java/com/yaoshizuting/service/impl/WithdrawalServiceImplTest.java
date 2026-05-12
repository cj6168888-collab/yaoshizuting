package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.User;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.WithdrawalMapper;
import com.yaoshizuting.service.PolicyConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceImplTest {

    @Mock
    private WithdrawalMapper withdrawalMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PolicyConfigService policyConfigService;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    @Test
    void createWithdrawalDeductsBalanceAndStoresFeeAmounts() {
        User user = buildUser(10L, "1000.00");
        when(userMapper.selectById(10L)).thenReturn(user);
        when(policyConfigService.getConfigValue("WITHDRAWAL_MIN_AMOUNT")).thenReturn(new BigDecimal("100.00"));
        when(policyConfigService.getConfigValue("WITHDRAWAL_FEE_RATE")).thenReturn(new BigDecimal("0.01"));

        Withdrawal withdrawal = withdrawalService.createWithdrawal(
                10L,
                new BigDecimal("200.00"),
                1,
                "account-no",
                "account-name",
                "bank-name");

        assertEquals(new BigDecimal("800.00"), user.getBalance());
        verify(userMapper).updateById(user);
        verify(withdrawalMapper).insert(withdrawal);
        assertEquals(10L, withdrawal.getUserId());
        assertEquals(new BigDecimal("200.00"), withdrawal.getAmount());
        assertEquals(new BigDecimal("2.00"), withdrawal.getFee());
        assertEquals(new BigDecimal("198.00"), withdrawal.getActualAmount());
        assertEquals(0, withdrawal.getStatus());
        assertNotNull(withdrawal.getWithdrawSn());
    }

    @Test
    void createWithdrawalRejectsAmountBelowMinimum() {
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L, "1000.00"));
        when(policyConfigService.getConfigValue("WITHDRAWAL_MIN_AMOUNT")).thenReturn(new BigDecimal("100.00"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.createWithdrawal(
                        10L,
                        new BigDecimal("99.99"),
                        1,
                        "account-no",
                        "account-name",
                        null));

        assertEquals("提现金额不能低于100.00元", exception.getMessage());
        verify(withdrawalMapper, never()).insert(any());
    }

    @Test
    void createWithdrawalRejectsMissingUser() {
        when(userMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.createWithdrawal(
                        10L,
                        new BigDecimal("200.00"),
                        1,
                        "account-no",
                        "account-name",
                        null));

        assertEquals("用户不存在", exception.getMessage());
        verify(policyConfigService, never()).getConfigValue(any());
        verify(withdrawalMapper, never()).insert(any());
    }

    @Test
    void createWithdrawalRejectsInsufficientBalance() {
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L, "50.00"));
        when(policyConfigService.getConfigValue("WITHDRAWAL_MIN_AMOUNT")).thenReturn(new BigDecimal("10.00"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.createWithdrawal(
                        10L,
                        new BigDecimal("60.00"),
                        1,
                        "account-no",
                        "account-name",
                        null));

        assertEquals("余额不足", exception.getMessage());
        verify(withdrawalMapper, never()).insert(any());
    }

    @Test
    void approveWithdrawalMarksPendingRequestApproved() {
        Withdrawal withdrawal = buildWithdrawal(1L, 10L, "200.00", 0);
        when(withdrawalMapper.selectById(1L)).thenReturn(withdrawal);

        withdrawalService.approveWithdrawal(1L, true, "审核通过");

        assertEquals(1, withdrawal.getStatus());
        assertEquals("审核通过", withdrawal.getRemark());
        assertNotNull(withdrawal.getAuditTime());
        verify(withdrawalMapper).updateById(withdrawal);
    }

    @Test
    void approveWithdrawalAllowsNullRemark() {
        Withdrawal withdrawal = buildWithdrawal(1L, 10L, "200.00", 0);
        when(withdrawalMapper.selectById(1L)).thenReturn(withdrawal);

        withdrawalService.approveWithdrawal(1L, true, null);

        assertEquals(1, withdrawal.getStatus());
        assertNull(withdrawal.getRemark());
        assertNotNull(withdrawal.getAuditTime());
        verify(withdrawalMapper).updateById(withdrawal);
    }

    @Test
    void approveWithdrawalRejectsMissingRecord() {
        when(withdrawalMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.approveWithdrawal(1L, true, "审核通过"));

        assertEquals("提现记录不存在", exception.getMessage());
        verify(withdrawalMapper, never()).updateById(any());
    }

    @Test
    void approveWithdrawalRejectsNonPendingRequest() {
        when(withdrawalMapper.selectById(1L)).thenReturn(buildWithdrawal(1L, 10L, "200.00", 2));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.approveWithdrawal(1L, true, "审核通过"));

        assertEquals("该提现申请状态异常", exception.getMessage());
        verify(withdrawalMapper, never()).updateById(any());
    }

    @Test
    void approveWithdrawalRejectPathRefundsBalance() {
        Withdrawal withdrawal = buildWithdrawal(1L, 10L, "200.00", 0);
        User user = buildUser(10L, "800.00");
        when(withdrawalMapper.selectById(1L)).thenReturn(withdrawal);
        when(userMapper.selectById(10L)).thenReturn(user);

        withdrawalService.approveWithdrawal(1L, false, "资料不符");

        assertEquals(new BigDecimal("1000.00"), user.getBalance());
        assertEquals(3, withdrawal.getStatus());
        assertEquals("资料不符", withdrawal.getRemark());
        verify(userMapper).updateById(user);
        verify(withdrawalMapper).updateById(withdrawal);
    }

    @Test
    void completeWithdrawalMarksApprovedRequestComplete() {
        Withdrawal withdrawal = buildWithdrawal(1L, 10L, "200.00", 1);
        when(withdrawalMapper.selectById(1L)).thenReturn(withdrawal);

        withdrawalService.completeWithdrawal(1L, "TX-001");

        assertEquals(2, withdrawal.getStatus());
        assertEquals("打款成功", withdrawal.getRemark());
        assertNotNull(withdrawal.getCompleteTime());
        verify(withdrawalMapper).updateById(withdrawal);
    }

    @Test
    void completeWithdrawalRejectsMissingRecord() {
        when(withdrawalMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.completeWithdrawal(1L, "TX-001"));

        assertEquals("提现记录不存在", exception.getMessage());
        verify(withdrawalMapper, never()).updateById(any());
    }

    @Test
    void completeWithdrawalRejectsNonApprovedRequest() {
        when(withdrawalMapper.selectById(1L)).thenReturn(buildWithdrawal(1L, 10L, "200.00", 0));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.completeWithdrawal(1L, "TX-001"));

        assertEquals("该提现申请非待打款状态", exception.getMessage());
        verify(withdrawalMapper, never()).updateById(any());
    }

    @Test
    void rejectWithdrawalRefundsBalanceAndStoresRemark() {
        Withdrawal withdrawal = buildWithdrawal(1L, 10L, "200.00", 0);
        User user = buildUser(10L, "800.00");
        when(withdrawalMapper.selectById(1L)).thenReturn(withdrawal);
        when(userMapper.selectById(10L)).thenReturn(user);

        withdrawalService.rejectWithdrawal(1L, "主动驳回");

        ArgumentCaptor<Withdrawal> captor = ArgumentCaptor.forClass(Withdrawal.class);
        verify(withdrawalMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("1000.00"), user.getBalance());
        assertEquals(3, captor.getValue().getStatus());
        assertEquals("主动驳回", captor.getValue().getRemark());
        assertNotNull(captor.getValue().getAuditTime());
    }

    @Test
    void rejectWithdrawalRejectsMissingRecord() {
        when(withdrawalMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.rejectWithdrawal(1L, "主动驳回"));

        assertEquals("提现记录不存在", exception.getMessage());
        verify(userMapper, never()).selectById(any());
        verify(withdrawalMapper, never()).updateById(any());
    }

    @Test
    void rejectWithdrawalRejectsNonPendingRequest() {
        when(withdrawalMapper.selectById(1L)).thenReturn(buildWithdrawal(1L, 10L, "200.00", 2));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> withdrawalService.rejectWithdrawal(1L, "主动驳回"));

        assertEquals("该提现申请状态异常", exception.getMessage());
        verify(userMapper, never()).selectById(any());
        verify(withdrawalMapper, never()).updateById(any());
    }

    @Test
    void rejectWithdrawalStillUpdatesStatusWhenUserIsMissing() {
        Withdrawal withdrawal = buildWithdrawal(1L, 10L, "200.00", 0);
        when(withdrawalMapper.selectById(1L)).thenReturn(withdrawal);
        when(userMapper.selectById(10L)).thenReturn(null);

        withdrawalService.rejectWithdrawal(1L, "用户已删除");

        assertEquals(3, withdrawal.getStatus());
        assertEquals("用户已删除", withdrawal.getRemark());
        assertNotNull(withdrawal.getAuditTime());
        verify(userMapper, never()).updateById(any());
        verify(withdrawalMapper).updateById(withdrawal);
    }

    private User buildUser(Long id, String balance) {
        User user = new User();
        user.setId(id);
        user.setBalance(new BigDecimal(balance));
        return user;
    }

    private Withdrawal buildWithdrawal(Long id, Long userId, String amount, Integer status) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(id);
        withdrawal.setUserId(userId);
        withdrawal.setAmount(new BigDecimal(amount));
        withdrawal.setStatus(status);
        return withdrawal;
    }
}
