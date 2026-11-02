package com.qq.ijay997.masking;

import org.apache.commons.lang3.StringUtils;

/**
 * 纯函数脱敏规则，供 JSON 序列化器与文件处理器共用。
 * <p>
 * 所有规则遇到 null 原样返回；格式不符合预期时统一降级为 {@code ***}，不抛异常。
 *
 * @author ijay997
 */
public final class MaskRules {

    private static final String FALLBACK = "***";

    private MaskRules() {
    }

    /**
     * 按类型脱敏。
     *
     * @param type  脱敏类型
     * @param value 原始值
     * @return 脱敏后的值；null 原样返回
     */
    public static String mask(SensitiveType type, String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return value;
        }
        if (type == null) {
            return value;
        }
        switch (type) {
            case EMAIL:
                return maskEmail(value);
            case PHONE:
                return maskPhone(value);
            case NAME:
                return maskName(value);
            case ID_CARD:
                return maskKeepHeadTail(value, 6, 4);
            case BANK_CARD:
                return maskKeepHeadTail(value, 4, 4);
            case ADDRESS:
                return maskAddress(value);
            default:
                return FALLBACK;
        }
    }

    /**
     * 邮箱：保留本地部分首字符与完整域名，例如 zhangsan@example.com -&gt; z***@example.com。
     */
    private static String maskEmail(String value) {
        int at = value.lastIndexOf('@');
        if (at <= 0 || at == value.length() - 1) {
            return FALLBACK;
        }
        String local = value.substring(0, at);
        String domain = value.substring(at + 1);
        if (local.length() <= 1) {
            return "*@" + domain;
        }
        return local.charAt(0) + "***@" + domain;
    }

    /**
     * 手机号：保留前 3 位与后 4 位；长度不足 7 位时全部遮蔽。
     */
    private static String maskPhone(String value) {
        if (value.length() < 7) {
            return FALLBACK;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    /**
     * 姓名：保留首字符，其余全部遮蔽。
     */
    private static String maskName(String value) {
        if (value.length() <= 1) {
            return "*";
        }
        StringBuilder builder = new StringBuilder(value.length());
        builder.append(value.charAt(0));
        for (int i = 1; i < value.length(); i++) {
            builder.append('*');
        }
        return builder.toString();
    }

    /**
     * 保留头尾的通用规则，如身份证、银行卡。
     */
    private static String maskKeepHeadTail(String value, int head, int tail) {
        if (value.length() < head + tail + 1) {
            return FALLBACK;
        }
        StringBuilder builder = new StringBuilder(value.length());
        builder.append(value, 0, head);
        for (int i = 0; i < value.length() - head - tail; i++) {
            builder.append('*');
        }
        builder.append(value, value.length() - tail, value.length());
        return builder.toString();
    }

    /**
     * 地址：保留前 6 个字符。
     */
    private static String maskAddress(String value) {
        if (value.length() <= 6) {
            return FALLBACK;
        }
        return value.substring(0, 6) + "***";
    }
}
