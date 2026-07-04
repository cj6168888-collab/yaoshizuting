package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.ProfitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProfitLogMapper extends BaseMapper<ProfitLog> {

    @Select("SELECT * FROM gyt_profit_log WHERE order_sn = #{orderSn} AND type = #{type} AND receiver_id = #{receiverId} AND deleted = 0")
    ProfitLog selectByUniqueKey(@Param("orderSn") String orderSn, @Param("type") String type, @Param("receiverId") Long receiverId);

    @Select("SELECT * FROM gyt_profit_log WHERE receiver_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<ProfitLog> selectByReceiverId(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM gyt_profit_log WHERE receiver_id = #{userId} AND status = 1 AND deleted = 0")
    BigDecimal sumByReceiverId(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM gyt_profit_log WHERE status = 1 AND DATE(create_time) = CURRENT_DATE AND deleted = 0")
    BigDecimal sumTodayProfit();

    @Select("""
            SELECT type, COALESCE(SUM(amount), 0) AS amount, COUNT(*) AS count
            FROM gyt_profit_log
            WHERE status = 1 AND deleted = 0
            GROUP BY type
            ORDER BY amount DESC
            """)
    List<Map<String, Object>> groupByType();
}
