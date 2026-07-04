package com.yaoshizuting.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.CampaignSignupRequest;
import com.yaoshizuting.dto.PartnerCampaignRequest;
import com.yaoshizuting.entity.CampaignSignup;
import com.yaoshizuting.entity.PartnerCampaign;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.CampaignSignupMapper;
import com.yaoshizuting.mapper.PartnerCampaignMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.utils.OrderNoUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/campaign")
@RequiredArgsConstructor
public class PartnerCampaignController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PartnerCampaignMapper campaignMapper;
    private final CampaignSignupMapper signupMapper;
    private final UserMapper userMapper;

    @GetMapping("/my")
    public ApiResponse<List<PartnerCampaign>> myCampaigns(HttpServletRequest request) {
        Long userId = currentUserId(request);
        return ApiResponse.success(campaignMapper.selectList(new LambdaQueryWrapper<PartnerCampaign>()
                .eq(PartnerCampaign::getOwnerId, userId)
                .orderByDesc(PartnerCampaign::getCreateTime)));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody PartnerCampaignRequest body, HttpServletRequest request) {
        Long userId = currentUserId(request);
        PartnerCampaign campaign = new PartnerCampaign();
        campaign.setOwnerId(userId);
        campaign.setShareCode(generateUniqueShareCode());
        applyRequest(campaign, body);
        campaign.setSignedCount(0);
        campaignMapper.insert(campaign);
        return ApiResponse.success(enrichCampaign(campaign, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody PartnerCampaignRequest body,
                                                   HttpServletRequest request) {
        Long userId = currentUserId(request);
        PartnerCampaign campaign = campaignMapper.selectById(id);
        if (campaign == null || !userId.equals(campaign.getOwnerId())) {
            return ApiResponse.error(404, "活动不存在");
        }
        applyRequest(campaign, body);
        campaignMapper.updateById(campaign);
        return ApiResponse.success(enrichCampaign(campaign, request));
    }

    @GetMapping("/{id}/signups")
    public ApiResponse<List<CampaignSignup>> signups(@PathVariable Long id, HttpServletRequest request) {
        Long userId = currentUserId(request);
        PartnerCampaign campaign = campaignMapper.selectById(id);
        if (campaign == null || !userId.equals(campaign.getOwnerId())) {
            return ApiResponse.error(404, "活动不存在");
        }
        return ApiResponse.success(signupMapper.selectList(new LambdaQueryWrapper<CampaignSignup>()
                .eq(CampaignSignup::getCampaignId, id)
                .orderByDesc(CampaignSignup::getCreateTime)));
    }

    @GetMapping("/public/{shareCode}")
    public ApiResponse<Map<String, Object>> publicCampaign(@PathVariable String shareCode, HttpServletRequest request) {
        PartnerCampaign campaign = findPublicCampaign(shareCode);
        return ApiResponse.success(enrichCampaign(campaign, request));
    }

    @PostMapping("/public/{shareCode}/signup")
    public ApiResponse<CampaignSignup> signup(@PathVariable String shareCode,
                                              @RequestBody CampaignSignupRequest body,
                                              HttpServletRequest request) {
        PartnerCampaign campaign = findPublicCampaign(shareCode);
        if (campaign.getQuota() != null && campaign.getQuota() > 0
                && campaign.getSignedCount() != null && campaign.getSignedCount() >= campaign.getQuota()) {
            return ApiResponse.error(400, "活动名额已满");
        }
        if (body == null || !isValidMobile(body.getMobile())) {
            return ApiResponse.error(400, "请输入正确的手机号");
        }
        CampaignSignup existingSignup = signupMapper.selectOne(new LambdaQueryWrapper<CampaignSignup>()
                .eq(CampaignSignup::getCampaignId, campaign.getId())
                .eq(CampaignSignup::getMobile, body.getMobile())
                .last("LIMIT 1"));
        if (existingSignup != null) {
            return ApiResponse.success(existingSignup);
        }
        Long userId = optionalUserId(request);
        CampaignSignup signup = new CampaignSignup();
        signup.setCampaignId(campaign.getId());
        signup.setOwnerId(campaign.getOwnerId());
        signup.setUserId(userId);
        signup.setMobile(body.getMobile());
        signup.setNickname(StrUtil.blankToDefault(body.getNickname(), "活动客户"));
        signup.setOrderSn("ACT" + OrderNoUtils.generateOrderSn());
        signup.setAmount(campaign.getPrice() == null ? BigDecimal.ZERO : campaign.getPrice());
        signup.setVoucherCode("HX" + OrderNoUtils.generateOrderSn().substring(6));
        signup.setStatus(signup.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 0 : 1);
        signup.setRemark(body.getRemark());
        signupMapper.insert(signup);

        campaignMapper.update(null, new LambdaUpdateWrapper<PartnerCampaign>()
                .setSql("signed_count = signed_count + 1")
                .eq(PartnerCampaign::getId, campaign.getId()));
        return ApiResponse.success(signup);
    }

    private void applyRequest(PartnerCampaign campaign, PartnerCampaignRequest body) {
        if (body == null) {
            throw new BusinessException(400, "请填写活动信息");
        }
        if (StrUtil.isBlank(body.getStoreName())) {
            throw new BusinessException(400, "请填写门店名称");
        }
        if (StrUtil.isBlank(body.getTitle())) {
            throw new BusinessException(400, "请填写活动标题");
        }
        campaign.setStoreName(body.getStoreName());
        campaign.setCity(body.getCity());
        campaign.setPhone(body.getPhone());
        campaign.setAddress(body.getAddress());
        campaign.setTitle(body.getTitle());
        campaign.setSubtitle(body.getSubtitle());
        campaign.setPrice(body.getPrice() == null ? BigDecimal.ZERO : body.getPrice());
        campaign.setBenefitText(body.getBenefitText());
        campaign.setGiftText(body.getGiftText());
        campaign.setShareRewardText(body.getShareRewardText());
        campaign.setQuota(body.getQuota() == null ? 0 : Math.max(0, body.getQuota()));
        campaign.setPosterImage(body.getPosterImage());
        campaign.setStatus(body.getStatus() == null ? 1 : body.getStatus());
    }

    private PartnerCampaign findPublicCampaign(String shareCode) {
        PartnerCampaign campaign = campaignMapper.selectOne(new LambdaQueryWrapper<PartnerCampaign>()
                .eq(PartnerCampaign::getShareCode, shareCode)
                .eq(PartnerCampaign::getStatus, 1));
        if (campaign == null) {
            throw new BusinessException(404, "活动不存在或已下架");
        }
        return campaign;
    }

    private Map<String, Object> enrichCampaign(PartnerCampaign campaign, HttpServletRequest request) {
        User owner = userMapper.selectById(campaign.getOwnerId());
        String url = generateCampaignUrl(request, campaign.getShareCode());
        return Map.of(
                "campaign", campaign,
                "ownerNickname", owner == null ? "" : StrUtil.blankToDefault(owner.getNickname(), owner.getMobile()),
                "shareUrl", url
        );
    }

    private Long currentUserId(HttpServletRequest request) {
        Long userId = optionalUserId(request);
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private Long optionalUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof String text && StrUtil.isNotBlank(text)) {
            return Long.parseLong(text);
        }
        return null;
    }

    private boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^1[3-9]\\d{9}$");
    }

    private String generateUniqueShareCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode();
            Long count = campaignMapper.selectCount(new LambdaQueryWrapper<PartnerCampaign>()
                    .eq(PartnerCampaign::getShareCode, code));
            if (count == 0) {
                return code;
            }
        }
        throw new BusinessException(500, "生成活动码失败");
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private String generateCampaignUrl(HttpServletRequest request, String shareCode) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String scheme = StrUtil.blankToDefault(forwardedProto, request.getScheme());
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String host = StrUtil.blankToDefault(forwardedHost, request.getHeader("Host"));
        if (StrUtil.isBlank(host)) {
            host = request.getServerName();
        }
        String encoded = java.net.URLEncoder.encode(shareCode, StandardCharsets.UTF_8);
        return scheme + "://" + host + "/?campaign=" + encoded;
    }
}
