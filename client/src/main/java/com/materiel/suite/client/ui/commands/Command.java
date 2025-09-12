package com.materiel.suite.client.ui.commands;

public interface Command {
  /** applique l'action */
  void execute() throws Exception;
  /** annule l'action */
  void undo() throws Exception;
  /** description courte pour debug/menu */
  default String label(){ return getClass().getSimpleName(); }
}
