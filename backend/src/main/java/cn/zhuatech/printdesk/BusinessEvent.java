// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.time.*;

/** 内容最小化业务履历和幂等请求指纹，不存凭据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "business_event")
public class BusinessEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "action", nullable = false, length = 40)
  public String action = "";

  @Column(name = "note", nullable = false, length = 1000)
  public String note = "";

  @Column(name = "request_id", nullable = false, length = 36)
  public String requestId = "";

  @Column(name = "fingerprint", nullable = false, length = 64)
  public String fingerprint = "";

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "metadata_json", nullable = false, length = 4000)
  public String metadata = "{}";
}
