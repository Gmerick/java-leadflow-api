package io.github.gmerick.leadflow;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;

@Repository
public class LeadRepository {
  private final JdbcTemplate jdbc;
  private final RowMapper<Campaign> campaignMapper =
      (rs, n) ->
          new Campaign(
              rs.getLong("id"),
              rs.getString("name"),
              rs.getString("channel"),
              rs.getBigDecimal("cost"));
  private final RowMapper<Lead> leadMapper =
      (rs, n) ->
          new Lead(
              rs.getLong("id"),
              rs.getLong("campaign_id"),
              rs.getString("name"),
              rs.getString("email"),
              rs.getString("phone"),
              Lead.Status.valueOf(rs.getString("status")),
              rs.getBigDecimal("revenue"));

  public LeadRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private long key(KeyHolder keys) {
    return Objects.requireNonNull(keys.getKeys()).get("ID") instanceof Number id
        ? id.longValue()
        : Objects.requireNonNull(keys.getKey()).longValue();
  }

  public long createCampaign(String name, String channel, BigDecimal cost) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var p =
              c.prepareStatement(
                  "INSERT INTO campaigns(name,channel,cost) VALUES(?,?,?)",
                  Statement.RETURN_GENERATED_KEYS);
          p.setString(1, name);
          p.setString(2, channel);
          p.setBigDecimal(3, cost);
          return p;
        },
        keys);
    return key(keys);
  }

  public Optional<Campaign> campaign(long id) {
    return jdbc.query("SELECT * FROM campaigns WHERE id=?", campaignMapper, id).stream()
        .findFirst();
  }

  public List<Campaign> campaigns() {
    return jdbc.query("SELECT * FROM campaigns ORDER BY id", campaignMapper);
  }

  public long createLead(long campaignId, String name, String email, String phone) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var p =
              c.prepareStatement(
                  "INSERT INTO leads(campaign_id,name,email,phone,status,revenue)"
                      + " VALUES(?,?,?,?,'NEW',0)",
                  Statement.RETURN_GENERATED_KEYS);
          p.setLong(1, campaignId);
          p.setString(2, name);
          p.setString(3, email);
          p.setString(4, phone);
          return p;
        },
        keys);
    return key(keys);
  }

  public Optional<Lead> find(long id) {
    return jdbc.query("SELECT * FROM leads WHERE id=?", leadMapper, id).stream().findFirst();
  }

  public List<Lead> list(Long campaignId, int limit, int offset) {
    if (campaignId == null)
      return jdbc.query(
          "SELECT * FROM leads ORDER BY id LIMIT ? OFFSET ?", leadMapper, limit, offset);
    return jdbc.query(
        "SELECT * FROM leads WHERE campaign_id=? ORDER BY id LIMIT ? OFFSET ?",
        leadMapper,
        campaignId,
        limit,
        offset);
  }

  public List<Lead> allForReport() {
    return jdbc.query("SELECT * FROM leads ORDER BY id", leadMapper);
  }

  public boolean convert(long id, BigDecimal revenue) {
    return jdbc.update(
            "UPDATE leads SET status='CONVERTED',revenue=? WHERE id=? AND status='NEW'",
            revenue,
            id)
        == 1;
  }
}
