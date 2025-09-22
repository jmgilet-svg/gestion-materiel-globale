package com.materiel.suite.client.service.api;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.MailService;

import java.util.Base64;
import java.util.List;

/** Client REST simple pour l'envoi d'email avec pièces jointes multiples. */
public class ApiMailService implements MailService {
  private final RestClient rc;
  private final MailService fallback;

  public ApiMailService(RestClient rc, MailService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public void sendWithAttachments(List<String> to,
                                  List<String> cc,
                                  List<String> bcc,
                                  String subject,
                                  String body,
                                  List<Attachment> attachments){
    try {
      String payload = toJson(to, cc, bcc, subject, body, attachments);
      rc.post("/api/v2/mail/send", payload);
    } catch (Exception ex){
      if (fallback != null){
        fallback.sendWithAttachments(to, cc, bcc, subject, body, attachments);
      }
    }
  }

  private String toJson(List<String> to,
                        List<String> cc,
                        List<String> bcc,
                        String subject,
                        String body,
                        List<Attachment> attachments){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    first = appendArray(sb, first, "to", to);
    first = appendArray(sb, first, "cc", cc);
    first = appendArray(sb, first, "bcc", bcc);
    first = appendStringField(sb, first, "subject", subject);
    first = appendStringField(sb, first, "body", body);
    appendAttachments(sb, first, attachments);
    sb.append('}');
    return sb.toString();
  }

  private boolean appendArray(StringBuilder sb, boolean first, String name, List<String> values){
    if (!first){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (values == null){
      sb.append("null");
    } else {
      sb.append('[');
      for (int i = 0; i < values.size(); i++){
        if (i > 0){
          sb.append(',');
        }
        String value = values.get(i);
        if (value == null){
          sb.append("null");
        } else {
          sb.append('"').append(escape(value)).append('"');
        }
      }
      sb.append(']');
    }
    return false;
  }

  private boolean appendStringField(StringBuilder sb, boolean first, String name, String value){
    if (!first){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append('"').append(escape(value)).append('"');
    }
    return false;
  }

  private void appendAttachments(StringBuilder sb, boolean first, List<Attachment> attachments){
    if (!first){
      sb.append(',');
    }
    sb.append("\"attachments\":");
    if (attachments == null){
      sb.append("null");
      return;
    }
    sb.append('[');
    for (int i = 0; i < attachments.size(); i++){
      if (i > 0){
        sb.append(',');
      }
      Attachment att = attachments.get(i);
      if (att == null){
        sb.append("null");
        continue;
      }
      sb.append('{');
      sb.append("\"name\":");
      sb.append(att.name() == null ? "null" : '"' + escape(att.name()) + '"');
      sb.append(',');
      String ct = att.contentType() == null || att.contentType().isBlank()
          ? "application/octet-stream" : att.contentType();
      sb.append("\"contentType\":");
      sb.append('"').append(escape(ct)).append('"');
      sb.append(',');
      sb.append("\"base64\":");
      byte[] bytes = att.bytes();
      String base64 = bytes == null || bytes.length == 0 ? "" : Base64.getEncoder().encodeToString(bytes);
      sb.append('"').append(escape(base64)).append('"');
      sb.append('}');
    }
    sb.append(']');
  }

  private String escape(String value){
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < value.length(); i++){
      char c = value.charAt(i);
      if (c == '\\' || c == '"'){
        sb.append('\\').append(c);
      } else if (c < 0x20){
        sb.append(String.format("\\u%04x", (int) c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
