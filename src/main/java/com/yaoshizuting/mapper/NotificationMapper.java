package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM gyt_notification WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    Long countUnread(@Param("userId") Long userId);
}
