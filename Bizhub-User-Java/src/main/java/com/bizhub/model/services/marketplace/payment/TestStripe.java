package com.bizhub.model.services.marketplace.payment;

import com.bizhub.model.marketplace.CommandeJoinProduit;
import java.math.BigDecimal;

/**
 * ✅ TEST STRIPE — Exécutez ce fichier directement dans IntelliJ
 * Clic droit sur le fichier → Run 'TestStripe.main()'
 *
 * Ce test vérifie :
 *   1. stripe.properties est trouvé et lu correctement
 *   2. La clé Stripe est valide
 *   3. Un lien Checkout est généré
 *   4. Le navigateur s'ouvre avec la page Stripe
 */
public class TestStripe {

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════");
        System.out.println("  TEST STRIPE — BizHub Marketplace");
        System.out.println("═══════════════════════════════════════\n");

        // ── Étape 1 : Créer une fausse commande ─────────
        System.out.println("📦 Étape 1 — Création commande test...");
        CommandeJoinProduit commande = new CommandeJoinProduit();
        commande.setIdCommande(999);
        commande.setIdClient(1);
        commande.setIdProduit(1);
        commande.setProduitNom("Produit Test BizHub");
        commande.setQuantiteCommande(2);
        commande.setPrix(new BigDecimal("19.99"));   // 2 × 19.99 = 39.98 EUR
        commande.setStatut("confirmee");
        System.out.println("   ✓ Commande créée : #999 | 2 × 19.99 EUR = 39.98 EUR\n");

        // ── Étape 2 : Appeler PaymentService ────────────
        System.out.println("💳 Étape 2 — Appel PaymentService...");
        PaymentService paymentService = new PaymentService();
        PaymentResult result = paymentService.creerEtOuvrirLienPaiement(commande);

        // ── Étape 3 : Afficher le résultat ───────────────
        System.out.println("\n═══════════════════════════════════════");
        if (result.isSuccess()) {
            System.out.println("✅ SUCCÈS — API Stripe fonctionne !");
            System.out.println("   Session ID  : " + result.getSessionId());
            System.out.println("   URL Stripe  : " + result.getPaymentUrl());
            System.out.println("\n🌐 Le navigateur devrait s'être ouvert.");
            System.out.println("   Si non, copiez l'URL ci-dessus dans votre navigateur.");
            System.out.println("\n💳 Carte de test Stripe :");
            System.out.println("   Numéro  : 4242 4242 4242 4242");
            System.out.println("   Date    : 12/26");
            System.out.println("   CVC     : 123");
            System.out.println("   Email   : test@bizhub.com");
        } else {
            System.out.println("❌ ÉCHEC — Erreur détectée :");
            System.out.println("   " + result.getErrorMessage());
            System.out.println("\n🔧 Solutions possibles :");
            System.out.println("   • stripe.properties introuvable → vérifiez src/main/resources/stripe.properties");
            System.out.println("   • Clé invalide → vérifiez stripe.secret.key dans stripe.properties");
            System.out.println("   • Dépendance manquante → Maven Reload + vérifiez pom.xml");
        }
        System.out.println("═══════════════════════════════════════");
    }
}