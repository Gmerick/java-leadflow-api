package io.github.gmerick.leadflow;

import java.math.BigDecimal;

public record Campaign(long id, String name, String channel, BigDecimal cost) {}
