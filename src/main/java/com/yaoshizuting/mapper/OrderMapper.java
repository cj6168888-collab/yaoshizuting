package com.yaoshizuting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaoshizuting.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM gyt_order WHERE order_sn = #{orderSn} AND deleted = 0")
    Order selectByOrderSn(@Param("orderSn") String orderSn);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM gyt_order WHERE status IN (1, 2, 3) AND deleted = 0")
    BigDecimal sumTotalRevenue();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM gyt_order WHERE status IN (1, 2, 3) AND DATE(create_time) = CURRENT_DATE AND deleted = 0")
    BigDecimal sumTodayRevenue();

    @Select("""
            SELECT DATE(create_time) AS date, COALESCE(SUM(amount), 0) AS amount
            FROM gyt_order
            WHERE status IN (1, 2, 3)
              AND create_time >= DATE_SUB(CURRENT_DATE, INTERVAL #{days} DAY)
              AND deleted = 0
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
            """)
    List<Map<String, Object>> dailyRevenue(@Param("days") int days);

    @Select("""
            SELECT o.order_sn AS orderSn,
                   o.user_id AS userId,
                   u.mobile AS userMobile,
                   u.nickname AS userNickname,
                   o.order_type AS orderType,
                   o.amount AS amount,
                   o.status AS status,
                   o.create_time AS createTime
            FROM gyt_order o
            LEFT JOIN gyt_user u ON u.id = o.user_id AND u.deleted = 0
            WHERE o.deleted = 0
            ORDER BY o.create_time DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> recentOrders(@Param("limit") int limit);
}
