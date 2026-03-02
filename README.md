# BizHub – Gestion des Utilisateurs & Avis (JavaFX + JDBC)

Ce mini-projet implémente **uniquement** :
- Authentification + session
- Dashboard admin avec stats
- Gestion utilisateurs (CRUD + filtres + pagination 20/page + export CSV + toggle active)
- Gestion avis (liste avec jointure user + formation + filtre rating + export CSV + suppression admin)
- Profil utilisateur (self-service : update infos + change password)

> Les autres modules BizHub (marketplace, projets, communauté) sont ignorés.

## Prérequis
- Java 17
- Maven
- MySQL/MariaDB avec une base `BizHub`

## Base de données
1. Crée/importer le schéma BizHub :
   - Utilise ton fichier `BizHub.sql` (à la racine du projet).

2. Charger des données de test :
   - Script : `src/main/resources/sql/bizhub_users_reviews_seed.sql`

Les identifiants de test :
- **Admin**: `admin@bizhub.tn` / `admin123`
- **Formateur**: `trainer@bizhub.tn` / `admin123`
- **User**: `user1@bizhub.tn` / `admin123`
- **Inactive**: `inactive@bizhub.tn` / `admin123` (login refusé)

## Lancer l’application
Commande Maven (plugin JavaFX) :

```bash
mvn javafx:run
```

> La connexion DB utilise `src/main/java/utils/MyDatabase.java`.
> Par défaut : `jdbc:mysql://localhost:3306/BizHub`, user `root`, password vide.

## Configuration SMTP (email de validation)
Après un paiement réussi, l’application peut envoyer un email de confirmation. Configurez SMTP de l’une des façons suivantes (par priorité) :

1. **Fichier**  
   Éditez `src/main/resources/application.properties` et remplissez :
   - `SMTP_HOST` (ex. `smtp.gmail.com`, `smtp.office365.com`)
   - `SMTP_PORT` (souvent `587`)
   - `SMTP_USER` (votre adresse email)
   - `SMTP_PASS` (mot de passe ou « mot de passe d’application » Gmail/Outlook)
   - `SMTP_FROM` (optionnel, par défaut = SMTP_USER)

2. **Variables d’environnement**  
   Définissez `SMTP_HOST`, `SMTP_USER`, `SMTP_PASS`, etc. dans l’environnement avant de lancer l’app.

3. **Propriétés système**  
   `mvn javafx:run -DSMTP_HOST=smtp.gmail.com -DSMTP_USER=... -DSMTP_PASS=...`

Si SMTP n’est pas configuré, le message « Paiement enregistré. Pour recevoir l’email de validation, configurez SMTP… » s’affiche après un paiement réussi.

L’email envoyé est personnalisé (nom du participant, formation, date) et contient en **pièce jointe un PDF** (attestation de participation / fiche d’inscription) avec les informations de la participation. Pour afficher votre **logo** sur ce PDF, placez une image `logo.png` dans `src/main/resources/com/bizhub/images/` (sinon le texte « BizHub » est affiché).

### Lien Google Meet automatique
Dans « Créer un meet » (liste des formations), le bouton **Créer le lien automatiquement** crée un événement sur Google Calendar avec un lien Meet et remplit le champ. À configurer :
1. **Google Cloud** : créez un projet, activez l’API **Google Calendar**.
2. **Compte de service** : IAM & Admin → Comptes de service → Créer → Téléchargez la clé JSON.
3. **Calendrier** : partagez votre calendrier Google avec l’email du compte de service (droits « Modifier »).
4. **application.properties** (ou variables d’environnement) :
   - `GOOGLE_MEET_CREDENTIALS_PATH` = chemin vers le fichier JSON (ou `GOOGLE_APPLICATION_CREDENTIALS`).
   - `GOOGLE_CALENDAR_ID` = ID du calendrier (souvent votre adresse email).

## Architecture
- `com.bizhub.App` : point d’entrée JavaFX
- `com.bizhub.controllers` : contrôleurs FXML
- `com.bizhub.dao` : DAO JDBC
- `com.bizhub.models` : modèles
- `com.bizhub.services` : auth/session/navigation/validation/reporting
- `src/main/resources/com/bizhub/fxml` : vues FXML
- `src/main/resources/com/bizhub/css` : thème (Material-ish)

## Notes de sécurité
- Les mots de passe sont hashés avec BCrypt (jBCrypt).
- Suppression d’avis : **admin seulement**.
- Toggle active user : **admin seulement**.

## Next
- Ajout de lazy loading réel + filtres SQL côté serveur
- Notifications toast (au lieu des Alert)
- Logs d’activité admin en base

