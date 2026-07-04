package com.yaoshizuting.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartnerCampaignRequest {
    private String storeName;
    private String city;
    private String phone;
    private String address;
    private String title;
    private String subtitle;
    private BigDecimal price;
    private String benefitText;
    private String giftText;
    private String shareRewardText;
    private Integer quota;
    private String posterImage;
    private Integer status;
}
