package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.PolicyConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PolicyConfigMapper extends BaseMapper<PolicyConfig> {

    @Select("SELECT * FROM gyt_config_policy WHERE config_key = #{key} AND deleted = 0")
    PolicyConfig selectByKey(@Param("key") String key);
}
