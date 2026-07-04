package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.SupportMessageRequest;
import com.yaoshizuting.dto.SupportMessageView;
import com.yaoshizuting.entity.SupportMessage;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.SupportMessageMapper;
import com.yaoshizuting.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportMessageMapper supportMessageMapper;
    private final UserMapper userMapper;

    @GetMapping("/messages")
    public ApiResponse<List<SupportMessageView>> myMessages(HttpServletRequest request) {
        Long userId = requireUserId(request);
        List<SupportMessage> messages = supportMessageMapper.selectList(new LambdaQueryWrapper<SupportMessage>()
                .eq(SupportMessage::getUserId, userId)
                .eq(SupportMessage::getDeleted, 0)
                .orderByAsc(SupportMessage::getCreateTime)
                .orderByAsc(SupportMessage::getId));
        return ApiResponse.success(enrich(messages));
    }

    @PostMapping("/messages")
    public ApiResponse<SupportMessageView> sendMessage(HttpServletRequest request, @RequestBody SupportMessageRequest body) {
        Long userId = requireUserId(request);
        SupportMessage message = buildMessage(userId, userId, "USER", body);
        supportMessageMapper.insert(message);
        return ApiResponse.success(enrich(List.of(message)).get(0));
    }

    @GetMapping("/admin/messages")
    public ApiResponse<Map<String, Object>> adminMessages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<SupportMessage> wrapper = new LambdaQueryWrapper<SupportMessage>()
                .eq(userId != null, SupportMessage::getUserId, userId)
                .like(keyword != null && !keyword.isBlank(), SupportMessage::getContent, keyword)
                .eq(SupportMessage::getDeleted, 0)
                .orderByDesc(SupportMessage::getCreateTime)
                .orderByDesc(SupportMessage::getId);
        Page<SupportMessage> result = supportMessageMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return ApiResponse.success(Map.of(
                "records", enrich(result.getRecords()),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @PostMapping("/admin/messages/{userId}/reply")
    public ApiResponse<SupportMessageView> reply(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestBody SupportMessageRequest body) {

        Long staffId = requireUserId(request);
        User target = userMapper.selectById(userId);
        if (target == null || Integer.valueOf(1).equals(target.getDeleted())) {
            throw new BusinessException(404, "会员不存在");
        }
        SupportMessage message = buildMessage(userId, staffId, "STAFF", body);
        supportMessageMapper.insert(message);
        return ApiResponse.success(enrich(List.of(message)).get(0));
    }

    private SupportMessage buildMessage(Long userId, Long senderUserId, String senderType, SupportMessageRequest body) {
        if (body == null || body.getContent() == null || body.getContent().trim().isEmpty()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        SupportMessage message = new SupportMessage();
        message.setUserId(userId);
        message.setSenderUserId(senderUserId);
        message.setSenderType(senderType);
        message.setMessageType(normalizeMessageType(body.getMessageType()));
        message.setContent(body.getContent().trim());
        message.setIsRead(0);
        message.setDeleted(0);
        return message;
    }

    private List<SupportMessageView> enrich(List<SupportMessage> messages) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (SupportMessage message : messages) {
            if (message.getUserId() != null) {
                userIds.add(message.getUserId());
            }
            if (message.getSenderUserId() != null) {
                userIds.add(message.getSenderUserId());
            }
        }
        Map<Long, User> users = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));

        return messages.stream().map(message -> {
            SupportMessageView view = SupportMessageView.from(message);
            User owner = users.get(message.getUserId());
            User sender = users.get(message.getSenderUserId());
            if (owner != null) {
                view.setUserMobile(owner.getMobile());
                view.setUserNickname(owner.getNickname());
            }
            if (sender != null) {
                view.setSenderMobile(sender.getMobile());
                view.setSenderNickname(sender.getNickname());
            }
            return view;
        }).toList();
    }

    private Long requireUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (userId instanceof Long value) {
            return value;
        }
        return Long.parseLong(Objects.toString(userId));
    }

    private String normalizeMessageType(String type) {
        if (type == null || type.isBlank()) {
            return "TEXT";
        }
        String value = type.trim().toUpperCase();
        return "TEXT".equals(value) ? value : "TEXT";
    }
}
