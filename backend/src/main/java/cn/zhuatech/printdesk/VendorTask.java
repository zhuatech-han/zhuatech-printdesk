// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 委外加工约定与部分收回，接收数量决定已确认成本。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "vendor_task")
public class VendorTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "line_id", nullable = false)
  public Long lineId;

  @Column(name = "supplier_id", nullable = false)
  public Long supplierId;

  @Column(name = "supplier_name", nullable = false, length = 120)
  public String supplierName = "";

  @Column(name = "description", nullable = false, length = 600)
  public String description = "";

  @Column(name = "reference", nullable = false, length = 160)
  public String reference = "";

  @Column(name = "state", nullable = false, length = 20)
  public String state = "REQUESTED";

  @Column(name = "quantity", nullable = false)
  public int quantity = 0;

  @Column(name = "received", nullable = false)
  public int received = 0;

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "recognized", nullable = false, precision = 18, scale = 2)
  public BigDecimal recognized = BigDecimal.ZERO;

  @Column(name = "due_date", nullable = false)
  public LocalDate dueDate;
}
