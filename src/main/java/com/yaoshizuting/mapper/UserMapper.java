package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM gyt_user WHERE mobile = #{mobile} AND deleted = 0")
    User selectByMobile(@Param("mobile") String mobile);

    @Select("SELECT * FROM gyt_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM gyt_user WHERE tree_path LIKE CONCAT(#{treePath}, '%') AND deleted = 0")
    List<User> selectByTreePath(@Param("treePath") String treePath);

    @Select("SELECT COUNT(*) FROM gyt_user WHERE parent_id = #{userId} AND deleted = 0")
    Integer countChildren(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(balance), 0) FROM gyt_user WHERE deleted = 0")
    BigDecimal sumBalance();

    @Select("SELECT COALESCE(SUM(total_earnings), 0) FROM gyt_user WHERE deleted = 0")
    BigDecimal sumTotalEarnings();

    @Select("""
            SELECT role, COUNT(*) AS count
            FROM gyt_user
            WHERE deleted = 0
            GROUP BY role
            ORDER BY role
            """)
    List<Map<String, Object>> countByRole();

    @Select("""
            SELECT DATE(create_time) AS date, COUNT(*) AS count
            FROM gyt_user
            WHERE create_time >= DATE_SUB(CURRENT_DATE, INTERVAL #{days} DAY)
              AND deleted = 0
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
            """)
    List<Map<String, Object>> dailyNewUsers(@Param("days") int days);
}
