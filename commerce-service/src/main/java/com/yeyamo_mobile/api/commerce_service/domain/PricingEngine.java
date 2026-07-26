package com.yeyamo_mobile.api.commerce_service.domain;
import java.math.*;import static com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.*;
public final class PricingEngine{
 public record Input(BigDecimal subtotal,BigDecimal tax,BigDecimal serviceFee,DiscountType discountType,BigDecimal discountValue,BigDecimal maximumDiscount,BigDecimal commissionPercent,BigDecimal commissionFixed,BigDecimal commissionMaximum,String currency){}
 public record Result(BigDecimal subtotal,BigDecimal discountAmount,BigDecimal taxAmount,BigDecimal serviceFee,BigDecimal commissionAmount,BigDecimal totalAmount,BigDecimal partnerNetAmount){}
 public Result calculate(Input i){int scale="XAF".equals(i.currency())||"XOF".equals(i.currency())?0:2;RoundingMode rm=RoundingMode.HALF_UP;BigDecimal sub=positive(i.subtotal());BigDecimal fee=positive(i.serviceFee());BigDecimal discount=BigDecimal.ZERO;
  if(i.discountType()==DiscountType.PERCENTAGE)discount=sub.multiply(positive(i.discountValue())).divide(new BigDecimal("100"),8,rm);
  if(i.discountType()==DiscountType.FIXED_AMOUNT)discount=positive(i.discountValue());
  if(i.discountType()==DiscountType.FREE_SERVICE_FEE)discount=fee;
  if(i.maximumDiscount()!=null)discount=discount.min(positive(i.maximumDiscount()));discount=discount.min(sub.add(fee));
  BigDecimal total=sub.add(positive(i.tax())).add(fee).subtract(discount).max(BigDecimal.ZERO);
  BigDecimal commission=sub.multiply(positive(i.commissionPercent())).divide(new BigDecimal("100"),8,rm).add(positive(i.commissionFixed())).min(sub);if(i.commissionMaximum()!=null)commission=commission.min(positive(i.commissionMaximum()));
  BigDecimal partnerDiscount=i.discountType()==DiscountType.FREE_SERVICE_FEE?BigDecimal.ZERO:discount.min(sub);
  return new Result(q(sub,scale,rm),q(discount,scale,rm),q(positive(i.tax()),scale,rm),q(fee,scale,rm),q(commission,scale,rm),q(total,scale,rm),q(sub.subtract(partnerDiscount).subtract(commission).max(BigDecimal.ZERO),scale,rm));}
 private BigDecimal positive(BigDecimal n){return n==null?BigDecimal.ZERO:n.max(BigDecimal.ZERO);}private BigDecimal q(BigDecimal n,int s,RoundingMode r){return n.setScale(s,r);}
}
