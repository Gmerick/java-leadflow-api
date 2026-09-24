package io.github.gmerick.leadflow;

import java.math.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {
  public record CampaignReport(
      long campaignId,
      String campaign,
      String channel,
      long leads,
      long conversions,
      BigDecimal conversionRatePercent,
      BigDecimal cost,
      BigDecimal revenue,
      BigDecimal costPerLead,
      BigDecimal roiPercent) {}

  private final LeadRepository repository;

  public LeadService(LeadRepository repository) {
    this.repository = repository;
  }

  public Campaign campaign(long id) {
    return repository
        .campaign(id)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Campanha não encontrada."));
  }

  public Campaign createCampaign(LeadController.CreateCampaign request) {
    long id =
        repository.createCampaign(
            request.name().trim(),
            request.channel().trim().toUpperCase(Locale.ROOT),
            request.cost());
    return campaign(id);
  }

  public List<Campaign> campaigns() {
    return repository.campaigns();
  }

  /**
   * Aceita DDD + número, opcionalmente com código brasileiro 55. Não valida existência ou
   * titularidade.
   */
  static String normalizePhone(String raw) {
    if (raw == null || !raw.matches("[+0-9(). -]+"))
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "Telefone deve conter somente dígitos e formatação usual.");
    String digits = raw.replaceAll("[^0-9]", "");
    if (digits.length() >= 12 && digits.startsWith("55")) digits = digits.substring(2);
    if (!digits.matches("[1-9][0-9][2-9][0-9]{7,8}"))
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "Informe DDD e telefone brasileiro com 10 ou 11 dígitos.");
    return digits;
  }

  @Transactional
  public Lead create(LeadController.CreateLead request) {
    campaign(request.campaignId());
    long id =
        repository.createLead(
            request.campaignId(),
            request.name().trim(),
            request.email().trim().toLowerCase(Locale.ROOT),
            normalizePhone(request.phone()));
    return get(id);
  }

  public Lead get(long id) {
    return repository
        .find(id)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Lead não encontrado."));
  }

  public List<Lead> list(Long campaignId, int limit, int offset) {
    if (limit < 1 || limit > 100 || offset < 0)
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "limit deve ser de 1 a 100 e offset não negativo.");
    if (campaignId != null) campaign(campaignId);
    return repository.list(campaignId, limit, offset);
  }

  @Transactional
  public Lead convert(long id, BigDecimal revenue) {
    get(id);
    if (!repository.convert(id, revenue))
      throw new BusinessException(
          HttpStatus.CONFLICT, "Lead já convertido; receita não pode ser contabilizada novamente.");
    return get(id);
  }

  @Transactional(
      readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public List<CampaignReport> report() {
    // Agrupamento em memória evidencia Collections/Streams; para grandes volumes, usar GROUP BY no
    // banco.
    Map<Long, List<Lead>> groups =
        repository.allForReport().stream().collect(Collectors.groupingBy(Lead::campaignId));
    return repository.campaigns().stream()
        .map(
            c -> {
              List<Lead> leads = groups.getOrDefault(c.id(), List.of());
              long conversions =
                  leads.stream().filter(l -> l.status() == Lead.Status.CONVERTED).count();
              BigDecimal revenue =
                  leads.stream()
                      .map(Lead::revenue)
                      .reduce(BigDecimal.ZERO, BigDecimal::add)
                      .setScale(2);
              BigDecimal rate =
                  leads.isEmpty()
                      ? BigDecimal.ZERO.setScale(2)
                      : BigDecimal.valueOf(conversions)
                          .multiply(BigDecimal.valueOf(100))
                          .divide(BigDecimal.valueOf(leads.size()), 2, RoundingMode.HALF_UP);
              BigDecimal cpl =
                  leads.isEmpty()
                      ? null
                      : c.cost().divide(BigDecimal.valueOf(leads.size()), 2, RoundingMode.HALF_UP);
              BigDecimal roi =
                  c.cost().signum() == 0
                      ? null
                      : revenue
                          .subtract(c.cost())
                          .multiply(BigDecimal.valueOf(100))
                          .divide(c.cost(), 2, RoundingMode.HALF_UP);
              return new CampaignReport(
                  c.id(),
                  c.name(),
                  c.channel(),
                  leads.size(),
                  conversions,
                  rate,
                  c.cost(),
                  revenue,
                  cpl,
                  roi);
            })
        .toList();
  }
}
