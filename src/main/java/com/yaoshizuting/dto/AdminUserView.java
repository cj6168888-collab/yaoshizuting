package com.yaoshizuting.dto;

import com.yaoshizuting.entity.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserView {

    private Long id;
    private String username;
    private String mobile;
    private String nickname;
    private String avatar;
    private Integer role;
    private Long parentId;
    private String parentMobile;
    private String parentNickname;
    private String treePath;
    private Integer agentCount;
    private Integer storeCount;
    private BigDecimal balance;
    private BigDecimal totalEarnings;
    private Integer status;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminUserView from(User user) {
        AdminUserView view = new AdminUserView();
        BeanUtils.copyProperties(user, view);
        return view;
    }
}
