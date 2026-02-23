package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.CommandeRepository;
import com.bizhub.model.services.common.ui.toastUtil;
import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.ProduitService;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.marketplace.CommandeService;
import com.bizhub.model.services.marketplace.ProduitServiceService;
import com.bizhub.model.services.marketplace.payment.PaymentResult;
import com.bizhub.model.services.marketplace.payment.PaymentService;

// ── NOUVEAUX IMPORTS pour animations + thread ────────────
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;
// ─────────────────────────────────────────────────────────

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandeController {

    private static final Logger LOGGER = Logger.getLogger(CommandeController.class.getName());

    private static final int    QTE_MAX          = 10_000;
    private static final String STATUT_ATTENTE   = "en_attente";
    private static final String STATUT_CONFIRMEE = "confirmee";
    private static final String STATUT_LIVREE    = "livree";
    private static final String STATUT_ANNULEE   = "annule";

    private static final String BORDER_OK  = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_ERR = "-fx-border-color:#EF4444;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_DEF = "";

    // ── FXML Layout ──────────────────────────────────────
    @FXML private VBox boxAdd;
    @FXML private VBox boxManage;
    @FXML private Text titleForm;

    // ── FXML Startup ─────────────────────────────────────
    @FXML private ComboBox<ProduitService> cbProduit;
    @FXML private TextField                tfQuantite;
    @FXML private Button                   btnAjouter;
    @FXML private Button                   btnSupprimer;

    @FXML private HBox  hboxValProduit;
    @FXML private Label iconValProduit;
    @FXML private Label lblValProduit;

    @FXML private HBox  hboxValQte;
    @FXML private Label iconValQte;
    @FXML private Label lblValQte;

    @FXML private HBox  hboxMsgAdd;
    @FXML private Label iconMsgAdd;
    @FXML private Label lblMsgAdd;

    @FXML private HBox  hboxMsgDel;
    @FXML private Label iconMsgDel;
    @FXML private Label lblMsgDel;

    // ── NOUVEAUX FXML : bloc paiement animé ─────────────
    /** VBox verte qui apparaît quand une commande "confirmee" est sélectionnée */
    @FXML private VBox        boxPaiement;
    /** Infos de la commande sélectionnée (nom produit, qté...) */
    @FXML private Label       lblInfoPaiement;
    /** ProgressBar indéterminée pendant l'appel Stripe */
    @FXML private ProgressBar progressPaiement;
    /** Messages résultat paiement inline */
    @FXML private HBox  hboxMsgPaiement;
    @FXML private Label iconMsgPaiement;
    @FXML private Label lblMsgPaiement;
    // ─────────────────────────────────────────────────────

    // ── FXML Investisseur ────────────────────────────────
    @FXML private Button btnConfirmer;
    @FXML private Button btnAnnuler;

    @FXML private HBox  hboxSelectedInfo;
    @FXML private Label lblSelectedInfo;

    @FXML private HBox  hboxMsgStatut;
    @FXML private Label iconMsgStatut;
    @FXML private Label lblMsgStatut;

    // ── FXML Filtres ─────────────────────────────────────
    @FXML private Label            lblFilterClient;
    @FXML private TextField        tfFilterClient;
    @FXML private ComboBox<String> cbFilterStatut;

    // ── FXML KPIs ────────────────────────────────────────
    @FXML private Text kpiTotal;
    @FXML private Text kpiAttente;
    @FXML private Text kpiConfirmee;
    @FXML private Text kpiLivree;
    @FXML private Text kpiAnnulee;

    // ── FXML Table ───────────────────────────────────────
    @FXML private TableView<CommandeJoinProduit>            tableCommandes;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colIdCommande;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colIdClient;
    @FXML private TableColumn<CommandeJoinProduit, String>  colProduitNom;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colQte;
    @FXML private TableColumn<CommandeJoinProduit, String>  colStatut;

    // ── FXML bouton payer ─────────────────────────────────
    @FXML private Button btnPayer;

    // ── Services / Data ──────────────────────────────────
    private final CommandeService       commandeService = new CommandeService();
    private final ProduitServiceService produitService  = new ProduitServiceService();
    private final PaymentService        paymentService  = new PaymentService();
    private final CommandeRepository    commandeRepo    = new CommandeRepository();

    private final ObservableList<CommandeJoinProduit> masterData   = FXCollections.observableArrayList();
    private       FilteredList<CommandeJoinProduit>   filteredData;

    // =====================================================
    // INITIALISATION
    // =====================================================
    @FXML
    public void onPayer() {
        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // 1) sécurité : startup uniquement
        String role = AppSession.getCurrentUser().getUserType().toLowerCase();
        if (!role.contains("startup")) {
            return; // ou message
        }

        // 2) seulement si confirmée
        if (!"confirmee".equalsIgnoreCase(sel.getStatut())) {
            // afficher msg inline : "commande pas confirmée"
            return;
        }

        try {
            // 3) si on a déjà une URL en DB -> ouvrir direct
            String url = commandeRepo.getPaymentUrl(sel.getIdCommande()); // à créer si tu n’as pas
            if (url != null && !url.isBlank()) {
                paymentService.openInBrowser(url);
                return;
            }

            // 4) sinon : créer Stripe + sauvegarder + ouvrir
            PaymentResult pay = paymentService.initiateStripeCheckout(sel);

            if (pay.isSuccess()) {
                commandeRepo.setPaymentInitiatedIfNull(sel.getIdCommande(), pay.getRef(), pay.getPaymentUrl());
                paymentService.openInBrowser(pay.getPaymentUrl());
            } else {
                // afficher erreur
            }

        } catch (Exception e) {
            // afficher erreur
        }
    }

    @FXML
    public void initialize() {

        setVM(tableCommandes, true);

        if (colIdCommande != null) colIdCommande.setCellValueFactory(new PropertyValueFactory<>("idCommande"));
        if (colIdClient   != null) colIdClient  .setCellValueFactory(new PropertyValueFactory<>("idClient"));
        if (colProduitNom != null) colProduitNom.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
        if (colQte        != null) colQte       .setCellValueFactory(new PropertyValueFactory<>("quantiteCommande"));
        if (colStatut     != null) colStatut    .setCellValueFactory(new PropertyValueFactory<>("statut"));

        setupStatutBadgesStartup();
        setupRowHoverGlowStartup();
        hideColumn(colIdCommande);
        hideColumn(colIdClient);

        if (cbFilterStatut != null) {
            cbFilterStatut.setItems(FXCollections.observableArrayList(
                    "Tous", STATUT_ATTENTE, STATUT_CONFIRMEE, STATUT_LIVREE, STATUT_ANNULEE));
            if (cbFilterStatut.getValue() == null) cbFilterStatut.setValue("Tous");
        }

        filteredData = new FilteredList<>(masterData, x -> true);
        SortedList<CommandeJoinProduit> sorted = new SortedList<>(filteredData);
        if (tableCommandes != null) {
            sorted.comparatorProperty().bind(tableCommandes.comparatorProperty());
            tableCommandes.setItems(sorted);
        }

        if (cbFilterStatut != null)
            cbFilterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
        if (tfFilterClient != null)
            tfFilterClient.textProperty().addListener((obs, o, n) -> applyFilters());

        if (tableCommandes != null)
            tableCommandes.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, sel) -> onSelectionChanged(sel));

        if (tfQuantite != null)
            tfQuantite.textProperty().addListener((obs, o, n) -> validerQuantiteRealTime(n));

        if (btnConfirmer != null) btnConfirmer.setDisable(true);
        if (btnAnnuler   != null) btnAnnuler.setDisable(true);

        // Bloc paiement caché au démarrage
        setVM(boxPaiement, false);
        setVM(progressPaiement, false);
        setVM(hboxMsgPaiement, false);

        loadProduits();
        applyRoleUI();
        refreshJoin();
    }

    // ── Badges + hover (inchangés) ───────────────────────
    private void setupStatutBadgesStartup() {
        if (colStatut == null) return;
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.setStyle("-fx-padding:4 10;-fx-font-weight:800;"
                    + "-fx-background-radius:999;-fx-border-radius:999;-fx-border-width:1;"
                    + "-fx-font-size:11px;"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String base = "-fx-padding:4 10;-fx-font-weight:800;"
                        + "-fx-background-radius:999;-fx-border-radius:999;-fx-border-width:1;-fx-font-size:11px;";
                switch (item.trim().toLowerCase()) {
                    case "en_attente" -> { badge.setText("⏳ En attente");
                        badge.setStyle(base + "-fx-background-color:rgba(245,158,11,0.18);"
                                + "-fx-text-fill:#FCD34D;-fx-border-color:rgba(245,158,11,0.55);"); }
                    case "confirmee"  -> { badge.setText("✅ Confirmée");
                        badge.setStyle(base + "-fx-background-color:rgba(16,185,129,0.18);"
                                + "-fx-text-fill:#6EE7B7;-fx-border-color:rgba(16,185,129,0.55);"); }
                    case "livree"     -> { badge.setText("📦 Livrée");
                        badge.setStyle(base + "-fx-background-color:rgba(59,130,246,0.18);"
                                + "-fx-text-fill:#93C5FD;-fx-border-color:rgba(59,130,246,0.55);"); }
                    case "annule"     -> { badge.setText("⛔ Annulée");
                        badge.setStyle(base + "-fx-background-color:rgba(239,68,68,0.18);"
                                + "-fx-text-fill:#FCA5A5;-fx-border-color:rgba(239,68,68,0.55);"); }
                    default -> badge.setText(item);
                }
                setGraphic(badge);
            }
        });
    }

    private void setupRowHoverGlowStartup() {
        if (tableCommandes == null) return;
        tableCommandes.setRowFactory(tv -> {
            TableRow<CommandeJoinProduit> row = new TableRow<>();
            row.hoverProperty().addListener((obs, oldV, hover) -> {
                if (row.isEmpty()) { row.setStyle(""); return; }
                row.setStyle(hover ? "-fx-background-color:rgba(251,191,36,0.07);" : "");
            });
            return row;
        });
    }

    // =====================================================
    // RÔLES
    // =====================================================
    private String getCurrentRole() {
        var u = AppSession.getCurrentUser();
        if (u == null || u.getUserType() == null) return "";
        return u.getUserType().trim().toLowerCase();
    }

    private boolean isStartup()      { return getCurrentRole().contains("startup"); }
    private boolean isInvestisseur() { return getCurrentRole().contains("investisseur"); }

    private void applyRoleUI() {
        boolean startup = isStartup();
        boolean invest  = isInvestisseur();
        LOGGER.info(">>> applyRoleUI | userType='" + getCurrentRole()
                + "' | startup=" + startup + " | invest=" + invest);
        setVM(boxAdd,          startup);
        setVM(boxManage,       invest);
        setVM(lblFilterClient, invest);
        setVM(tfFilterClient,  invest);
        setVM(tableCommandes,  true);
        if (titleForm != null)
            titleForm.setText(startup ? "Passer une commande" : "Gestion des commandes");
    }

    // =====================================================
    // SÉLECTION TABLE
    // ── NOUVEAU : affiche boxPaiement si startup + confirmée
    // =====================================================
    private void onSelectionChanged(CommandeJoinProduit sel) {
        resetMsg(hboxMsgStatut, iconMsgStatut, lblMsgStatut);

        // Reset bloc paiement à chaque changement de sélection
        setVM(boxPaiement, false);
        setVM(progressPaiement, false);
        resetMsg(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement);
        if (btnPayer != null) {
            btnPayer.setDisable(false);
            btnPayer.setText("💳  Payer cette commande");
        }

        if (btnConfirmer != null) btnConfirmer.setDisable(true);
        if (btnAnnuler   != null) btnAnnuler.setDisable(true);

        if (sel == null) { setVM(hboxSelectedInfo, false); return; }

        if (lblSelectedInfo != null)
            lblSelectedInfo.setText("Commande #" + sel.getIdCommande()
                    + "  |  " + sel.getProduitNom()
                    + "  |  Qté : " + sel.getQuantiteCommande()
                    + "  |  Statut : " + sel.getStatut());
        setVM(hboxSelectedInfo, true);

        // ── NOUVEAU : startup + confirmée → afficher bloc paiement ──
        if (isStartup() && STATUT_CONFIRMEE.equalsIgnoreCase(safe(sel.getStatut()))) {
            if (lblInfoPaiement != null)
                lblInfoPaiement.setText("Commande #" + sel.getIdCommande()
                        + " confirmée ✅ — " + sel.getProduitNom()
                        + "  (x" + sel.getQuantiteCommande() + ")");
            setVM(boxPaiement, true);
            animerApparition(boxPaiement);
            animerPulse(btnPayer);
            return;
        }

        // ── Investisseur : activer boutons si en_attente ──
        if (isInvestisseur()) {
            boolean editable = STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()));
            if (btnConfirmer != null) btnConfirmer.setDisable(!editable);
            if (btnAnnuler   != null) btnAnnuler.setDisable(!editable);
        }
    }

    // =====================================================
    // PAIEMENT — onPayerCommande() avec animation + thread
    // =====================================================
    @FXML
    public void onPayerCommande() {
        resetMsg(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement);

        CommandeJoinProduit sel = tableCommandes != null
                ? tableCommandes.getSelectionModel().getSelectedItem() : null;

        if (sel == null) { toastUtil.error("Sélectionnez une commande."); return; }
        if (!STATUT_CONFIRMEE.equalsIgnoreCase(safe(sel.getStatut()))) {
            toastUtil.warning("Paiement disponible uniquement après confirmation ✅");
            return;
        }

        // ── Animation démarrage ───────────────────────────
        if (btnPayer != null) {
            btnPayer.setDisable(true);
            btnPayer.setText("⏳  Connexion à Stripe...");
        }
        setVM(progressPaiement, true);
        if (progressPaiement != null) progressPaiement.setProgress(-1); // indéterminé = animé

        // ── Appel Stripe en arrière-plan (pas sur le thread UI) ──
        CommandeJoinProduit cmdFinal = sel;
        Task<PaymentResult> task = new Task<>() {
            @Override protected PaymentResult call() {
                return paymentService.creerEtOuvrirLienPaiement(cmdFinal);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            PaymentResult res = task.getValue();
            // Arrêter l'animation
            stopProgress();

            if (!res.isSuccess()) {
                if (btnPayer != null) {
                    btnPayer.setDisable(false);
                    btnPayer.setText("💳  Payer cette commande");
                }
                toastUtil.error("Stripe : " + res.getErrorMessage());
                showErr(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null,
                        "Erreur Stripe : " + res.getErrorMessage());
                return;
            }

            // ── Persister le lien (anti-doublon) ─────────
            try {
                int updated = commandeRepo.setPaymentInitiatedIfNull(
                        cmdFinal.getIdCommande(),
                        res.getRef(),
                        res.getPaymentUrl()
                );
                if (updated == 1) toastUtil.success("💳 Lien de paiement ouvert !");
                else               toastUtil.ai("💡 Paiement déjà initié, lien ré-ouvert.");
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "setPaymentInitiatedIfNull", ex);
                toastUtil.success("💳 Lien de paiement ouvert !");
            }

            // ── Bouton → état succès ──────────────────────
            if (btnPayer != null) {
                btnPayer.setText("✅  Paiement lancé !");
                btnPayer.setStyle(
                        "-fx-background-color:linear-gradient(to right,#059669,#047857);"
                                + "-fx-text-fill:white;-fx-font-weight:900;-fx-font-size:14;"
                                + "-fx-background-radius:14;-fx-padding:14 18;-fx-cursor:hand;"
                                + "-fx-effect:dropshadow(gaussian,rgba(5,150,105,0.45),12,0.3,0,4);");
            }

            showOk(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null,
                    "✓ Page Stripe ouverte dans votre navigateur.\n"
                            + "Carte test : 4242 4242 4242 4242  |  Date : 12/26  |  CVC : 123");

            refreshCommandes();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            stopProgress();
            if (btnPayer != null) {
                btnPayer.setDisable(false);
                btnPayer.setText("💳  Payer cette commande");
            }
            String err = task.getException() != null
                    ? task.getException().getMessage() : "Erreur inconnue";
            toastUtil.error("Erreur : " + err);
            showErr(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null, "Erreur : " + err);
        }));

        new Thread(task, "stripe-thread").start();
    }

    private void stopProgress() {
        if (progressPaiement != null) progressPaiement.setProgress(1);
        setVM(progressPaiement, false);
    }

    // =====================================================
    // ANIMATIONS
    // =====================================================

    /** Apparition fluide : opacité 0 → 1 en 400ms */
    private void animerApparition(Node node) {
        if (node == null) return;
        node.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(400),  new KeyValue(node.opacityProperty(), 1))
        ).play();
    }

    /** Pulse × 3 sur le bouton pour attirer l'attention */
    private void animerPulse(Button btn) {
        if (btn == null) return;
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(btn.scaleXProperty(), 1.0),
                        new KeyValue(btn.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(btn.scaleXProperty(), 1.06),
                        new KeyValue(btn.scaleYProperty(), 1.06)),
                new KeyFrame(Duration.millis(560),
                        new KeyValue(btn.scaleXProperty(), 1.0),
                        new KeyValue(btn.scaleYProperty(), 1.0))
        );
        pulse.setCycleCount(3);
        pulse.play();
    }

    // =====================================================
    // REFRESH
    // =====================================================
    @FXML
    public void refreshJoin() {
        try {
            if (AppSession.getCurrentUser() == null) { masterData.clear(); updateKpis(); return; }
            if (isStartup())
                masterData.setAll(commandeService.getByClientJoinProduit(
                        AppSession.getCurrentUser().getUserId()));
            else if (isInvestisseur()) {
                masterData.setAll(commandeService.getAllJoinProduit());
                LOGGER.info("Investisseur — commandes chargées : " + masterData.size());
            } else {
                masterData.clear();
                LOGGER.warning("Rôle non reconnu : '" + getCurrentRole() + "'");
            }
            applyFilters();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "refreshJoin", e);
            if (isStartup()) showErr(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, "Erreur : " + e.getMessage());
            else showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Erreur : " + e.getMessage());
        }
    }

    private void refreshCommandes() { refreshJoin(); }

    // =====================================================
    // STARTUP : AJOUTER (inchangé)
    // =====================================================
    @FXML
    private void onAjouter() {
        resetAllValidation();
        boolean valid = true;

        ProduitService produit = cbProduit != null ? cbProduit.getValue() : null;
        if (produit == null) {
            showErr(hboxValProduit, iconValProduit, lblValProduit, cbProduit, "Sélectionnez un produit.");
            valid = false;
        } else {
            showOk(hboxValProduit, iconValProduit, lblValProduit, cbProduit, "Produit sélectionné.");
        }

        int qte;
        try {
            qte = parseQuantite(tfQuantite != null ? tfQuantite.getText() : "");
            showOk(hboxValQte, iconValQte, lblValQte, tfQuantite, "Quantité valide.");
        } catch (IllegalArgumentException e) {
            showErr(hboxValQte, iconValQte, lblValQte, tfQuantite, e.getMessage());
            return;
        }

        if (!valid) return;

        try {
            if (!isStartup()) throw new IllegalStateException("Action réservée aux Startup.");
            if (AppSession.getCurrentUser() == null) throw new IllegalStateException("Session expirée.");

            Commande c = new Commande();
            c.setIdClient(AppSession.getCurrentUser().getUserId());
            c.setIdProduit(produit.getIdProduit());
            c.setQuantite(qte);
            c.setStatut(STATUT_ATTENTE);

            commandeService.ajouter(c);
            clearStartupForm();
            refreshJoin();
            showOk(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, "✓ Commande ajoutée avec succès !");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAjouter", e);
            showErr(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, e.getMessage());
        }
    }

    // =====================================================
    // STARTUP : SUPPRIMER (inchangé)
    // =====================================================
    @FXML
    private void onSupprimer() {
        resetMsg(hboxMsgDel, iconMsgDel, lblMsgDel);
        if (tableCommandes == null) return;
        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();
        if (sel == null) { showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, "Sélectionnez une commande."); return; }
        int me = AppSession.getCurrentUser().getUserId();
        if (sel.getIdClient() != me) { showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, "Vous ne pouvez supprimer que vos propres commandes."); return; }
        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null,
                    "Statut « " + sel.getStatut() + " » — suppression impossible.\nSeules les commandes en attente sont supprimables."); return;
        }
        try {
            commandeService.supprimer(sel.getIdCommande());
            refreshJoin();
            showOk(hboxMsgDel, iconMsgDel, lblMsgDel, null, "✓ Commande #" + sel.getIdCommande() + " supprimée.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onSupprimer", e);
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, e.getMessage());
        }
    }

    // =====================================================
    // INVESTISSEUR : CONFIRMER (inchangé)
    // =====================================================
    @FXML
    private void onConfirmer() {
        resetMsg(hboxMsgStatut, iconMsgStatut, lblMsgStatut);
        if (!isInvestisseur()) { showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Action réservée à l'investisseur."); return; }
        if (tableCommandes == null) return;
        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();
        if (sel == null) { showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Sélectionnez une commande."); return; }
        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "Impossible — statut actuel : « " + sel.getStatut() + " ».\nSeules les commandes en attente peuvent être confirmées."); return;
        }
        try {
            commandeService.changerStatut(sel.getIdCommande(), STATUT_CONFIRMEE);
            refreshJoin();
            showOk(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "✓ Commande #" + sel.getIdCommande() + " confirmée.\nLe startup peut maintenant la payer.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onConfirmer", e);
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, e.getMessage());
        }
    }

    // =====================================================
    // INVESTISSEUR : ANNULER (inchangé)
    // =====================================================
    @FXML
    private void onAnnuler() {
        resetMsg(hboxMsgStatut, iconMsgStatut, lblMsgStatut);
        if (!isInvestisseur()) { showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Action réservée à l'investisseur."); return; }
        if (tableCommandes == null) return;
        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();
        if (sel == null) { showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Sélectionnez une commande."); return; }
        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "Impossible — statut actuel : « " + sel.getStatut() + " ».\nSeules les commandes en attente peuvent être annulées."); return;
        }
        try {
            commandeService.changerStatut(sel.getIdCommande(), STATUT_ANNULEE);
            refreshJoin();
            showOk(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "✓ Commande #" + sel.getIdCommande() + " annulée.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAnnuler", e);
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, e.getMessage());
        }
    }

    // =====================================================
    // CHARGEMENT PRODUITS (inchangé)
    // =====================================================
    private void loadProduits() {
        try {
            if (cbProduit == null) return;
            cbProduit.setItems(FXCollections.observableArrayList(produitService.getAllDisponibles()));
            cbProduit.setConverter(new StringConverter<>() {
                @Override public String toString(ProduitService p) {
                    return p == null ? "" : p.getNom() + "  —  " + p.getPrix() + " TND";
                }
                @Override public ProduitService fromString(String s) { return null; }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "loadProduits", e);
            showErr(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, "Impossible de charger les produits : " + e.getMessage());
        }
    }

    // =====================================================
    // FILTRES + KPIs (inchangés)
    // =====================================================
    private void applyFilters() {
        if (filteredData == null) return;
        String statut    = cbFilterStatut == null ? "Tous" : safe(cbFilterStatut.getValue());
        String clientTxt = tfFilterClient == null ? "" : safe(tfFilterClient.getText()).trim();
        if (tfFilterClient != null) tfFilterClient.setStyle(BORDER_DEF);
        filteredData.setPredicate(c -> {
            boolean okStatut = "Tous".equalsIgnoreCase(statut)
                    || (c.getStatut() != null && c.getStatut().equalsIgnoreCase(statut));
            boolean okClient = true;
            if (isInvestisseur() && !clientTxt.isEmpty()) {
                if (!clientTxt.matches("\\d+")) { if (tfFilterClient != null) tfFilterClient.setStyle(BORDER_ERR); okClient = false; }
                else okClient = (c.getIdClient() == Integer.parseInt(clientTxt));
            }
            return okStatut && okClient;
        });
        updateKpis();
    }

    private void updateKpis() {
        int total = 0, att = 0, conf = 0, liv = 0, ann = 0;
        for (CommandeJoinProduit c : masterData) {
            total++;
            switch (safe(c.getStatut()).toLowerCase()) {
                case "en_attente" -> att++;
                case "confirmee"  -> conf++;
                case "livree"     -> liv++;
                case "annule"     -> ann++;
            }
        }
        setText(kpiTotal, total); setText(kpiAttente, att); setText(kpiConfirmee, conf);
        setText(kpiLivree, liv); setText(kpiAnnulee, ann);
    }

    private void validerQuantiteRealTime(String val) {
        if (val == null || val.isBlank()) {
            resetMsg(hboxValQte, iconValQte, lblValQte);
            if (tfQuantite != null) tfQuantite.setStyle(BORDER_DEF); return;
        }
        try { parseQuantite(val); showOk(hboxValQte, iconValQte, lblValQte, tfQuantite, "Quantité valide ✓"); }
        catch (IllegalArgumentException e) { showErr(hboxValQte, iconValQte, lblValQte, tfQuantite, e.getMessage()); }
    }

    // =====================================================
    // HELPERS (inchangés + nouveau pour hboxMsgPaiement)
    // =====================================================
    private void showErr(HBox hbox, Label icon, Label lbl, Control field, String msg) {
        if (field != null) field.setStyle(BORDER_ERR);
        applyStyle(icon, lbl, "✗", msg, "#EF4444");
        setVM(hbox, true);
    }

    private void showOk(HBox hbox, Label icon, Label lbl, Control field, String msg) {
        if (field != null) field.setStyle(BORDER_OK);
        applyStyle(icon, lbl, "✓", msg, "#10B981");
        setVM(hbox, true);
    }

    private void applyStyle(Label icon, Label lbl, String icone, String msg, String color) {
        String style = "-fx-text-fill:" + color + ";-fx-font-weight:700;-fx-font-size:12px;";
        if (icon != null) { icon.setText(icone); icon.setStyle(style + "-fx-font-size:13px;"); }
        if (lbl  != null) { lbl.setText(msg);    lbl.setStyle(style); }
    }

    private void resetMsg(HBox hbox, Label icon, Label lbl) {
        setVM(hbox, false);
        if (icon != null) { icon.setText(""); icon.setStyle(""); }
        if (lbl  != null) { lbl.setText("");  lbl.setStyle(""); }
    }

    private void resetAllValidation() {
        resetMsg(hboxValProduit, iconValProduit, lblValProduit);
        resetMsg(hboxValQte,     iconValQte,     lblValQte);
        resetMsg(hboxMsgAdd,     iconMsgAdd,     lblMsgAdd);
        resetMsg(hboxMsgDel,     iconMsgDel,     lblMsgDel);
        resetMsg(hboxMsgStatut,  iconMsgStatut,  lblMsgStatut);
        resetMsg(hboxMsgPaiement,iconMsgPaiement,lblMsgPaiement); // ← NOUVEAU
        if (cbProduit  != null) cbProduit.setStyle(BORDER_DEF);
        if (tfQuantite != null) tfQuantite.setStyle(BORDER_DEF);
    }

    private void clearStartupForm() {
        if (tfQuantite     != null) tfQuantite.clear();
        if (cbProduit      != null) cbProduit.getSelectionModel().clearSelection();
        if (tableCommandes != null) tableCommandes.getSelectionModel().clearSelection();
        resetAllValidation();
        setVM(boxPaiement, false); // ← NOUVEAU
    }

    private int parseQuantite(String value) {
        String v = safe(value).trim();
        if (v.isEmpty()) throw new IllegalArgumentException("La quantité est obligatoire.");
        if (!v.matches("\\d+")) throw new IllegalArgumentException("Entrez un nombre entier (ex: 3).");
        int qte = Integer.parseInt(v);
        if (qte <= 0) throw new IllegalArgumentException("La quantité doit être supérieure à 0.");
        if (qte > QTE_MAX) throw new IllegalArgumentException("Maximum : " + QTE_MAX + " unités.");
        return qte;
    }

    private void setVM(Node node, boolean v) { if (node != null) { node.setVisible(v); node.setManaged(v); } }
    private void setText(Text t, int v)       { if (t != null) t.setText(String.valueOf(v)); }
    private void hideColumn(TableColumn<?, ?> col) {
        if (col == null) return;
        col.setVisible(false); col.setMinWidth(0); col.setPrefWidth(0);
        col.setMaxWidth(0); col.setResizable(false);
    }
    private String safe(String s) { return s == null ? "" : s; }
}
