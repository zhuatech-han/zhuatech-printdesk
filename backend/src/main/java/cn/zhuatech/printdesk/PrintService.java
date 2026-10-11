// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

/** 报价、客户审稿、材料、制作与结算的独立事务实现。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class PrintService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final Clock clock;
  final JsonMapper json = JsonMapper.builder().build();

  public PrintService(Store db, AccessService access, AdminService admin, Clock clock) {
    this.db = db;
    this.access = access;
    this.admin = admin;
    this.clock = clock;
  }

  void lock() {
    admin.lock();
  }

  boolean customer() {
    return access.role().scope.equals("CUSTOMER");
  }

  List<PrintLine> lines(long id) {
    return db.query(PrintLine.class, "from PrintLine where orderId=?1 order by id", id);
  }

  <T> List<T> records(Class<T> c, long id) {
    return db.query(c, "from " + c.getSimpleName() + " where orderId=?1 order by id", id);
  }

  boolean readable(PrintOrder o) {
    if (customer()) {
      var c = db.get(Customer.class, o.customerId);
      return c.enabled
          && Objects.equals(c.accountId, access.current().id)
          && o.revision > 0
          && !Set.of("DRAFT").contains(o.state);
    }
    if (!access.department(o.departmentId)) return false;
    return !access.role().scope.equals("ASSIGNED")
        || Objects.equals(o.assigneeId, access.current().id)
        || Objects.equals(o.ownerId, access.current().id);
  }

  PrintOrder get(long id) {
    access.require("orders");
    var o = db.get(PrintOrder.class, id);
    if (!readable(o)) throw new Problem(403, "OUT_OF_SCOPE");
    return o;
  }

  void staff(String p) {
    access.require(p);
    if (customer()) throw new Problem(403, "FORBIDDEN");
  }

  void state(PrintOrder o, String... s) {
    Rules.check(db.get(Department.class, o.departmentId).enabled, "DEPARTMENT_DISABLED");
    Rules.check(Arrays.asList(s).contains(o.state), "STATE_INVALID");
  }

  LocalDate date(Object v) {
    try {
      return LocalDate.parse(Objects.toString(v, ""));
    } catch (Exception e) {
      throw new Problem(400, "DATE_INVALID");
    }
  }

  String encode(Object o) {
    return json.writeValueAsString(o);
  }

  String requestId(Map<String, Object> b) {
    String s = Rules.text(b.get("requestId"), 36, true);
    try {
      Rules.check(UUID.fromString(s).toString().equals(s), "REQUEST_INVALID");
    } catch (Exception e) {
      throw new Problem(400, "REQUEST_INVALID");
    }
    return s;
  }

  String fingerprint(String action, Object body) {
    return Pricing.hash((action + encode(body)).getBytes(StandardCharsets.UTF_8));
  }

  boolean duplicate(String action, Map<String, Object> b, long orderId, String hash) {
    var prior =
        db.query(BusinessEvent.class, "from BusinessEvent where requestId=?1", requestId(b));
    if (prior.isEmpty()) return false;
    var e = prior.getFirst();
    Rules.check(
        e.orderId == orderId
            && e.actorId.equals(access.current().id)
            && e.action.equals(action)
            && e.fingerprint.equals(hash),
        "REQUEST_REUSED");
    return true;
  }

  void version(PrintOrder o, Map<String, Object> b) {
    Rules.check(o.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
  }

  void event(PrintOrder o, String action, Map<String, Object> b, String hash) {
    var e = new BusinessEvent();
    e.orderId = o.id;
    e.actorId = access.current().id;
    e.action = action;
    e.note = Rules.paragraph(b.get("note"), 1000, false);
    e.requestId = requestId(b);
    e.fingerprint = hash;
    e.metadata = metadata(o, action, b);
    e.createdAt = clock.instant();
    db.save(e);
    o.version++;
    o.updatedAt = clock.instant();
    access.audit("ORDER_" + action, o.id, o.departmentId);
  }

  /** 只记录已校验的动作字段，不记录密码、会话、文件二进制或无关输入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  String metadata(PrintOrder o, String action, Map<String, Object> b) {
    var out = new LinkedHashMap<String, Object>();
    var keys =
        switch (action) {
          case "UPLOAD" -> List.of("fileId", "kind", "filename");
          case "ASSIGN" -> List.of("assigneeId");
          case "PROOF_ACCEPT", "PROOF_REJECT" -> List.of("proofId");
          case "ISSUE" -> List.of("lineId", "quantity");
          case "RETURN" -> List.of("lineId", "quantity", "sourceId");
          case "WORK" -> List.of("lineId", "good", "scrap", "minutes", "hourlyRate");
          case "RECEIVE" -> List.of("amount", "reference");
          case "REFUND" -> List.of("amount", "sourceId", "reference");
          case "VENDOR_CREATE" ->
              List.of("lineId", "supplierId", "quantity", "amount", "description", "dueDate");
          case "VENDOR_CONFIRM" -> List.of("taskId", "reference");
          case "VENDOR_RECEIVE" -> List.of("taskId", "quantity", "reference");
          case "VENDOR_CANCEL" -> List.of("taskId");
          case "DELIVER" -> List.of("reference", "method", "address");
          default -> List.<String>of();
        };
    var numeric =
        Set.of(
            "fileId",
            "assigneeId",
            "proofId",
            "lineId",
            "quantity",
            "sourceId",
            "good",
            "scrap",
            "minutes",
            "hourlyRate",
            "amount",
            "taskId",
            "supplierId");
    for (var k : keys) {
      var v = b.get(k);
      if (v != null)
        out.put(
            k,
            numeric.contains(k)
                ? new BigDecimal(v.toString()).stripTrailingZeros().toPlainString()
                : v);
    }
    if (Set.of("QUOTE", "ACCEPT", "REJECT", "REVISE").contains(action))
      out.put("quoteRevision", o.revision);
    return encode(out);
  }

  /** 未获得商业金额权限的岗位不读收退款或委外明细。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  Map<String, Object> eventView(BusinessEvent e) {
    var m = new LinkedHashMap<String, Object>();
    m.put("action", e.action);
    m.put("actorId", e.actorId);
    m.put("createdAt", e.createdAt);
    boolean restricted =
        !amounts()
            && (Set.of("RECEIVE", "REFUND").contains(e.action) || e.action.startsWith("VENDOR_"));
    m.put("note", restricted ? "" : e.note);
    var data = json.readValue(e.metadata, LinkedHashMap.class);
    if (restricted) data.clear();
    if (!amounts()) data.remove("hourlyRate");
    m.put("details", data);
    return m;
  }

  boolean amounts() {
    return customer() || access.has("sales") || access.has("finance") || access.has("reports");
  }

  BigDecimal paid(long id) {
    return records(MoneyEntry.class, id).stream()
        .map(p -> p.kind.equals("REFUND") ? p.amount.negate() : p.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  BigDecimal materialCost(long id) {
    return records(StockTxn.class, id).stream()
        .map(p -> p.kind.equals("RETURN") ? p.amount.negate() : p.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  BigDecimal workCost(long id) {
    return records(WorkEntry.class, id).stream()
        .map(p -> p.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  BigDecimal vendorCost(long id) {
    return records(VendorTask.class, id).stream()
        .map(p -> p.recognized)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  ProofFile latest(long id) {
    var list = records(ProofFile.class, id).stream().filter(p -> p.kind.equals("PROOF")).toList();
    return list.isEmpty() ? null : list.getLast();
  }

  Map<String, Object> lineView(PrintLine l) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", l.id);
    m.put("name", l.name);
    m.put("quantity", l.quantity);
    m.put("widthMm", l.widthMm);
    m.put("heightMm", l.heightMm);
    m.put("good", l.good);
    m.put("scrap", l.scrap);
    m.put("accepted", l.accepted);
    m.put("shipped", l.shipped);
    m.put("materialName", l.materialName);
    m.put("unit", l.unit);
    if (amounts()) m.put("quoted", l.quoted);
    if (!customer()) {
      m.put("materialId", l.materialId);
      m.put("usage", l.usage);
      if (amounts()) {
        m.put("materialCost", l.materialCost);
        m.put("printRate", l.printRate);
        m.put("finishRate", l.finishRate);
        m.put("setup", l.setup);
        m.put("markupPercent", l.markupPercent);
      }
    }
    return m;
  }

  Map<String, Object> summary(PrintOrder o) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", o.id);
    m.put("number", String.format("PD-%06d", o.id));
    m.put("version", o.version);
    m.put("revision", o.revision);
    m.put("state", o.state);
    m.put("title", o.title);
    m.put("customerId", o.customerId);
    m.put("customerName", o.customerName);
    m.put("departmentId", o.departmentId);
    m.put("ownerId", o.ownerId);
    m.put("assigneeId", o.assigneeId);
    m.put("category", o.category);
    m.put("dueDate", o.dueDate);
    m.put("currency", o.currency);
    m.put("updatedAt", o.updatedAt);
    m.put("instructions", o.instructions);
    if (amounts()) {
      m.put("subtotal", o.subtotal);
      m.put("discount", o.discount);
      m.put("taxPercent", o.taxPercent);
      m.put("total", o.total);
      m.put("paid", paid(o.id));
    }
    return m;
  }

  /** 明确按角色删除内部成本和员工信息；客户只读本人订单。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> detail(long id) {
    return detail(get(id));
  }

  Map<String, Object> detail(PrintOrder o) {
    var m = summary(o);
    m.put("lines", lines(o.id).stream().map(this::lineView).toList());
    m.put("files", records(ProofFile.class, o.id));
    m.put("latestProofId", latest(o.id) == null ? null : latest(o.id).id);
    m.put("dispatches", records(Dispatch.class, o.id));
    if (amounts()) m.put("quotes", records(QuoteRevision.class, o.id));
    if (!customer()) {
      m.put(
          "events",
          records(BusinessEvent.class, o.id).stream()
              .filter(
                  e ->
                      amounts()
                          || (!Set.of("RECEIVE", "REFUND").contains(e.action)
                              && !e.action.startsWith("VENDOR_")))
              .map(this::eventView)
              .toList());
      m.put(
          "stock",
          records(StockTxn.class, o.id).stream()
              .map(
                  x -> {
                    var v = new LinkedHashMap<String, Object>();
                    v.put("id", x.id);
                    v.put("lineId", x.lineId);
                    v.put("kind", x.kind);
                    v.put("quantity", x.quantity);
                    v.put("reference", x.reference);
                    if (amounts()) v.put("amount", x.amount);
                    return v;
                  })
              .toList());
      m.put(
          "work",
          records(WorkEntry.class, o.id).stream()
              .map(
                  x -> {
                    var v = new LinkedHashMap<String, Object>();
                    v.put("lineId", x.lineId);
                    v.put("good", x.good);
                    v.put("scrap", x.scrap);
                    v.put("minutes", x.minutes);
                    v.put("actorId", x.actorId);
                    v.put("note", x.note);
                    if (amounts()) v.put("amount", x.amount);
                    return v;
                  })
              .toList());
      if (amounts()) {
        m.put("vendorTasks", records(VendorTask.class, o.id));
        m.put("vendorReceipts", records(VendorReceipt.class, o.id));
        m.put("materialCost", materialCost(o.id));
        m.put("laborCost", workCost(o.id));
        m.put("vendorCost", vendorCost(o.id));
        m.put(
            "cost",
            materialCost(o.id)
                .add(workCost(o.id))
                .add(vendorCost(o.id))
                .setScale(2, RoundingMode.HALF_UP));
        m.put("payments", records(MoneyEntry.class, o.id));
      }
    }
    return m;
  }

  /** 查询先做数据范围过滤，随后搜索、状态、排序和分页。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> list(String q, String state, int page, int size, String sort) {
    access.require("orders");
    Rules.check(
        page >= 1 && size >= 1 && size <= 50 && Set.of("newest", "due", "customer").contains(sort),
        "INVALID_INPUT");
    q = Rules.text(q, 100, false).toLowerCase(Locale.ROOT);
    final String query = q;
    var out =
        db.all(PrintOrder.class).stream()
            .filter(this::readable)
            .filter(o -> state.isEmpty() || state.equals(o.state))
            .filter(
                o ->
                    (o.title + " " + o.customerName + " " + String.format("PD-%06d", o.id))
                        .toLowerCase(Locale.ROOT)
                        .contains(query))
            .sorted(
                sort.equals("due")
                    ? Comparator.comparing(o -> o.dueDate)
                    : sort.equals("customer")
                        ? Comparator.comparing(o -> o.customerName)
                        : Comparator.comparing((PrintOrder o) -> o.id).reversed())
            .toList();
    long start = (long) (page - 1) * size;
    return Map.of(
        "total",
        out.size(),
        "items",
        out.stream().skip(start).limit(size).map(this::summary).toList());
  }

  /** 选项只返回当前身份需要的目录，不泄露其他客户或成本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> options() {
    access.require("orders");
    var out = new LinkedHashMap<String, Object>();
    out.put(
        "departments",
        customer()
            ? List.of()
            : db.all(Department.class).stream()
                .filter(d -> d.enabled && access.department(d.id))
                .toList());
    out.put("categories", db.all(DictionaryEntry.class).stream().filter(d -> d.enabled).toList());
    out.put(
        "customers",
        customer()
            ? List.of()
            : db.all(Customer.class).stream()
                .filter(c -> c.enabled && access.department(c.departmentId))
                .toList());
    out.put(
        "materials",
        customer()
            ? List.of()
            : db.all(Material.class).stream()
                .filter(c -> c.enabled && access.department(c.departmentId))
                .map(
                    x -> {
                      var v = new LinkedHashMap<String, Object>();
                      v.put("id", x.id);
                      v.put("departmentId", x.departmentId);
                      v.put("name", x.name);
                      v.put("unit", x.unit);
                      v.put("currency", x.currency);
                      v.put("stock", x.stock);
                      if (amounts()) v.put("cost", x.cost);
                      return v;
                    })
                .toList());
    out.put(
        "suppliers",
        customer()
            ? List.of()
            : db.all(Supplier.class).stream()
                .filter(s -> s.enabled && access.department(s.departmentId))
                .toList());
    out.put(
        "accounts",
        customer()
            ? List.of()
            : db.all(Account.class).stream()
                .filter(a -> a.enabled && access.department(a.departmentId))
                .map(
                    a ->
                        Map.of(
                            "id",
                            a.id,
                            "displayName",
                            a.displayName,
                            "departmentId",
                            a.departmentId,
                            "scope",
                            db.get(AccessRole.class, a.roleId).scope,
                            "production",
                            db.get(AccessRole.class, a.roleId).permissions.contains("production")))
                .toList());
    return out;
  }

  /** 创建或更新未发布报价；新版本需要客户重新确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> save(Long id, Map<String, Object> b) {
    staff("sales");
    lock();
    String hash = fingerprint("SAVE", b);
    if (id == null) {
      var old =
          db.query(BusinessEvent.class, "from BusinessEvent where requestId=?1", requestId(b));
      if (!old.isEmpty()) {
        var o = get(old.getFirst().orderId);
        duplicate("SAVE", b, o.id, hash);
        return detail(o);
      }
    }
    var o = id == null ? new PrintOrder() : get(id);
    if (id != null) {
      if (duplicate("SAVE", b, id, hash)) return detail(o);
      version(o, b);
      state(o, "DRAFT", "CHANGES");
    } else {
      Rules.check(db.all(PrintOrder.class).size() < admin.setting("max_orders"), "RESOURCE_LIMIT");
      o.customerId = Rules.id(b.get("customerId"));
      var c = db.get(Customer.class, o.customerId);
      Rules.check(c.enabled, "CUSTOMER_DISABLED");
      access.library(c.departmentId, "sales");
      o.departmentId = c.departmentId;
      o.ownerId = access.current().id;
      o.createdAt = clock.instant();
      o.updatedAt = clock.instant();
      o.dueDate = date(b.get("dueDate"));
      db.save(o);
    }
    var c = db.get(Customer.class, o.customerId);
    Rules.check(db.get(Department.class, o.departmentId).enabled, "DEPARTMENT_DISABLED");
    Rules.check(c.enabled, "CUSTOMER_DISABLED");
    Rules.check(Objects.equals(o.customerId, Rules.id(b.get("customerId"))), "IDENTITY_LOCKED");
    o.customerName = c.name;
    o.title = Rules.text(b.get("title"), 160, true);
    o.instructions = Rules.paragraph(b.get("instructions"), 2000, false);
    o.dueDate = date(b.get("dueDate"));
    Rules.check(!o.dueDate.isBefore(LocalDate.now(clock)), "DATE_INVALID");
    o.currency = Rules.text(b.get("currency"), 3, true);
    Rules.check(Set.of("CNY", "USD", "EUR", "GBP").contains(o.currency), "CURRENCY_INVALID");
    o.category = Rules.text(b.get("category"), 60, true);
    Rules.check(
        db.all(DictionaryEntry.class).stream()
            .anyMatch(x -> x.enabled && x.code.equals(o.category)),
        "CATEGORY_DISABLED");
    o.discount = Pricing.money(b.get("discount"));
    o.taxPercent = Pricing.percent(b.get("taxPercent"), 100);
    if (!(b.get("lines") instanceof List<?> input) || input.isEmpty() || input.size() > 20)
      throw new Problem(400, "LINES_INVALID");
    for (var l : lines(o.id)) db.delete(l);
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var raw : input) {
      if (!(raw instanceof Map<?, ?> row)) throw new Problem(400, "LINES_INVALID");
      var l = new PrintLine();
      l.orderId = o.id;
      l.materialId = Rules.id(row.get("materialId"));
      var mat = db.get(Material.class, l.materialId);
      Rules.check(
          mat.enabled && mat.departmentId.equals(o.departmentId) && mat.currency.equals(o.currency),
          "MATERIAL_INVALID");
      l.name = Rules.text(row.get("name"), 160, true);
      l.quantity = Rules.integer(row.get("quantity"), 1, 1000000);
      l.widthMm = Rules.integer((row.containsKey("widthMm") ? row.get("widthMm") : 0), 0, 100000);
      l.heightMm =
          Rules.integer((row.containsKey("heightMm") ? row.get("heightMm") : 0), 0, 100000);
      l.usage =
          mat.unit.equals("M2")
              ? Pricing.area(l.widthMm, l.heightMm)
              : Pricing.decimal(
                  row.get("usage"), 4, new BigDecimal("0.0001"), new BigDecimal("100000"));
      l.materialCost = mat.cost;
      l.materialName = mat.name;
      l.unit = mat.unit;
      l.printRate = Pricing.money(row.get("printRate"));
      l.finishRate = Pricing.money(row.get("finishRate"));
      l.setup = Pricing.money(row.get("setup"));
      l.markupPercent = Pricing.percent(row.get("markupPercent"), 500);
      l.quoted = Pricing.line(l);
      subtotal = subtotal.add(l.quoted);
      db.save(l);
    }
    o.subtotal = subtotal;
    o.total = Pricing.total(subtotal, o.discount, o.taxPercent);
    o.state = "DRAFT";
    event(o, "SAVE", b, hash);
    db.flush();
    return detail(o);
  }

  /** 发布、审签、派工、报工、质检、交货和收退款；幂等与版本校验在同一事务内。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> action(long id, String action, Map<String, Object> b) {
    lock();
    var o = get(id);
    String hash = fingerprint(action, b);
    if (duplicate(action, b, id, hash)) return detail(o);
    version(o, b);
    switch (action) {
      case "QUOTE" -> {
        staff("sales");
        state(o, "DRAFT", "CHANGES");
        Rules.check(db.get(Customer.class, o.customerId).enabled, "CUSTOMER_DISABLED");
        var q = new QuoteRevision();
        q.orderId = o.id;
        q.number = ++o.revision;
        q.createdAt = clock.instant();
        q.decision = "PENDING";
        q.snapshot =
            encode(
                Map.of(
                    "title",
                    o.title,
                    "dueDate",
                    o.dueDate.toString(),
                    "currency",
                    o.currency,
                    "subtotal",
                    o.subtotal,
                    "discount",
                    o.discount,
                    "taxPercent",
                    o.taxPercent,
                    "total",
                    o.total,
                    "lines",
                    lines(id).stream()
                        .map(
                            l ->
                                Map.of(
                                    "name",
                                    l.name,
                                    "quantity",
                                    l.quantity,
                                    "widthMm",
                                    l.widthMm,
                                    "heightMm",
                                    l.heightMm,
                                    "quoted",
                                    l.quoted))
                        .toList()));
        db.save(q);
        o.state = "QUOTED";
      }
      case "ACCEPT", "REJECT" -> {
        Rules.check(customer(), "CUSTOMER_ONLY");
        state(o, "QUOTED");
        if (action.equals("REJECT")) Rules.paragraph(b.get("note"), 1000, true);
        var q =
            records(QuoteRevision.class, id).stream()
                .filter(v -> v.number == o.revision)
                .findFirst()
                .orElseThrow();
        q.decision = action.equals("ACCEPT") ? "ACCEPTED" : "REJECTED";
        q.reviewerId = access.current().id;
        q.reviewedAt = clock.instant();
        o.state = action.equals("ACCEPT") ? "ACCEPTED" : "CHANGES";
      }
      case "PROOF_ACCEPT", "PROOF_REJECT" -> {
        Rules.check(customer(), "CUSTOMER_ONLY");
        state(o, "QUOTED", "ACCEPTED");
        var p = latest(id);
        Rules.check(
            p != null && p.id.equals(Rules.id(b.get("proofId"))) && p.status.equals("PENDING"),
            "PROOF_STALE");
        p.status = action.equals("PROOF_ACCEPT") ? "APPROVED" : "REJECTED";
        p.reviewerId = access.current().id;
        p.reviewedAt = clock.instant();
        p.feedback = Rules.paragraph(b.get("note"), 1000, action.equals("PROOF_REJECT"));
      }
      case "REVISE" -> {
        staff("sales");
        state(o, "QUOTED", "ACCEPTED");
        Rules.paragraph(b.get("note"), 1000, true);
        Rules.check(
            paid(id).signum() == 0 && records(VendorTask.class, id).isEmpty(),
            "REVISION_COMMITMENT_EXISTS");
        o.state = "DRAFT";
      }
      case "ASSIGN" -> {
        staff("sales");
        state(o, "DRAFT", "QUOTED", "ACCEPTED", "CHANGES", "IN_PROGRESS");
        var a = db.get(Account.class, Rules.id(b.get("assigneeId")));
        Rules.check(
            a.enabled
                && a.departmentId.equals(o.departmentId)
                && db.get(AccessRole.class, a.roleId).permissions.contains("production")
                && !db.get(AccessRole.class, a.roleId).scope.equals("CUSTOMER"),
            "ASSIGNEE_INVALID");
        o.assigneeId = a.id;
      }
      case "START" -> {
        staff("production");
        state(o, "ACCEPTED");
        Rules.check(Objects.equals(o.assigneeId, access.current().id), "ASSIGNEE_ONLY");
        Rules.check(latest(id) != null && latest(id).status.equals("APPROVED"), "PROOF_REQUIRED");
        o.state = "IN_PROGRESS";
      }
      case "ISSUE", "RETURN" -> {
        staff("production");
        state(o, "IN_PROGRESS");
        stock(o, action, b);
      }
      case "WORK" -> {
        staff("production");
        state(o, "IN_PROGRESS");
        Rules.check(Objects.equals(o.assigneeId, access.current().id), "ASSIGNEE_ONLY");
        work(o, b);
      }
      case "QUALITY_PASS", "QUALITY_FAIL" -> {
        staff("quality");
        state(o, "IN_PROGRESS");
        Rules.check(
            !Objects.equals(o.assigneeId, access.current().id)
                && records(WorkEntry.class, id).stream()
                    .noneMatch(w -> w.actorId.equals(access.current().id)),
            "INDEPENDENT_REVIEW_REQUIRED");
        Rules.check(
            lines(id).stream().allMatch(l -> l.good == l.quantity), "PRODUCTION_INCOMPLETE");
        Rules.check(
            records(VendorTask.class, id).stream()
                .allMatch(v -> Set.of("RECEIVED", "CANCELLED").contains(v.state)),
            "VENDOR_PENDING");
        if (action.equals("QUALITY_PASS")) {
          for (var l : lines(id)) l.accepted = l.good;
          o.state = "READY";
        } else {
          Rules.paragraph(b.get("note"), 1000, true);
          for (var l : lines(id)) {
            l.good = 0;
            l.accepted = 0;
          }
        }
      }
      case "DELIVER" -> {
        staff("delivery");
        state(o, "READY", "PART_DELIVERED");
        deliver(o, b);
      }
      case "RECEIVE", "REFUND" -> {
        staff("finance");
        Rules.check(
            !Set.of("DRAFT", "QUOTED", "CHANGES", "CLOSED", "CANCELLED").contains(o.state),
            "STATE_INVALID");
        money(o, action, b);
      }
      case "CLOSE" -> {
        staff("sales");
        state(o, "DELIVERED");
        Rules.check(paid(id).compareTo(o.total) == 0, "PAYMENT_INCOMPLETE");
        o.state = "CLOSED";
      }
      case "CANCEL" -> {
        staff("sales");
        state(o, "DRAFT", "CHANGES", "QUOTED", "ACCEPTED", "IN_PROGRESS", "READY");
        Rules.paragraph(b.get("note"), 1000, true);
        Rules.check(paid(id).signum() == 0, "REFUND_REQUIRED");
        Rules.check(
            records(VendorTask.class, id).stream()
                .allMatch(v -> Set.of("RECEIVED", "CANCELLED").contains(v.state)),
            "VENDOR_PENDING");
        o.state = "CANCELLED";
      }
      case "VENDOR_CREATE", "VENDOR_CONFIRM", "VENDOR_RECEIVE", "VENDOR_CANCEL" -> {
        staff("sales");
        state(o, "ACCEPTED", "IN_PROGRESS");
        vendor(o, action, b);
      }
      case "NOTE" -> {
        staff("sales");
        Rules.paragraph(b.get("note"), 1000, true);
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    event(o, action, b, hash);
    db.flush();
    return detail(o);
  }

  PrintLine line(long order, Object id) {
    var l = db.get(PrintLine.class, Rules.id(id));
    Rules.check(l.orderId == order, "LINE_INVALID");
    return l;
  }

  /** 领退料按原流水和已消耗数量守恒，成本随移动平均计算。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void stock(PrintOrder o, String action, Map<String, Object> b) {
    var x = new StockTxn();
    x.orderId = o.id;
    x.actorId = access.current().id;
    x.kind = action;
    x.createdAt = clock.instant();
    x.reference = requestId(b);
    x.note = Rules.paragraph(b.get("note"), 600, true);
    x.quantity =
        Pricing.decimal(
            b.get("quantity"), 4, new BigDecimal("0.0001"), new BigDecimal("1000000000"));
    var l = line(o.id, b.get("lineId"));
    x.lineId = l.id;
    x.materialId = l.materialId;
    var m = db.get(Material.class, l.materialId);
    if (action.equals("ISSUE")) {
      Rules.check(m.enabled && m.stock.compareTo(x.quantity) >= 0, "STOCK_SHORTAGE");
      var issued =
          records(StockTxn.class, o.id).stream()
              .filter(t -> t.lineId.equals(l.id))
              .map(t -> t.kind.equals("RETURN") ? t.quantity.negate() : t.quantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Rules.check(
          issued
                  .add(x.quantity)
                  .compareTo(
                      l.usage
                          .multiply(BigDecimal.valueOf(l.quantity))
                          .multiply(BigDecimal.valueOf(3)))
              <= 0,
          "ISSUE_LIMIT");
      x.cost = m.cost;
      x.amount = x.quantity.multiply(x.cost).setScale(4, RoundingMode.HALF_UP);
      if (x.quantity.compareTo(m.stock) == 0) x.amount = m.value;
      m.stock = m.stock.subtract(x.quantity);
      m.value = m.value.subtract(x.amount);
    } else {
      var source = db.get(StockTxn.class, Rules.id(b.get("sourceId")));
      Rules.check(
          source.kind.equals("ISSUE")
              && Objects.equals(source.orderId, o.id)
              && source.lineId.equals(l.id),
          "RETURN_SOURCE_INVALID");
      var returned =
          db.query(StockTxn.class, "from StockTxn where sourceId=?1", source.id).stream()
              .map(t -> t.quantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Rules.check(returned.add(x.quantity).compareTo(source.quantity) <= 0, "RETURN_EXCEEDS_ISSUE");
      var outputs =
          records(WorkEntry.class, o.id).stream()
              .filter(w -> w.lineId.equals(l.id))
              .mapToLong(w -> (long) w.good + w.scrap)
              .sum();
      var net =
          records(StockTxn.class, o.id).stream()
              .filter(t -> t.lineId.equals(l.id))
              .map(t -> t.kind.equals("RETURN") ? t.quantity.negate() : t.quantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Rules.check(
          net.subtract(x.quantity).compareTo(l.usage.multiply(BigDecimal.valueOf(outputs))) >= 0,
          "USED_MATERIAL_LOCKED");
      x.sourceId = source.id;
      x.cost = source.cost;
      var priorAmount =
          db.query(StockTxn.class, "from StockTxn where sourceId=?1", source.id).stream()
              .map(t -> t.amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      x.amount =
          returned.add(x.quantity).compareTo(source.quantity) == 0
              ? source.amount.subtract(priorAmount)
              : x.quantity.multiply(x.cost).setScale(4, RoundingMode.HALF_UP);
      m.stock = m.stock.add(x.quantity);
      m.value = m.value.add(x.amount);
    }
    Rules.check(m.value.signum() >= 0, "STOCK_VALUE_INVALID");
    m.cost =
        m.stock.signum() == 0 ? BigDecimal.ZERO : m.value.divide(m.stock, 4, RoundingMode.HALF_UP);
    m.version++;
    db.save(x);
  }

  /** 实际报工验证累计产出和足量材料，保留返工成本。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void work(PrintOrder o, Map<String, Object> b) {
    var l = line(o.id, b.get("lineId"));
    var w = new WorkEntry();
    w.orderId = o.id;
    w.lineId = l.id;
    w.actorId = access.current().id;
    w.good = Rules.integer(b.get("good"), 0, l.quantity - l.good);
    w.scrap = Rules.integer(b.get("scrap"), 0, l.quantity * 3 - l.scrap);
    w.minutes = Rules.integer(b.get("minutes"), 1, 100000);
    Rules.check(w.good + w.scrap > 0, "OUTPUT_REQUIRED");
    Rules.check(
        records(StockTxn.class, o.id).stream()
            .anyMatch(t -> t.kind.equals("ISSUE") && t.lineId.equals(l.id)),
        "MATERIAL_NOT_ISSUED");
    var priorOutput =
        records(WorkEntry.class, o.id).stream()
            .filter(x -> x.lineId.equals(l.id))
            .mapToLong(x -> (long) x.good + x.scrap)
            .sum();
    long output = priorOutput + w.good + w.scrap;
    Rules.check(output <= (long) l.quantity * 3, "OUTPUT_LIMIT");
    var netIssued =
        records(StockTxn.class, o.id).stream()
            .filter(x -> x.lineId.equals(l.id))
            .map(x -> x.kind.equals("RETURN") ? x.quantity.negate() : x.quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Rules.check(
        netIssued.compareTo(l.usage.multiply(BigDecimal.valueOf(output))) >= 0,
        "MATERIAL_NOT_ISSUED");
    w.hourlyRate = Pricing.money(b.get("hourlyRate"));
    w.amount =
        w.hourlyRate
            .multiply(BigDecimal.valueOf(w.minutes))
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    w.note = Rules.paragraph(b.get("note"), 600, false);
    w.createdAt = clock.instant();
    l.good += w.good;
    l.scrap += w.scrap;
    db.save(w);
  }

  /** 委外状态、分批收回凭据与比例成本在同一事务内更新。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void vendor(PrintOrder o, String action, Map<String, Object> b) {
    if (action.equals("VENDOR_CREATE")) {
      var l = line(o.id, b.get("lineId"));
      var s = db.get(Supplier.class, Rules.id(b.get("supplierId")));
      Rules.check(s.enabled && s.departmentId.equals(o.departmentId), "SUPPLIER_INVALID");
      var v = new VendorTask();
      v.orderId = o.id;
      v.lineId = l.id;
      v.supplierId = s.id;
      v.supplierName = s.name;
      v.description = Rules.paragraph(b.get("description"), 600, true);
      v.quantity = Rules.integer(b.get("quantity"), 1, l.quantity);
      v.amount = Pricing.money(b.get("amount"));
      v.dueDate = date(b.get("dueDate"));
      db.save(v);
      return;
    }
    var v = db.get(VendorTask.class, Rules.id(b.get("taskId")));
    Rules.check(v.orderId.equals(o.id), "VENDOR_INVALID");
    switch (action) {
      case "VENDOR_CONFIRM" -> {
        Rules.check(v.state.equals("REQUESTED"), "STATE_INVALID");
        v.reference = Rules.text(b.get("reference"), 160, true);
        v.state = "CONFIRMED";
      }
      case "VENDOR_RECEIVE" -> {
        Rules.check(v.state.equals("CONFIRMED"), "STATE_INVALID");
        String reference = Rules.text(b.get("reference"), 160, true);
        Rules.check(
            db.query(
                    VendorReceipt.class,
                    "from VendorReceipt where taskId=?1 and reference=?2",
                    v.id,
                    reference)
                .isEmpty(),
            "REFERENCE_DUPLICATE");
        var prior = v.recognized;
        int n = Rules.integer(b.get("quantity"), 1, v.quantity - v.received);
        v.received += n;
        v.recognized =
            v.amount
                .multiply(BigDecimal.valueOf(v.received))
                .divide(BigDecimal.valueOf(v.quantity), 2, RoundingMode.HALF_UP);
        var receipt = new VendorReceipt();
        receipt.orderId = o.id;
        receipt.taskId = v.id;
        receipt.quantity = n;
        receipt.amount = v.recognized.subtract(prior);
        receipt.reference = reference;
        receipt.actorId = access.current().id;
        receipt.createdAt = clock.instant();
        db.save(receipt);
        if (v.received == v.quantity) v.state = "RECEIVED";
      }
      case "VENDOR_CANCEL" -> {
        Rules.check(
            v.state.equals("REQUESTED") || v.state.equals("CONFIRMED") && v.received == 0,
            "STATE_INVALID");
        Rules.paragraph(b.get("note"), 1000, true);
        v.state = "CANCELLED";
      }
      default -> throw new Problem(400, "INVALID_INPUT");
    }
  }

  /** 分批交货以合格未交数量为上限，不伪造物流结果。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void deliver(PrintOrder o, Map<String, Object> b) {
    if (!(b.get("lines") instanceof List<?> list) || list.isEmpty() || list.size() > 20)
      throw new Problem(400, "LINES_INVALID");
    var out = new ArrayList<Map<String, Object>>();
    var ids = new HashSet<Long>();
    for (var raw : list) {
      if (!(raw instanceof Map<?, ?> row)) throw new Problem(400, "LINES_INVALID");
      var l = line(o.id, row.get("lineId"));
      Rules.check(ids.add(l.id), "LINE_DUPLICATE");
      int n = Rules.integer(row.get("quantity"), 1, l.accepted - l.shipped);
      l.shipped += n;
      out.add(Map.of("lineId", l.id, "name", l.name, "quantity", n));
    }
    var d = new Dispatch();
    d.orderId = o.id;
    d.reference = Rules.text(b.get("reference"), 160, true);
    d.method = Rules.text(b.get("method"), 20, true);
    Rules.check(Set.of("PICKUP", "SHIP", "DELIVERY").contains(d.method), "INVALID_INPUT");
    d.address = Rules.text(b.get("address"), 600, true);
    d.lines = encode(out);
    d.actorId = access.current().id;
    d.createdAt = clock.instant();
    db.save(d);
    o.state =
        lines(o.id).stream().allMatch(l -> l.shipped == l.quantity)
            ? "DELIVERED"
            : "PART_DELIVERED";
  }

  /** 外部钱款只记账，退款引用原款且不能超额。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void money(PrintOrder o, String action, Map<String, Object> b) {
    var m = new MoneyEntry();
    m.orderId = o.id;
    m.kind = action;
    m.actorId = access.current().id;
    m.createdAt = clock.instant();
    m.amount = Pricing.money(b.get("amount"));
    Rules.check(m.amount.signum() > 0, "AMOUNT_INVALID");
    m.reference = Rules.text(b.get("reference"), 160, true);
    m.note = Rules.paragraph(b.get("note"), 600, true);
    if (action.equals("RECEIVE")) {
      Rules.check(paid(o.id).add(m.amount).compareTo(o.total) <= 0, "OVERPAYMENT");
    } else {
      var source = db.get(MoneyEntry.class, Rules.id(b.get("sourceId")));
      Rules.check(
          source.kind.equals("RECEIVE") && source.orderId.equals(o.id), "REFUND_SOURCE_INVALID");
      var prior =
          db.query(MoneyEntry.class, "from MoneyEntry where sourceId=?1", source.id).stream()
              .map(v -> v.amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Rules.check(prior.add(m.amount).compareTo(source.amount) <= 0, "REFUND_EXCEEDS_PAYMENT");
      m.sourceId = source.id;
    }
    db.save(m);
  }

  /** 私有文件上传保留不可变版本，最新样稿才能被客户批准。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> upload(long id, Map<String, Object> b, MultipartFile file) {
    lock();
    var o = get(id);
    String kind = Rules.text(b.get("kind"), 20, true);
    Rules.check(Set.of("SOURCE", "PROOF").contains(kind), "FILE_INVALID");
    if (customer()) Rules.check(kind.equals("SOURCE"), "CUSTOMER_ACTION_INVALID");
    else access.require("artwork");
    state(o, "DRAFT", "CHANGES", "QUOTED", "ACCEPTED");
    try {
      byte[] data = file.getBytes();
      String hash =
          fingerprint(
              "UPLOAD",
              Map.of(
                  "input",
                  b,
                  "file",
                  Pricing.hash(data),
                  "name",
                  Objects.toString(file.getOriginalFilename(), "")));
      if (duplicate("UPLOAD", b, id, hash)) return detail(o);
      version(o, b);
      Rules.check(data.length > 0 && data.length <= 5 * 1024 * 1024, "FILE_TOO_LARGE");
      Rules.check(records(ProofFile.class, id).size() < admin.setting("max_files"), "FILE_LIMIT");
      String name = Rules.text(file.getOriginalFilename(), 160, true);
      Rules.check(!name.contains("/") && !name.contains("\\"), "FILE_INVALID");
      String ext = name.toLowerCase(Locale.ROOT);
      String media;
      if (ext.endsWith(".pdf")
          && data.length >= 5
          && new String(data, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
        media = "application/pdf";
      else if (ext.endsWith(".png")
          && data.length >= 8
          && Arrays.equals(
              Arrays.copyOf(data, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
        media = "image/png";
      else if ((ext.endsWith(".jpg") || ext.endsWith(".jpeg"))
          && data.length >= 3
          && data[0] == (byte) 255
          && data[1] == (byte) 216
          && data[2] == (byte) 255) media = "image/jpeg";
      else throw new Problem(400, "FILE_INVALID");
      var p = new ProofFile();
      p.orderId = id;
      p.kind = kind;
      p.filename = name;
      p.content = data;
      p.mediaType = media;
      p.sha256 = Pricing.hash(data);
      p.size = data.length;
      p.createdAt = clock.instant();
      p.actorId = access.current().id;
      db.save(p);
      var entry = new LinkedHashMap<String, Object>(b);
      entry.put("fileId", p.id);
      entry.put("kind", p.kind);
      entry.put("filename", p.filename);
      event(o, "UPLOAD", entry, hash);
      db.flush();
      return detail(o);
    } catch (java.io.IOException e) {
      throw new Problem(400, "FILE_INVALID");
    }
  }

  /** 每次下载再次核验归属，不公开二进制路径。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public ProofFile file(long id) {
    var p = db.get(ProofFile.class, id);
    get(p.orderId);
    return p;
  }

  /** 客户、物料、供应商真实目录，已引用数据只能停用。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object masters(String kind) {
    staff("masters");
    return switch (kind) {
      case "customers" ->
          db.all(Customer.class).stream().filter(c -> access.department(c.departmentId)).toList();
      case "materials" ->
          db.all(Material.class).stream().filter(c -> access.department(c.departmentId)).toList();
      case "suppliers" ->
          db.all(Supplier.class).stream().filter(c -> access.department(c.departmentId)).toList();
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  /** 主数据写入保留身份，客户账号须是该组织专用客户角色。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object masterSave(String kind, Long id, Map<String, Object> b) {
    staff("masters");
    lock();
    Long dept = Rules.id(b.get("departmentId"));
    access.library(dept, "masters");
    Rules.check(db.get(Department.class, dept).enabled, "DEPARTMENT_DISABLED");
    switch (kind) {
      case "customers" -> {
        var c = id == null ? new Customer() : db.get(Customer.class, id);
        if (id != null) {
          Rules.check(c.departmentId.equals(dept), "IDENTITY_LOCKED");
          Rules.check(c.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
        }
        c.departmentId = dept;
        c.name = Rules.text(b.get("name"), 120, true);
        c.contact = Rules.text(b.get("contact"), 120, false);
        c.email = Rules.text(b.get("email"), 160, false);
        if (!c.email.isEmpty())
          Rules.check(c.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "EMAIL_INVALID");
        c.address = Rules.text(b.get("address"), 600, false);
        c.accountId =
            b.get("accountId") == null || Objects.toString(b.get("accountId")).isBlank()
                ? null
                : Rules.id(b.get("accountId"));
        if (c.accountId != null) {
          var a = db.get(Account.class, c.accountId);
          Rules.check(
              a.enabled
                  && a.departmentId.equals(dept)
                  && db.get(AccessRole.class, a.roleId).scope.equals("CUSTOMER"),
              "CUSTOMER_ACCOUNT_INVALID");
        }
        c.enabled = Rules.flag(b.get("enabled"));
        if (id == null) db.save(c);
        else c.version++;
        access.audit("CUSTOMER_SAVE", c.id, dept);
        return c;
      }
      case "materials" -> {
        var m = id == null ? new Material() : db.get(Material.class, id);
        if (id != null) {
          Rules.check(
              m.departmentId.equals(dept) && m.code.equals(b.get("code")), "IDENTITY_LOCKED");
          Rules.check(m.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
        }
        m.departmentId = dept;
        m.code = Rules.text(b.get("code"), 60, true);
        Rules.check(m.code.matches("[A-Z0-9_.-]{2,60}"), "INVALID_INPUT");
        m.name = Rules.text(b.get("name"), 120, true);
        String unit = Rules.text(b.get("unit"), 20, true);
        Rules.check(Set.of("SHEET", "M2", "PIECE").contains(unit), "UNIT_INVALID");
        if (id != null) Rules.check(m.unit.equals(unit), "IDENTITY_LOCKED");
        m.unit = unit;
        String cur = Rules.text(b.getOrDefault("currency", "CNY"), 3, true);
        Rules.check(Set.of("CNY", "USD", "EUR", "GBP").contains(cur), "CURRENCY_INVALID");
        if (id != null) Rules.check(m.currency.equals(cur), "IDENTITY_LOCKED");
        m.currency = cur;
        m.enabled = Rules.flag(b.get("enabled"));
        if (id == null) db.save(m);
        else m.version++;
        access.audit("MATERIAL_SAVE", m.id, dept);
        return m;
      }
      case "suppliers" -> {
        var s = id == null ? new Supplier() : db.get(Supplier.class, id);
        if (id != null) {
          Rules.check(s.departmentId.equals(dept), "IDENTITY_LOCKED");
          Rules.check(s.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
        }
        s.departmentId = dept;
        s.name = Rules.text(b.get("name"), 120, true);
        s.contact = Rules.text(b.get("contact"), 160, false);
        s.enabled = Rules.flag(b.get("enabled"));
        if (id == null) db.save(s);
        else s.version++;
        access.audit("SUPPLIER_SAVE", s.id, dept);
        return s;
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
  }

  /** 实际入库按凭据去重，移动平均值可追溯；不向供应商发采购。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object receiveMaterial(long id, Map<String, Object> b) {
    staff("masters");
    lock();
    var m = db.get(Material.class, id);
    access.library(m.departmentId, "masters");
    Rules.check(m.enabled, "MATERIAL_INVALID");
    Rules.check(m.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
    String ref = Rules.text(b.get("reference"), 160, true);
    Rules.check(
        db.query(
                StockTxn.class,
                "from StockTxn where materialId=?1 and kind='RECEIVE' and reference=?2",
                id,
                ref)
            .isEmpty(),
        "REFERENCE_DUPLICATE");
    var x = new StockTxn();
    x.materialId = id;
    x.kind = "RECEIVE";
    x.quantity =
        Pricing.decimal(
            b.get("quantity"), 4, new BigDecimal("0.0001"), new BigDecimal("1000000000"));
    x.cost = Pricing.decimal(b.get("cost"), 4, BigDecimal.ZERO, new BigDecimal("1000000000"));
    x.amount = x.quantity.multiply(x.cost).setScale(4, RoundingMode.HALF_UP);
    Rules.check(
        x.amount.precision() <= 18
            && m.stock.add(x.quantity).precision() <= 18
            && m.value.add(x.amount).precision() <= 18,
        "AMOUNT_LIMIT");
    x.reference = ref;
    x.note = Rules.paragraph(b.get("note"), 600, false);
    x.actorId = access.current().id;
    x.createdAt = clock.instant();
    db.save(x);
    m.stock = m.stock.add(x.quantity);
    m.value = m.value.add(x.amount);
    m.cost = m.value.divide(m.stock, 4, RoundingMode.HALF_UP);
    m.version++;
    access.audit("MATERIAL_RECEIVE", m.id, m.departmentId);
    return m;
  }

  /** 删除从未报价/上传/生产的草稿，保留内容最小化删除审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object deleteDraft(long id, Map<String, Object> b) {
    staff("sales");
    lock();
    var o = get(id);
    version(o, b);
    state(o, "DRAFT");
    Rules.check(o.revision == 0 && records(ProofFile.class, id).isEmpty(), "RECORD_REFERENCED");
    for (var e : records(BusinessEvent.class, id)) db.delete(e);
    for (var l : lines(id)) db.delete(l);
    access.audit("ORDER_DELETE", o.id, o.departmentId);
    db.delete(o);
    return Map.of("ok", true);
  }

  /** 真实订单、收款与成本按币种分别汇总；报价不称收入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object reports() {
    staff("reports");
    var orders = db.all(PrintOrder.class).stream().filter(this::readable).toList();
    var states = new TreeMap<String, Long>();
    for (var o : orders) states.merge(o.state, 1L, Long::sum);
    var currencies = new TreeMap<String, Map<String, BigDecimal>>();
    for (var o : orders) {
      var m = currencies.computeIfAbsent(o.currency, k -> new TreeMap<>());
      if (!Set.of("DRAFT", "CHANGES", "CANCELLED").contains(o.state))
        m.merge("quoted", o.total, BigDecimal::add);
      m.merge("received", paid(o.id), BigDecimal::add);
      var cost = materialCost(o.id).add(workCost(o.id)).add(vendorCost(o.id));
      m.merge("cost", cost, BigDecimal::add);
      if (o.state.equals("CLOSED"))
        m.merge("closedMargin", o.subtotal.subtract(o.discount).subtract(cost), BigDecimal::add);
    }
    return Map.of(
        "states",
        states,
        "currencies",
        currencies,
        "total",
        orders.size(),
        "late",
        orders.stream()
            .filter(
                o ->
                    !Set.of("CLOSED", "CANCELLED", "DELIVERED").contains(o.state)
                        && o.dueDate.isBefore(LocalDate.now(clock)))
            .count());
  }

  /** CSV防公式执行且记录审计，仅导出范围内商业摘要。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public String export() {
    staff("export");
    access.audit("ORDER_EXPORT", "ALL", access.current().departmentId);
    var s =
        new StringBuilder(
            "\uFEFFNumber,Title,Customer,State,Currency,Quoted,Net received,Material cost,Labor cost,Outsource cost,Due\r\n");
    for (var o : db.all(PrintOrder.class).stream().filter(this::readable).toList()) {
      s.append(
              String.join(
                  ",",
                  List.of(
                          String.format("PD-%06d", o.id),
                          o.title,
                          o.customerName,
                          o.state,
                          o.currency,
                          o.total.toPlainString(),
                          paid(o.id).toPlainString(),
                          materialCost(o.id).toPlainString(),
                          workCost(o.id).toPlainString(),
                          vendorCost(o.id).toPlainString(),
                          o.dueDate.toString())
                      .stream()
                      .map(Rules::csv)
                      .toList()))
          .append("\r\n");
    }
    return s.toString();
  }

  /** 审计记录不包含稿件、报价正文、密码及客户回执。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object audit() {
    staff("audit");
    return db.all(AuditEvent.class).stream()
        .filter(a -> access.department(a.departmentId))
        .sorted(Comparator.comparing((AuditEvent a) -> a.id).reversed())
        .toList();
  }
}
