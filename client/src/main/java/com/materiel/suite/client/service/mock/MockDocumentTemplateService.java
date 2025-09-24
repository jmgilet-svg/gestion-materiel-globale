package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.service.DocumentTemplateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stockage en mémoire de templates HTML avec valeurs par défaut. */
public class MockDocumentTemplateService implements DocumentTemplateService {
  private final Map<String, Template> store = new ConcurrentHashMap<>();
  private final Map<String, Asset> assets = new ConcurrentHashMap<>();

  public MockDocumentTemplateService(){
    saveInternal(defaultTemplate("QUOTE", "default", "Modèle devis"));
    saveInternal(defaultTemplate("INVOICE", "default", "Modèle facture"));
    saveInternal(defaultTemplate("EMAIL", "default", "Modèle email"));
    Template partial = defaultTemplate("PARTIAL", "cgv", "Conditions générales");
    partial.setContent("<p>Conditions générales de vente</p>");
    saveInternal(partial);
  }

  @Override
  public List<Template> list(String type){
    List<Template> result = new ArrayList<>();
    for (Template t : store.values()){
      if (type == null || type.equalsIgnoreCase(t.getType())){
        result.add(copy(t));
      }
    }
    return result;
  }

  @Override
  public Template save(Template template){
    Template copy = template == null ? new Template() : copy(template);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    saveInternal(copy);
    return copy(copy);
  }

  @Override
  public void delete(String id){
    if (id != null){
      store.remove(id);
    }
  }

  @Override
  public List<Asset> listAssets(){
    List<Asset> result = new ArrayList<>();
    for (Asset asset : assets.values()){
      result.add(copy(asset));
    }
    return result;
  }

  @Override
  public Asset saveAsset(Asset asset){
    Asset copy = asset == null ? new Asset() : copy(asset);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    assets.put(copy.getId(), copy(copy));
    return copy(copy);
  }

  @Override
  public void deleteAsset(String id){
    if (id != null){
      assets.remove(id);
    }
  }

  private void saveInternal(Template template){
    store.put(template.getId(), copy(template));
  }

  private Template copy(Template src){
    Template t = new Template();
    t.setId(src.getId());
    t.setAgencyId(src.getAgencyId());
    t.setType(src.getType());
    t.setKey(src.getKey());
    t.setName(src.getName());
    t.setContent(src.getContent());
    return t;
  }

  private Asset copy(Asset src){
    Asset asset = new Asset();
    asset.setId(src.getId());
    asset.setAgencyId(src.getAgencyId());
    asset.setKey(src.getKey());
    asset.setName(src.getName());
    asset.setContentType(src.getContentType());
    asset.setBase64(src.getBase64());
    return asset;
  }

  private Template defaultTemplate(String type, String key, String name){
    Template t = new Template();
    t.setId(type.toLowerCase() + "-" + key);
    t.setType(type);
    t.setKey(key);
    t.setName(name);
    if ("EMAIL".equalsIgnoreCase(type)){
      t.setContent("""
<!DOCTYPE html><html><body>
<p>Bonjour {{client.name}},</p>
<p>Veuillez trouver ci-joint votre document.</p>
{{lines.tableHtml}}
<p>Cordialement,<br/>{{agency.name}}</p>
</body></html>
""");
    } else if ("INVOICE".equalsIgnoreCase(type)){
      t.setContent("""
<!DOCTYPE html><html><body>
<h1>Facture {{invoice.number}}</h1>
<p>Client : {{client.name}}</p>
<table style=\"width:100%;border-collapse:collapse\">
  <thead><tr><th>Désignation</th><th>Qté</th><th>PU HT</th><th>Total HT</th></tr></thead>
  <tbody>
    {{lines.rows}}
  </tbody>
</table>
<p>Total TTC : {{invoice.totalTtc}} €</p>
</body></html>
""");
    } else {
      t.setContent("""
<!DOCTYPE html><html><body>
<h1>Devis {{quote.reference}}</h1>
<p>Agence : {{agency.name}}</p>
<table style=\"width:100%;border-collapse:collapse\">
  <thead><tr><th>Désignation</th><th>Qté</th><th>PU HT</th><th>Total HT</th></tr></thead>
  <tbody>
    {{lines.rows}}
  </tbody>
</table>
<p>Total HT : {{quote.totalHt}} €</p>
</body></html>
""");
    }
    return t;
  }
}
