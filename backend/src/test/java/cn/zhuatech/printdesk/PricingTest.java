// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import org.junit.jupiter.api.Test;

/** 独立报价精度和业务边界，禁止以浮点或静默舍入产生错价。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class PricingTest {
  @Test
  void costsAndSetupThenMarkup() {
    var l = new PrintLine();
    l.quantity = 10;
    l.usage = new BigDecimal("1");
    l.materialCost = new BigDecimal("2");
    l.printRate = new BigDecimal("1");
    l.finishRate = BigDecimal.ZERO;
    l.setup = new BigDecimal("5");
    l.markupPercent = new BigDecimal("20");
    assertEquals(new BigDecimal("42.00"), Pricing.line(l));
  }

  @Test
  void areaRoundsUpToMaterialPrecision() {
    assertEquals(new BigDecimal("0.6997"), Pricing.area(1234, 567));
  }

  @Test
  void exactSquareMetre() {
    assertEquals(new BigDecimal("1.0000"), Pricing.area(1000, 1000));
  }

  @Test
  void missingDimensionsRejected() {
    assertThrows(Problem.class, () -> Pricing.area(0, 100));
  }

  @Test
  void excessMoneyPrecisionRejected() {
    assertThrows(Problem.class, () -> Pricing.money("1.001"));
  }

  @Test
  void negativeMoneyRejected() {
    assertThrows(Problem.class, () -> Pricing.money("-1"));
  }

  @Test
  void nonNumericRejected() {
    assertThrows(Problem.class, () -> Pricing.money("NaN"));
  }

  @Test
  void discountBeforeTax() {
    assertEquals(
        new BigDecimal("99.00"),
        Pricing.total(new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("10")));
  }

  @Test
  void discountCannotExceedQuote() {
    assertThrows(
        Problem.class, () -> Pricing.total(BigDecimal.ONE, new BigDecimal("2"), BigDecimal.ZERO));
  }

  @Test
  void formulaCsvEscaped() {
    assertTrue(Rules.csv(" \t=1+1").startsWith("\"'"));
  }

  @Test
  void integersNotTruncated() {
    assertThrows(Problem.class, () -> Rules.integer("1.1", 1, 10));
  }

  @Test
  void hashStable() {
    assertEquals(Pricing.hash(new byte[] {1, 2}), Pricing.hash(new byte[] {1, 2}));
  }
}
