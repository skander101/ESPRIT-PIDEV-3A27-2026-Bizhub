package com.bizhub.participation.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Participation {
    private int id;
    private int formationId;
    private int userId;
    private LocalDateTime dateAffectation;
    private String remarques;
    private String paymentStatus;
    private String paymentProvider;
    private String paymentRef;
    private BigDecimal amount;
    private LocalDateTime paidAt;

    public Participation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getDateAffectation() {
        return dateAffectation;
    }

    public void setDateAffectation(LocalDateTime dateAffectation) {
        this.dateAffectation = dateAffectation;
    }

    public String getRemarques() {
        return remarques;
    }

    public void setRemarques(String remarques) {
        this.remarques = remarques;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
