package com.materiel.suite.client.service.api;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.DocumentWorkflowService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApiWorkflowService implements DocumentWorkflowService {
  private final RestClient rc;
  public ApiWorkflowService(RestClient rc){ this.rc = rc; }

  private void postWithMatch(String path, long version) throws Exception {
    Map<String,String> h = new HashMap<>();
    if (version>0) h.put("If-Match", "W/\""+version+"\"");
    rc.post(path, "{}", h);
  }
  @Override public void orderConfirm(UUID id, long v) throws Exception { postWithMatch("/api/v1/orders/"+id+":confirm", v); }
  @Override public void orderLock(UUID id, long v) throws Exception { postWithMatch("/api/v1/orders/"+id+":lock", v); }
  @Override public void orderCancel(UUID id, long v) throws Exception { postWithMatch("/api/v1/orders/"+id+":cancel", v); }
  @Override public void deliveryDeliver(UUID id, long v) throws Exception { postWithMatch("/api/v1/delivery-notes/"+id+":deliver", v); }
  @Override public void deliveryLock(UUID id, long v) throws Exception { postWithMatch("/api/v1/delivery-notes/"+id+":lock", v); }
  @Override public void deliveryCancel(UUID id, long v) throws Exception { postWithMatch("/api/v1/delivery-notes/"+id+":cancel", v); }
  @Override public void invoiceIssue(UUID id, long v) throws Exception { postWithMatch("/api/v1/invoices/"+id+":issue", v); }
  @Override public void invoicePay(UUID id, long v) throws Exception { postWithMatch("/api/v1/invoices/"+id+":pay", v); }
  @Override public void invoiceCancel(UUID id, long v) throws Exception { postWithMatch("/api/v1/invoices/"+id+":cancel", v); }
}

