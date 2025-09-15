package com.materiel.suite.backend.v1.service;

import com.materiel.suite.backend.v1.domain.DocLine;
import com.materiel.suite.backend.v1.domain.DocLineEmb;
import com.materiel.suite.backend.v1.domain.Quote;
import com.materiel.suite.backend.v1.domain.OrderEntity;
import com.materiel.suite.backend.v1.domain.DeliveryNoteEntity;
import com.materiel.suite.backend.v1.domain.InvoiceEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TotalsCalculator {

  public void recomputeTotals(Quote q){
    BigDecimal totalHt = BigDecimal.ZERO;
    BigDecimal totalVat = BigDecimal.ZERO;
    for (DocLine l : q.getLines()){
      computeLine(l);
      totalHt = totalHt.add(nz(l.getLineHt()));
      totalVat = totalVat.add(nz(l.getLineVat()));
    }
    q.setTotalHt(round2(totalHt));
    q.setTotalVat(round2(totalVat));
    q.setTotalTtc(round2(totalHt.add(totalVat)));
  }

  // Overloads for JPA entities
  public void recomputeTotals(OrderEntity o){ totalsFromEmb(o.getLines(), o::setTotalHt, o::setTotalVat, o::setTotalTtc); }
  public void recomputeTotals(DeliveryNoteEntity d){ totalsFromEmb(d.getLines(), d::setTotalHt, d::setTotalVat, d::setTotalTtc); }
  public void recomputeTotals(InvoiceEntity i){ totalsFromEmb(i.getLines(), i::setTotalHt, i::setTotalVat, i::setTotalTtc); }

  public void computeLine(DocLine l){
    BigDecimal qty = nz(l.getQty());
    BigDecimal pu = nz(l.getUnitPrice());
    BigDecimal discPct = nz(l.getDiscountPct());
    BigDecimal vatPct = nz(l.getVatPct());

    BigDecimal gross = qty.multiply(pu);
    BigDecimal discount = gross.multiply(discPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    BigDecimal net = gross.subtract(discount);
    BigDecimal vat = net.multiply(vatPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

    l.setLineHt(round2(net));
    l.setLineVat(round2(vat));
    l.setLineTtc(round2(net.add(vat)));
  }

  public void computeLine(DocLineEmb l){
    BigDecimal qty = nz(l.getQty());
    BigDecimal pu = nz(l.getUnitPrice());
    BigDecimal discPct = nz(l.getDiscountPct());
    BigDecimal vatPct = nz(l.getVatPct());

    BigDecimal gross = qty.multiply(pu);
    BigDecimal discount = gross.multiply(discPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    BigDecimal net = gross.subtract(discount);
    BigDecimal vat = net.multiply(vatPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

    l.setLineHt(round2(net));
    l.setLineVat(round2(vat));
    l.setLineTtc(round2(net.add(vat)));
  }

  private interface TriConsumer {
    void set(BigDecimal a);
  }
  private void totalsFromEmb(java.util.List<DocLineEmb> lines,
                             java.util.function.Consumer<BigDecimal> setHt,
                             java.util.function.Consumer<BigDecimal> setVat,
                             java.util.function.Consumer<BigDecimal> setTtc){
    BigDecimal totalHt = BigDecimal.ZERO;
    BigDecimal totalVat = BigDecimal.ZERO;
    for (DocLineEmb l : lines){
      computeLine(l);
      totalHt = totalHt.add(nz(l.getLineHt()));
      totalVat = totalVat.add(nz(l.getLineVat()));
    }
    setHt.accept(round2(totalHt));
    setVat.accept(round2(totalVat));
    setTtc.accept(round2(totalHt.add(totalVat)));
  }

  private static BigDecimal nz(BigDecimal b){ return b==null? BigDecimal.ZERO : b; }
  private static BigDecimal round2(BigDecimal b){ return b.setScale(2, RoundingMode.HALF_UP); }
}
