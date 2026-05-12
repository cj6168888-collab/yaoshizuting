package com.yaoshizuting.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderNoUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateOrderSn() {
        return "ORD" + LocalDateTime.now().format(DATE_TIME_FORMATTER) + RandomUtil.randomNumbers(6);
    }

    public static String generateWithdrawSn() {
        return "WTH" + LocalDateTime.now().format(DATE_TIME_FORMATTER) + RandomUtil.randomNumbers(6);
    }

    public static String generateSimpleUUID() {
        return IdUtil.fastSimpleUUID();
    }
}
