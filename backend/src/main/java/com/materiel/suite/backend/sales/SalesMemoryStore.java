package com.materiel.suite.backend.sales;

import com.materiel.suite.backend.sales.dto.InvoiceV2Dto;
import com.materiel.suite.backend.sales.dto.QuoteV2Dto;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Stockage in-memory partagé entre les contrôleurs mock de ventes v2. */
final class SalesMemoryStore {
  private static final AtomicInteger QUOTE_SEQ = new AtomicInteger(1);
  private static final AtomicInteger INVOICE_SEQ = new AtomicInteger(1);
  private static final Map<String, QuoteV2Dto> QUOTES = new ConcurrentHashMap<>();
  private static final Map<String, InvoiceV2Dto> INVOICES = new ConcurrentHashMap<>();

  private SalesMemoryStore(){
  }

  static List<QuoteV2Dto> listQuotes(){
    return new ArrayList<>(QUOTES.values());
  }

  static QuoteV2Dto getQuote(String id){
    return id == null ? null : QUOTES.get(id);
  }

  static void putQuote(QuoteV2Dto quote){
    if (quote != null && quote.getId() != null){
      QUOTES.put(quote.getId(), quote);
    }
  }

  static void removeQuote(String id){
    if (id != null){
      QUOTES.remove(id);
    }
  }

  static String nextQuoteReference(){
    return String.format("Q%s-%04d", Year.now(), QUOTE_SEQ.getAndIncrement());
  }

  static List<InvoiceV2Dto> listInvoices(){
    return new ArrayList<>(INVOICES.values());
  }

  static InvoiceV2Dto getInvoice(String id){
    return id == null ? null : INVOICES.get(id);
  }

  static void putInvoice(InvoiceV2Dto invoice){
    if (invoice != null && invoice.getId() != null){
      INVOICES.put(invoice.getId(), invoice);
    }
  }

  static void removeInvoice(String id){
    if (id != null){
      INVOICES.remove(id);
    }
  }

  static String nextInvoiceNumber(){
    return String.format("F%s-%04d", Year.now(), INVOICE_SEQ.getAndIncrement());
  }
}
