package com.qq.ijay997.masking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 脱敏规则单元测试：覆盖各类型的正常值与异常格式降级。
 *
 * @author ijay997
 */
class MaskRulesTest {

    @Test
    @DisplayName("邮箱：保留首字符与域名；缺失 @ 或 @ 位置非法时降级为 ***")
    void shouldMaskEmail() {
        assertAll(
                () -> assertEquals("z***@example.com", MaskRules.mask(SensitiveType.EMAIL, "zhangsan@example.com")),
                () -> assertEquals("*@example.com", MaskRules.mask(SensitiveType.EMAIL, "a@example.com")),
                () -> assertEquals("***", MaskRules.mask(SensitiveType.EMAIL, "no-at-sign")),
                () -> assertEquals("***", MaskRules.mask(SensitiveType.EMAIL, "@example.com")),
                () -> assertEquals("***", MaskRules.mask(SensitiveType.EMAIL, "abc@"))
        );
    }

    @Test
    @DisplayName("手机号：保留前 3 位与后 4 位；长度不足 7 位降级为 ***")
    void shouldMaskPhone() {
        assertAll(
                () -> assertEquals("138****5678", MaskRules.mask(SensitiveType.PHONE, "13812345678")),
                () -> assertEquals("***", MaskRules.mask(SensitiveType.PHONE, "123456")),
                () -> assertEquals("123****4567", MaskRules.mask(SensitiveType.PHONE, "1234567"))
        );
    }

    @Test
    @DisplayName("姓名：保留首字符，其余全部遮蔽")
    void shouldMaskName() {
        assertAll(
                () -> assertEquals("张*", MaskRules.mask(SensitiveType.NAME, "张三")),
                () -> assertEquals("*", MaskRules.mask(SensitiveType.NAME, "张")),
                () -> assertEquals("欧***", MaskRules.mask(SensitiveType.NAME, "欧阳娜娜"))
        );
    }

    @Test
    @DisplayName("身份证：保留前 6 位与后 4 位；过短降级为 ***")
    void shouldMaskIdCard() {
        assertAll(
                () -> assertEquals("110101********1234",
                        MaskRules.mask(SensitiveType.ID_CARD, "110101199001011234")),
                () -> assertEquals("***", MaskRules.mask(SensitiveType.ID_CARD, "12345"))
        );
    }

    @Test
    @DisplayName("银行卡：保留前 4 位与后 4 位")
    void shouldMaskBankCard() {
        assertEquals("6222***********0123",
                MaskRules.mask(SensitiveType.BANK_CARD, "6222021234567890123"));
    }

    @Test
    @DisplayName("地址：保留前 6 个字符；过短降级为 ***")
    void shouldMaskAddress() {
        assertAll(
                () -> assertEquals("北京市朝阳区***", MaskRules.mask(SensitiveType.ADDRESS, "北京市朝阳区建国路88号")),
                () -> assertEquals("***", MaskRules.mask(SensitiveType.ADDRESS, "北京"))
        );
    }

    @Test
    @DisplayName("边界：null、空串、类型为 null 原样返回")
    void shouldKeepNullAndEmpty() {
        assertAll(
                () -> assertNull(MaskRules.mask(SensitiveType.EMAIL, null)),
                () -> assertEquals("", MaskRules.mask(SensitiveType.EMAIL, "")),
                () -> assertEquals("zhangsan@example.com", MaskRules.mask(null, "zhangsan@example.com"))
        );
    }
}
