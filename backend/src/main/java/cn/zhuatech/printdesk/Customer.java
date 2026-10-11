// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import jakarta.persistence.*;
import java.time.*;

/** 客户及唯一登录绑定，停用不删除历史。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "customer")
public class Customer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "account_id", nullable = true)
  public Long accountId;

  @Column(name = "name", nullable = false, length = 120)
  public String name = "";

  @Column(name = "contact", nullable = false, length = 120)
  public String contact = "";

  @Column(name = "email", nullable = false, length = 160)
  public String email = "";

  @Column(name = "address", nullable = false, length = 600)
  public String address = "";

  @Column(name = "enabled", nullable = false)
  public boolean enabled = true;

  @Column(name = "version", nullable = false)
  public long version = 1;
}
