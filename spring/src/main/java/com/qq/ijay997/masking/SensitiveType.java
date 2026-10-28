package com.qq.ijay997.masking;

/**
 * 敏感数据类型及其对应的脱敏规则。
 *
 * @author ijay997
 */
public enum SensitiveType {

    /** 邮箱：保留本地部分首字符与域名 */
    EMAIL,

    /** 手机号：保留前 3 位与后 4 位 */
    PHONE,

    /** 姓名：保留首字符 */
    NAME,

    /** 身份证号：保留前 6 位与后 4 位 */
    ID_CARD,

    /** 银行卡号：保留前 4 位与后 4 位 */
    BANK_CARD,

    /** 地址：保留前 6 个字符 */
    ADDRESS
}
