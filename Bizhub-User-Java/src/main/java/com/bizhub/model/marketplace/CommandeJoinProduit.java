package com.bizhub.model.marketplace;

import java.math.BigDecimal;

public class CommandeJoinProduit {

    // ── Champs commande ──────────────────────────────────
    private int    idCommande;
    private int    idClient;
    private int    idProduit;
    private int    quantiteCommande;
    private String statut;
    private java.util.Date dateCommande;

    // ── Champs produit (jointure) ────────────────────────
    private String     produitNom;
    private BigDecimal prix;          // ← mappé depuis la requête SQL JOIN

    // ── Priority Engine ──────────────────────────────────
    private int    priorityScore;
    private String priorityLabel;
    private boolean autoConfirmRecommended;
    private String  autoReason;

    // ── Paiement Stripe ──────────────────────────────────
    private String paymentStatus;
    private String paymentRef;
    private String paymentUrl;

    // =====================================================
    // GETTERS / SETTERS — Commande
    // =====================================================
    public int    getIdCommande()       { return idCommande; }
    public void   setIdCommande(int v)  { this.idCommande = v; }

    public int    getIdClient()         { return idClient; }
    public void   setIdClient(int v)    { this.idClient = v; }

    public int    getIdProduit()        { return idProduit; }
    public void   setIdProduit(int v)   { this.idProduit = v; }

    public int    getQuantiteCommande()      { return quantiteCommande; }
    public void   setQuantiteCommande(int v) { this.quantiteCommande = v; }

    public String getStatut()           { return statut; }
    public void   setStatut(String v)   { this.statut = v; }

    public java.util.Date getDateCommande()          { return dateCommande; }
    public void           setDateCommande(java.util.Date v) { this.dateCommande = v; }

    // =====================================================
    // GETTERS / SETTERS — Produit (jointure SQL)
    // =====================================================
    public String getProduitNom()          { return produitNom; }
    public void   setProduitNom(String v)  { this.produitNom = v; }

    /**
     * Prix unitaire du produit lié à la commande.
     * Mappé depuis la colonne p.prix dans la requête SQL JOIN.
     * Utilisé par PaymentService pour calculer le montant Stripe.
     */
    public BigDecimal getPrix()          { return prix; }
    public void       setPrix(BigDecimal v) { this.prix = v; }

    // =====================================================
    // GETTERS / SETTERS — Priority Engine
    // =====================================================
    public int     getPriorityScore()        { return priorityScore; }
    public void    setPriorityScore(int v)   { this.priorityScore = v; }

    public String  getPriorityLabel()        { return priorityLabel; }
    public void    setPriorityLabel(String v){ this.priorityLabel = v; }

    public boolean isAutoConfirmRecommended()       { return autoConfirmRecommended; }
    public void    setAutoConfirmRecommended(boolean v) { this.autoConfirmRecommended = v; }

    public String  getAutoReason()           { return autoReason; }
    public void    setAutoReason(String v)   { this.autoReason = v; }

    // =====================================================
    // GETTERS / SETTERS — Paiement Stripe
    // =====================================================
    public String getPaymentStatus()         { return paymentStatus; }
    public void   setPaymentStatus(String v) { this.paymentStatus = v; }

    public String getPaymentRef()            { return paymentRef; }
    public void   setPaymentRef(String v)    { this.paymentRef = v; }

    public String getPaymentUrl()            { return paymentUrl; }
    public void   setPaymentUrl(String v)    { this.paymentUrl = v; }
}