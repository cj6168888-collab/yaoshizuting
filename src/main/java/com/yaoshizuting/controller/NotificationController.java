package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.NotificationCreateRequest;
import com.yaoshizuting.entity.Notification;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.NotificationMapper;
import com.yaoshizuting.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ApiResponse.success(Map.of("count", notificationMapper.countUnread(userId)));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = requireUserId(request);
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getDeleted, 0)
                        .orderByDesc(Notification::getCreateTime)
                        .orderByDesc(Notification::getId));
        return ApiResponse.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(HttpServletRequest request, @PathVariable Long id) {
        Long userId = requireUserId(request);
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || Integer.valueOf(1).equals(notification.getDeleted())) {
            throw new BusinessException(404, "通知不存在");
        }
        if (!Objects.equals(notification.getUserId(), userId)) {
            throw new BusinessException(403, "无权操作此通知");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = requireUserId(request);
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .eq(Notification::getDeleted, 0));
        return ApiResponse.success();
    }

    @GetMapping("/admin/list")
    public ApiResponse<Map<String, Object>> adminList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<Notification>()
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Notification::getTitle, keyword)
                                .or()
                                .like(Notification::getContent, keyword))
                        .eq(Notification::getType, "SYSTEM")
                        .eq(Notification::getDeleted, 0)
                        .orderByDesc(Notification::getCreateTime)
                        .orderByDesc(Notification::getId));
        return ApiResponse.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @PostMapping("/admin/broadcast")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> broadcast(@RequestBody NotificationCreateRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "通知标题不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(400, "通知内容不能为空");
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0));
        for (User user : users) {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setType("SYSTEM");
            notification.setTitle(request.getTitle().trim());
            notification.setContent(request.getContent().trim());
            notification.setRefType("announcement");
            notification.setIsRead(0);
            notification.setDeleted(0);
            notificationMapper.insert(notification);
        }
        return ApiResponse.success(Map.of("targetCount", users.size()));
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
}
