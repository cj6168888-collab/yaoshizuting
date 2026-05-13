package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {

    @Select("SELECT COALESCE(SUM(actual_amount), 0) FROM gyt_withdrawal WHERE user_id = #{userId} AND status = 2 AND deleted = 0")
    BigDecimal sumByUserId(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM gyt_withdrawal WHERE user_id = #{userId} AND status = #{status} AND deleted = 0")
    BigDecimal sumAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT COALESCE(SUM(actual_amount), 0) FROM gyt_withdrawal WHERE status = #{status} AND deleted = 0")
    BigDecimal sumByStatus(@Param("status") Integer status);
}
