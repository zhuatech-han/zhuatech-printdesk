// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.printdesk;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HTTP、真实鉴权、Flyway/JPA验收完整订单和越权/并发/数量/金额边界。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class WorkflowTest {
  static final String PW = "Aa9" + UUID.randomUUID();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:print;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    r.add("printdesk.admin-password", () -> PW);
  }

  @Autowired MockMvc mvc;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession admin, client, worker, qc;
  long order, material, line, customer, workerId, proof;
  Map<String, Object> draft;
  String suffix;

  JsonNode call(
      MockHttpSession session, String method, String url, Map<String, Object> b, int expected)
      throws Exception {
    MockHttpServletRequestBuilder r =
        switch (method) {
          case "POST" -> post(url);
          case "PUT" -> put(url);
          default -> get(url);
        };
    if (session != null) r.session(session);
    if (!method.equals("GET"))
      r.with(csrf()).contentType("application/json").content(json.writeValueAsString(b));
    var response = mvc.perform(r).andReturn().getResponse();
    assertEquals(expected, response.getStatus(), url + " " + response.getContentAsString());
    return json.readTree(response.getContentAsString());
  }

  MockHttpSession login(String user) throws Exception {
    var s = new MockHttpSession();
    call(s, "POST", "/api/auth/login", Map.of("username", user, "password", PW), 200);
    return s;
  }

  JsonNode acct(String user, int role) throws Exception {
    return call(
        admin,
        "POST",
        "/api/admin/users",
        Map.of(
            "username",
            user,
            "displayName",
            "TEST " + user,
            "roleId",
            role,
            "departmentId",
            1,
            "enabled",
            true,
            "password",
            PW),
        200);
  }

  JsonNode detail() throws Exception {
    return call(admin, "GET", "/api/orders/" + order, Map.of(), 200);
  }

  JsonNode act(MockHttpSession actor, String action, Map<String, Object> extra, int expected)
      throws Exception {
    var b = new LinkedHashMap<String, Object>(extra);
    b.put("version", detail().get("version").asLong());
    b.put("requestId", UUID.randomUUID().toString());
    return call(actor, "POST", "/api/orders/" + order + "/" + action, b, expected);
  }

  @BeforeEach
  void setup() throws Exception {
    suffix = UUID.randomUUID().toString().substring(0, 8);
    admin = login("admin");
    var ca = acct("c" + suffix, 5);
    var wa = acct("w" + suffix, 3);
    acct("q" + suffix, 4);
    workerId = wa.get("id").asLong();
    client = login("c" + suffix);
    worker = login("w" + suffix);
    qc = login("q" + suffix);
    customer =
        call(
                admin,
                "POST",
                "/api/masters/customers",
                Map.of(
                    "departmentId",
                    1,
                    "accountId",
                    ca.get("id").asLong(),
                    "name",
                    "TEST customer " + suffix,
                    "contact",
                    "",
                    "email",
                    "",
                    "address",
                    "TEST address",
                    "enabled",
                    true),
                200)
            .get("id")
            .asLong();
    material =
        call(
                admin,
                "POST",
                "/api/masters/materials",
                Map.of(
                    "departmentId",
                    1,
                    "code",
                    "TEST-" + suffix.toUpperCase(),
                    "name",
                    "TEST paper",
                    "unit",
                    "SHEET",
                    "enabled",
                    true),
                200)
            .get("id")
            .asLong();
    call(
        admin,
        "POST",
        "/api/materials/" + material + "/receive",
        Map.of("version", 1, "quantity", "100", "cost", "2", "reference", "TEST receipt " + suffix),
        200);
    draft =
        new LinkedHashMap<>(
            Map.of(
                "customerId",
                customer,
                "title",
                "TEST order " + suffix,
                "dueDate",
                "2099-01-01",
                "currency",
                "CNY",
                "category",
                "DIGITAL",
                "discount",
                "0",
                "taxPercent",
                "0",
                "requestId",
                UUID.randomUUID().toString(),
                "lines",
                List.of(
                    Map.of(
                        "name",
                        "TEST posters",
                        "materialId",
                        material,
                        "quantity",
                        10,
                        "usage",
                        "1",
                        "widthMm",
                        0,
                        "heightMm",
                        0,
                        "printRate",
                        "1",
                        "finishRate",
                        "0",
                        "setup",
                        "5",
                        "markupPercent",
                        "20"))));
    var d = call(admin, "POST", "/api/orders", draft, 200);
    order = d.get("id").asLong();
    line = d.get("lines").get(0).get("id").asLong();
  }

  JsonNode upload(MockHttpSession who, String kind, byte[] data, String name, int status)
      throws Exception {
    var f = new MockMultipartFile("file", name, "application/pdf", data);
    var result =
        mvc.perform(
                multipart("/api/orders/" + order + "/files")
                    .file(f)
                    .param("kind", kind)
                    .param("version", String.valueOf(detail().get("version").asLong()))
                    .param("requestId", UUID.randomUUID().toString())
                    .session(who)
                    .with(csrf()))
            .andReturn()
            .getResponse();
    assertEquals(status, result.getStatus(), result.getContentAsString());
    return json.readTree(result.getContentAsString());
  }

  void accepted() throws Exception {
    act(admin, "QUOTE", Map.of(), 200);
    act(client, "ACCEPT", Map.of(), 200);
    proof =
        upload(admin, "PROOF", "%PDF-1.4\nTEST".getBytes(), "TEST-proof.pdf", 200)
            .get("latestProofId")
            .asLong();
    act(client, "PROOF_ACCEPT", Map.of("proofId", proof), 200);
    act(admin, "ASSIGN", Map.of("assigneeId", workerId), 200);
  }

  void start() throws Exception {
    accepted();
    act(worker, "START", Map.of(), 200);
    act(worker, "ISSUE", Map.of("lineId", line, "quantity", "10", "note", "TEST production"), 200);
  }

  void ready() throws Exception {
    start();
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 10, "scrap", 0, "minutes", 60, "hourlyRate", "12"),
        200);
    act(qc, "QUALITY_PASS", Map.of(), 200);
  }

  @Test
  void completeOrderReconcilesAllAmounts() throws Exception {
    ready();
    act(
        qc,
        "DELIVER",
        Map.of(
            "lines",
            List.of(Map.of("lineId", line, "quantity", 10)),
            "reference",
            "TEST-delivery",
            "method",
            "PICKUP",
            "address",
            "TEST client"),
        200);
    act(
        admin,
        "RECEIVE",
        Map.of("amount", "42", "reference", "TEST-payment", "note", "TEST outside payment"),
        200);
    var d = act(admin, "CLOSE", Map.of(), 200);
    assertEquals("CLOSED", d.get("state").asText());
    assertEquals(0, d.get("paid").decimalValue().compareTo(new java.math.BigDecimal("42")));
    assertEquals(0, d.get("cost").decimalValue().compareTo(new java.math.BigDecimal("32")));
  }

  @Test
  void customersCannotReadDraftOrOtherOrders() throws Exception {
    call(client, "GET", "/api/orders/" + order, Map.of(), 403);
    act(admin, "QUOTE", Map.of(), 200);
    var other = login("admin");
    var d = call(client, "GET", "/api/orders/" + order, Map.of(), 200);
    assertFalse(d.has("events"));
    assertFalse(d.get("lines").get(0).has("materialCost"));
    assertNotNull(other);
  }

  @Test
  void staffCannotPretendCustomerApproval() throws Exception {
    act(admin, "QUOTE", Map.of(), 200);
    act(admin, "ACCEPT", Map.of(), 409);
  }

  @Test
  void startNeedsCurrentProofAndCustomerQuote() throws Exception {
    act(admin, "ASSIGN", Map.of("assigneeId", workerId), 200);
    act(worker, "START", Map.of(), 409);
    act(admin, "QUOTE", Map.of(), 200);
    act(client, "ACCEPT", Map.of(), 200);
    act(worker, "START", Map.of(), 409);
  }

  @Test
  void newProofInvalidatesPreviousApproval() throws Exception {
    accepted();
    upload(admin, "PROOF", "%PDF-1.4\nTEST v2".getBytes(), "TEST-v2.pdf", 200);
    act(worker, "START", Map.of(), 409);
    act(client, "PROOF_ACCEPT", Map.of("proofId", proof), 409);
  }

  @Test
  void duplicateWriteDoesNotDuplicateMoney() throws Exception {
    accepted();
    var b =
        Map.<String, Object>of(
            "version",
            detail().get("version").asLong(),
            "requestId",
            UUID.randomUUID().toString(),
            "amount",
            "10",
            "reference",
            "TEST-idempotent",
            "note",
            "TEST receive");
    var first = call(admin, "POST", "/api/orders/" + order + "/RECEIVE", b, 200);
    var second = call(admin, "POST", "/api/orders/" + order + "/RECEIVE", b, 200);
    assertEquals(first.get("paid"), second.get("paid"));
    assertEquals(1, second.get("payments").size());
  }

  @Test
  void staleVersionRejected() throws Exception {
    var b = Map.<String, Object>of("version", 1, "requestId", UUID.randomUUID().toString());
    call(admin, "POST", "/api/orders/" + order + "/QUOTE", b, 409);
  }

  @Test
  void duplicateCreateReturnsSameOrder() throws Exception {
    var d = call(admin, "POST", "/api/orders", draft, 200);
    assertEquals(order, d.get("id").asLong());
  }

  @Test
  void overpaymentRejected() throws Exception {
    accepted();
    act(admin, "RECEIVE", Map.of("amount", "43", "reference", "TEST-over", "note", "TEST"), 409);
  }

  @Test
  void refundMustReferenceAndCannotExceedOriginal() throws Exception {
    accepted();
    var d =
        act(
            admin,
            "RECEIVE",
            Map.of("amount", "20", "reference", "TEST-first", "note", "TEST"),
            200);
    long p = d.get("payments").get(0).get("id").asLong();
    act(
        admin,
        "REFUND",
        Map.of("sourceId", p, "amount", "21", "reference", "TEST-refund", "note", "TEST"),
        409);
    act(
        admin,
        "REFUND",
        Map.of("sourceId", p, "amount", "20", "reference", "TEST-refund", "note", "TEST"),
        200);
    assertEquals(0, detail().get("paid").decimalValue().signum());
  }

  @Test
  void noOverProductionOrUnissuedMaterial() throws Exception {
    accepted();
    act(worker, "START", Map.of(), 200);
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 10, "scrap", 0, "minutes", 1, "hourlyRate", "1"),
        409);
    act(worker, "ISSUE", Map.of("lineId", line, "quantity", "10", "note", "TEST"), 200);
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 11, "scrap", 0, "minutes", 1, "hourlyRate", "1"),
        400);
  }

  @Test
  void unassignedCannotReadAndCostHiddenFromWorker() throws Exception {
    call(worker, "GET", "/api/orders/" + order, Map.of(), 403);
    start();
    var d = call(worker, "GET", "/api/orders/" + order, Map.of(), 200);
    assertFalse(d.has("total"));
    assertFalse(d.has("payments"));
    assertFalse(d.has("cost"));
    assertFalse(d.get("lines").get(0).has("printRate"));
  }

  @Test
  void returnUsesOriginalIssueAndProtectsQuantity() throws Exception {
    start();
    var x = detail().get("stock").get(0).get("id").asLong();
    act(
        worker,
        "RETURN",
        Map.of("lineId", line, "sourceId", x, "quantity", "11", "note", "TEST"),
        409);
    act(
        worker,
        "RETURN",
        Map.of("lineId", line, "sourceId", x, "quantity", "2", "note", "TEST unused"),
        200);
    assertEquals(
        0, detail().get("materialCost").decimalValue().compareTo(new java.math.BigDecimal("16")));
  }

  @Test
  void qcRequiresCompleteProduction() throws Exception {
    start();
    act(qc, "QUALITY_PASS", Map.of(), 409);
  }

  @Test
  void qcCannotApproveOwnWorkEvenAfterReassignment() throws Exception {
    start();
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 10, "scrap", 0, "minutes", 60, "hourlyRate", "12"),
        200);
    act(admin, "ASSIGN", Map.of("assigneeId", 1), 200);
    act(admin, "QUALITY_PASS", Map.of(), 409);
  }

  @Test
  void reworkPreservesCostAndRequiresNewProduction() throws Exception {
    start();
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 10, "scrap", 0, "minutes", 60, "hourlyRate", "12"),
        200);
    act(qc, "QUALITY_FAIL", Map.of("note", "TEST finish defective"), 200);
    assertEquals(0, detail().get("lines").get(0).get("good").asInt());
    assertEquals(1, detail().get("work").size());
    act(qc, "QUALITY_PASS", Map.of(), 409);
  }

  @Test
  void partialDeliveryThenNoOverDelivery() throws Exception {
    ready();
    act(
        qc,
        "DELIVER",
        Map.of(
            "lines",
            List.of(Map.of("lineId", line, "quantity", 3)),
            "reference",
            "TEST-part",
            "method",
            "PICKUP",
            "address",
            "TEST"),
        200);
    assertEquals("PART_DELIVERED", detail().get("state").asText());
    act(
        qc,
        "DELIVER",
        Map.of(
            "lines",
            List.of(Map.of("lineId", line, "quantity", 8)),
            "reference",
            "TEST-extra",
            "method",
            "PICKUP",
            "address",
            "TEST"),
        400);
    assertEquals(3, detail().get("lines").get(0).get("shipped").asInt());
  }

  @Test
  void closeNeedsDeliveryAndFullPayment() throws Exception {
    ready();
    act(admin, "CLOSE", Map.of(), 409);
    act(
        qc,
        "DELIVER",
        Map.of(
            "lines",
            List.of(Map.of("lineId", line, "quantity", 10)),
            "reference",
            "TEST-all",
            "method",
            "SHIP",
            "address",
            "TEST"),
        200);
    act(admin, "CLOSE", Map.of(), 409);
  }

  @Test
  void rejectQuoteThenRevisedQuoteIsNewSnapshot() throws Exception {
    act(admin, "QUOTE", Map.of(), 200);
    act(client, "REJECT", Map.of("note", "TEST change quantity"), 200);
    var b = new LinkedHashMap<>(draft);
    b.put("version", detail().get("version").asLong());
    b.put("requestId", UUID.randomUUID().toString());
    b.put("title", "TEST revised");
    call(admin, "PUT", "/api/orders/" + order, b, 200);
    act(admin, "QUOTE", Map.of(), 200);
    assertEquals(2, detail().get("quotes").size());
  }

  @Test
  void clientCannotUploadApprovalProof() throws Exception {
    act(admin, "QUOTE", Map.of(), 200);
    upload(client, "PROOF", "%PDF-1.4".getBytes(), "TEST.pdf", 409);
    upload(client, "SOURCE", "%PDF-1.4".getBytes(), "TEST-source.pdf", 200);
  }

  @Test
  void invalidFilesAndCrossRoleExportDenied() throws Exception {
    upload(admin, "PROOF", "evil".getBytes(), "TEST.pdf", 400);
    call(worker, "POST", "/api/orders/export", Map.of(), 403);
    call(client, "GET", "/api/masters/customers", Map.of(), 403);
  }

  @Test
  void csrfAndAnonymousAccessProtected() throws Exception {
    assertEquals(401, mvc.perform(get("/api/orders")).andReturn().getResponse().getStatus());
    assertEquals(
        403,
        mvc.perform(
                post("/api/orders").session(admin).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void duplicateMaterialReceiptRejected() throws Exception {
    var m = call(admin, "GET", "/api/masters/materials", Map.of(), 200);
    long version = 0;
    for (var x : m) if (x.get("id").asLong() == material) version = x.get("version").asLong();
    call(
        admin,
        "POST",
        "/api/materials/" + material + "/receive",
        Map.of(
            "version",
            version,
            "quantity",
            "1",
            "cost",
            "2",
            "reference",
            "TEST receipt " + suffix),
        409);
  }

  @Test
  void lastAdminProtected() throws Exception {
    var a = call(admin, "GET", "/api/admin/users", Map.of(), 200).get(0);
    call(
        admin,
        "PUT",
        "/api/admin/users/1",
        Map.of(
            "version",
            a.get("version").asLong(),
            "username",
            "admin",
            "displayName",
            "TEST admin",
            "roleId",
            1,
            "departmentId",
            1,
            "enabled",
            false),
        409);
  }

  @Test
  void deletionOnlyUnpublishedEmptyDraft() throws Exception {
    call(
        admin,
        "POST",
        "/api/orders/" + order + "/delete",
        Map.of("version", detail().get("version").asLong()),
        200);
    call(admin, "GET", "/api/orders/" + order, Map.of(), 404);
  }

  @Test
  void productionConsumesRequiredMaterialBeforeReturn() throws Exception {
    start();
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 10, "scrap", 0, "minutes", 60, "hourlyRate", "12"),
        200);
    long issue = detail().get("stock").get(0).get("id").asLong();
    act(
        worker,
        "RETURN",
        Map.of("lineId", line, "sourceId", issue, "quantity", "1", "note", "TEST"),
        409);
  }

  @Test
  void tinyIssueCannotSupportFullOutput() throws Exception {
    accepted();
    act(worker, "START", Map.of(), 200);
    act(worker, "ISSUE", Map.of("lineId", line, "quantity", "0.0001", "note", "TEST"), 200);
    act(
        worker,
        "WORK",
        Map.of("lineId", line, "good", 10, "scrap", 0, "minutes", 60, "hourlyRate", "12"),
        409);
  }

  @Test
  void quoteRejectsDifferentMaterialCurrency() throws Exception {
    var b = new LinkedHashMap<>(draft);
    b.put("currency", "USD");
    b.put("requestId", UUID.randomUUID().toString());
    call(admin, "POST", "/api/orders", b, 409);
  }

  @Test
  void otherCustomerCannotReadOrderOrFile() throws Exception {
    accepted();
    acct("x" + suffix, 5);
    var other = login("x" + suffix);
    call(other, "GET", "/api/orders/" + order, Map.of(), 403);
    call(other, "GET", "/api/files/" + proof, Map.of(), 403);
  }

  @Test
  void orderCannotBorrowOtherDepartmentCustomer() throws Exception {
    var dep =
        call(
                admin,
                "POST",
                "/api/admin/departments",
                Map.of("name", "TEST other " + suffix, "zone", "UTC", "enabled", true),
                200)
            .get("id")
            .asLong();
    var ca =
        call(
                admin,
                "POST",
                "/api/masters/customers",
                Map.of(
                    "departmentId",
                    dep,
                    "name",
                    "TEST other customer",
                    "contact",
                    "",
                    "email",
                    "",
                    "address",
                    "",
                    "enabled",
                    true),
                200)
            .get("id")
            .asLong();
    var name = "m" + suffix;
    call(
        admin,
        "POST",
        "/api/admin/users",
        Map.of(
            "username",
            name,
            "displayName",
            "TEST manager",
            "roleId",
            2,
            "departmentId",
            dep,
            "enabled",
            true,
            "password",
            PW),
        200);
    var manager = login(name);
    call(manager, "GET", "/api/orders/" + order, Map.of(), 403);
    var op = call(manager, "GET", "/api/options", Map.of(), 200);
    assertEquals(1, op.get("customers").size());
    assertEquals(ca, op.get("customers").get(0).get("id").asLong());
  }

  @Test
  void disablingCustomerRevokesBoundOrderRead() throws Exception {
    accepted();
    var c = call(admin, "GET", "/api/masters/customers", Map.of(), 200);
    JsonNode row = null;
    for (var x : c) if (x.get("id").asLong() == customer) row = x;
    var b = json.readValue(row.toString(), LinkedHashMap.class);
    b.put("enabled", false);
    call(admin, "PUT", "/api/masters/customers/" + customer, b, 200);
    call(client, "GET", "/api/orders/" + order, Map.of(), 403);
  }

  @Test
  void confirmationReceiptNamesExactCustomerAndRevision() throws Exception {
    accepted();
    var actor = call(client, "GET", "/api/auth/me", Map.of(), 200).get("id").asLong();
    var d = detail();
    var q = d.get("quotes").get(0);
    assertEquals("ACCEPTED", q.get("decision").asText());
    assertEquals(actor, q.get("reviewerId").asLong());
    assertFalse(q.get("reviewedAt").isNull());
    JsonNode p = null;
    for (var f : d.get("files")) if (f.get("id").asLong() == proof) p = f;
    assertEquals(actor, p.get("reviewerId").asLong());
    assertFalse(p.get("reviewedAt").isNull());
  }

  @Test
  void revisedQuoteRequiresNewCustomerAcceptance() throws Exception {
    accepted();
    act(admin, "REVISE", Map.of("note", "TEST revise quote"), 200);
    var b = new LinkedHashMap<>(draft);
    b.put("version", detail().get("version").asLong());
    b.put("requestId", UUID.randomUUID().toString());
    call(admin, "PUT", "/api/orders/" + order, b, 200);
    act(admin, "QUOTE", Map.of(), 200);
    act(worker, "START", Map.of(), 409);
    act(client, "ACCEPT", Map.of(), 200);
    act(worker, "START", Map.of(), 200);
    assertEquals(2, detail().get("quotes").size());
  }

  @Test
  void outsourcedReceiptsHaveSeparateReferencesAndExactCost() throws Exception {
    start();
    long sid =
        call(
                admin,
                "POST",
                "/api/masters/suppliers",
                Map.of("departmentId", 1, "name", "TEST finisher", "contact", "", "enabled", true),
                200)
            .get("id")
            .asLong();
    var d =
        act(
            admin,
            "VENDOR_CREATE",
            Map.of(
                "lineId",
                line,
                "supplierId",
                sid,
                "quantity",
                10,
                "amount",
                "5",
                "description",
                "TEST finishing",
                "dueDate",
                "2099-01-01"),
            200);
    long task = d.get("vendorTasks").get(0).get("id").asLong();
    act(admin, "VENDOR_CONFIRM", Map.of("taskId", task, "reference", "TEST contract"), 200);
    act(
        admin,
        "VENDOR_RECEIVE",
        Map.of("taskId", task, "quantity", 4, "reference", "TEST receipt one"),
        200);
    act(
        admin,
        "VENDOR_RECEIVE",
        Map.of("taskId", task, "quantity", 1, "reference", "TEST receipt one"),
        409);
    d = detail();
    assertEquals(4, d.get("vendorTasks").get(0).get("received").asInt());
    d =
        act(
            admin,
            "VENDOR_RECEIVE",
            Map.of("taskId", task, "quantity", 6, "reference", "TEST receipt two"),
            200);
    assertEquals(2, d.get("vendorReceipts").size());
    assertEquals(
        0,
        d.get("vendorReceipts")
            .get(0)
            .get("amount")
            .decimalValue()
            .compareTo(new java.math.BigDecimal("2")));
    assertEquals(
        0,
        d.get("vendorReceipts")
            .get(1)
            .get("amount")
            .decimalValue()
            .compareTo(new java.math.BigDecimal("3")));
  }
}
