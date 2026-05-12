package com.yaoshizuting.dto;

import lombok.Data;

@Data
public class TeamNodeDTO {
    private Long userId;
    private Integer role;
    private Long parentId;
    private String treePath;
    private String nickname;
    private String mobile;
}
