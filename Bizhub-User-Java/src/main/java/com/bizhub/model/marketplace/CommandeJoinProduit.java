package com.bizhub.model.marketplace;

import java.time.LocalDateTime;

public class CommandeJoinProduit {

    // ===== Base commande =====
    private int idCommande;
    private int idClient;
    private int idProduit;
    private int quantiteCommande;
    private String statut;
    private String produitNom;
    private LocalDateTime dateCommande;

    // ===== Paiement =====
    private String paymentStatus;  // non_initie, en_cours, paid, failed...
    private String paymentRef;
    private String paymentUrl;
    private boolean estPayee;
    private LocalDateTime paidAt;

    // ===== IA Priorité (utilisée par ProduitServiceController) =====
    private int priorityScore;                 // ex 0..500
    private String priorityLabel;              // "HAUTE", "MOYENNE", "BASSE"
    private boolean autoConfirmRecommended;    // true/false
    private String autoReason;                 // texte explicatif

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getQuantiteCommande() { return quantiteCommande; }
    public void setQuantiteCommande(int quantiteCommande) { this.quantiteCommande = quantiteCommande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    // ===== Paiement =====
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public boolean isEstPayee() { return estPayee; }
    public void setEstPayee(boolean estPayee) { this.estPayee = estPayee; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    // ===== IA Priorité =====
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public boolean isAutoConfirmRecommended() { return autoConfirmRecommended; }
    public void setAutoConfirmRecommended(boolean autoConfirmRecommended) { this.autoConfirmRecommended = autoConfirmRecommended; }

    public String getAutoReason() { return autoReason; }
    public void setAutoReason(String autoReason) { this.autoReason = autoReason; }
}