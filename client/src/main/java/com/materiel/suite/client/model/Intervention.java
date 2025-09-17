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
  private InterventionType type;
  private String address;
  private String description;
  private String internalNote;
  private String closingNote;
  private String signatureBy;
  private LocalDateTime signatureAt;
  private String signaturePngBase64;
  private LocalDateTime actualStart;
  private LocalDateTime actualEnd;
  private final List<Contact> contacts = new ArrayList<>();
  private final List<DocumentLine> quoteDraft = new ArrayList<>();

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

  public InterventionType getType(){
    return type == null ? null : copy(type);
  }

  public void setType(InterventionType type){
    this.type = type == null ? null : copy(type);
  }

  public String getAddress(){ return address; }
  public void setAddress(String address){ this.address = address; }

  public String getDescription(){ return description; }
  public void setDescription(String description){ this.description = description; }

  public String getInternalNote(){ return internalNote; }
  public void setInternalNote(String internalNote){ this.internalNote = internalNote; }

  public String getClosingNote(){ return closingNote; }
  public void setClosingNote(String closingNote){ this.closingNote = closingNote; }

  public String getSignatureBy(){ return signatureBy; }
  public void setSignatureBy(String signatureBy){ this.signatureBy = signatureBy; }

  public LocalDateTime getSignatureAt(){ return signatureAt; }
  public void setSignatureAt(LocalDateTime signatureAt){ this.signatureAt = signatureAt; }

  public String getSignaturePngBase64(){ return signaturePngBase64; }
  public void setSignaturePngBase64(String signaturePngBase64){ this.signaturePngBase64 = signaturePngBase64; }

  public LocalDateTime getActualStart(){ return actualStart; }
  public void setActualStart(LocalDateTime actualStart){ this.actualStart = actualStart; }

  public LocalDateTime getActualEnd(){ return actualEnd; }
  public void setActualEnd(LocalDateTime actualEnd){ this.actualEnd = actualEnd; }

  public List<Contact> getContacts(){
    List<Contact> list = new ArrayList<>();
    for (Contact c : contacts){
      Contact copy = copy(c);
      if (copy != null){
        list.add(copy);
      }
    }
    return list;
  }

  public void setContacts(List<Contact> list){
    contacts.clear();
    if (list == null){
      return;
    }
    for (Contact c : list){
      Contact copy = copy(c);
      if (copy != null){
        contacts.add(copy);
      }
    }
  }

  public Intervention addContact(Contact contact){
    if (contact != null){
      Contact copy = copy(contact);
      if (copy != null){
        contacts.add(copy);
      }
    }
    return this;
  }

  public List<DocumentLine> getQuoteDraft(){
    List<DocumentLine> list = new ArrayList<>();
    for (DocumentLine line : quoteDraft){
      DocumentLine copy = copy(line);
      if (copy != null){
        list.add(copy);
      }
    }
    return list;
  }

  public void setQuoteDraft(List<DocumentLine> lines){
    quoteDraft.clear();
    if (lines == null){
      return;
    }
    for (DocumentLine line : lines){
      DocumentLine copy = copy(line);
      if (copy != null){
        quoteDraft.add(copy);
      }
    }
  }

  public Intervention addQuoteLine(DocumentLine line){
    if (line != null){
      DocumentLine copy = copy(line);
      if (copy != null){
        quoteDraft.add(copy);
      }
    }
    return this;
  }

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

  private InterventionType copy(InterventionType src){
    if (src == null){
      return null;
    }
    InterventionType copy = new InterventionType();
    copy.setCode(src.getCode());
    copy.setLabel(src.getLabel());
    copy.setIconKey(src.getIconKey());
    return copy;
  }

  private Contact copy(Contact src){
    if (src == null){
      return null;
    }
    Contact copy = new Contact();
    copy.setId(src.getId());
    copy.setClientId(src.getClientId());
    copy.setFirstName(src.getFirstName());
    copy.setLastName(src.getLastName());
    copy.setEmail(src.getEmail());
    copy.setPhone(src.getPhone());
    copy.setRole(src.getRole());
    copy.setArchived(src.isArchived());
    return copy;
  }

  private DocumentLine copy(DocumentLine src){
    if (src == null){
      return null;
    }
    DocumentLine copy = new DocumentLine();
    copy.setDesignation(src.getDesignation());
    copy.setQuantite(src.getQuantite());
    copy.setUnite(src.getUnite());
    copy.setPrixUnitaireHT(src.getPrixUnitaireHT());
    copy.setRemisePct(src.getRemisePct());
    copy.setTvaPct(src.getTvaPct());
    return copy;
  }

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
