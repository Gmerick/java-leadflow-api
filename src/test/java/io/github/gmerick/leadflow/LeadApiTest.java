package io.github.gmerick.leadflow;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:leadflow_test;DB_CLOSE_DELAY=-1"})
class LeadApiTest extends HttpTestSupport {
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM leads");
    jdbc.update("DELETE FROM campaigns");
  }

  long campaign(String cost) throws Exception {
    var result =
        call(
            "POST",
            "/api/campaigns",
            "{\"name\":\"Laboratório\",\"channel\":\"email\",\"cost\":" + cost + "}");
    assertEquals(201, result.status());
    return result.body().get("id").asLong();
  }

  Result lead(long campaign, String email, String phone) throws Exception {
    return call(
        "POST",
        "/api/leads",
        "{\"campaignId\":"
            + campaign
            + ",\"name\":\"Pessoa Exemplo\",\"email\":\""
            + email
            + "\",\"phone\":\""
            + phone
            + "\"}");
  }

  @Test
  void createNormalizesAndPersists() throws Exception {
    long campaign = campaign("100");
    var result = lead(campaign, "PESSOA@example.com", "+55 (11) 90000-0001");
    assertEquals(201, result.status());
    assertEquals("pessoa@example.com", result.body().get("email").asText());
    assertEquals("11900000001", result.body().get("phone").asText());
    assertEquals(
        "EMAIL", call("GET", "/api/campaigns/" + campaign, null).body().get("channel").asText());
    assertEquals("11900000001", jdbc.queryForObject("SELECT phone FROM leads", String.class));
    assertEquals(
        200, call("GET", result.headers().firstValue("Location").orElseThrow(), null).status());
  }

  @Test
  void duplicateEmailOrPhoneIsBlockedGlobally() throws Exception {
    long campaign = campaign("100");
    lead(campaign, "pessoa@example.com", "11900000001");
    assertEquals(409, lead(campaign, "PESSOA@example.com", "11900000002").status());
    assertEquals(409, lead(campaign, "outra@example.com", "+55 (11) 90000-0001").status());
    assertEquals(409, lead(campaign("0"), "pessoa@example.com", "11900000003").status());
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM leads", Integer.class));
  }

  @Test
  void conversionCannotDoubleCountRevenue() throws Exception {
    long id = lead(campaign("100"), "pessoa@example.com", "11900000001").body().get("id").asLong();
    assertEquals(
        200, call("PATCH", "/api/leads/" + id + "/conversion", "{\"revenue\":300}").status());
    assertEquals(
        409, call("PATCH", "/api/leads/" + id + "/conversion", "{\"revenue\":600}").status());
    assertEquals(300, call("GET", "/api/leads/" + id, null).body().get("revenue").asInt());
  }

  @Test
  void reportCalculatesRateCostAndRoi() throws Exception {
    long c = campaign("100");
    long id = lead(c, "a@example.com", "11900000001").body().get("id").asLong();
    lead(c, "b@example.com", "11900000002");
    call("PATCH", "/api/leads/" + id + "/conversion", "{\"revenue\":300}");
    var report = call("GET", "/api/reports/campaigns", null).body().get(0);
    assertEquals(2, report.get("leads").asInt());
    assertEquals(1, report.get("conversions").asInt());
    assertEquals(50, report.get("conversionRatePercent").asInt());
    assertEquals(50, report.get("costPerLead").asInt());
    assertEquals(200, report.get("roiPercent").asInt());
  }

  @Test
  void reportHandlesNoLeadsAndZeroCost() throws Exception {
    campaign("0");
    var report = call("GET", "/api/reports/campaigns", null).body().get(0);
    assertEquals(0, report.get("leads").asInt());
    assertEquals(0, report.get("conversionRatePercent").asInt());
    assertTrue(report.get("costPerLead").isNull());
    assertTrue(report.get("roiPercent").isNull());
  }

  @Test
  void unknownCampaignAndLeadReturn404() throws Exception {
    assertEquals(404, lead(999999, "a@example.com", "11900000001").status());
    assertEquals(404, call("GET", "/api/leads/999999", null).status());
    assertEquals(404, call("GET", "/api/campaigns/999999", null).status());
  }

  @Test
  void invalidEmailAndPhoneReturn400() throws Exception {
    long c = campaign("0");
    assertEquals(400, lead(c, "invalid", "11900000001").status());
    assertEquals(400, lead(c, "a@example.com", "123").status());
    assertEquals(400, lead(c, "a@example.com", "abc11900000001").status());
  }

  @Test
  void invalidMoneyIsRejectedWithoutWrite() throws Exception {
    assertEquals(
        400,
        call("POST", "/api/campaigns", "{\"name\":\"x\",\"channel\":\"y\",\"cost\":-1}").status());
    assertEquals(
        400,
        call("POST", "/api/campaigns", "{\"name\":\"x\",\"channel\":\"y\",\"cost\":1.001}")
            .status());
    long id = lead(campaign("0"), "a@example.com", "11900000001").body().get("id").asLong();
    for (String value : new String[] {"0", "-1", "0.001", "10000000000"})
      assertEquals(
          400,
          call("PATCH", "/api/leads/" + id + "/conversion", "{\"revenue\":" + value + "}")
              .status());
    assertEquals("NEW", call("GET", "/api/leads/" + id, null).body().get("status").asText());
  }

  @Test
  void paginationAndCampaignFilter() throws Exception {
    long a = campaign("0"), b = campaign("0");
    lead(a, "a@example.com", "11900000001");
    lead(b, "b@example.com", "11900000002");
    assertEquals(1, call("GET", "/api/leads?campaignId=" + a, null).body().size());
    assertEquals(1, call("GET", "/api/leads?limit=1&offset=1", null).body().size());
    assertEquals(400, call("GET", "/api/leads?limit=0", null).status());
  }

  @Test
  void reportSeparatesCampaignsAndRounds() throws Exception {
    long c = campaign("10");
    lead(c, "a@example.com", "11900000001");
    lead(c, "b@example.com", "11900000002");
    lead(c, "c@example.com", "11900000003");
    campaign("20");
    var reports = call("GET", "/api/reports/campaigns", null).body();
    assertEquals(3.33, reports.get(0).get("costPerLead").asDouble(), 0.001);
    assertEquals(0, reports.get(1).get("leads").asInt());
    assertEquals(-100, reports.get(1).get("roiPercent").asInt());
  }

  @Test
  void malformedRequestsReturn400() throws Exception {
    assertEquals(400, call("POST", "/api/leads", "{").status());
    assertEquals(400, call("GET", "/api/leads/abc", null).status());
    assertEquals(400, call("POST", "/api/leads", "{}").status());
  }
}
