package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.PlanningService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ApiPlanningService implements PlanningService {
  private final RestClient rc; private final PlanningService fb;
  public ApiPlanningService(RestClient rc, PlanningService fb){ this.rc=rc; this.fb=fb; }
  @Override public List<Resource> listResources(){ try { return fb.listResources(); } catch(Exception e){ return fb.listResources(); } }
  @Override public Resource saveResource(Resource r){ try { return fb.saveResource(r); } catch(Exception e){ return fb.saveResource(r); } }
  @Override public void deleteResource(UUID id){ try { rc.delete("/api/resources/"+id); } catch(Exception ignore){} fb.deleteResource(id); }
  @Override public List<Intervention> listInterventions(LocalDate from, LocalDate to){ try { return fb.listInterventions(from,to); } catch(Exception e){ return fb.listInterventions(from,to); } }
  @Override public Intervention saveIntervention(Intervention i){ try { return fb.saveIntervention(i); } catch(Exception e){ return fb.saveIntervention(i); } }
  @Override public void deleteIntervention(UUID id){ try { rc.delete("/api/interventions/"+id); } catch(Exception ignore){} fb.deleteIntervention(id); }
}
