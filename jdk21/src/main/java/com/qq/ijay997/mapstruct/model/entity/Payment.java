package com.qq.ijay997.mapstruct.model.entity;

/**
 * 支付方式（JDK 21 密封接口）。
 * <p>
 * {@code permits} 明确限定实现类，编译器可据此做 switch 模式匹配的穷举校验，
 * 也让 MapStruct 的 {@code @SubclassMapping} 拥有与类型体系一致的封闭集合。
 */
public sealed interface Payment permits CreditCardPayment, CashPayment {
}
