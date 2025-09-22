package com.materiel.suite.client.ui.common;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * Petit éditeur HTML WYSIWYG permettant de basculer entre un mode Design et une
 * vue Source avec quelques actions de formatage de base.
 */
public class HtmlEditorPanel extends JPanel {
  private final JTabbedPane tabs = new JTabbedPane();
  private final JEditorPane design = new JEditorPane();
  private final JTextArea source = new JTextArea();
  private final HTMLEditorKit kit = new HTMLEditorKit();
  private final JToolBar toolbar = new JToolBar();
  private final JButton validateBtn = new JButton("Valider HTML");
  private final JLabel validationLabel = new JLabel(" ");

  public HtmlEditorPanel(){
    super(new BorderLayout());
    design.setEditorKit(kit);
    design.setContentType("text/html; charset=UTF-8");
    design.setEditable(true);
    design.setText("<p></p>");
    design.setCaretPosition(0);

    source.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    source.setLineWrap(false);

    tabs.addTab("Design", new JScrollPane(design));
    tabs.addTab("Source", new JScrollPane(source));
    tabs.addChangeListener(e -> syncTab());

    buildToolbar();

    JButton full = new JButton("Plein écran");
    full.addActionListener(e -> openFullscreenDialog(SwingUtilities.getWindowAncestor(this)));
    toolbar.addSeparator();
    toolbar.add(Box.createHorizontalGlue());
    toolbar.add(full);

    add(toolbar, BorderLayout.NORTH);
    add(tabs, BorderLayout.CENTER);
  }

  private void buildToolbar(){
    toolbar.setFloatable(false);
    toolbar.add(action("P", e -> wrap("<p>", "</p>"), "Paragraphe"));
    toolbar.add(action("H1", e -> wrap("<h1>", "</h1>"), "Titre 1"));
    toolbar.add(action("H2", e -> wrap("<h2>", "</h2>"), "Titre 2"));
    toolbar.addSeparator();
    toolbar.add(action("B", e -> wrap("<b>", "</b>"), "Gras"));
    toolbar.add(action("I", e -> wrap("<i>", "</i>"), "Italique"));
    toolbar.add(action("U", e -> wrap("<u>", "</u>"), "Souligné"));
    toolbar.addSeparator();
    toolbar.add(action("• Liste", e -> wrap("<ul><li>", "</li></ul>"), "Liste à puces"));
    toolbar.add(action("1. Liste", e -> wrap("<ol><li>", "</li></ol>"), "Liste ordonnée"));
    toolbar.addSeparator();
    toolbar.add(action("Lien", e -> insert("<a href=\"https://\">lien</a>"), "Insérer un lien"));
    toolbar.add(action("Image", e -> insert("<img src=\"cid:logo\" style=\"max-width:200px\"/>")
        , "Insérer une image (cid)"));
    toolbar.add(Box.createHorizontalStrut(16));
    toolbar.add(validateBtn);
    toolbar.add(Box.createHorizontalStrut(8));
    validationLabel.setForeground(new java.awt.Color(0x33, 0x66, 0x33));
    toolbar.add(validationLabel);
    validateBtn.addActionListener(e -> runValidation());
  }

  private AbstractAction action(String label, Consumer<ActionEvent> fn, String tip){
    return new AbstractAction(label){
      {
        putValue(SHORT_DESCRIPTION, tip);
      }

      @Override
      public void actionPerformed(ActionEvent e){
        if (!design.isEditable()){
          return;
        }
        ensureDesign();
        fn.accept(e);
      }
    };
  }

  private void ensureDesign(){
    if (tabs.getSelectedIndex() != 0){
      tabs.setSelectedIndex(0);
    }
  }

  private void wrap(String open, String close){
    Document doc = design.getDocument();
    int start = design.getSelectionStart();
    int end = design.getSelectionEnd();
    try {
      if (start == end){
        doc.insertString(start, open + close, null);
        design.setCaretPosition(start + open.length());
      } else {
        String selected = design.getSelectedText();
        doc.remove(start, end - start);
        doc.insertString(start, open + (selected == null ? "" : selected) + close, null);
        design.setCaretPosition(start + open.length());
      }
    } catch (BadLocationException ignore){
    }
  }

  private void insert(String html){
    Document doc = design.getDocument();
    int pos = design.getCaretPosition();
    try {
      doc.insertString(pos, html, null);
      design.setCaretPosition(pos + html.length());
    } catch (BadLocationException ignore){
    }
  }

  private void syncTab(){
    if (tabs.getSelectedIndex() == 1){
      source.setText(readDesignHtml());
      source.setCaretPosition(0);
    } else {
      setDesignHtml(source.getText());
    }
  }

  public void setHtml(String html){
    String normalized = html == null || html.isBlank() ? "<p></p>" : html;
    setDesignHtml(normalized);
    source.setText(normalized);
    if (tabs.getSelectedIndex() == 1){
      source.setCaretPosition(0);
    } else {
      design.setCaretPosition(Math.min(1, design.getDocument().getLength()));
    }
  }

  public String getHtml(){
    if (tabs.getSelectedIndex() == 1){
      return source.getText();
    }
    return readDesignHtml();
  }

  private void runValidation(){
    String html = getHtml();
    HtmlValidation.Result r = HtmlValidation.validate(html);
    if (r.ok){
      validationLabel.setForeground(new java.awt.Color(0x2e, 0x7d, 0x32));
      validationLabel.setText("HTML valide");
      validationLabel.setToolTipText(null);
    } else {
      validationLabel.setForeground(new java.awt.Color(0xc6, 0x28, 0x28));
      validationLabel.setText("⚠ " + r.message);
      validationLabel.setToolTipText(r.details);
    }
  }

  /** Ouvre un éditeur HTML modal en mode plein écran. */
  public void openFullscreenDialog(java.awt.Window owner){
    JDialog dialog = new JDialog(owner, "Édition (plein écran)", Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.setLayout(new BorderLayout());
    HtmlEditorPanel editor = new HtmlEditorPanel();
    editor.setHtml(getHtml());
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton ok = new JButton("Valider");
    JButton cancel = new JButton("Annuler");
    actions.add(ok);
    actions.add(cancel);
    dialog.add(editor, BorderLayout.CENTER);
    dialog.add(actions, BorderLayout.SOUTH);
    dialog.setResizable(true);
    dialog.setSize(1200, 800);
    dialog.setLocationRelativeTo(owner);
    ok.addActionListener(ev -> {
      setHtml(editor.getHtml());
      dialog.dispose();
    });
    cancel.addActionListener(ev -> dialog.dispose());
    dialog.setVisible(true);
  }

  public void insertText(String html){
    if (html == null || html.isEmpty()){
      return;
    }
    if (tabs.getSelectedIndex() == 1){
      int pos = source.getCaretPosition();
      source.insert(html, pos);
      source.setCaretPosition(pos + html.length());
      source.requestFocusInWindow();
    } else {
      Document doc = design.getDocument();
      int pos = design.getCaretPosition();
      try {
        doc.insertString(pos, html, null);
        design.setCaretPosition(pos + html.length());
      } catch (BadLocationException ignore){
      }
      design.requestFocusInWindow();
    }
  }

  public void setEditable(boolean editable){
    design.setEditable(editable);
    source.setEditable(editable);
    for (Component component : toolbar.getComponents()){
      component.setEnabled(editable);
    }
  }

  private void setDesignHtml(String html){
    String normalized = html == null || html.isBlank() ? "<p></p>" : html;
    try {
      HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
      kit.read(new StringReader(normalized), document, 0);
      design.setDocument(document);
    } catch (Exception ex){
      design.setText(normalized);
    }
  }

  private String readDesignHtml(){
    try {
      StringWriter writer = new StringWriter();
      kit.write(writer, design.getDocument(), 0, design.getDocument().getLength());
      return writer.toString();
    } catch (Exception ex){
      return design.getText();
    }
  }
}
