package com.materiel.suite.client.net;

import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.service.*;
import com.materiel.suite.client.service.mock.*;

public class ServiceFactory {
  private static AppConfig cfg;
  private static QuoteService quoteService;
  private static OrderService orderService;
  private static DeliveryNoteService deliveryNoteService;
  private static InvoiceService invoiceService;

  public static void init(AppConfig c) {
    cfg = c;
    switch (cfg.getMode()) {
      case mock -> initMock();
      case backend -> initBackend();
      default -> initMock();
    }
  }

  private static void initMock() {
    MockData.seedIfEmpty();
    quoteService = new MockQuoteService();
    orderService = new MockOrderService();
    deliveryNoteService = new MockDeliveryNoteService();
    invoiceService = new MockInvoiceService();
  }

  private static void initBackend() {
    // TODO: brancher le SDK OpenAPI quand le mode online sera disponible
    MockData.seedIfEmpty();
    quoteService = new MockQuoteService(); // fallback temporaire
    orderService = new MockOrderService();
    deliveryNoteService = new MockDeliveryNoteService();
    invoiceService = new MockInvoiceService();
  }

  public static QuoteService quotes(){ return quoteService; }
  public static OrderService orders(){ return orderService; }
  public static DeliveryNoteService deliveryNotes(){ return deliveryNoteService; }
  public static InvoiceService invoices(){ return invoiceService; }
}
