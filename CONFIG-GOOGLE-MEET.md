# Configurer le lien Google Meet automatique

Pour que le bouton **« Créer le lien automatiquement »** fonctionne dans BizHub (Créer un meet), suivez ces étapes **dans l’ordre**, sans en sauter.

---

## 1. Créer un projet Google Cloud

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/).
2. **Sélectionner un projet** → **Nouveau projet** (ex. nom : `sellami`).
3. **Créer**.

---

## 2. Activer l’API Google Calendar

1. Menu (☰) → **APIs et services** → **Bibliothèque**.
2. Recherchez **Google Calendar API** → **Activer**.

---

## 3. Aller au bon endroit pour créer le compte de service

- Vous pouvez être dans **IAM** au début.
- **Ce n’est pas dans IAM qu’on crée le compte de service.**

Dans le **menu à gauche** :

- Cliquez sur **Comptes de service** (juste en dessous de **IAM**).

---

## 4. Créer le compte de service

1. Bouton **Créer un compte de service**.
2. Remplir :
   - **Nom du compte** : `meet-service`
   - **ID** : se génère automatiquement
3. **Créer et continuer**.
4. **Rôles** → **Continuer** (ne rien ajouter).
5. **Accès utilisateurs** → **Terminer**.

Le compte de service est créé (ex. `meet-service@sellami-fw1paf.iam.gserviceaccount.com`).

---

## 5. Générer le fichier JSON (très important)

1. Cliquez sur le compte de service **meet-service**.
2. Onglet **Clés**.
3. **Ajouter une clé**.
4. Choisir **JSON**.
5. Le fichier se télécharge (ex. `sellami-fw1paf-xxxxx.json` ou `meet-service-123456.json`).

---

## 6. Déplacer le fichier JSON sur le PC

1. Créez le dossier : **`C:\google-keys\`**
2. Déplacez le fichier téléchargé dedans, par exemple :
   - **`C:\google-keys\meet-service-123456.json`**  
   (ou le nom exact du fichier que vous avez téléchargé)

---

## 7. Configurer la variable d’environnement (Windows)

1. **Win + R** → tapez : **`sysdm.cpl`** → Entrée.
2. Onglet **Avancé**.
3. **Variables d’environnement**.
4. Dans **Variables système**, cliquez **Nouvelle**.
5. Saisir :
   - **Nom** : `GOOGLE_MEET_CREDENTIALS_PATH`
   - **Valeur** : `C:\google-keys\meet-service-123456.json`  
     (adaptez le nom du fichier si besoin)
6. **OK** partout.

**Important** : redémarrez l’IDE (Cursor / IntelliJ) ou le PC pour que la variable soit prise en compte.

---

## 8. Récupérer le GOOGLE_CALENDAR_ID

**Cas simple (recommandé)**  
Utilisez votre adresse Gmail du calendrier que vous allez partager (étape 9) :

- **`benmoussaaziz.contact@gmail.com`**

**Ou** calendrier secondaire :

1. [Google Calendar](https://calendar.google.com) → **Paramètres** (⚙️).
2. Cliquez sur le calendrier concerné.
3. Copiez **ID du calendrier** (ex. `abc123@group.calendar.google.com`).

Dans **application.properties** (ou en variable d’environnement) :

```properties
GOOGLE_CALENDAR_ID=benmoussaaziz.contact@gmail.com
```

Ou en variable d’environnement Windows (même écran que l’étape 7) :

- **Nom** : `GOOGLE_CALENDAR_ID`
- **Valeur** : `benmoussaaziz.contact@gmail.com`

---

## 9. Partager le calendrier avec le compte de service

1. Ouvrez [Google Calendar](https://calendar.google.com).
2. À gauche → votre calendrier → **Paramètres et partage**.
3. **Partager avec des personnes** → **Ajouter des personnes**.
4. Collez l’**email du compte de service** :  
   **`meet-service@sellami-fw1paf.iam.gserviceaccount.com`**
5. Permission : **Modifier les événements**.
6. **Envoyer** / Enregistrer.

---

## 10. Test rapide dans le code Java

L’application BizHub lit déjà ces variables. Pour vérifier sous Windows :

- Redémarrez l’IDE après avoir défini les variables.
- Dans BizHub : **Formations** → **Créer un meet** sur une formation → choisir date/heure → **Créer le lien automatiquement**.

Si tout est OK, le champ **Lien Google Meet** se remplit avec un lien `https://meet.google.com/...`.

**Test optionnel** (pour déboguer) : ajouter temporairement dans `GoogleMeetService` ou au démarrage de l’app :

```java
System.out.println(System.getenv("GOOGLE_MEET_CREDENTIALS_PATH"));
System.out.println(System.getenv("GOOGLE_CALENDAR_ID"));
```

- Si le chemin et l’email (ou l’ID) s’affichent → configuration OK.
- Si `null` → variable mal configurée ou PC/IDE non redémarré.

---

## 11. Résultat final

Une fois configuré, vous pouvez :

- Créer un événement Google Calendar depuis BizHub.
- Générer automatiquement un lien Google Meet.
- Envoyer ce lien par email à tous les participants de la formation.

---

## Dépannage

| Message / problème | Solution |
|--------------------|----------|
| « Configurez GOOGLE_MEET_CREDENTIALS_PATH... » | Vérifiez la variable d’environnement (étape 7), le chemin exact du JSON, et redémarrez l’IDE. |
| `System.getenv("GOOGLE_MEET_CREDENTIALS_PATH")` affiche `null` | Variable créée en « Variables système » (pas seulement « Variables utilisateur »), puis redémarrage IDE/PC. |
| « Impossible de créer le lien... » | Le calendrier doit être partagé avec `meet-service@...` (étape 9) et `GOOGLE_CALENDAR_ID` doit être l’email ou l’ID de ce calendrier. |

**Alternative** : au lieu des variables d’environnement, vous pouvez tout mettre dans **`src/main/resources/application.properties`** :

```properties
GOOGLE_MEET_CREDENTIALS_PATH=C:/google-keys/meet-service-123456.json
GOOGLE_CALENDAR_ID=benmoussaaziz.contact@gmail.com
```

(Utilisez `/` dans le chemin sous Windows.)
