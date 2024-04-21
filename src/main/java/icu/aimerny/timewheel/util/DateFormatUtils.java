package icu.aimerny.timewheel.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateFormatUtils {

    private static final SimpleDateFormat stdDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static String stdDateFormat(long mill) {
        return stdDateFormatter.format(mill);
    }

    public static Long localDateTime2Mills(LocalDateTime localDateTime) {
        // 将LocalDateTime对象转换为Instant对象
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        // 获取时间戳（毫秒数）
        return instant.toEpochMilli();
    }

    public static LocalDateTime timestamp2LocalDatetime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
