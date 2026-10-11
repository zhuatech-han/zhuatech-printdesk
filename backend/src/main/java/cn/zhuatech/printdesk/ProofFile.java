// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.time.*;

/** 原始资料和样稿版本，二进制私有授权下载，不作印前检测。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "proof_file")
public class ProofFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "kind", nullable = false, length = 20)
  public String kind = "PROOF";

  @Column(name = "filename", nullable = false, length = 160)
  public String filename = "";

  @Column(name = "media_type", nullable = false, length = 80)
  public String mediaType = "";

  @Column(name = "sha256", nullable = false, length = 64)
  public String sha256 = "";

  @Column(name = "status", nullable = false, length = 20)
  public String status = "PENDING";

  @Column(name = "feedback", nullable = false, length = 1000)
  public String feedback = "";

  @Column(name = "size", nullable = false)
  public int size = 0;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Lob
  @Column(name = "content", nullable = false, columnDefinition = "longblob")
  public byte[] content;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "reviewer_id")
  public Long reviewerId;

  @Column(name = "reviewed_at")
  public java.time.Instant reviewedAt;
}
