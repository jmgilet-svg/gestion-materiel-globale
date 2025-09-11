package com.materiel.suite.client.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DocumentTotals {
  private double totalHT;
  private double totalTVA;
  private double totalTTC;
  public double getTotalHT(){ return totalHT; }
  public double getTotalTVA(){ return totalTVA; }
  public double getTotalTTC(){ return totalTTC; }
  public void set(double ht, double tva, double ttc){
    totalHT = round2(ht); totalTVA = round2(tva); totalTTC = round2(ttc);
  }
  public static DocumentTotals compute(List<DocumentLine> lines){
    double ht = 0, tva = 0, ttc = 0;
    for (var l : lines) { ht += l.lineHT(); tva += l.lineTVA(); ttc += l.lineTTC(); }
    DocumentTotals t = new DocumentTotals(); t.set(ht,tva,ttc); return t;
  }
  private static double round2(double d){
    return new BigDecimal(d).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
