package io.github.gmerick.leadflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LeadController {
  public record CreateCampaign(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 40) String channel,
      @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal cost) {}

  public record CreateLead(
      @NotNull @Positive Long campaignId,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 30) String phone) {}

  public record Conversion(
      @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2)
          BigDecimal revenue) {}

  private final LeadService service;

  public LeadController(LeadService service) {
    this.service = service;
  }

  @PostMapping("/campaigns")
  ResponseEntity<Campaign> campaign(@Valid @RequestBody CreateCampaign request) {
    Campaign c = service.createCampaign(request);
    return ResponseEntity.created(URI.create("/api/campaigns/" + c.id())).body(c);
  }

  @GetMapping("/campaigns")
  List<Campaign> campaigns() {
    return service.campaigns();
  }

  @GetMapping("/campaigns/{id}")
  Campaign campaign(@PathVariable long id) {
    return service.campaign(id);
  }

  @PostMapping("/leads")
  ResponseEntity<Lead> create(@Valid @RequestBody CreateLead request) {
    Lead l = service.create(request);
    return ResponseEntity.created(URI.create("/api/leads/" + l.id())).body(l);
  }

  @GetMapping("/leads")
  List<Lead> list(
      @RequestParam(required = false) Long campaignId,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    return service.list(campaignId, limit, offset);
  }

  @GetMapping("/leads/{id}")
  Lead get(@PathVariable long id) {
    return service.get(id);
  }

  @PatchMapping("/leads/{id}/conversion")
  Lead convert(@PathVariable long id, @Valid @RequestBody Conversion request) {
    return service.convert(id, request.revenue());
  }

  @GetMapping("/reports/campaigns")
  List<LeadService.CampaignReport> report() {
    return service.report();
  }
}
