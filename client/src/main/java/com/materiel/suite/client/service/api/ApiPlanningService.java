package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.PlanningService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ApiPlanningService implements PlanningService {
  private final RestClient rc; private final PlanningService fb;
  public ApiPlanningService(RestClient rc, PlanningService fallback){ this.rc=rc; this.fb=fallback; }
  @Override public List<Resource> listResources(){ try { return fb.listResources(); } catch(Exception e){ return fb.listResources(); } }
  @Override public Resource saveResource(Resource r){ try { return fb.saveResource(r); } catch(Exception e){ return fb.saveResource(r); } }
  @Override public void deleteResource(UUID id){ try { rc.delete("/api/resources/"+id); } catch(Exception ignore){} fb.deleteResource(id); }
  @Override public List<Intervention> listInterventions(LocalDate from, LocalDate to){
    try {
      String body = rc.get("/api/interventions?from="+from+"&to="+to);
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Intervention> list = new ArrayList<>();
      for (Object o : arr){
        Map<String,Object> m = SimpleJson.asObj(o);
        Intervention it = new Intervention();
        it.setId(UUID.fromString(SimpleJson.str(m.get("id"))));
        it.setResourceId(UUID.fromString(SimpleJson.str(m.get("resourceId"))));
        it.setLabel(SimpleJson.str(m.get("label")));
        it.setDateHeureDebut(LocalDateTime.parse(SimpleJson.str(m.get("dateHeureDebut"))));
        it.setDateHeureFin(LocalDateTime.parse(SimpleJson.str(m.get("dateHeureFin"))));
        it.setColor(SimpleJson.str(m.get("color")));
        list.add(it);
      }
      return list;
    } catch(Exception e){ return fb.listInterventions(from,to); }
  }
  @Override public Intervention saveIntervention(Intervention i){
    try {
      String json = "{\"id\":\""+(i.getId()==null?"":i.getId())+"\",\"resourceId\":\""+i.getResourceId()+"\",\"label\":\""+i.getLabel().replace("\"","\\\"")+"\",\"dateHeureDebut\":\""+i.getDateHeureDebut()+"\",\"dateHeureFin\":\""+i.getDateHeureFin()+"\",\"color\":\""+(i.getColor()==null?"":i.getColor())+"\"}";
      if (i.getId()==null){ rc.post("/api/interventions", json); } else { rc.put("/api/interventions/"+i.getId(), json); }
      return i;
    } catch(Exception e){ return fb.saveIntervention(i); }
  }
  @Override public void deleteIntervention(UUID id){ try { rc.delete("/api/interventions/"+id);} catch(Exception ignore){} fb.deleteIntervention(id); }
  @Override public List<Conflict> listConflicts(LocalDate from, LocalDate to){
    try {
      String body = rc.get("/api/planning/conflicts?from="+from+"&to="+to);
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
}
