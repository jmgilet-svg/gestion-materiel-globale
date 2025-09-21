package com.materiel.suite.backend.sales;

import com.materiel.suite.backend.sales.dto.QuoteV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/quotes")
public class QuoteCrudV2Controller {

  @GetMapping
  public List<QuoteV2Dto> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    Comparator<QuoteV2Dto> comparator = Comparator
        .comparing(QuoteV2Dto::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
        .reversed();
    return SalesMemoryStore.listQuotes().stream()
        .filter(q -> agencyId == null || agencyId.isBlank() || agencyId.equals(q.getAgencyId()))
        .sorted(comparator)
        .map(this::copyQuote)
        .toList();
  }

  @PostMapping
  public QuoteV2Dto create(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                           @RequestBody QuoteV2Dto input){
    QuoteV2Dto quote = copyQuote(input);
    if (quote.getId() == null || quote.getId().isBlank()){
      quote.setId(UUID.randomUUID().toString());
    }
    if (quote.getReference() == null || quote.getReference().isBlank()){
      quote.setReference(SalesMemoryStore.nextQuoteReference());
    }
    if (quote.getDate() == null){
      quote.setDate(LocalDate.now());
    }
    if (quote.getAgencyId() == null || quote.getAgencyId().isBlank()){
      quote.setAgencyId(agencyId);
    }
    if (quote.getSent() == null){
      quote.setSent(Boolean.FALSE);
    }
    SalesMemoryStore.putQuote(quote);
    return copyQuote(quote);
  }

  @PutMapping
  public QuoteV2Dto update(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                           @RequestBody QuoteV2Dto input){
    if (input == null || input.getId() == null || input.getId().isBlank()){
      throw new IllegalArgumentException("id required");
    }
    QuoteV2Dto quote = copyQuote(input);
    QuoteV2Dto existing = SalesMemoryStore.getQuote(quote.getId());
    if (quote.getReference() == null || quote.getReference().isBlank()){
      quote.setReference(existing != null && existing.getReference() != null
          ? existing.getReference()
          : SalesMemoryStore.nextQuoteReference());
    }
    if (quote.getDate() == null){
      quote.setDate(existing != null && existing.getDate() != null ? existing.getDate() : LocalDate.now());
    }
    if (quote.getAgencyId() == null || quote.getAgencyId().isBlank()){
      quote.setAgencyId(agencyId != null && !agencyId.isBlank()
          ? agencyId
          : existing == null ? null : existing.getAgencyId());
    }
    SalesMemoryStore.putQuote(quote);
    return copyQuote(quote);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id){
    SalesMemoryStore.removeQuote(id);
    return ResponseEntity.noContent().build();
  }

  private QuoteV2Dto copyQuote(QuoteV2Dto src){
    QuoteV2Dto copy = new QuoteV2Dto();
    if (src == null){
      return copy;
    }
    copy.setId(src.getId());
    copy.setReference(src.getReference());
    copy.setClientId(src.getClientId());
    copy.setClientName(src.getClientName());
    copy.setDate(src.getDate());
    copy.setStatus(src.getStatus());
    copy.setTotalHt(src.getTotalHt());
    copy.setTotalTtc(src.getTotalTtc());
    copy.setSent(src.getSent());
    copy.setAgencyId(src.getAgencyId());
    copy.setLines(src.getLines());
    return copy;
  }
}
