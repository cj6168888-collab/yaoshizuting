package com.yaoshizuting.dto;

import com.yaoshizuting.entity.SupportMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupportMessageView {
    private Long id;
    private Long userId;
    private Long senderUserId;
    private String senderType;
    private String messageType;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
    private String userMobile;
    private String userNickname;
    private String senderMobile;
    private String senderNickname;

    public static SupportMessageView from(SupportMessage message) {
        SupportMessageView view = new SupportMessageView();
        view.setId(message.getId());
        view.setUserId(message.getUserId());
        view.setSenderUserId(message.getSenderUserId());
        view.setSenderType(message.getSenderType());
        view.setMessageType(message.getMessageType());
        view.setContent(message.getContent());
        view.setIsRead(message.getIsRead());
        view.setCreateTime(message.getCreateTime());
        return view;
    }
}
