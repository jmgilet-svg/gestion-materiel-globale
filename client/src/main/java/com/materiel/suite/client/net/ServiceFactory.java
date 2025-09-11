package com.materiel.suite.client.net;

import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.service.*;
import com.materiel.suite.client.service.mock.*;
import com.materiel.suite.client.service.api.*;
import com.materiel.suite.client.net.RestClient;


public class ServiceFactory {
  private static AppConfig cfg;
  private static QuoteService quoteService;
  private static OrderService orderService;
  private static DeliveryNoteService deliveryNoteService;
  private static InvoiceService invoiceService;
  private static PlanningService planningService;


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
    planningService = new MockPlanningService();
  }

  private static void initBackend() {
    MockData.seedIfEmpty();
    String base = System.getenv().getOrDefault("GM_API_BASE", "http://localhost:8080");
    String token = System.getenv().getOrDefault("GM_API_TOKEN", "");
    RestClient rc = new RestClient(base, token);
    quoteService = new ApiQuoteService(rc, new MockQuoteService());
    orderService = new ApiOrderService(rc, new MockOrderService());
    deliveryNoteService = new ApiDeliveryNoteService(rc, new MockDeliveryNoteService());
    invoiceService = new ApiInvoiceService(rc, new MockInvoiceService());
    planningService = new ApiPlanningService(rc, new MockPlanningService());
  }

  public static QuoteService quotes(){ return quoteService; }
  public static OrderService orders(){ return orderService; }
  public static DeliveryNoteService deliveryNotes(){ return deliveryNoteService; }
  public static InvoiceService invoices(){ return invoiceService; }
  public static PlanningService planning(){ return planningService; }

}
