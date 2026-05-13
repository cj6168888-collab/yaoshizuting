package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.annotation.AuditLog;
import com.yaoshizuting.dto.AdminUserView;
import com.yaoshizuting.dto.AdminUserUpdateRequest;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.TeamService;
import com.yaoshizuting.utils.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final TeamService teamService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<User> wrapper = buildListWrapper(keyword, role, status);

        Page<User> result = userMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        List<AdminUserView> records = enrichParents(result.getRecords());
        return ApiResponse.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status) {

        List<AdminUserView> records = enrichParents(userMapper.selectList(buildListWrapper(keyword, role, status).last("LIMIT 5000")));
        byte[] csv = CsvExportUtils.toCsv(
                List.of("会员ID", "手机号", "昵称", "角色", "状态", "上级ID", "上级手机号", "上级昵称", "代理数", "店铺数", "余额", "累计收益", "注册时间"),
                records.stream()
                        .map(item -> List.of(
                                item.getId(),
                                value(item.getMobile()),
                                value(item.getNickname()),
                                value(item.getRole()),
                                value(item.getStatus()),
                                value(item.getParentId()),
                                value(item.getParentMobile()),
                                value(item.getParentNickname()),
                                value(item.getAgentCount()),
                                value(item.getStoreCount()),
                                value(item.getBalance()),
                                value(item.getTotalEarnings()),
                                value(item.getCreateTime())
                        ))
                        .toList());

        return csvResponse("users.csv", csv);
    }

    @PutMapping("/{id}")
    @AuditLog(module = "人员管理", operation = "更新会员信息")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (request.getRole() != null) {
            validateRole(request.getRole());
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException(400, "账号状态无效");
            }
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);
        teamService.evictTeamTreeCaches(user);
        return ApiResponse.success(user);
    }

    private void validateRole(Integer role) {
        try {
            UserRole.fromCode(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "用户角色无效");
        }
    }

    private LambdaQueryWrapper<User> buildListWrapper(String keyword, Integer role, Integer status) {
        return new LambdaQueryWrapper<User>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getMobile, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .eq(role != null, User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);
    }

    private List<AdminUserView> enrichParents(List<User> users) {
        Set<Long> parentIds = users.stream()
                .map(User::getParentId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());

        Map<Long, User> parents = parentIds.isEmpty()
                ? Map.of()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, parentIds)).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        return users.stream()
                .map(user -> {
                    AdminUserView view = AdminUserView.from(user);
                    User parent = parents.get(user.getParentId());
                    if (parent != null) {
                        view.setParentMobile(parent.getMobile());
                        view.setParentNickname(parent.getNickname());
                    }
                    return view;
                })
                .toList();
    }

    private ResponseEntity<byte[]> csvResponse(String filename, byte[] csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
