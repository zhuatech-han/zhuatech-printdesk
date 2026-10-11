// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import java.math.*;
import java.util.*;

/** 明确十进制成本加成报价，面积向上取四位，行金额最后舍入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public final class Pricing {
  /** 拒绝超精度和超范围，不接受NaN或隐式金额舍入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static BigDecimal decimal(Object v, int scale, BigDecimal min, BigDecimal max) {
    try {
      var n = new BigDecimal(Objects.toString(v, ""));
      n = n.setScale(scale, RoundingMode.UNNECESSARY);
      if (n.compareTo(min) < 0 || n.compareTo(max) > 0 || n.precision() > 18)
        throw new IllegalArgumentException();
      return n;
    } catch (Exception e) {
      throw new Problem(400, "DECIMAL_INVALID");
    }
  }

  /** 两位非负金额，业务总额仍独立校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static BigDecimal money(Object v) {
    return decimal(v, 2, BigDecimal.ZERO, new BigDecimal("9999999999.99"));
  }

  /** 比率按百分数输入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static BigDecimal percent(Object v, int max) {
    return decimal(v, 4, BigDecimal.ZERO, BigDecimal.valueOf(max));
  }

  /** 米平方按外包络算耗材；不声称最优排样。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static BigDecimal area(int width, int height) {
    Rules.check(
        width > 0 && height > 0 && width <= 100000 && height <= 100000, "DIMENSIONS_INVALID");
    return BigDecimal.valueOf(width)
        .multiply(BigDecimal.valueOf(height))
        .divide(new BigDecimal("1000000"), 4, RoundingMode.CEILING);
  }

  /** 材料成本、加工、后道与开机费之和按加成计算售价。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static BigDecimal line(PrintLine l) {
    var base =
        l.materialCost
            .multiply(l.usage)
            .add(l.printRate)
            .add(l.finishRate)
            .multiply(BigDecimal.valueOf(l.quantity))
            .add(l.setup);
    return base.multiply(BigDecimal.ONE.add(l.markupPercent.movePointLeft(2)))
        .setScale(2, RoundingMode.HALF_UP);
  }

  /** 折扣先扣除、手工税率后计算，不提供税务开票。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static BigDecimal total(BigDecimal subtotal, BigDecimal discount, BigDecimal tax) {
    Rules.check(discount.compareTo(subtotal) <= 0, "DISCOUNT_EXCEEDS_TOTAL");
    var n =
        subtotal
            .subtract(discount)
            .multiply(BigDecimal.ONE.add(tax.movePointLeft(2)))
            .setScale(2, RoundingMode.HALF_UP);
    Rules.check(n.compareTo(new BigDecimal("9999999999.99")) <= 0, "AMOUNT_LIMIT");
    return n;
  }

  /** 摘要用于文件及幂等请求，二进制不写日志。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String hash(byte[] b) {
    try {
      return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));
    } catch (Exception e) {
      throw new IllegalStateException();
    }
  }

  private Pricing() {}
}
