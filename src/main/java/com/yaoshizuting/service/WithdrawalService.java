package com.yaoshizuting.service;

import com.yaoshizuting.entity.Withdrawal;

import java.math.BigDecimal;

public interface WithdrawalService {

    Withdrawal createWithdrawal(Long userId, BigDecimal amount, Integer withdrawType, String accountNo, String accountName, String bankName);

    void approveWithdrawal(Long withdrawalId, Boolean approved, String remark);

    void completeWithdrawal(Long withdrawalId, String transactionId);

    void rejectWithdrawal(Long withdrawalId, String remark);
}
