package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Interface minimale pour alimenter les différentes vues d'interventions. */
public interface InterventionView {
  JComponent getComponent();
  void setData(List<Intervention> list);
  void setOnOpen(Consumer<Intervention> onOpen);
  default void setOnMove(BiConsumer<Intervention, LocalDate> onMove){}
}
