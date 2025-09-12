package com.materiel.suite.client.ui.commands;

import javax.swing.*;
import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandBus {
  private static final CommandBus INSTANCE = new CommandBus();
  public static CommandBus get(){ return INSTANCE; }

  private final Deque<Command> undoStack = new ArrayDeque<>();
  private final Deque<Command> redoStack = new ArrayDeque<>();

  private CommandBus(){}

  public void submit(Command c){
    try {
      c.execute();
      undoStack.push(c);
      redoStack.clear();
    } catch (Exception e){
      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Erreur: "+e.getMessage()));
    }
  }

  public boolean canUndo(){ return !undoStack.isEmpty(); }
  public boolean canRedo(){ return !redoStack.isEmpty(); }

  public void undo(){
    if (undoStack.isEmpty()) return;
    Command c = undoStack.pop();
    try {
      c.undo();
      redoStack.push(c);
    } catch (Exception e){
      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Erreur undo: "+e.getMessage()));
    }
  }

  public void redo(){
    if (redoStack.isEmpty()) return;
    Command c = redoStack.pop();
    try {
      c.execute();
      undoStack.push(c);
    } catch (Exception e){
      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Erreur redo: "+e.getMessage()));
    }
  }
}
