package com.materiel.suite.client.ui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Palette de commandes (Ctrl/Cmd-K). Filtrage simple contenant / case-insensitive. */
public class CommandPalette extends JDialog {
  public static class Command {
    public final String title;
    public final String hint;
    public final String shortcut;
    public final Runnable action;

    public Command(String title, String hint, String shortcut, Runnable action){
      this.title = title;
      this.hint = hint;
      this.shortcut = shortcut;
      this.action = action;
    }

    @Override public String toString(){
      return title;
    }
  }

  private final JTextField search = new JTextField();
  private final DefaultListModel<Command> model = new DefaultListModel<>();
  private final JList<Command> list = new JList<>(model);
  private List<Command> all = List.of();
  private Consumer<Boolean> onClose;
  private Supplier<JComponent> helpSupplier;

  public CommandPalette(Window owner){
    super(owner, "Commandes", ModalityType.MODELESS);
    setSize(560, 420);
    setLocationRelativeTo(owner);
    setResizable(false);

    JPanel root = new JPanel(new BorderLayout(6, 6));
    root.setBorder(new EmptyBorder(10, 10, 10, 10));

    JPanel top = new JPanel(new BorderLayout(6, 6));
    top.add(new JLabel("Rechercher une commande"), BorderLayout.WEST);
    top.add(search, BorderLayout.CENTER);
    root.add(top, BorderLayout.NORTH);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new Renderer());
    JScrollPane scrollPane = new JScrollPane(list);
    root.add(scrollPane, BorderLayout.CENTER);

    JPanel bottom = new JPanel(new BorderLayout());
    JButton help = new JButton("?", UIManager.getIcon("OptionPane.questionIcon"));
    help.setToolTipText("Voir les raccourcis disponibles");
    help.addActionListener(e -> openHelp());
    bottom.add(help, BorderLayout.EAST);
    root.add(bottom, BorderLayout.SOUTH);

    setContentPane(root);

    search.getDocument().addDocumentListener(new DocumentListener(){
      @Override public void insertUpdate(DocumentEvent e){ refilter(); }
      @Override public void removeUpdate(DocumentEvent e){ refilter(); }
      @Override public void changedUpdate(DocumentEvent e){ refilter(); }
    });

    list.addMouseListener(new java.awt.event.MouseAdapter(){
      @Override public void mouseClicked(java.awt.event.MouseEvent e){
        if (e.getClickCount() == 2){
          runSelected();
        }
      }
    });

    list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "run");
    list.getActionMap().put("run", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        runSelected();
      }
    });

    getRootPane().registerKeyboardAction(e -> close(false),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  public void setCommands(List<Command> commands){
    if (commands == null){
      all = List.of();
    } else {
      all = new ArrayList<>(commands);
    }
    refilter();
  }

  public void open(Supplier<List<Command>> provider,
                   Supplier<JComponent> helpSupplier,
                   Consumer<Boolean> onClose){
    this.helpSupplier = helpSupplier;
    if (provider != null){
      List<Command> commands = provider.get();
      if (commands != null){
        setCommands(commands);
      }
    }
    this.onClose = onClose;
    search.setText("");
    refilter();
    setVisible(true);
    SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
  }

  public void open(Consumer<Boolean> onClose){
    open(null, null, onClose);
  }

  private void refilter(){
    String query = search.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    model.clear();
    if (all.isEmpty()){
      return;
    }
    List<Command> source = all;
    if (!normalized.isEmpty()){
      source = new ArrayList<>();
      for (Command command : all){
        if (command == null){
          continue;
        }
        if (matches(command, normalized)){
          source.add(command);
        }
      }
    }
    for (Command command : source){
      model.addElement(command);
    }
    if (!model.isEmpty()){
      list.setSelectedIndex(0);
    }
  }

  private boolean matches(Command command, String query){
    if (command.title != null && command.title.toLowerCase(Locale.ROOT).contains(query)){
      return true;
    }
    if (command.hint != null && command.hint.toLowerCase(Locale.ROOT).contains(query)){
      return true;
    }
    return command.shortcut != null && command.shortcut.toLowerCase(Locale.ROOT).contains(query);
  }

  private void runSelected(){
    Command selected = list.getSelectedValue();
    if (selected == null || selected.action == null){
      close(false);
      return;
    }
    close(true);
    selected.action.run();
  }

  private void close(boolean executed){
    setVisible(false);
    if (onClose != null){
      onClose.accept(executed);
    }
  }

  private void openHelp(){
    JComponent content = helpSupplier != null ? helpSupplier.get() : null;
    if (content == null){
      content = defaultHelp();
    }
    if (content.getParent() != null){
      content.getParent().remove(content);
    }
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
        "Raccourcis & aide",
        ModalityType.MODELESS);
    dialog.setSize(560, 400);
    dialog.setLocationRelativeTo(this);
    dialog.setContentPane(new JScrollPane(content));
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
  }

  public static JComponent defaultHelp(){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0;
    gc.gridy = 0;

    panel.add(title("Palette & navigation"), gc);
    gc.gridy++;
    panel.add(item("Ouvrir la palette", "Ctrl/Cmd + K"), gc);

    gc.gridy++;
    panel.add(title("Planning"), gc);
    gc.gridy++;
    panel.add(item("Prévisualiser devis (sélection)", "P"), gc);
    gc.gridy++;
    panel.add(item("Générer devis (sélection)", "D"), gc);
    gc.gridy++;
    panel.add(item("Changer de filtre", "F"), gc);
    gc.gridy++;
    panel.add(item("Recharger", "R"), gc);

    gc.gridy++;
    panel.add(title("Intervention"), gc);
    gc.gridy++;
    panel.add(item("Générer le devis", "Ctrl/Cmd + G"), gc);
    gc.gridy++;
    panel.add(item("Regénérer lignes depuis ressources", "Ctrl/Cmd + R"), gc);
    gc.gridy++;
    panel.add(item("Étapes Intervention / Devis / Facturation", "Ctrl/Cmd + 1 / 2 / 3"), gc);

    return panel;
  }

  private static JLabel title(String text){
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  private static JPanel item(String description, String shortcut){
    JPanel row = new JPanel(new BorderLayout(8, 0));
    row.add(new JLabel(description), BorderLayout.CENTER);
    JLabel ks = new JLabel(shortcut);
    ks.setForeground(new Color(0x455A64));
    row.add(ks, BorderLayout.EAST);
    return row;
  }

  static class Renderer extends JPanel implements ListCellRenderer<Command> {
    private final JLabel title = new JLabel();
    private final JLabel hint = new JLabel();
    private final JLabel shortcut = new JLabel();

    Renderer(){
      super(new BorderLayout(6, 0));
      title.setFont(title.getFont().deriveFont(Font.BOLD));
      hint.setForeground(new Color(0x607D8B));
      shortcut.setForeground(new Color(0x455A64));

      JPanel left = new JPanel(new GridLayout(2, 1));
      left.setOpaque(false);
      left.add(title);
      left.add(hint);
      add(left, BorderLayout.CENTER);
      add(shortcut, BorderLayout.EAST);
      setBorder(new EmptyBorder(6, 8, 6, 8));
    }

    @Override public Component getListCellRendererComponent(JList<? extends Command> list,
                                                           Command value,
                                                           int index,
                                                           boolean isSelected,
                                                           boolean cellHasFocus){
      setOpaque(true);
      setBackground(isSelected ? new Color(0xDCEAFB) : Color.WHITE);
      title.setText(value != null ? Objects.toString(value.title, "") : "");
      hint.setText(value != null ? Objects.toString(value.hint, "") : "");
      shortcut.setText(value != null ? Objects.toString(value.shortcut, "") : "");
      return this;
    }
  }
}
