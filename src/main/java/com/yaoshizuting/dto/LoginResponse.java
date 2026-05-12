package com.yaoshizuting.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private Integer role;
    private String nickname;
    private String mobile;
    private String avatar;
    private Long parentId;
    private String treePath;
}
