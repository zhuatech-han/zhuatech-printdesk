// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.time.*;

/** 委外供应商目录，不调用外部供应商服务。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "supplier")
public class Supplier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "name", nullable = false, length = 120)
  public String name = "";

  @Column(name = "contact", nullable = false, length = 160)
  public String contact = "";

  @Column(name = "enabled", nullable = false)
  public boolean enabled = true;

  @Column(name = "version", nullable = false)
  public long version = 1;
}
