package com.materiel.suite.client.ui.planning;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Affecte un "lane" (voix) aux items pour éviter le chevauchement visuel.
 */
public final class LaneLayout {
  private LaneLayout(){}
  public static final class Lane { public final int index; public Lane(int i){ this.index=i; } }

  public static <T> Map<T, Lane> computeLanes(Collection<T> items,
                                              Function<T, LocalDateTime> start,
                                              Function<T, LocalDateTime> end) {
    List<T> list = new ArrayList<>(items);
    list.sort(Comparator.comparing(start));
    Map<T,Lane> out = new HashMap<>();
    List<LocalDateTime> laneEnds = new ArrayList<>();
    for (T it : list){
      LocalDateTime s = start.apply(it);
      LocalDateTime e = end.apply(it);
      int laneIndex = -1;
      for (int i=0;i<laneEnds.size();i++){
        if (!laneEnds.get(i).isAfter(s)){ laneIndex = i; break; }
      }
      if (laneIndex==-1){ laneIndex = laneEnds.size(); laneEnds.add(e); }
      else { laneEnds.set(laneIndex, e); }
      out.put(it, new Lane(laneIndex));
    }
    return out;
  }
}
