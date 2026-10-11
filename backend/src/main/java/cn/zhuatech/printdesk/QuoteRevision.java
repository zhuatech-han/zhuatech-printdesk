// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.time.*;

/** 报价发布版本永久快照，后续修改不覆盖。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "quote_revision")
public class QuoteRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "number", nullable = false)
  public int number = 0;

  @Column(name = "snapshot", nullable = false, columnDefinition = "longtext")
  public String snapshot = "";

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "reviewer_id")
  public Long reviewerId;

  @Column(name = "reviewed_at")
  public java.time.Instant reviewedAt;

  @Column(name = "decision", length = 20)
  public String decision;
}
