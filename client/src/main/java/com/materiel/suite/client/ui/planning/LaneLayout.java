package com.materiel.suite.client.ui.planning;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

public final class LaneLayout {
  private LaneLayout(){}
  public static final class Lane { public final int index; public Lane(int i){ this.index=i; } }

  public static <T> Map<T,Lane> computeLanes(Collection<T> items,
                                             Function<T, LocalDate> start,
                                             Function<T, LocalDate> end){
    List<T> list = new ArrayList<>(items);
    list.sort(Comparator.comparing(start));
    Map<T,Lane> out = new HashMap<>();
    List<LocalDate> laneEnds = new ArrayList<>();
    for (T it : list){
      LocalDate s = start.apply(it);
      LocalDate e = end.apply(it);
      int laneIndex = -1;
      for(int i=0;i<laneEnds.size();i++){
        if (laneEnds.get(i).isBefore(s)){ laneIndex=i; break; }
      }
      if(laneIndex==-1){ laneIndex=laneEnds.size(); laneEnds.add(e); }
      else { laneEnds.set(laneIndex, e); }
      out.put(it, new Lane(laneIndex));
    }
    return out;
  }
}
