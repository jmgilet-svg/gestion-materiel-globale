package com.materiel.suite.client.service.api;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.MailService;

import java.util.Base64;

/** Client REST simple pour l'envoi d'email avec pièce jointe. */
public class ApiMailService implements MailService {
  private final RestClient rc;
  private final MailService fallback;

  public ApiMailService(RestClient rc, MailService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public void sendWithAttachment(String to, String subject, String body,
                                 String attachmentName, byte[] attachmentBytes, String contentType){
    try {
      String payload = toJson(to, subject, body, attachmentName, attachmentBytes, contentType);
      rc.post("/api/v2/mail/send", payload);
    } catch (Exception ex){
      if (fallback != null){
        fallback.sendWithAttachment(to, subject, body, attachmentName, attachmentBytes, contentType);
      }
    }
  }

  private String toJson(String to, String subject, String body,
                        String attachmentName, byte[] attachmentBytes, String contentType){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    first = appendStringField(sb, first, "to", to);
    first = appendStringField(sb, first, "subject", subject);
    first = appendStringField(sb, first, "body", body);
    first = appendStringField(sb, first, "attachmentName", attachmentName);
    first = appendStringField(sb, first, "contentType", contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);
    String base64 = attachmentBytes == null || attachmentBytes.length == 0
        ? ""
        : Base64.getEncoder().encodeToString(attachmentBytes);
    appendStringField(sb, first, "attachmentBase64", base64);
    sb.append('}');
    return sb.toString();
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
