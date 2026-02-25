package com.bizhub.model.marketplace;

import java.time.LocalDateTime;

public class Commande {
    private int idCommande;
    private int idClient;
    private int idProduit;
    private int quantite;
    private LocalDateTime dateCommande;
    private String statut; // en_attente, confirmee, livree, annule

    // === Paiement (nouveau) ===
    private String paymentStatus;  // ex: non_initie, en_cours, paid, failed
    private String paymentRef;     // id stripe / ref interne
    private String paymentUrl;     // url checkout
    private boolean estPayee;      // 0/1
    private LocalDateTime paidAt;  // date paiement

    public Commande() {}

    public Commande(int idCommande, int idClient, int idProduit, int quantite,
                    LocalDateTime dateCommande, String statut,
                    String paymentStatus, String paymentRef, String paymentUrl,
                    boolean estPayee, LocalDateTime paidAt) {
        this.idCommande = idCommande;
        this.idClient = idClient;
        this.idProduit = idProduit;
        this.quantite = quantite;
        this.dateCommande = dateCommande;
        this.statut = statut;
        this.paymentStatus = paymentStatus;
        this.paymentRef = paymentRef;
        this.paymentUrl = paymentUrl;
        this.estPayee = estPayee;
        this.paidAt = paidAt;
    }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    // === getters/setters paiement ===
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public boolean isEstPayee() { return estPayee; }     // important pour TableView
    public void setEstPayee(boolean estPayee) { this.estPayee = estPayee; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}