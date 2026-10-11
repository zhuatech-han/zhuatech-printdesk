// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 初始化组织、角色、菜单和参数；不注入业务数据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  static final List<String> CODES =
      List.of(
          "orders",
          "sales",
          "artwork",
          "production",
          "quality",
          "delivery",
          "finance",
          "masters",
          "export",
          "reports",
          "users",
          "roles",
          "settings",
          "audit");
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String username, password;

  /** 空库管理员使用私有环境变量。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Bootstrap(
      Store db,
      BCryptPasswordEncoder encoder,
      @Value("${printdesk.admin-username}") String u,
      @Value("${printdesk.admin-password}") String p) {
    this.db = db;
    this.encoder = encoder;
    username = u;
    password = p;
  }

  /** 重启不重置管理员和既有数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    if (!username.matches("[a-zA-Z0-9_.-]{3,60}"))
      throw new IllegalStateException("Invalid administrator name");
    for (var code : CODES) {
      var p = new Permission();
      p.code = code;
      p.name = code;
      db.save(p);
    }
    var d = new Department();
    d.name = "主组织 / Main organization";
    d.zone = "Asia/Shanghai";
    db.save(d);
    var admin = role("管理员 / Administrator", "ALL", CODES);
    role(
        "销售与制作管理 / Sales manager",
        "DEPARTMENT",
        List.of(
            "orders",
            "sales",
            "artwork",
            "production",
            "quality",
            "delivery",
            "finance",
            "masters",
            "export",
            "reports",
            "audit"));
    role("制作员工 / Production operator", "ASSIGNED", List.of("orders", "production"));
    role("质检与交货 / Quality and delivery", "DEPARTMENT", List.of("orders", "quality", "delivery"));
    role("客户 / Customer", "CUSTOMER", List.of("orders"));
    role("财务查阅 / Finance", "DEPARTMENT", List.of("orders", "finance", "reports", "export"));
    var a = new Account();
    a.username = username.toLowerCase(Locale.ROOT);
    a.displayName = "管理员 / Administrator";
    a.passwordHash = encoder.encode(password);
    a.roleId = admin.id;
    a.departmentId = d.id;
    db.save(a);
    String[][] menus = {
      {"orders", "订单工作台", "Orders", "orders"},
      {"masters", "客户与物料", "Customers & materials", "masters"},
      {"reports", "经营统计", "Reports", "reports"},
      {"users", "登录账号", "Accounts", "users"},
      {"roles", "角色与权限", "Roles & permissions", "roles"},
      {"settings", "组织与设置", "Organizations & settings", "settings"},
      {"audit", "操作记录", "Audit trail", "audit"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    String[][] categories = {
      {"DIGITAL", "数码印刷", "Digital print"},
      {"SIGNAGE", "喷绘标识", "Signage"},
      {"CUSTOM", "定制制作", "Custom production"}
    };
    for (var v : categories) {
      var x = new DictionaryEntry();
      x.type = "CATEGORY";
      x.code = v[0];
      x.name = v[1];
      x.nameEn = v[2];
      db.save(x);
    }
    for (var v : new String[][] {{"max_orders", "1000"}, {"max_files", "10"}}) {
      var x = new SystemSetting();
      x.code = v[0];
      x.value = v[1];
      db.save(x);
    }
  }

  private AccessRole role(String n, String scope, List<String> codes) {
    var r = new AccessRole();
    r.name = n;
    r.scope = scope;
    r.permissions.addAll(codes);
    return db.save(r);
  }
}
