package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.PlanningValidation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link PlanningService} backed by the REST API.
 * In case of any network or parsing error, operations transparently
 * fall back to the provided fallback service.
 */
public class ApiPlanningService implements PlanningService {
  private final RestClient rc; private final PlanningService fb;
  public ApiPlanningService(RestClient rc, PlanningService fallback){ this.rc=rc; this.fb=fallback; }

  private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Override public List<Resource> listResources(){
    try {
      String body = rc.get("/api/v1/resources");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Resource> out = new ArrayList<>();
      for (Object o : arr){
        var m = SimpleJson.asObj(o);
        Resource r = new Resource();
        fillResource(r, m);
        out.add(r);
      }
      Map<String, ResourceType> catalog = fetchResourceTypeCatalog();
      if (!catalog.isEmpty()){
        for (Resource r : out){
          mergeType(r, catalog);
        }
      }
      return out;
    } catch(Exception e){ return fb.listResources(); }
  }

  @Override public Resource saveResource(Resource r){
    try {
      Map<String,Object> m = new LinkedHashMap<>();
      if (r.getId()!=null) m.put("id", r.getId().toString());
      m.put("name", r.getName());
      ResourceType type = r.getType();
      if (type!=null){
        Map<String,Object> tm = toMap(type);
        if (!tm.isEmpty()) m.put("type", tm);
      }
      m.put("unitPriceHt", r.getUnitPriceHt());
      m.put("color", r.getColor());
      m.put("notes", r.getNotes());
      m.put("email", r.getEmail());
      m.put("agencyId", r.getAgencyId());
      // === CRM-INJECT BEGIN: resource-api-write ===
      m.put("capacity", r.getCapacity());
      m.put("tags", r.getTags());
      m.put("weeklyUnavailability", r.getWeeklyUnavailability());
      if (!r.getUnavailabilities().isEmpty()){
        List<Map<String,Object>> ua = new ArrayList<>();
        for (Unavailability u : r.getUnavailabilities()){
          ua.add(toMap(u));
        }
        m.put("unavailabilities", ua);
      }
      // === CRM-INJECT END ===
      String json = toJson(m);
      String body = (r.getId()==null? rc.post("/api/v1/resources", json) : rc.put("/api/v1/resources/"+r.getId(), json));
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      fillResource(r, map);
      mergeType(r, fetchResourceTypeCatalog());
      return r;
    } catch(Exception e){ return fb.saveResource(r); }
  }

  @Override public Resource getResource(UUID id){
    if (id == null){
      return null;
    }
    try {
      String body = rc.get("/api/v1/resources/" + id);
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      Resource resource = new Resource();
      fillResource(resource, map);
      mergeType(resource, fetchResourceTypeCatalog());
      return resource;
    } catch(Exception e){
      return fb.getResource(id);
    }
  }

  @Override public void deleteResource(UUID id){
    try { rc.delete("/api/v1/resources/"+id); } catch(Exception ignore){}
    fb.deleteResource(id);
  }

  @Override public List<ResourceType> listResourceTypes(){
    try {
      String body = rc.get("/api/v1/resource-types");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<ResourceType> list = new ArrayList<>();
      for (Object o : arr){
        ResourceType t = parseResourceType(o);
        if (t!=null) list.add(t);
      }
      return list;
    } catch(Exception e){
      return fb.listResourceTypes();
    }
  }

  @Override public ResourceType createResourceType(ResourceType type){
    String code = type!=null? type.getCode() : null;
    if (code==null || code.isBlank()) throw new IllegalArgumentException("code requis");
    try {
      String body = rc.post("/api/v1/resource-types", toJson(toMap(type)));
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      ResourceType created = parseResourceType(map);
      return created!=null? created : type;
    } catch(Exception e){
      return fb.createResourceType(type);
    }
  }

  @Override public ResourceType updateResourceType(ResourceType type){
    String code = type!=null? type.getCode() : null;
    if (code==null || code.isBlank()) throw new IllegalArgumentException("code requis");
    try {
      String body = rc.put("/api/v1/resource-types/"+code, toJson(toMap(type)));
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      ResourceType updated = parseResourceType(map);
      return updated!=null? updated : type;
    } catch(Exception e){
      return fb.updateResourceType(type);
    }
  }

  @Override public void deleteResourceType(String code){
    if (code==null || code.isBlank()) return;
    try { rc.delete("/api/v1/resource-types/"+code); } catch(Exception ignore){}
    fb.deleteResourceType(code);
  }

  @Override public List<Unavailability> listResourceUnavailabilities(UUID resourceId){
    try {
      String body = rc.get("/api/v1/resources/"+resourceId+"/unavailability");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Unavailability> list = new ArrayList<>();
      for (Object o : arr){
        if (o instanceof Map<?,?> mm) list.add(parseUnavailability(mm));
      }
      return list;
    } catch(Exception e){
      return fb.listResourceUnavailabilities(resourceId);
    }
  }

  @Override public Unavailability addUnavailability(UUID resourceId, Unavailability u){
    try {
      String body = rc.post("/api/v1/resources/"+resourceId+"/unavailability", toJson(toMap(u)));
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      return parseUnavailability(map);
    } catch(Exception e){
      return fb.addUnavailability(resourceId, u);
    }
  }

  @Override public void deleteUnavailability(UUID resourceId, UUID unavailabilityId){
    try { rc.delete("/api/v1/resources/"+resourceId+"/unavailability/"+unavailabilityId); } catch(Exception ignore){}
    fb.deleteUnavailability(resourceId, unavailabilityId);
  }

  @Override public List<Intervention> listInterventions(LocalDate from, LocalDate to){
    try {
      String body = rc.get("/api/v1/interventions?from="+DF.format(from)+"&to="+DF.format(to));
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Intervention> out = new ArrayList<>();
      for (Object o : arr){
        var m = SimpleJson.asObj(o);
        out.add(fromMap(m));
      }
      return out;
    } catch(Exception e){ return fb.listInterventions(from,to); }
  }

  @Override public Intervention saveIntervention(Intervention i){
    try {
      String json = toJson(toMap(i));
      String body = (i.getId()==null? rc.post("/api/v1/interventions", json) : rc.put("/api/v1/interventions/"+i.getId(), json));
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      return fromMap(map);
    } catch(Exception e){ return fb.saveIntervention(i); }
  }

  @Override public void deleteIntervention(UUID id){
    try { rc.delete("/api/v1/interventions/"+id);} catch(Exception ignore){}
    fb.deleteIntervention(id);
  }

  @Override public PlanningValidation validate(Intervention i){
    try {
      String body = rc.post("/api/v1/interventions/validate", toJson(toMap(i)));
      var m = SimpleJson.asObj(SimpleJson.parse(body));
      PlanningValidation v = new PlanningValidation();
      v.ok = SimpleJson.bool(m.get("ok"));
      var arr = SimpleJson.asArr(m.get("suggestions"));
      for (Object o : arr){
        var mm = SimpleJson.asObj(o);
        PlanningValidation.Suggestion s = new PlanningValidation.Suggestion();
        String rid = SimpleJson.str(mm.get("resourceId"));
        if (rid!=null && !rid.isBlank()) s.resourceId = UUID.fromString(rid);
        String sdt = SimpleJson.str(mm.get("startDateTime"));
        String edt = SimpleJson.str(mm.get("endDateTime"));
        if (sdt!=null && !sdt.isBlank()) s.startDateTime = LocalDateTime.parse(sdt);
        if (edt!=null && !edt.isBlank()) s.endDateTime = LocalDateTime.parse(edt);
        s.label = SimpleJson.str(mm.get("label"));
        v.suggestions.add(s);
      }
      return v;
    } catch(Exception e){
      return PlanningService.super.validate(i);
    }
  }

  @Override public List<Conflict> listConflicts(LocalDate from, LocalDate to){
    try {
      String body = rc.get("/api/v1/planning/conflicts?from="+DF.format(from)+"&to="+DF.format(to));
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Conflict> list = new ArrayList<>();
      for (Object o : arr){
        var m = SimpleJson.asObj(o);
        list.add(new Conflict(
            UUID.fromString(SimpleJson.str(m.get("a"))),
            UUID.fromString(SimpleJson.str(m.get("b"))),
            UUID.fromString(SimpleJson.str(m.get("resourceId")))
        ));
      }
      return list;
    } catch(Exception e){ return fb.listConflicts(from,to); }
  }

  @Override public boolean resolveShift(UUID id, int minutes){
    try {
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("action","shift");
      m.put("id", id.toString());
      m.put("minutes", minutes);
      rc.post("/api/v1/planning/resolve", toJson(m));
      return true;
    } catch(Exception e){ return fb.resolveShift(id, minutes); }
  }

  @Override public boolean resolveReassign(UUID id, UUID resourceId){
    try {
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("action","reassign");
      m.put("id", id.toString());
      m.put("resourceId", resourceId.toString());
      rc.post("/api/v1/planning/resolve", toJson(m));
      return true;
    } catch(Exception e){ return fb.resolveReassign(id, resourceId); }
  }

  @Override public boolean resolveSplit(UUID id, LocalDateTime splitAt){
    try {
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("action","split");
      m.put("id", id.toString());
      m.put("splitAt", splitAt.format(DTF));
      rc.post("/api/v1/planning/resolve", toJson(m));
      return true;
    } catch(Exception e){ return fb.resolveSplit(id, splitAt); }
  }

  /* ===== Mapping helpers ===== */
  private Map<String,Object> toMap(Intervention it){
    Map<String,Object> m = new LinkedHashMap<>();
    if (it.getId()!=null) m.put("id", it.getId().toString());
    m.put("resourceId", it.getResourceId()!=null? it.getResourceId().toString() : null);
    List<ResourceRef> refs = it.getResources();
    if (!refs.isEmpty()){
      List<Map<String,Object>> arr = new ArrayList<>();
      for (ResourceRef ref : refs){
        Map<String,Object> rm = toMap(ref);
        if (!rm.isEmpty()) arr.add(rm);
      }
      if (!arr.isEmpty()) m.put("resources", arr);
    }
    // === CRM-INJECT BEGIN: planning-api-client-id ===
    m.put("clientId", it.getClientId()!=null? it.getClientId().toString() : null);
    m.put("agencyId", it.getAgencyId());
    // === CRM-INJECT END ===
    m.put("label", it.getLabel());
    m.put("color", it.getColor());
    m.put("description", it.getDescription());
    m.put("internalNote", it.getInternalNote());
    m.put("closingNote", it.getClosingNote());
    m.put("signatureBy", it.getSignatureBy());
    m.put("signatureAt", it.getSignatureAt()!=null? it.getSignatureAt().format(DTF) : null);
    m.put("signaturePngBase64", it.getSignaturePngBase64());
    if (it.getDateDebut()!=null) m.put("dateDebut", it.getDateDebut().toString());
    if (it.getDateFin()!=null) m.put("dateFin", it.getDateFin().toString());
    if (it.getStartDateTime()!=null) m.put("startDateTime", it.getStartDateTime().format(DTF));
    if (it.getEndDateTime()!=null) m.put("endDateTime", it.getEndDateTime().format(DTF));
    if (it.getStatus()!=null) m.put("status", it.getStatus());
    return m;
  }

  private Map<String,Object> toMap(ResourceRef ref){
    Map<String,Object> map = new LinkedHashMap<>();
    if (ref==null) return map;
    if (ref.getId()!=null) map.put("id", ref.getId().toString());
    if (ref.getName()!=null && !ref.getName().isBlank()) map.put("name", ref.getName());
    if (ref.getIcon()!=null && !ref.getIcon().isBlank()) map.put("icon", ref.getIcon());
    return map;
  }

  private Map<String,Object> toMap(ResourceType type){
    Map<String,Object> map = new LinkedHashMap<>();
    if (type==null) return map;
    if (type.getCode()!=null && !type.getCode().isBlank()) map.put("code", type.getCode());
    if (type.getLabel()!=null && !type.getLabel().isBlank()) map.put("label", type.getLabel());
    if (type.getIcon()!=null && !type.getIcon().isBlank()) map.put("icon", type.getIcon());
    return map;
  }

  private Intervention fromMap(Map<String,Object> m){
    Intervention it = new Intervention();
    String id = SimpleJson.str(m.get("id"));
    if (id!=null && !id.isBlank()) it.setId(UUID.fromString(id));
    String rid = SimpleJson.str(m.get("resourceId"));
    List<ResourceRef> refs = parseResourceRefs(m.get("resources"));
    if (!refs.isEmpty()){
      it.setResources(refs);
      if ((rid==null || rid.isBlank()) && refs.get(0)!=null && refs.get(0).getId()!=null){
        rid = refs.get(0).getId().toString();
      }
    }
    if ((rid==null || rid.isBlank()) && m.get("resource")!=null){
      Map<String,Object> res = SimpleJson.asObj(m.get("resource"));
      ResourceRef ref = parseResourceRef(res);
      if (ref!=null){
        it.setResources(List.of(ref));
        if (ref.getId()!=null) rid = ref.getId().toString();
      }
    }
    if (rid!=null && !rid.isBlank()) it.setResourceId(UUID.fromString(rid));
    // === CRM-INJECT BEGIN: planning-api-client-mapping ===
    String cid = SimpleJson.str(m.get("clientId"));
    if (cid!=null && !cid.isBlank()) it.setClientId(UUID.fromString(cid));
    String cname = SimpleJson.str(m.get("clientName"));
    if (cname!=null) it.setClientName(cname);
    String agencyId = SimpleJson.str(m.get("agencyId"));
    if (agencyId!=null) it.setAgencyId(agencyId);
    String agencyLabel = SimpleJson.str(m.get("agency"));
    if (agencyLabel!=null) it.setAgency(agencyLabel);
    // === CRM-INJECT END ===
    it.setLabel(SimpleJson.str(m.get("label")));
    it.setColor(SimpleJson.str(m.get("color")));
    it.setDescription(SimpleJson.str(m.get("description")));
    it.setInternalNote(SimpleJson.str(m.get("internalNote")));
    it.setClosingNote(SimpleJson.str(m.get("closingNote")));
    it.setSignatureBy(SimpleJson.str(m.get("signatureBy")));
    String sigAt = SimpleJson.str(m.get("signatureAt"));
    if (sigAt!=null && !sigAt.isBlank()){
      try { it.setSignatureAt(LocalDateTime.parse(sigAt)); } catch(Exception ignore){ it.setSignatureAt(null); }
    } else {
      it.setSignatureAt(null);
    }
    it.setSignaturePngBase64(SimpleJson.str(m.get("signaturePngBase64")));
    String d1 = SimpleJson.str(m.get("dateDebut"));
    String d2 = SimpleJson.str(m.get("dateFin"));
    if (d1!=null && !d1.isBlank()) it.setDateDebut(LocalDate.parse(d1));
    if (d2!=null && !d2.isBlank()) it.setDateFin(LocalDate.parse(d2));
    String sdt = SimpleJson.str(m.get("startDateTime"));
    String edt = SimpleJson.str(m.get("endDateTime"));
    try {
      if (sdt!=null && !sdt.isBlank()) it.setDateHeureDebut(LocalDateTime.parse(sdt));
      if (edt!=null && !edt.isBlank()) it.setDateHeureFin(LocalDateTime.parse(edt));
    } catch(Exception ignore){}
    String st = SimpleJson.str(m.get("status"));
    if (st!=null && !st.isBlank()) it.setStatus(st);
    return it;
  }

  private List<ResourceRef> parseResourceRefs(Object value){
    List<ResourceRef> list = new ArrayList<>();
    if (value instanceof List<?> arr){
      for (Object o : arr){
        if (o instanceof Map<?,?> mm){
          ResourceRef ref = parseResourceRef(mm);
          if (ref!=null) list.add(ref);
        }
      }
    } else if (value instanceof Map<?,?> mm){
      ResourceRef ref = parseResourceRef(mm);
      if (ref!=null) list.add(ref);
    }
    return list;
  }

  private ResourceRef parseResourceRef(Map<?,?> map){
    if (map==null) return null;
    ResourceRef ref = new ResourceRef();
    String id = SimpleJson.str(map.get("id"));
    if (id!=null && !id.isBlank()){
      try { ref.setId(UUID.fromString(id)); } catch(Exception ignore){}
    }
    String name = SimpleJson.str(map.get("name"));
    if (name!=null && !name.isBlank()) ref.setName(name);
    String icon = SimpleJson.str(map.get("icon"));
    if (icon!=null && !icon.isBlank()) ref.setIcon(icon);
    if (ref.getId()==null && (ref.getName()==null || ref.getName().isBlank())
        && (ref.getIcon()==null || ref.getIcon().isBlank())){
      return null;
    }
    return ref;
  }

  private String toJson(Map<String,Object> m){
    return toJsonMap(m);
  }

  private String toJsonMap(Map<?,?> map){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (var e : map.entrySet()){
      Object key = e.getKey();
      if (!(key instanceof String)) continue;
      if (!first) sb.append(',');
      first = false;
      sb.append('"').append(escape((String) key)).append('"').append(':');
      appendJsonValue(sb, e.getValue());
    }
    sb.append('}');
    return sb.toString();
  }

  private String toJsonList(List<?> list){
    StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (Object v : list){
      if (!first) sb.append(',');
      first = false;
      appendJsonValue(sb, v);
    }
    sb.append(']');
    return sb.toString();
  }

  private void appendJsonValue(StringBuilder sb, Object v){
    if (v==null){ sb.append("null"); }
    else if (v instanceof Number || v instanceof Boolean){ sb.append(v.toString()); }
    else if (v instanceof Map<?,?> map){ sb.append(toJsonMap(map)); }
    else if (v instanceof List<?> list){ sb.append(toJsonList(list)); }
    else { sb.append('"').append(escape(v.toString())).append('"'); }
  }

  private String escape(String s){ return s.replace("\\","\\\\").replace("\"","\\\""); }

  private Map<String, ResourceType> fetchResourceTypeCatalog(){
    Map<String, ResourceType> map = new LinkedHashMap<>();
    try {
      List<ResourceType> types = listResourceTypes();
      for (ResourceType t : types){
        if (t==null) continue;
        String code = t.getCode();
        if (code!=null && !code.isBlank()) map.put(code, t);
      }
    } catch(Exception ignore){}
    return map;
  }

  private void mergeType(Resource resource, Map<String, ResourceType> catalog){
    if (resource==null) return;
    mergeType(resource.getType(), catalog);
  }

  private void mergeType(ResourceType type, Map<String, ResourceType> catalog){
    if (type==null || catalog==null || catalog.isEmpty()) return;
    String code = type.getCode();
    if (code==null || code.isBlank()) return;
    ResourceType ref = catalog.get(code);
    if (ref==null) return;
    if ((type.getLabel()==null || type.getLabel().isBlank()) && ref.getLabel()!=null){
      type.setLabel(ref.getLabel());
    }
    if (ref.getIcon()!=null && !ref.getIcon().isBlank()){
      type.setIcon(ref.getIcon());
    }
  }

  private void fillResource(Resource target, Map<?,?> data){
    if (target == null || data == null){
      return;
    }
    String id = SimpleJson.str(data.get("id"));
    if (id != null && !id.isBlank()){
      try {
        target.setId(UUID.fromString(id));
      } catch(IllegalArgumentException ignore){
        target.setId(null);
      }
    } else {
      target.setId(null);
    }
    target.setName(SimpleJson.str(data.get("name")));
    target.setType(parseResourceType(data.get("type")));
    target.setUnitPriceHt(parseBigDecimal(data.get("unitPriceHt")));
    target.setColor(SimpleJson.str(data.get("color")));
    target.setNotes(SimpleJson.str(data.get("notes")));
    target.setState(SimpleJson.str(data.get("state")));
    target.setEmail(SimpleJson.str(data.get("email")));
    target.setAgencyId(SimpleJson.str(data.get("agencyId")));
    // === CRM-INJECT BEGIN: resource-api-fill ===
    Object cap = data.get("capacity");
    if (cap instanceof Number n){
      target.setCapacity((int) Math.max(1, n.intValue()));
    } else {
      String sc = SimpleJson.str(cap);
      if (sc!=null && !sc.isBlank()){
        try {
          target.setCapacity(Math.max(1, (int) Double.parseDouble(sc)));
        } catch(NumberFormatException ignore){}
      }
    }
    String tags = SimpleJson.str(data.get("tags"));
    if (tags!=null){
      target.setTags(tags);
    }
    String weekly = SimpleJson.str(data.get("weeklyUnavailability"));
    if (weekly!=null){
      target.setWeeklyUnavailability(weekly);
    }
    // === CRM-INJECT END ===
    target.setUnavailabilities(parseUnavailabilityList(data.get("unavailabilities")));
  }

  private ResourceType parseResourceType(Object value){
    if (value==null) return null;
    if (value instanceof Map<?,?> map){
      String code = SimpleJson.str(map.get("code"));
      String label = SimpleJson.str(map.get("label"));
      if ((code==null || code.isBlank()) && label!=null && !label.isBlank()) code = label;
      if (code==null || code.isBlank()) return null;
      ResourceType type = new ResourceType(code, label);
      String icon = SimpleJson.str(map.get("icon"));
      if (icon!=null && !icon.isBlank()) type.setIcon(icon);
      return type;
    }
    String code = SimpleJson.str(value);
    if (code==null || code.isBlank()) return null;
    return new ResourceType(code, code);
  }

  private BigDecimal parseBigDecimal(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof BigDecimal bd){
      return bd;
    }
    if (value instanceof Number number){
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException ex){
      return null;
    }
  }

  private List<Unavailability> parseUnavailabilityList(Object value){
    List<Unavailability> list = new ArrayList<>();
    if (!(value instanceof List<?> arr)) return list;
    for (Object o : arr){
      if (o instanceof Map<?,?> mm) list.add(parseUnavailability(mm));
    }
    return list;
  }

  private Unavailability parseUnavailability(Map<?,?> mm){
    Unavailability u = new Unavailability();
    String id = SimpleJson.str(mm.get("id"));
    if (id!=null && !id.isBlank()){
      try { u.setId(UUID.fromString(id)); } catch(Exception ignore){}
    }
    String start = SimpleJson.str(mm.get("start"));
    String end = SimpleJson.str(mm.get("end"));
    try {
      if (start!=null && !start.isBlank()) u.setStart(LocalDateTime.parse(start));
    } catch(Exception ignore){}
    try {
      if (end!=null && !end.isBlank()) u.setEnd(LocalDateTime.parse(end));
    } catch(Exception ignore){}
    String reason = SimpleJson.str(mm.get("reason"));
    if (reason!=null) u.setReason(reason);
    return u;
  }

  private Map<String,Object> toMap(Unavailability u){
    Map<String,Object> m = new LinkedHashMap<>();
    if (u.getId()!=null) m.put("id", u.getId().toString());
    if (u.getStart()!=null) m.put("start", u.getStart().format(DTF));
    if (u.getEnd()!=null) m.put("end", u.getEnd().format(DTF));
    if (u.getReason()!=null) m.put("reason", u.getReason());
    return m;
  }
}

