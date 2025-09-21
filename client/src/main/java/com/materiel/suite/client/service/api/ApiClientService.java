package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.ClientService;

import java.util.*;

public class ApiClientService implements ClientService {
  private final RestClient rc;
  private final ClientService fb;
  public ApiClientService(RestClient rc, ClientService fallback){ this.rc=rc; this.fb=fallback; }

  @Override public List<Client> list(){
    try {
      String body = rc.get("/api/v1/clients");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Client> out = new ArrayList<>();
      for (Object o : arr) out.add(fromMap(SimpleJson.asObj(o)));
      return out;
    } catch(Exception e){ return fb.list(); }
  }
  @Override public Client get(UUID id){
    try { return fromMap(SimpleJson.asObj(SimpleJson.parse(rc.get("/api/v1/clients/"+id)))); }
    catch(Exception e){ return fb.get(id); }
  }
  @Override public Client save(Client c){
    try {
      String json = toJson(toMap(c));
      String body = (c.getId()==null? rc.post("/api/v1/clients", json) : rc.put("/api/v1/clients/"+c.getId(), json));
      return fromMap(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.save(c); }
  }
  @Override public void delete(UUID id){
    try { rc.delete("/api/v1/clients/"+id); } catch(Exception ignore){}
    fb.delete(id);
  }

  @Override public List<Contact> listContacts(UUID clientId){
    try {
      String body = rc.get("/api/v1/clients/"+clientId+"/contacts");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Contact> out = new ArrayList<>();
      for (Object o : arr) out.add(fromMapC(SimpleJson.asObj(o), clientId));
      return out;
    } catch(Exception e){ return fb.listContacts(clientId); }
  }
  @Override public Contact saveContact(UUID clientId, Contact ct){
    try {
      String json = toJson(toMapC(ct));
      String body = (ct.getId()==null?
          rc.post("/api/v1/clients/"+clientId+"/contacts", json) :
          rc.put("/api/v1/clients/"+clientId+"/contacts/"+ct.getId(), json));
      return fromMapC(SimpleJson.asObj(SimpleJson.parse(body)), clientId);
    } catch(Exception e){ return fb.saveContact(clientId, ct); }
  }
  @Override public void deleteContact(UUID clientId, UUID contactId){
    try { rc.delete("/api/v1/clients/"+clientId+"/contacts/"+contactId); } catch(Exception ignore){}
    fb.deleteContact(clientId, contactId);
  }

  /* Mapping helpers */
  private Map<String,Object> toMap(Client c){
    Map<String,Object> m = new LinkedHashMap<>();
    if (c.getId()!=null) m.put("id", c.getId().toString());
    m.put("name", c.getName()); m.put("code", c.getCode());
    m.put("email", c.getEmail()); m.put("phone", c.getPhone());
    m.put("vatNumber", c.getVatNumber());
    m.put("billingAddress", c.getBillingAddress());
    m.put("shippingAddress", c.getShippingAddress());
    m.put("notes", c.getNotes());
    m.put("agencyId", c.getAgencyId());
    return m;
  }
  private Client fromMap(Map<String,Object> m){
    Client c = new Client();
    String id = SimpleJson.str(m.get("id")); if (id!=null) c.setId(UUID.fromString(id));
    c.setName(SimpleJson.str(m.get("name")));
    c.setCode(SimpleJson.str(m.get("code")));
    c.setEmail(SimpleJson.str(m.get("email")));
    c.setPhone(SimpleJson.str(m.get("phone")));
    c.setVatNumber(SimpleJson.str(m.get("vatNumber")));
    c.setBillingAddress(SimpleJson.str(m.get("billingAddress")));
    c.setShippingAddress(SimpleJson.str(m.get("shippingAddress")));
    c.setNotes(SimpleJson.str(m.get("notes")));
    c.setAgencyId(SimpleJson.str(m.get("agencyId")));
    return c;
  }
  private Map<String,Object> toMapC(Contact c){
    Map<String,Object> m = new LinkedHashMap<>();
    if (c.getId()!=null) m.put("id", c.getId().toString());
    m.put("firstName", c.getFirstName()); m.put("lastName", c.getLastName());
    m.put("email", c.getEmail()); m.put("phone", c.getPhone());
    m.put("role", c.getRole()); m.put("archived", c.isArchived());
    return m;
  }
  private Contact fromMapC(Map<String,Object> m, UUID clientId){
    Contact c = new Contact();
    String id = SimpleJson.str(m.get("id")); if (id!=null) c.setId(UUID.fromString(id));
    c.setClientId(clientId);
    c.setFirstName(SimpleJson.str(m.get("firstName")));
    c.setLastName(SimpleJson.str(m.get("lastName")));
    c.setEmail(SimpleJson.str(m.get("email")));
    c.setPhone(SimpleJson.str(m.get("phone")));
    c.setRole(SimpleJson.str(m.get("role")));
    Object a = m.get("archived"); c.setArchived(a!=null && Boolean.parseBoolean(String.valueOf(a)));
    return c;
  }
  private String toJson(Map<String,Object> m){
    StringBuilder sb = new StringBuilder("{"); boolean first=true;
    for (var e : m.entrySet()){
      if (!first) sb.append(','); first=false;
      sb.append('"').append(e.getKey()).append('"').append(':');
      Object v = e.getValue();
      if (v==null) sb.append("null");
      else if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
      else sb.append('"').append(String.valueOf(v).replace("\\","\\\\").replace("\"","\\\"")).append('"');
    }
    return sb.append('}').toString();
  }
}

