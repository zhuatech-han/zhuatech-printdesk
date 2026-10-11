// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** 每次实际委外收回的独立数量、成本增量与凭据，不能覆盖累计历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "vendor_receipt")
public class VendorReceipt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "task_id", nullable = false)
  public Long taskId;

  @Column(name = "quantity", nullable = false)
  public int quantity;

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  public BigDecimal amount;

  @Column(name = "reference", nullable = false, length = 160)
  public String reference;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
