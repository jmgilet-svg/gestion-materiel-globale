package com.materiel.suite.backend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class DocumentLine {
    private UUID id = UUID.randomUUID();
    private String designation;
    private String unite;
    private BigDecimal quantite = BigDecimal.ZERO;
    private BigDecimal prixUnitaireHT = BigDecimal.ZERO;
    private BigDecimal remisePct = BigDecimal.ZERO;
    private BigDecimal tvaPct = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }
    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }
    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }
    public BigDecimal getRemisePct() { return remisePct; }
    public void setRemisePct(BigDecimal remisePct) { this.remisePct = remisePct; }
    public BigDecimal getTvaPct() { return tvaPct; }
    public void setTvaPct(BigDecimal tvaPct) { this.tvaPct = tvaPct; }

    public BigDecimal getTotalHT() {
        BigDecimal base = prixUnitaireHT.multiply(quantite);
        BigDecimal remise = base.multiply(remisePct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return base.subtract(remise).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalTVA() {
        return getTotalHT().multiply(tvaPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalTTC() {
        return getTotalHT().add(getTotalTVA());
    }
}
