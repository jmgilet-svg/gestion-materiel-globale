package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Fabrique simplifiée pour transformer une intervention en devis. */
public final class QuoteGenerator {
  private QuoteGenerator(){
  }

  public static Quote buildQuoteFromIntervention(Intervention intervention, List<BillingLine> billingLines){
    Quote quote = new Quote();
    if (intervention != null){
      quote.setClientId(intervention.getClientId());
      List<Contact> contacts = intervention.getContacts();
      if (contacts != null && !contacts.isEmpty()){
        Contact first = contacts.get(0);
        if (first != null){
          quote.setContactId(first.getId());
        }
      }
      quote.setCustomerName(safeName(intervention));
    }
    quote.setDate(LocalDate.now());
    quote.setStatus("Brouillon");
    List<DocumentLine> lines = toDocumentLines(billingLines);
    quote.setLines(lines);
    quote.recomputeTotals();
    return quote;
  }

  public static List<DocumentLine> toDocumentLines(List<BillingLine> lines){
    ArrayList<DocumentLine> result = new ArrayList<>();
    if (lines == null){
      return result;
    }
    for (BillingLine line : lines){
      if (line == null){
        continue;
      }
      DocumentLine doc = new DocumentLine();
      doc.setDesignation(line.getDesignation());
      BigDecimal quantity = line.getQuantity();
      doc.setQuantite(quantity != null ? quantity.doubleValue() : 0d);
      doc.setUnite(line.getUnit());
      BigDecimal unitPrice = line.getUnitPriceHt();
      doc.setPrixUnitaireHT(unitPrice != null ? unitPrice.doubleValue() : 0d);
      doc.setRemisePct(0d);
      doc.setTvaPct(Money.vatPercent().doubleValue());
      result.add(doc);
    }
    return result;
  }

  private static String safeName(Intervention intervention){
    if (intervention == null){
      return "";
    }
    String name = intervention.getClientName();
    if (name == null || name.isBlank()){
      name = intervention.getLabel();
    }
    return name == null ? "" : name;
  }
}
