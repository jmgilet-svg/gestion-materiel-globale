package com.materiel.suite.client.model;

import com.materiel.suite.client.util.Money;
import java.util.List;

public class DocumentTotals {
  private double totalHT;
  private double totalTVA;
  private double totalTTC;
  public double getTotalHT(){ return totalHT; }
  public double getTotalTVA(){ return totalTVA; }
  public double getTotalTTC(){ return totalTTC; }
  public void set(double ht, double tva, double ttc){
    totalHT = Money.round(ht);
    totalTVA = Money.round(tva);
    totalTTC = Money.round(ttc);
  }
  public static DocumentTotals compute(List<DocumentLine> lines){
    double ht = 0, tva = 0, ttc = 0;
    for (var l : lines) { ht += l.lineHT(); tva += l.lineTVA(); ttc += l.lineTTC(); }
    DocumentTotals t = new DocumentTotals(); t.set(ht,tva,ttc); return t;
  }
}
