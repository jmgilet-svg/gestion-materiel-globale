package com.materiel.suite.backend.web;

import com.materiel.suite.backend.model.Conflict;
import com.materiel.suite.backend.model.Intervention;
import com.materiel.suite.backend.model.Resource;
import com.materiel.suite.backend.model.ResourceRef;
import com.materiel.suite.backend.store.InMemoryStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {
  private final InMemoryStore store;
  public PlanningController(InMemoryStore s){ this.store=s; }

  @GetMapping("/conflicts")
  public List<Conflict> conflicts(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
    var list = store.interventions(from, to);
    list.sort(Comparator.comparing(Intervention::getDateHeureDebut));
    List<Conflict> out = new ArrayList<>();
    list.stream().map(Intervention::getResourceId).distinct().forEach(resId -> {
      Intervention last = null;
      for (var it : list){
        if (!it.getResourceId().equals(resId)) continue;
        if (last!=null && !it.getDateHeureDebut().isAfter(last.getDateHeureFin())){
          out.add(new Conflict(last.getId(), it.getId(), resId));
        }
        if (last==null || it.getDateHeureFin().isAfter(last.getDateHeureFin())) last = it;
      }
    });
    return out;
  }

  public static record ResolveRequest(
      String action,      // shift | reassign | split
      UUID id,
      Integer minutes,    // for shift
      UUID resourceId,    // for reassign
      LocalDateTime splitAt // for split
  ){}

  @PostMapping("/resolve")
  public Intervention resolve(@RequestBody ResolveRequest req){
    if (req==null || req.id()==null || req.action()==null) throw new IllegalArgumentException("action/id requis");
    Optional<Intervention> opt = store.findIntervention(req.id());
    if (opt.isEmpty()) throw new IllegalArgumentException("Intervention introuvable: "+req.id());
    Intervention it = opt.get();
    switch (req.action()){
      case "shift" -> {
        int m = Optional.ofNullable(req.minutes()).orElse(30);
        it.setDateHeureDebut(it.getDateHeureDebut().plusMinutes(m));
        it.setDateHeureFin(it.getDateHeureFin().plusMinutes(m));
        return store.save(it);
      }
      case "reassign" -> {
        if (req.resourceId()==null) throw new IllegalArgumentException("resourceId requis");
        var dayFrom = it.getDateHeureDebut().toLocalDate();
        var dayTo = it.getDateHeureFin().toLocalDate();
        var existing = store.interventions(dayFrom, dayTo);
        for (var other : existing){
          if (!other.getResourceId().equals(req.resourceId())) continue;
          boolean overlap = !it.getDateHeureFin().isBefore(other.getDateHeureDebut())
              && !it.getDateHeureDebut().isAfter(other.getDateHeureFin());
          if (overlap && !other.getId().equals(it.getId())){
            throw new IllegalStateException("Conflit sur la ressource cible");
          }
        }
        it.setResourceId(req.resourceId());
        Resource target = store.resources().stream()
            .filter(r -> r.getId().equals(req.resourceId()))
            .findFirst()
            .orElse(null);
        if (target!=null){
          it.setResources(List.of(new ResourceRef(target.getId(), target.getName(), target.getIcon())));
        }
        return store.save(it);
      }
      case "split" -> {
        if (req.splitAt()==null) throw new IllegalArgumentException("splitAt requis");
        LocalDateTime t = req.splitAt();
        if (!t.isAfter(it.getDateHeureDebut()) || !it.getDateHeureFin().isAfter(t)){
          throw new IllegalArgumentException("splitAt hors intervalle");
        }
        Intervention tail = new Intervention();
        tail.setId(UUID.randomUUID());
        tail.setResourceId(it.getResourceId());
        tail.setResources(it.getResources());
        tail.setLabel(it.getLabel()+" (suite)");
        tail.setColor(it.getColor());
        tail.setDateHeureDebut(t);
        tail.setDateHeureFin(it.getDateHeureFin());
        store.save(tail);
        it.setDateHeureFin(t.minusMinutes(1));
        return store.save(it);
      }
      default -> throw new IllegalArgumentException("action inconnue: "+req.action());
    }
  }
}
