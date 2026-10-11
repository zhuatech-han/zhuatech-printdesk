// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 实际收发存流水；退料逐笔引用领料成本，订单成本可追溯。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "stock_txn")
public class StockTxn {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "material_id", nullable = false)
  public Long materialId;

  @Column(name = "order_id", nullable = true)
  public Long orderId;

  @Column(name = "line_id", nullable = true)
  public Long lineId;

  @Column(name = "source_id", nullable = true)
  public Long sourceId;

  @Column(name = "kind", nullable = false, length = 20)
  public String kind = "";

  @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
  public BigDecimal quantity = BigDecimal.ZERO;

  @Column(name = "cost", nullable = false, precision = 18, scale = 4)
  public BigDecimal cost = BigDecimal.ZERO;

  @Column(name = "amount", nullable = false, precision = 18, scale = 4)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "reference", nullable = false, length = 160)
  public String reference = "";

  @Column(name = "note", nullable = false, length = 600)
  public String note = "";

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
