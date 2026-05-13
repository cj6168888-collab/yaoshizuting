package com.yaoshizuting.service.impl;

import cn.hutool.core.util.StrUtil;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.WithdrawalMapper;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.WithdrawalService;
import com.yaoshizuting.utils.OrderNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalMapper withdrawalMapper;
    private final UserMapper userMapper;
    private final PolicyConfigService policyConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Withdrawal createWithdrawal(Long userId, BigDecimal amount, Integer withdrawType, 
                                        String accountNo, String accountName, String bankName) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("提现金额必须大于0");
        }
        if (withdrawType == null || withdrawType < 1 || withdrawType > 3) {
            throw new BusinessException("提现方式无效");
        }
        if (StrUtil.isBlank(accountNo) || StrUtil.isBlank(accountName)) {
            throw new BusinessException("请完善收款信息");
        }
        if (withdrawType == 3 && StrUtil.isBlank(bankName)) {
            throw new BusinessException("银行卡提现需填写开户行");
        }

        BigDecimal minAmount = policyConfigService.getConfigValue("WITHDRAWAL_MIN_AMOUNT");
        if (amount.compareTo(minAmount) < 0) {
            throw new BusinessException("提现金额不能低于" + minAmount + "元");
        }

        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("余额不足");
        }

        BigDecimal feeRate = policyConfigService.getConfigValue("WITHDRAWAL_FEE_RATE");
        BigDecimal fee = amount.multiply(feeRate).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal actualAmount = amount.subtract(fee);

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setWithdrawSn("WD" + OrderNoUtils.generateOrderSn());
        withdrawal.setAmount(amount);
        withdrawal.setFee(fee);
        withdrawal.setActualAmount(actualAmount);
        withdrawal.setWithdrawType(withdrawType);
        withdrawal.setAccountNo(accountNo);
        withdrawal.setAccountName(accountName);
        withdrawal.setBankName(bankName);
        withdrawal.setStatus(0);
        withdrawal.setCreateTime(LocalDateTime.now());
        withdrawal.setUpdateTime(LocalDateTime.now());

        user.setBalance(user.getBalance().subtract(amount));
        userMapper.updateById(user);

        withdrawalMapper.insert(withdrawal);

        log.info("创建提现申请: userId={}, amount={}, fee={}", userId, amount, fee);
        return withdrawal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long withdrawalId, Boolean approved, String remark) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException("提现记录不存在");
        }

        if (withdrawal.getStatus() != 0) {
            throw new BusinessException("该提现申请状态异常");
        }

        if (approved) {
            withdrawal.setStatus(1);
            withdrawal.setAuditTime(LocalDateTime.now().toString());
        } else {
            rejectWithdrawal(withdrawalId, remark);
            return;
        }

        if (remark != null) {
            withdrawal.setRemark(remark);
        }

        withdrawal.setUpdateTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);

        log.info("审核提现申请: withdrawalId={}, approved={}", withdrawalId, approved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWithdrawal(Long withdrawalId, String transactionId) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException("提现记录不存在");
        }

        if (withdrawal.getStatus() != 1) {
            throw new BusinessException("该提现申请非待打款状态");
        }

        withdrawal.setStatus(2);
        withdrawal.setCompleteTime(LocalDateTime.now().toString());
        withdrawal.setRemark("打款成功");
        withdrawal.setUpdateTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);

        log.info("完成提现打款: withdrawalId={}, transactionId={}", withdrawalId, transactionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdrawal(Long withdrawalId, String remark) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException("提现记录不存在");
        }

        if (withdrawal.getStatus() != 0) {
            throw new BusinessException("该提现申请状态异常");
        }

        User user = userMapper.selectById(withdrawal.getUserId());
        if (user != null) {
            user.setBalance(user.getBalance().add(withdrawal.getAmount()));
            userMapper.updateById(user);
        }

        withdrawal.setStatus(3);
        withdrawal.setRemark(remark);
        withdrawal.setAuditTime(LocalDateTime.now().toString());
        withdrawal.setUpdateTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);

        log.info("拒绝提现申请: withdrawalId={}, refundAmount={}", withdrawalId, withdrawal.getAmount());
    }
}
