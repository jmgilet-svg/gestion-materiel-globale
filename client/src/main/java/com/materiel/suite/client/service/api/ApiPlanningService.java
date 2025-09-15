package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.PlanningValidation;

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
        r.setId(UUID.fromString(SimpleJson.str(m.get("id"))));
        r.setName(SimpleJson.str(m.getOrDefault("name","")));
        out.add(r);
      }
      return out;
    } catch(Exception e){ return fb.listResources(); }
  }

  @Override public Resource saveResource(Resource r){
    try {
      Map<String,Object> m = new LinkedHashMap<>();
      if (r.getId()!=null) m.put("id", r.getId().toString());
      m.put("name", r.getName());
      String json = toJson(m);
      String body = (r.getId()==null? rc.post("/api/v1/resources", json) : rc.put("/api/v1/resources/"+r.getId(), json));
      var map = SimpleJson.asObj(SimpleJson.parse(body));
      r.setId(UUID.fromString(SimpleJson.str(map.get("id"))));
      r.setName(SimpleJson.str(map.getOrDefault("name","")));
      return r;
    } catch(Exception e){ return fb.saveResource(r); }
  }

  @Override public void deleteResource(UUID id){
    try { rc.delete("/api/v1/resources/"+id); } catch(Exception ignore){}
    fb.deleteResource(id);
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
    m.put("label", it.getLabel());
    m.put("color", it.getColor());
    if (it.getDateDebut()!=null) m.put("dateDebut", it.getDateDebut().toString());
    if (it.getDateFin()!=null) m.put("dateFin", it.getDateFin().toString());
    if (it.getStartDateTime()!=null) m.put("startDateTime", it.getStartDateTime().format(DTF));
    if (it.getEndDateTime()!=null) m.put("endDateTime", it.getEndDateTime().format(DTF));
    if (it.getStatus()!=null) m.put("status", it.getStatus());
    return m;
  }

  private Intervention fromMap(Map<String,Object> m){
    Intervention it = new Intervention();
    String id = SimpleJson.str(m.get("id"));
    if (id!=null && !id.isBlank()) it.setId(UUID.fromString(id));
    String rid = SimpleJson.str(m.get("resourceId"));
    if (rid==null && m.get("resource")!=null){
      Map<String,Object> res = SimpleJson.asObj(m.get("resource"));
      rid = SimpleJson.str(res.get("id"));
    }
    if (rid!=null && !rid.isBlank()) it.setResourceId(UUID.fromString(rid));
    it.setLabel(SimpleJson.str(m.get("label")));
    it.setColor(SimpleJson.str(m.get("color")));
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

  private String toJson(Map<String,Object> m){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (var e : m.entrySet()){
      if (!first) sb.append(',');
      first = false;
      sb.append('"').append(e.getKey()).append('"').append(':');
      Object v = e.getValue();
      if (v==null){ sb.append("null"); }
      else if (v instanceof Number || v instanceof Boolean){ sb.append(v.toString()); }
      else { sb.append('"').append(escape(v.toString())).append('"'); }
    }
    sb.append('}');
    return sb.toString();
  }

  private String escape(String s){ return s.replace("\\","\\\\").replace("\"","\\\""); }
}

