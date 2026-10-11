// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 订单商业快照、责任人与明确状态；版本由写锁保护。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "print_order")
public class PrintOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "customer_id", nullable = false)
  public Long customerId;

  @Column(name = "owner_id", nullable = false)
  public Long ownerId;

  @Column(name = "assignee_id", nullable = true)
  public Long assigneeId;

  @Column(name = "title", nullable = false, length = 160)
  public String title = "";

  @Column(name = "customer_name", nullable = false, length = 120)
  public String customerName = "";

  @Column(name = "category", nullable = false, length = 60)
  public String category = "";

  @Column(name = "currency", nullable = false, length = 3)
  public String currency = "CNY";

  @Column(name = "state", nullable = false, length = 30)
  public String state = "DRAFT";

  @Column(name = "instructions", nullable = false, length = 2000)
  public String instructions = "";

  @Column(name = "due_date", nullable = false)
  public LocalDate dueDate;

  @Column(name = "discount", nullable = false, precision = 18, scale = 2)
  public BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "tax_percent", nullable = false, precision = 10, scale = 4)
  public BigDecimal taxPercent = BigDecimal.ZERO;

  @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
  public BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "total", nullable = false, precision = 18, scale = 2)
  public BigDecimal total = BigDecimal.ZERO;

  @Column(name = "revision", nullable = false)
  public int revision = 0;

  @Column(name = "version", nullable = false)
  public long version = 1;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
