package com.materiel.suite.client.ui.common;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.text.StyledEditorKit;

/**
 * Small toolbar with basic rich-text helpers for {@link JEditorPane} HTML editors.
 */
public class RichHtmlToolbar extends JToolBar {
  private final JEditorPane editor;

  public RichHtmlToolbar(JEditorPane editor) {
    super();
    this.editor = editor;
    setFloatable(false);
    if (editor != null && (editor.getContentType() == null || !editor.getContentType().equalsIgnoreCase("text/html"))) {
      editor.setContentType("text/html");
    }
    add(actionButton(new StyledEditorKit.BoldAction(), "B"));
    add(actionButton(new StyledEditorKit.ItalicAction(), "I"));
    add(actionButton(new StyledEditorKit.UnderlineAction(), "U"));
    addSeparator();
    add(wrapButton("H1", "<h1>", "</h1>", "Titre"));
    add(wrapButton("H2", "<h2>", "</h2>", "Sous-titre"));
    add(insertButton("UL", "<ul><li>Élément</li></ul>"));
    add(insertButton("OL", "<ol><li>Élément</li></ol>"));
    addSeparator();
    add(linkButton());
  }

  private JButton actionButton(Action action, String label) {
    JButton button = new JButton(action);
    button.setText(label);
    button.setFocusable(false);
    return button;
  }

  private JButton wrapButton(String label, String openTag, String closeTag, String fallbackText) {
    JButton button = new JButton(label);
    button.setFocusable(false);
    button.addActionListener(e -> {
      if (editor == null) {
        return;
      }
      String selected = editor.getSelectedText();
      if (selected == null || selected.isBlank()) {
        selected = fallbackText == null ? "" : fallbackText;
      }
      editor.replaceSelection(openTag + selected + closeTag);
      focusEditor();
    });
    return button;
  }

  private JButton insertButton(String label, String markup) {
    JButton button = new JButton(label);
    button.setFocusable(false);
    button.addActionListener(e -> {
      if (editor == null) {
        return;
      }
      editor.replaceSelection(markup);
      focusEditor();
    });
    return button;
  }

  private JButton linkButton() {
    JButton button = new JButton("Lien");
    button.setFocusable(false);
    button.addActionListener(e -> {
      if (editor == null) {
        return;
      }
      String url = JOptionPane.showInputDialog(editor, "URL:", "https://");
      if (url == null || url.isBlank()) {
        return;
      }
      String selected = editor.getSelectedText();
      if (selected == null || selected.isBlank()) {
        selected = url;
      }
      editor.replaceSelection("<a href=\"" + url + "\">" + selected + "</a>");
      focusEditor();
    });
    return button;
  }

  private void focusEditor() {
    if (editor != null) {
      editor.requestFocusInWindow();
    }
  }
}
