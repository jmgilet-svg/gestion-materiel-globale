package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.TimelineEvent;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.TimelineService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Client REST minimaliste pour l'historique d'intervention (API v2). */
public class ApiTimelineService implements TimelineService {
  private final RestClient restClient;
  private final TimelineService fallback;

  public ApiTimelineService(RestClient restClient, TimelineService fallback){
    this.restClient = restClient;
    this.fallback = fallback;
  }

  @Override
  public List<TimelineEvent> list(String interventionId){
    if (interventionId == null || interventionId.isBlank() || restClient == null){
      return List.of();
    }
    try {
      String body = restClient.get("/api/v2/interventions/" + interventionId + "/timeline");
      List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<TimelineEvent> events = new ArrayList<>();
      for (Object item : arr){
        events.add(parseEvent(SimpleJson.asObj(item)));
      }
      return events;
    } catch (Exception ex){
      return fallback != null ? fallback.list(interventionId) : List.of();
    }
  }

  @Override
  public TimelineEvent append(String interventionId, TimelineEvent event){
    if (interventionId == null || interventionId.isBlank() || restClient == null){
      return null;
    }
    if (event == null){
      return null;
    }
    try {
      String body = restClient.post("/api/v2/interventions/" + interventionId + "/timeline", toJson(event));
      return parseEvent(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch (Exception ex){
      return fallback != null ? fallback.append(interventionId, event) : null;
    }
  }

  private TimelineEvent parseEvent(Map<String, Object> map){
    TimelineEvent event = new TimelineEvent();
    event.setId(SimpleJson.str(map.get("id")));
    event.setInterventionId(SimpleJson.str(map.get("interventionId")));
    event.setType(SimpleJson.str(map.get("type")));
    event.setMessage(SimpleJson.str(map.get("message")));
    event.setAuthor(SimpleJson.str(map.get("author")));
    String timestamp = SimpleJson.str(map.get("timestamp"));
    if (timestamp != null && !timestamp.isBlank()){
      try {
        event.setTimestamp(Instant.parse(timestamp));
      } catch (Exception ignore){
        event.setTimestamp(null);
      }
    }
    return event;
  }

  private String toJson(TimelineEvent event){
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    boolean first = true;
    first = appendStringField(sb, first, "timestamp",
        event.getTimestamp() == null ? null : event.getTimestamp().toString());
    first = appendStringField(sb, first, "type", event.getType());
    first = appendStringField(sb, first, "message", event.getMessage());
    appendStringField(sb, first, "author", event.getAuthor());
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
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < value.length(); i++){
      char c = value.charAt(i);
      if (c == '"' || c == '\\'){
        out.append('\\').append(c);
      } else if (c == '\n'){
        out.append("\\n");
      } else if (c == '\r'){
        out.append("\\r");
      } else if (c == '\t'){
        out.append("\\t");
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
