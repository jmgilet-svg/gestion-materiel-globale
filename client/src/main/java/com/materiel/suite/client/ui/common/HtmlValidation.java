package com.materiel.suite.client.ui.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validation HTML très simple : équilibre des balises / imbrication. */
public final class HtmlValidation {
  private HtmlValidation(){
  }

  public static class Result {
    public final boolean ok;
    public final String message;
    public final String details;

    public Result(boolean ok, String message, String details){
      this.ok = ok;
      this.message = message;
      this.details = details;
    }
  }

  private static final Set<String> VOID = Set.of(
      "br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "param",
      "source", "track", "wbr");
  private static final Pattern TAG = Pattern.compile("<\\s*(/)?\\s*([A-Za-z0-9]+)([^>]*)>");

  public static Result validate(String html){
    if (html == null || html.isBlank()){
      return new Result(true, "Vide", "Document vide");
    }
    Deque<String> stack = new ArrayDeque<>();
    StringBuilder problems = new StringBuilder();
    Matcher matcher = TAG.matcher(html);
    while (matcher.find()){
      boolean closing = matcher.group(1) != null;
      String name = matcher.group(2).toLowerCase(Locale.ROOT);
      String tail = matcher.group(3) == null ? "" : matcher.group(3);
      boolean selfClose = tail.contains("/>");
      if (VOID.contains(name) || selfClose){
        continue;
      }
      if (!closing){
        stack.push(name);
      } else if (stack.isEmpty()){
        problems.append("Balise fermante sans ouvrante: </").append(name).append(">\n");
      } else {
        String open = stack.pop();
        if (!open.equals(name)){
          problems.append("Imbrication incohérente: attendu </").append(open).append(">, trouvé </")
              .append(name).append(">\n");
        }
      }
    }
    if (!stack.isEmpty()){
      problems.append("Balises non fermées: ").append(String.join(", ", stack)).append("\n");
    }
    String issues = problems.toString();
    if (!issues.isEmpty()){
      issues = issues.stripTrailing();
    }
    boolean ok = issues.isEmpty();
    return new Result(ok, ok ? "OK" : "Erreurs HTML", issues);
  }
}
