// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 员工与客户入口由同一事务服务执行角色与归属校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final PrintService service;
  final AdminService admin;

  public ApiController(PrintService service, AdminService admin) {
    this.service = service;
    this.admin = admin;
  }

  /** 当前身份最小业务选项。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/options")
  public Object options() {
    return service.options();
  }

  /** 数据范围内订单分页。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/orders")
  public Object list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "newest") String sort) {
    return service.list(q, state, page, size, sort);
  }

  /** 订单详情按客户或员工角色删减内部信息。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/orders/{id}")
  public Object detail(@PathVariable long id) {
    return service.detail(id);
  }

  /** 新建成本加成报价草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/orders")
  public Object create(@RequestBody Map<String, Object> b) {
    return service.save(null, b);
  }

  /** 修订尚未确认的报价。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/orders/{id}")
  public Object save(@PathVariable long id, @RequestBody Map<String, Object> b) {
    return service.save(id, b);
  }

  /** 合法状态下版本与幂等保护的业务动作。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/orders/{id}/{action}")
  public Object action(
      @PathVariable long id, @PathVariable String action, @RequestBody Map<String, Object> b) {
    return service.action(id, action, b);
  }

  /** 保留原始资料与样稿实际文件，不覆盖旧版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/orders/{id}/files")
  public Object upload(
      @PathVariable long id,
      @RequestParam String kind,
      @RequestParam String version,
      @RequestParam String requestId,
      @RequestParam MultipartFile file) {
    return service.upload(
        id, Map.of("kind", kind, "version", version, "requestId", requestId), file);
  }

  /** 删除未发布且无文件的草稿，引用历史不删除。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/orders/{id}/delete")
  public Object deleteDraft(@PathVariable long id, @RequestBody Map<String, Object> b) {
    return service.deleteDraft(id, b);
  }

  /** 私有文件强制下载并再次验权。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/files/{id}")
  public ResponseEntity<byte[]> file(@PathVariable long id) {
    var f = service.file(id);
    return ResponseEntity.ok()
        .header(
            "Content-Disposition",
            ContentDisposition.attachment()
                .filename(f.filename, StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(f.content);
  }

  /** 受控主数据列表。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/masters/{kind}")
  public Object masters(@PathVariable String kind) {
    return service.masters(kind);
  }

  /** 新建客户、物料、委外供应商。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/masters/{kind}")
  public Object masterCreate(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return service.masterSave(kind, null, b);
  }

  /** 已引用主数据仅修改可变资料或停用。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/masters/{kind}/{id}")
  public Object masterSave(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return service.masterSave(kind, id, b);
  }

  /** 实际到货凭据、数量、成本记账，拒绝重复凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/materials/{id}/receive")
  public Object receive(@PathVariable long id, @RequestBody Map<String, Object> b) {
    return service.receiveMaterial(id, b);
  }

  /** 币种隔离的真实经营统计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports")
  public Object reports() {
    return service.reports();
  }

  /** 授权导出并记录审计，不使用有副作用的GET。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/orders/export")
  public ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=orders.csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(service.export().getBytes(StandardCharsets.UTF_8));
  }

  /** 内容最小化审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  public Object audit() {
    return service.audit();
  }

  /** 系统账号管理选项。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/options")
  public Object adminOptions() {
    return admin.options();
  }

  /** 系统目录需要相应权限和ALL范围。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{kind}")
  public Object adminRead(@PathVariable String kind) {
    return admin.read(kind);
  }

  /** 建立账号、角色、组织、分类。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{kind}")
  public Object adminCreate(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return admin.save(kind, null, b);
  }

  /** 更新目录并保护最后一个管理员。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{kind}/{id}")
  public Object adminSave(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return admin.save(kind, id, b);
  }
}
