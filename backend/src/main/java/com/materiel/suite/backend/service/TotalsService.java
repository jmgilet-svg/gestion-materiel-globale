package com.materiel.suite.backend.service;

import com.materiel.suite.backend.model.DocumentLine;
import com.materiel.suite.backend.model.Order;
import com.materiel.suite.backend.model.Quote;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TotalsService {
    public void computeTotals(Quote quote) {
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        for (DocumentLine l : quote.getLines()) {
            totalHT = totalHT.add(l.getTotalHT());
            totalTVA = totalTVA.add(l.getTotalTVA());
        }
        quote.setTotalHT(totalHT);
        quote.setTotalTVA(totalTVA);
        quote.setTotalTTC(totalHT.add(totalTVA));
    }

	public void computeTotals(Order order) {
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        for (DocumentLine l : order.getLines()) {
            totalHT = totalHT.add(l.getTotalHT());
            totalTVA = totalTVA.add(l.getTotalTVA());
        }
        order.setTotalHT(totalHT);
        order.setTotalTVA(totalTVA);
        order.setTotalTTC(totalHT.add(totalTVA));
    }
		
}
