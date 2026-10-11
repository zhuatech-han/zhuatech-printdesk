// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 外部收款与原收款退款，正额存储并按类型计算净额。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "money_entry")
public class MoneyEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "source_id", nullable = true)
  public Long sourceId;

  @Column(name = "kind", nullable = false, length = 20)
  public String kind = "RECEIVE";

  @Column(name = "reference", nullable = false, length = 160)
  public String reference = "";

  @Column(name = "note", nullable = false, length = 600)
  public String note = "";

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
