package com.qq.ijay997.mapstruct.mapper;

import com.qq.ijay997.mapstruct.model.dto.CashPaymentDTO;
import com.qq.ijay997.mapstruct.model.dto.CreditCardPaymentDTO;
import com.qq.ijay997.mapstruct.model.dto.PaymentDTO;
import com.qq.ijay997.mapstruct.model.entity.CashPayment;
import com.qq.ijay997.mapstruct.model.entity.CreditCardPayment;
import com.qq.ijay997.mapstruct.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;

/**
 * 支付 Mapper —— 演示两大能力：
 * <pre>
 *  1. 抽象类形式的 Mapper（相比接口，可以放状态字段/复杂公共逻辑）
 *  2. {@code @SubclassMapping} 多态映射，完美匹配 JDK 21 sealed 类型体系
 *  3. 自定义业务方法中使用 switch 模式匹配（sealed 穷举，无需 default）
 * </pre>
 */
@Mapper
public abstract class PaymentMapper {

    public static final PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

    /**
     * 父类型 -&gt; 父 DTO：通过 {@link SubclassMapping} 声明每个密封子类的去向，
     * 生成的代码会用 instanceof 链在运行时选择具体映射方法。
     */
    @SubclassMapping(source = CreditCardPayment.class, target = CreditCardPaymentDTO.class)
    @SubclassMapping(source = CashPayment.class, target = CashPaymentDTO.class)
    public abstract PaymentDTO map(Payment payment);

    @Mappings({
            // constant：目标字段与源无关，恒为固定值（常用于类型标记字段）
            @Mapping(target = "type", constant = "CREDIT_CARD"),
            @Mapping(target = "maskedCardNo", source = "cardNo", qualifiedByName = "maskCard"),
            @Mapping(target = "paidAt", source = "paidAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    })
    public abstract CreditCardPaymentDTO map(CreditCardPayment payment);

    @Mappings({
            @Mapping(target = "type", constant = "CASH"),
            @Mapping(target = "cashReceived", source = "cashReceived", numberFormat = "0.00"),
            @Mapping(target = "change", source = "change", numberFormat = "0.00")
    })
    public abstract CashPaymentDTO map(CashPayment payment);

    /**
     * 业务描述方法（不参与映射，仅展示 JDK 21 switch 模式匹配）：
     * 因为 Payment 是 sealed 接口，编译器校验 switch 已穷举所有 permits 子类，
     * 新增支付方式而漏改这里会直接编译失败。
     */
    public String describe(Payment payment) {
        if (payment == null) {
            return "无支付信息";
        }
        return switch (payment) {
            case CreditCardPayment cc ->
                    "信用卡支付[" + cc.bank() + "] 尾号 " + tail(cc.cardNo());
            case CashPayment c ->
                    "现金支付 实收 " + c.cashReceived() + " 找零 " + c.change();
        };
    }

    /** 银行卡号脱敏：6222021234561234 -&gt; 6222********1234 */
    @Named("maskCard")
    String maskCard(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return cardNo;
        }
        return cardNo.substring(0, 4) + "********" + cardNo.substring(cardNo.length() - 4);
    }

    private String tail(String cardNo) {
        return cardNo == null || cardNo.length() < 4 ? "****" : cardNo.substring(cardNo.length() - 4);
    }
}
