package com.materiel.suite.client.net;

import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.service.*;
import com.materiel.suite.client.service.api.*;
import com.materiel.suite.client.service.mock.*;
import com.materiel.suite.client.users.UserService;

public class ServiceFactory {
  private static AppConfig cfg;
  private static RestClient restClient;
  private static QuoteService quoteService;
  private static SalesService salesService;
  private static OrderService orderService;
  private static DeliveryNoteService deliveryNoteService;
  private static InvoiceService invoiceService;
  private static PlanningService planningService;
  private static DocumentWorkflowService workflowService;
  private static ClientService clientService;
  private static InterventionTypeService interventionTypeService;
  private static ResourceTypeService resourceTypeService;
  private static TemplateService templateService;
  private static TimelineService timelineService;
  private static AuthService authService;
  private static UserService userService;
  private static MailService mailService;

  public static void init(AppConfig c) {
    cfg = c;
    AuthContext.clear();
    authService = null;
    userService = null;
    templateService = null;
    salesService = null;
    timelineService = null;
    mailService = null;
    switch (cfg.getMode()) {
      case "mock" -> initMock();
      case "backend" -> initBackend();
      default -> initMock();
    }
  }

  private static void initMock() {
    MockData.seedIfEmpty();
    restClient = null;
    quoteService = new MockQuoteService();
    salesService = new MockSalesService();
    orderService = new MockOrderService();
    deliveryNoteService = new MockDeliveryNoteService();
    invoiceService = new MockInvoiceService();
    planningService = new MockPlanningService();
    workflowService = new MockWorkflowService();
    clientService = new MockClientService();
    interventionTypeService = new MockInterventionTypeService();
    resourceTypeService = new MockResourceTypeService();
    templateService = new MockTemplateService();
    timelineService = new MockTimelineService();
    mailService = new MockMailService();
    MockUserService mockUsers = new MockUserService();
    userService = mockUsers;
    authService = new MockAuthService(mockUsers);
  }

  private static void initBackend() {
    MockData.seedIfEmpty();
    String base = System.getenv().getOrDefault("GM_API_BASE", "http://localhost:8080");
    String token = System.getenv().getOrDefault("GM_API_TOKEN", "");
    RestClient rc = new RestClient(base, token);
    restClient = rc;
    quoteService = new ApiQuoteService(rc, new MockQuoteService());
    salesService = new ApiSalesService(rc, new MockSalesService());
    orderService = new ApiOrderService(rc, new MockOrderService());
    deliveryNoteService = new ApiDeliveryNoteService(rc, new MockDeliveryNoteService());
    invoiceService = new ApiInvoiceService(rc, new MockInvoiceService());
    planningService = new ApiPlanningService(rc, new MockPlanningService());
    workflowService = new ApiWorkflowService(rc);
    clientService = new ApiClientService(rc, new MockClientService());
    interventionTypeService = new ApiInterventionTypeService(rc, new MockInterventionTypeService());
    resourceTypeService = new ApiResourceTypeService(rc, new MockResourceTypeService());
    templateService = new ApiTemplateService(rc, new MockTemplateService());
    timelineService = new ApiTimelineService(rc, new MockTimelineService());
    mailService = new ApiMailService(rc, new MockMailService());
    MockUserService mockUsers = new MockUserService();
    userService = new ApiUserService(rc, mockUsers);
    authService = new ApiAuthService(rc, new MockAuthService(mockUsers));
  }

  public static QuoteService quotes(){ return quoteService; }
  public static SalesService sales(){ return salesService; }
  public static OrderService orders(){ return orderService; }
  public static DeliveryNoteService deliveryNotes(){ return deliveryNoteService; }
  public static InvoiceService invoices(){ return invoiceService; }
  public static PlanningService planning(){ return planningService; }
  public static DocumentWorkflowService workflow(){ return workflowService; }
  public static ClientService clients(){ return clientService; }
  public static InterventionTypeService interventionTypes(){ return interventionTypeService; }
  public static ResourceTypeService resourceTypes(){ return resourceTypeService; }
  public static TemplateService templates(){ return templateService; }
  public static TimelineService timeline(){ return timelineService; }
  public static MailService mail(){ return mailService; }
  public static RestClient http(){ return restClient; }
  public static AuthService auth(){ return authService; }
  public static UserService users(){ return userService; }
}

