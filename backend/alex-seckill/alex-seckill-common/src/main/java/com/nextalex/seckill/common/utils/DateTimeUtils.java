package com.nextalex.seckill.common.utils;

import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeUtils {

    /** 时区 */
    private static final ZoneId BUSINESS_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private DateTimeUtils() {}

    /**
     * 将业务本地时间转换为 Unix 毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(BUSINESS_ZONE_ID)
                .toInstant()
                .toEpochMilli();
    }

}
