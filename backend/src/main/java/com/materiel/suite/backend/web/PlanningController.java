package com.materiel.suite.backend.web;

import com.materiel.suite.backend.model.Conflict;
import com.materiel.suite.backend.model.Intervention;
import com.materiel.suite.backend.store.InMemoryStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
}
