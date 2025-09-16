package com.materiel.suite.client.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Intervention {
  private UUID id;
  private final List<ResourceRef> resources = new ArrayList<>();
  // === CRM-INJECT BEGIN: intervention-client-id-field ===
  private UUID clientId;
  // === CRM-INJECT END ===
  private String label;
  private LocalDateTime dateHeureDebut;
  private LocalDateTime dateHeureFin;
  private String color; // hex

  // Champs enrichis pour rendu "carte"
  private String clientName;
  private String siteLabel;
  private String craneName;
  private String truckName;
  private String driverName;
  private String agency;
  private String status; // PLANNED, CONFIRMED, DONE, CANCELED...
  private boolean favorite;
  private boolean locked;
  private String quoteNumber;
  private String orderNumber;
  private String deliveryNumber;
  private String invoiceNumber;

  public Intervention(){}
  public Intervention(UUID id, UUID resourceId, String label, LocalDate start, LocalDate end, String color){
    this(id, resourceId, label, start.atStartOfDay(), end.atTime(LocalTime.of(18,0)), color);
  }
  public Intervention(UUID id, UUID resourceId, String label, LocalDateTime start, LocalDateTime end, String color){
    this.id = id;
    setResourceId(resourceId);
    this.label = label;
    this.dateHeureDebut = start;
    this.dateHeureFin = end;
    this.color = color;
  }

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id = id; }
  public UUID getResourceId(){
    ResourceRef ref = resources.isEmpty()? null : resources.get(0);
    return ref==null? null : ref.getId();
  }
  public void setResourceId(UUID resourceId){
    if (resourceId==null){
      if (!resources.isEmpty()){
        ResourceRef ref = resources.get(0);
        ref.setId(null);
        if (ref.getName()==null && ref.getIcon()==null){
          resources.remove(0);
        }
      }
      return;
    }
    ResourceRef ref;
    if (resources.isEmpty()){
      ref = new ResourceRef();
      resources.add(ref);
    } else {
      ref = resources.get(0);
      if (ref==null){
        ref = new ResourceRef();
        resources.set(0, ref);
      }
    }
    ref.setId(resourceId);
  }
  public List<ResourceRef> getResources(){ return new ArrayList<>(resources); }
  public void setResources(List<ResourceRef> refs){
    resources.clear();
    if (refs==null) return;
    for (ResourceRef ref : refs){
      if (ref==null) continue;
      resources.add(new ResourceRef(ref.getId(), ref.getName(), ref.getIcon()));
    }
  }
  public Intervention addResource(ResourceRef ref){
    if (ref!=null) resources.add(new ResourceRef(ref.getId(), ref.getName(), ref.getIcon()));
    return this;
  }
  public ResourceRef primaryResource(){ return resources.isEmpty()? null : resources.get(0); }
  // === CRM-INJECT BEGIN: intervention-client-id-accessors ===
  public UUID getClientId(){ return clientId; }
  public void setClientId(UUID clientId){ this.clientId = clientId; }
  // === CRM-INJECT END ===
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label = label; }

  // API horaire
  public LocalDateTime getDateHeureDebut(){ return dateHeureDebut; }
  public void setDateHeureDebut(LocalDateTime v){ this.dateHeureDebut = v; }
  public LocalDateTime getDateHeureFin(){ return dateHeureFin; }
  public void setDateHeureFin(LocalDateTime v){ this.dateHeureFin = v; }

  // Compat legacy (jour)
  public LocalDate getDateDebut(){ return dateHeureDebut==null? null : dateHeureDebut.toLocalDate(); }
  public void setDateDebut(LocalDate d){ if (d!=null){ this.dateHeureDebut = d.atTime(this.dateHeureDebut!=null? this.dateHeureDebut.toLocalTime() : LocalTime.of(8,0)); } }
  public LocalDate getDateFin(){ return dateHeureFin==null? null : dateHeureFin.toLocalDate(); }
  public void setDateFin(LocalDate d){ if (d!=null){ this.dateHeureFin = d.atTime(this.dateHeureFin!=null? this.dateHeureFin.toLocalTime() : LocalTime.of(17,0)); } }

  public String getColor(){ return color; }
  public void setColor(String color){ this.color = color; }

  /** Start/End en LocalDateTime, toujours non-nulls (avec fallback raisonnable). */
  public LocalDateTime getStartDateTime(){
    if (dateHeureDebut != null) return dateHeureDebut;
    LocalDate d = getDateDebut();
    if (d != null) return d.atStartOfDay();
    // Fallback minimal : maintenant
    return LocalDateTime.now();
  }

  public LocalDateTime getEndDateTime(){
    if (dateHeureFin != null) return dateHeureFin;
    LocalDate d = getDateFin();
    if (d != null){
      // Inclusif journée : fin = début de jour suivant
      return d.plusDays(1).atStartOfDay();
    }
    // Fallback : +1h après le début pour rester valide
    return getStartDateTime().plusHours(1);
  }

  public String getClientName(){ return clientName; }
  public void setClientName(String clientName){ this.clientName = clientName; }
  public String getSiteLabel(){ return siteLabel; }
  public void setSiteLabel(String siteLabel){ this.siteLabel = siteLabel; }
  public String getCraneName(){ return craneName; }
  public void setCraneName(String craneName){ this.craneName = craneName; }
  public String getTruckName(){ return truckName; }
  public void setTruckName(String truckName){ this.truckName = truckName; }
  public String getDriverName(){ return driverName; }
  public void setDriverName(String driverName){ this.driverName = driverName; }
  public String getAgency(){ return agency; }
  public void setAgency(String agency){ this.agency = agency; }
  public String getStatus(){ return status; }
  public void setStatus(String status){ this.status = status; }
  public boolean isFavorite(){ return favorite; }
  public void setFavorite(boolean favorite){ this.favorite = favorite; }
  public boolean isLocked(){ return locked; }
  public void setLocked(boolean locked){ this.locked = locked; }
  public String getQuoteNumber(){ return quoteNumber; }
  public void setQuoteNumber(String quoteNumber){ this.quoteNumber = quoteNumber; }
  public String getOrderNumber(){ return orderNumber; }
  public void setOrderNumber(String orderNumber){ this.orderNumber = orderNumber; }
  public String getDeliveryNumber(){ return deliveryNumber; }
  public void setDeliveryNumber(String deliveryNumber){ this.deliveryNumber = deliveryNumber; }
  public String getInvoiceNumber(){ return invoiceNumber; }
  public void setInvoiceNumber(String invoiceNumber){ this.invoiceNumber = invoiceNumber; }

  /** Libellé heure pour rendu. */
  public String prettyTimeRange(){
    if (dateHeureDebut!=null && dateHeureFin!=null){
      LocalTime s = dateHeureDebut.toLocalTime();
      LocalTime e = dateHeureFin.toLocalTime();
      return String.format("%02d:%02d–%02d:%02d", s.getHour(), s.getMinute(), e.getHour(), e.getMinute());
    }
    return (getDateDebut()!=null? getDateDebut().toString() : "—")+
        " → "+(getDateFin()!=null? getDateFin().toString() : "—");
  }
  public String driverInitials(){
    if (driverName==null || driverName.isBlank()) return "";
    String[] p = driverName.trim().split("\\s+");
    String a = p[0].substring(0,1).toUpperCase();
    String b = p.length>1? p[p.length-1].substring(0,1).toUpperCase() : "";
    return a+b;
  }
}
