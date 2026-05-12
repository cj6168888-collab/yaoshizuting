package com.yaoshizuting.service;

import com.yaoshizuting.dto.WalletResponse;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.User;

public interface ProfitService {

    void processJoinStoreProfit(Order order);

    void processJoinAgentProfit(Order order);

    void processJoinPartnerProfit(Order order);

    WalletResponse getWalletInfo(Long userId);
    
    void processPartnerRecruitAgentProfit(User partner, User newAgent);
}
