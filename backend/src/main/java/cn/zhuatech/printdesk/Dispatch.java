// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.time.*;

/** 实际分批交货明细和外部凭据，不伪造物流集成。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "dispatch")
public class Dispatch {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "reference", nullable = false, length = 160)
  public String reference = "";

  @Column(name = "method", nullable = false, length = 20)
  public String method = "PICKUP";

  @Column(name = "address", nullable = false, length = 600)
  public String address = "";

  @Column(name = "lines_json", nullable = false, columnDefinition = "longtext")
  public String lines = "";

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
