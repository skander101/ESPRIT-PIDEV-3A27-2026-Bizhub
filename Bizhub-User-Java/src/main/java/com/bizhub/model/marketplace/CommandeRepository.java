package com.bizhub.model.marketplace;

import com.bizhub.model.services.common.dao.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeRepository {

    private final Connection cnx;

    public CommandeRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // =====================================================
    // INSERT
    // =====================================================
    public void add(Commande c) throws SQLException {
        if (c == null) throw new IllegalArgumentException("Commande null");
        if (c.getIdClient() <= 0) throw new IllegalArgumentException("Id client invalide");
        if (c.getIdProduit() <= 0) throw new IllegalArgumentException("Id produit invalide");
        if (c.getQuantite() <= 0) throw new IllegalArgumentException("Quantité invalide");

        if (c.getStatut() == null || c.getStatut().isBlank()) c.setStatut("en_attente");

        String sql = "INSERT INTO commande (id_client, id_produit, quantite, statut) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getIdClient());
            ps.setInt(2, c.getIdProduit());
            ps.setInt(3, c.getQuantite());
            ps.setString(4, c.getStatut());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setIdCommande(rs.getInt(1));
            }
        }
    }

    // =====================================================
    // UPDATE STATUT (simple)
    // =====================================================
    public void updateStatut(int idCommande, String nouveauStatut) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        if (nouveauStatut == null || nouveauStatut.isBlank()) throw new IllegalArgumentException("Statut invalide");

        String sql = "UPDATE commande SET statut=? WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        }
    }

    // ✅ UPDATE “safe” (seulement si en_attente)
    public int updateStatutIfEnAttente(int idCommande, String nouveauStatut) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        if (nouveauStatut == null || nouveauStatut.isBlank()) throw new IllegalArgumentException("Statut invalide");

        String sql = "UPDATE commande SET statut=? WHERE id_commande=? AND statut='en_attente'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idCommande);
            return ps.executeUpdate(); // 1 si changé, 0 sinon
        }
    }

    // =====================================================
    // DELETE
    // =====================================================
    public void delete(int idCommande) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");

        String sql = "DELETE FROM commande WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ps.executeUpdate();
        }
    }

    // =====================================================
    // FIND ALL (Commande)
    // =====================================================
    public List<Commande> findAll() throws SQLException {
        String sql = "SELECT * FROM commande ORDER BY id_commande DESC";
        List<Commande> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCommande(rs));
        }
        return list;
    }

    // =====================================================
    // FIND ALL JOIN (CommandeJoinProduit)
    // =====================================================
    public List<CommandeJoinProduit> findAllJoinProduit() throws SQLException {
        String sql =
                "SELECT c.id_commande, c.id_client, c.id_produit, " +
                        "       c.quantite AS quantite_commande, c.statut, " +
                        "       c.payment_status, c.payment_ref, c.payment_url, " +
                        "       p.nom AS produit_nom " +
                        "FROM commande c " +
                        "JOIN produit_service p ON p.id_produit = c.id_produit " +
                        "ORDER BY c.id_commande DESC";

        List<CommandeJoinProduit> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapJoin(rs));
        }
        return list;
    }

    // =====================================================
    // FIND BY CLIENT JOIN
    // =====================================================
    public List<CommandeJoinProduit> findByClientJoinProduit(int idClient) throws SQLException {
        if (idClient <= 0) throw new IllegalArgumentException("Id client invalide");

        String sql =
                "SELECT c.id_commande, c.id_client, c.id_produit, " +
                        "       c.quantite AS quantite_commande, c.statut, " +
                        "       c.payment_status, c.payment_ref, c.payment_url, " +
                        "       p.nom AS produit_nom " +
                        "FROM commande c " +
                        "JOIN produit_service p ON p.id_produit = c.id_produit " +
                        "WHERE c.id_client = ? " +
                        "ORDER BY c.id_commande DESC";

        List<CommandeJoinProduit> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJoin(rs));
            }
        }
        return list;
    }

    // =====================================================
    // FIND BY OWNER JOIN (investisseur propriétaire des produits)
    // =====================================================
    public List<CommandeJoinProduit> findByOwnerJoinProduit(int ownerId) throws SQLException {
        if (ownerId <= 0) throw new IllegalArgumentException("OwnerId invalide");

        String sql =
                "SELECT c.id_commande, c.id_client, c.id_produit, " +
                        "       c.quantite AS quantite_commande, c.statut, " +
                        "       c.payment_status, c.payment_ref, c.payment_url, " +
                        "       p.nom AS produit_nom " +
                        "FROM commande c " +
                        "JOIN produit_service p ON p.id_produit = c.id_produit " +
                        "WHERE p.owner_user_id = ? " +
                        "ORDER BY c.id_commande DESC";

        List<CommandeJoinProduit> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJoin(rs)); // mapJoin inclut payment_*
                }
            }
        }
        return list;
    }

    // =====================================================
    // PAYMENT INIT (atomic)
    // =====================================================
    public int setPaymentInitiatedIfNull(int idCommande, String ref, String url) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        if (ref == null || ref.isBlank()) throw new IllegalArgumentException("ref invalide");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("url invalide");

        String sql =
                "UPDATE commande " +
                        "SET payment_status='en_cours', payment_ref=?, payment_url=? " +
                        "WHERE id_commande=? AND (payment_ref IS NULL OR payment_ref='')";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ref);
            ps.setString(2, url);
            ps.setInt(3, idCommande);
            return ps.executeUpdate(); // 1 si ok, 0 si déjà initié
        }
    }

    // =====================================================
    // MAPPERS
    // =====================================================
    private Commande mapCommande(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setIdCommande(rs.getInt("id_commande"));
        c.setIdClient(rs.getInt("id_client"));
        c.setIdProduit(rs.getInt("id_produit"));
        c.setQuantite(rs.getInt("quantite"));
        c.setStatut(rs.getString("statut"));
        // ⚠️ Ne pas mettre payment_* ici sauf si ta classe Commande a ces champs.
        return c;
    }

    private CommandeJoinProduit mapJoin(ResultSet rs) throws SQLException {
        CommandeJoinProduit j = new CommandeJoinProduit();
        j.setIdCommande(rs.getInt("id_commande"));
        j.setIdClient(rs.getInt("id_client"));
        j.setIdProduit(rs.getInt("id_produit"));
        j.setQuantiteCommande(rs.getInt("quantite_commande"));
        j.setStatut(rs.getString("statut"));
        j.setProduitNom(rs.getString("produit_nom"));

        // ✅ Payment fields (safe)
        setIfColumnExists(rs, "payment_status", j::setPaymentStatus);
        setIfColumnExists(rs, "payment_ref",    j::setPaymentRef);
        setIfColumnExists(rs, "payment_url",    j::setPaymentUrl);

        return j;
    }

    // =====================================================
    // UTIL: set only if column exists in resultset
    // =====================================================
    private void setIfColumnExists(ResultSet rs, String col, SqlConsumer<String> setter) throws SQLException {
        if (hasColumn(rs, col)) {
            setter.accept(rs.getString(col));
        }
    }

    private boolean hasColumn(ResultSet rs, String col) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        for (int i = 1; i <= n; i++) {
            if (col.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }

    public String getPaymentUrl(int idCommande) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");

        String sql = "SELECT payment_url FROM commande WHERE id_commande = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String url = rs.getString("payment_url");
                    return (url == null || url.isBlank()) ? null : url;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface SqlConsumer<T> {
        void accept(T t) throws SQLException;
    }

}