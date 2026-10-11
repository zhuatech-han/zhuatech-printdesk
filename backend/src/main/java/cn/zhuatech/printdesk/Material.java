// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 物料数量与移动平均成本，余额只能由收发流水更新。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "material")
public class Material {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "code", nullable = false, length = 60)
  public String code = "";

  @Column(name = "name", nullable = false, length = 120)
  public String name = "";

  @Column(name = "unit", nullable = false, length = 20)
  public String unit = "PIECE";

  @Column(name = "currency", nullable = false, length = 3)
  public String currency = "CNY";

  @Column(name = "stock", nullable = false, precision = 18, scale = 4)
  public BigDecimal stock = BigDecimal.ZERO;

  @Column(name = "inventory_value", nullable = false, precision = 18, scale = 4)
  public BigDecimal value = BigDecimal.ZERO;

  @Column(name = "cost", nullable = false, precision = 18, scale = 4)
  public BigDecimal cost = BigDecimal.ZERO;

  @Column(name = "enabled", nullable = false)
  public boolean enabled = true;

  @Column(name = "version", nullable = false)
  public long version = 1;
}
