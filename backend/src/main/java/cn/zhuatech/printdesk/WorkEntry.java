// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 逐行实际报工及废品，成本保留在返工之后。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "work_entry")
public class WorkEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "line_id", nullable = false)
  public Long lineId;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "good", nullable = false)
  public int good = 0;

  @Column(name = "scrap", nullable = false)
  public int scrap = 0;

  @Column(name = "minutes", nullable = false)
  public int minutes = 0;

  @Column(name = "hourly_rate", nullable = false, precision = 18, scale = 2)
  public BigDecimal hourlyRate = BigDecimal.ZERO;

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "note", nullable = false, length = 600)
  public String note = "";

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
