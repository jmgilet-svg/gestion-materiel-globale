package com.materiel.suite.client.model;

import com.materiel.suite.client.util.Money;

public class DocumentLine {
  private String designation;
  private double quantite;
  private String unite;
  private double prixUnitaireHT;
  private double remisePct;
  private double tvaPct;

  public DocumentLine() {}
  public DocumentLine(String designation, double q, String u, double pu, double remise, double tva) {
    this.designation = designation; this.quantite = q; this.unite = u;
    this.prixUnitaireHT = pu; this.remisePct = remise; this.tvaPct = tva;
  }

  public String getDesignation() { return designation; }
  public void setDesignation(String v){ designation = v; }
  public double getQuantite() { return quantite; }
  public void setQuantite(double v){ quantite = v; }
  public String getUnite() { return unite; }
  public void setUnite(String v){ unite = v; }
  public double getPrixUnitaireHT() { return prixUnitaireHT; }
  public void setPrixUnitaireHT(double v){ prixUnitaireHT = v; }
  public double getRemisePct() { return remisePct; }
  public void setRemisePct(double v){ remisePct = v; }
  public double getTvaPct() { return tvaPct; }
  public void setTvaPct(double v){ tvaPct = v; }

  public double lineHT() {
    double base = quantite * prixUnitaireHT;
    double remise = base * (remisePct / 100.0);
    double ht = base - remise;
    return Money.round(ht);
  }
  public double lineTVA() {
    double v = lineHT() * (tvaPct / 100.0);
    return Money.round(v);
  }
  public double lineTTC() {
    return Money.round(lineHT() + lineTVA());
  }
}
