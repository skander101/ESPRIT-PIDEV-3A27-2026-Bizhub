package com.bizhub.participation.dao;

import com.bizhub.participation.model.Participation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParticipationDAO {

    private final Connection cnx;

    public ParticipationDAO(Connection cnx) {
        this.cnx = cnx;
    }

    public void create(Participation p) throws SQLException {
        String sql = "INSERT INTO `participation` (`user_id`, `date_affectation`, `remarques`, `formation_id`, `payment_status`, `payment_provider`, `payment_ref`, `amount`, `paid_at`) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, p.getUserId());
            pst.setObject(2, p.getDateAffectation() == null ? LocalDateTime.now() : p.getDateAffectation(), Types.TIMESTAMP);
            pst.setString(3, p.getRemarques() != null ? p.getRemarques() : "");
            pst.setInt(4, p.getFormationId());
            pst.setString(5, p.getPaymentStatus() == null ? "PENDING" : p.getPaymentStatus());
            pst.setString(6, p.getPaymentProvider());
            pst.setString(7, p.getPaymentRef());
            pst.setBigDecimal(8, p.getAmount() == null ? new java.math.BigDecimal("0.00") : p.getAmount());
            pst.setObject(9, p.getPaidAt(), Types.TIMESTAMP);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                } else {
                    Optional<Participation> inserted = findByFormationAndUser(p.getFormationId(), p.getUserId());
                    inserted.ifPresent(found -> p.setId(found.getId()));
                }
            }
        }
    }

    public Optional<Participation> findById(int id) throws SQLException {
        String sql = "SELECT `id_candidature`, `formation_id`, `user_id`, `date_affectation`, `remarques`, `payment_status`, `payment_provider`, `payment_ref`, `amount`, `paid_at` FROM `participation` WHERE `id_candidature`=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public List<Participation> findAll() throws SQLException {
        String sql = "SELECT `id_candidature`, `formation_id`, `user_id`, `date_affectation`, `remarques`, `payment_status`, `payment_provider`, `payment_ref`, `amount`, `paid_at` FROM `participation` ORDER BY `date_affectation` DESC";
        List<Participation> out = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    public List<Participation> findByFormationId(int formationId) throws SQLException {
        String sql = "SELECT `id_candidature`, `formation_id`, `user_id`, `date_affectation`, `remarques`, `payment_status`, `payment_provider`, `payment_ref`, `amount`, `paid_at` FROM `participation` WHERE `formation_id`=? ORDER BY `date_affectation` DESC";
        List<Participation> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    public List<Participation> findByUserId(int userId) throws SQLException {
        String sql = "SELECT `id_candidature`, `formation_id`, `user_id`, `date_affectation`, `remarques`, `payment_status`, `payment_provider`, `payment_ref`, `amount`, `paid_at` FROM `participation` WHERE `user_id`=? ORDER BY `date_affectation` DESC";
        List<Participation> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    /** Returns the participation if this user already participates in this formation. */
    public Optional<Participation> findByFormationAndUser(int formationId, int userId) throws SQLException {
        String sql = "SELECT `id_candidature`, `formation_id`, `user_id`, `date_affectation`, `remarques`, `payment_status`, `payment_provider`, `payment_ref`, `amount`, `paid_at` FROM `participation` WHERE `formation_id`=? AND `user_id`=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            pst.setInt(2, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public void update(Participation p) throws SQLException {
        if (p.getId() > 0) {
            String sql = "UPDATE `participation` SET `formation_id`=?, `user_id`=?, `date_affectation`=?, `remarques`=?, `payment_status`=?, `payment_provider`=?, `payment_ref`=?, `amount`=?, `paid_at`=? WHERE `id_candidature`=?";
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, p.getFormationId());
                pst.setInt(2, p.getUserId());
                pst.setObject(3, p.getDateAffectation(), Types.TIMESTAMP);
                pst.setString(4, p.getRemarques() != null ? p.getRemarques() : "");
                pst.setString(5, p.getPaymentStatus() == null ? "PENDING" : p.getPaymentStatus());
                pst.setString(6, p.getPaymentProvider());
                pst.setString(7, p.getPaymentRef());
                pst.setBigDecimal(8, p.getAmount() == null ? new java.math.BigDecimal("0.00") : p.getAmount());
                pst.setObject(9, p.getPaidAt(), Types.TIMESTAMP);
                pst.setInt(10, p.getId());
                if (pst.executeUpdate() == 0) {
                    throw new SQLException("Participation introuvable (id #" + p.getId() + ").");
                }
            }
        } else {
            updateByFormationAndUser(p, p.getFormationId(), p.getUserId());
        }
    }

    /** Met à jour la participation identifiée par (formationId, userId). Utilisé quand id_candidature est NULL. */
    public void updateByFormationAndUser(Participation p, int formationIdKey, int userIdKey) throws SQLException {
        String sql = "UPDATE `participation` SET `formation_id`=?, `user_id`=?, `date_affectation`=?, `remarques`=?, `payment_status`=?, `payment_provider`=?, `payment_ref`=?, `amount`=?, `paid_at`=? WHERE `formation_id`=? AND `user_id`=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, p.getFormationId());
            pst.setInt(2, p.getUserId());
            pst.setObject(3, p.getDateAffectation(), Types.TIMESTAMP);
            pst.setString(4, p.getRemarques() != null ? p.getRemarques() : "");
            pst.setString(5, p.getPaymentStatus() == null ? "PENDING" : p.getPaymentStatus());
            pst.setString(6, p.getPaymentProvider());
            pst.setString(7, p.getPaymentRef());
            pst.setBigDecimal(8, p.getAmount() == null ? new java.math.BigDecimal("0.00") : p.getAmount());
            pst.setObject(9, p.getPaidAt(), Types.TIMESTAMP);
            pst.setInt(10, formationIdKey);
            pst.setInt(11, userIdKey);
            if (pst.executeUpdate() == 0) {
                throw new SQLException("Participation introuvable (formation #" + formationIdKey + ", user #" + userIdKey + ").");
            }
        }
    }

    public void delete(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Utilisez deleteByFormationAndUser pour une participation sans id_candidature.");
        }
        String sql = "DELETE FROM `participation` WHERE `id_candidature`=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            if (pst.executeUpdate() == 0) {
                throw new SQLException("Participation introuvable (id #" + id + ").");
            }
        }
    }

    /** Supprime la participation identifiée par (formation_id, user_id). Utilisé quand id_candidature est NULL. */
    public void deleteByFormationAndUser(int formationId, int userId) throws SQLException {
        String sql = "DELETE FROM `participation` WHERE `formation_id`=? AND `user_id`=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            pst.setInt(2, userId);
            if (pst.executeUpdate() == 0) {
                throw new SQLException("Participation introuvable (formation #" + formationId + ", user #" + userId + ").");
            }
        }
    }

    private static Participation mapRow(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        Object idVal = rs.getObject("id_candidature");
        p.setId(idVal == null ? 0 : ((Number) idVal).intValue());
        p.setFormationId(rs.getInt("formation_id"));
        p.setUserId(rs.getInt("user_id"));
        Timestamp ts = rs.getTimestamp("date_affectation");
        p.setDateAffectation(ts == null ? null : ts.toLocalDateTime());
        p.setRemarques(rs.getString("remarques"));
        p.setPaymentStatus(rs.getString("payment_status"));
        p.setPaymentProvider(rs.getString("payment_provider"));
        p.setPaymentRef(rs.getString("payment_ref"));
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        p.setAmount(amount);
        Timestamp tsPaid = rs.getTimestamp("paid_at");
        p.setPaidAt(tsPaid == null ? null : tsPaid.toLocalDateTime());
        return p;
    }
}
