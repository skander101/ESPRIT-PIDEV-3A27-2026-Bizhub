package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.CommandeRepository;

import java.sql.SQLException;
import java.util.List;

public class CommandeService {

    private final CommandeRepository repo = new CommandeRepository();

    // =========================
    // CRUD de base
    // =========================
    public void ajouter(Commande c) throws SQLException {
        repo.add(c);
    }

    public void changerStatut(int idCommande, String statut) throws SQLException {
        repo.updateStatut(idCommande, statut);
    }

    public int changerStatutSiEnAttente(int idCommande, String statut) throws SQLException {
        return repo.updateStatutIfEnAttente(idCommande, statut);
    }

    public void supprimer(int idCommande) throws SQLException {
        repo.delete(idCommande);
    }

    // =========================
    // LISTES (JOIN)
    // =========================
    public List<CommandeJoinProduit> getAllJoinProduit() throws SQLException {
        return repo.findAllJoinProduit();
    }

    public List<CommandeJoinProduit> getByClientJoinProduit(int idClient) throws SQLException {
        return repo.findByClientJoinProduit(idClient);
    }

    // ✅ IMPORTANT pour l’investisseur (commandes reçues sur SES produits)
    public List<CommandeJoinProduit> getByOwnerJoinProduit(int ownerId) throws SQLException {
        return repo.findByOwnerJoinProduit(ownerId);
    }

    // =========================
    // PAIEMENT
    // =========================
    public int setPaymentInitiatedIfNull(int idCommande, String ref, String url) throws SQLException {
        return repo.setPaymentInitiatedIfNull(idCommande, ref, url);
    }

    public String getPaymentUrl(int idCommande) throws SQLException {
        return repo.getPaymentUrl(idCommande);
    }

    public int markAsPaid(int idCommande, String paymentRef) throws SQLException {
        return repo.markAsPaid(idCommande, paymentRef);
    }
}