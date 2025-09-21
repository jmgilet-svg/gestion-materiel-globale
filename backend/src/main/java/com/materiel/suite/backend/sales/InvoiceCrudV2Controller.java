package com.materiel.suite.backend.sales;

import com.materiel.suite.backend.sales.dto.InvoiceV2Dto;
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
@RequestMapping("/api/v2/invoices")
public class InvoiceCrudV2Controller {

  @GetMapping
  public List<InvoiceV2Dto> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    Comparator<InvoiceV2Dto> comparator = Comparator
        .comparing(InvoiceV2Dto::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
        .reversed();
    return SalesMemoryStore.listInvoices().stream()
        .filter(f -> agencyId == null || agencyId.isBlank() || agencyId.equals(f.getAgencyId()))
        .sorted(comparator)
        .map(this::copyInvoice)
        .toList();
  }

  @PostMapping
  public InvoiceV2Dto create(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                             @RequestBody InvoiceV2Dto input){
    InvoiceV2Dto invoice = copyInvoice(input);
    if (invoice.getId() == null || invoice.getId().isBlank()){
      invoice.setId(UUID.randomUUID().toString());
    }
    if (invoice.getNumber() == null || invoice.getNumber().isBlank()){
      invoice.setNumber(SalesMemoryStore.nextInvoiceNumber());
    }
    if (invoice.getDate() == null){
      invoice.setDate(LocalDate.now());
    }
    if (invoice.getAgencyId() == null || invoice.getAgencyId().isBlank()){
      invoice.setAgencyId(agencyId);
    }
    SalesMemoryStore.putInvoice(invoice);
    return copyInvoice(invoice);
  }

  @PutMapping
  public InvoiceV2Dto update(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                             @RequestBody InvoiceV2Dto input){
    if (input == null || input.getId() == null || input.getId().isBlank()){
      throw new IllegalArgumentException("id required");
    }
    InvoiceV2Dto invoice = copyInvoice(input);
    InvoiceV2Dto existing = SalesMemoryStore.getInvoice(invoice.getId());
    if (invoice.getNumber() == null || invoice.getNumber().isBlank()){
      invoice.setNumber(existing != null && existing.getNumber() != null
          ? existing.getNumber()
          : SalesMemoryStore.nextInvoiceNumber());
    }
    if (invoice.getDate() == null){
      invoice.setDate(existing != null && existing.getDate() != null ? existing.getDate() : LocalDate.now());
    }
    if (invoice.getAgencyId() == null || invoice.getAgencyId().isBlank()){
      invoice.setAgencyId(agencyId != null && !agencyId.isBlank()
          ? agencyId
          : existing == null ? null : existing.getAgencyId());
    }
    SalesMemoryStore.putInvoice(invoice);
    return copyInvoice(invoice);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id){
    SalesMemoryStore.removeInvoice(id);
    return ResponseEntity.noContent().build();
  }

  private InvoiceV2Dto copyInvoice(InvoiceV2Dto src){
    InvoiceV2Dto copy = new InvoiceV2Dto();
    if (src == null){
      return copy;
    }
    copy.setId(src.getId());
    copy.setNumber(src.getNumber());
    copy.setClientId(src.getClientId());
    copy.setClientName(src.getClientName());
    copy.setDate(src.getDate());
    copy.setTotalHt(src.getTotalHt());
    copy.setTotalTtc(src.getTotalTtc());
    copy.setStatus(src.getStatus());
    copy.setAgencyId(src.getAgencyId());
    copy.setLines(src.getLines());
    return copy;
  }
}
