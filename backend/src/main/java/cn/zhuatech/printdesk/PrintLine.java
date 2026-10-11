// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 当前报价行、物料快照及良品质检交货数量，保持业务守恒。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "print_line")
public class PrintLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "material_id", nullable = false)
  public Long materialId;

  @Column(name = "name", nullable = false, length = 160)
  public String name = "";

  @Column(name = "unit", nullable = false, length = 20)
  public String unit = "";

  @Column(name = "material_name", nullable = false, length = 120)
  public String materialName = "";

  @Column(name = "quantity", nullable = false)
  public int quantity = 0;

  @Column(name = "width_mm", nullable = false)
  public int widthMm = 0;

  @Column(name = "height_mm", nullable = false)
  public int heightMm = 0;

  @Column(name = "material_usage", nullable = false, precision = 18, scale = 4)
  public BigDecimal usage = BigDecimal.ZERO;

  @Column(name = "material_cost", nullable = false, precision = 18, scale = 4)
  public BigDecimal materialCost = BigDecimal.ZERO;

  @Column(name = "print_rate", nullable = false, precision = 18, scale = 2)
  public BigDecimal printRate = BigDecimal.ZERO;

  @Column(name = "finish_rate", nullable = false, precision = 18, scale = 2)
  public BigDecimal finishRate = BigDecimal.ZERO;

  @Column(name = "setup", nullable = false, precision = 18, scale = 2)
  public BigDecimal setup = BigDecimal.ZERO;

  @Column(name = "markup_percent", nullable = false, precision = 10, scale = 4)
  public BigDecimal markupPercent = BigDecimal.ZERO;

  @Column(name = "quoted", nullable = false, precision = 18, scale = 2)
  public BigDecimal quoted = BigDecimal.ZERO;

  @Column(name = "good", nullable = false)
  public int good = 0;

  @Column(name = "scrap", nullable = false)
  public int scrap = 0;

  @Column(name = "accepted", nullable = false)
  public int accepted = 0;

  @Column(name = "shipped", nullable = false)
  public int shipped = 0;
}
