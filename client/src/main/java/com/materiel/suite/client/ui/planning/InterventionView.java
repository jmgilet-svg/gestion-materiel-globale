package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Interface minimale pour alimenter les différentes vues d'interventions. */
public interface InterventionView {
  JComponent getComponent();
  void setData(List<Intervention> list);
  void setOnOpen(Consumer<Intervention> onOpen);
  default void setOnMove(BiConsumer<Intervention, LocalDate> onMove){}
  default void setOnMoveDateTime(BiConsumer<Intervention, Date> onMoveDateTime){}
  default void setOnResizeDateTime(BiConsumer<Intervention, Date> onResizeDateTime){}
  default void setMode(String mode){}
  default List<Intervention> getSelection(){ return List.of(); }
  default void setSelectionListener(Consumer<List<Intervention>> listener){}
}
