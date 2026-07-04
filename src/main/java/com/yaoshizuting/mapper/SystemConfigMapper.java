package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM gyt_system_config WHERE config_key = #{key} AND deleted = 0")
    SystemConfig selectByKey(@Param("key") String key);
}
