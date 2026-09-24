package io.github.gmerick.leadflow;

import java.math.BigDecimal;

public record Lead(
    long id,
    long campaignId,
    String name,
    String email,
    String phone,
    Status status,
    BigDecimal revenue) {
  public enum Status {
    NEW,
    CONVERTED
  }
}
