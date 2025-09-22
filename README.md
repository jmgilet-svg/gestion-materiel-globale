# Gestion Matériel Globale — Suite Planning, Devis & Facturation

Application Swing/Java pour planifier les interventions (multi-ressources), gérer les clients/contacts,
générer des devis/factures, et envoyer des PDF détaillés par email — le tout **multi-agences**,
avec **modèles** (templates) HTML/PDF/Email et **icônes** en couleur pour les types de ressources.

## Sommaire
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Démarrage](#démarrage)
- [Configuration Agence](#configuration-agence)
- [Modèles (Templates) & PDF](#modèles-templates--pdf)
- [Emails (To/CC/BCC, preview, CSS agence)](#emails-toccbcc-preview-css-agence)
- [Multi-agences (X-Agency-Id)](#multi-agences-x-agency-id)
- [Sécurité & Rôles](#sécurité--rôles)
- [API v2 (endpoints)](#api-v2-endpoints)
- [Mock vs API](#mock-vs-api)
- [Raccourcis & UX](#raccourcis--ux)
- [Roadmap](#roadmap)

## Fonctionnalités
- **Planning** des interventions (drag & drop, filtres période semaine/mois, filtre **À deviser / Déjà devisé**),
  édition plein écran, multi-ressources (grues, camions…), contacts clients associés, signature PNG.
- **Workflow** : Intervention → **Devis** → **Facturation** (+ picto “dévisé”, génération de devis en masse).
- **Ressources** : CRUD, types avec **icônes SVG couleur** globalisées, indisponibilités (date début/fin).
- **Clients/Contacts** : CRUD, contact principal, recherche/tri, panel inline comme Ressources/Clients (pattern unifié).
- **Ventes** : Devis/BC/BL/Factures avec **tableaux éditables**, recherche/tri, export **CSV/Excel**,
  **génération de facture** depuis devis (sélection multiple).
- **PDF détaillé** (A4) : logo agence, adresses (client/agence), TVA, CGV, **tableau réel des lignes**.
- **Templates** (Paramètres → Templates) : CRUD **QUOTE/INVOICE/EMAIL**, éditeur **WYSIWYG** (Design↔Source),
  **plein écran**, **palette de variables** cliquables, preview PDF.
- **Emails** : envoi groupé **To/CC/BCC** avec **pièces jointes**, modèles EMAIL par agence (fusion `{{...}}`),
  **prévisualisation HTML live**, **aperçu mobile 375px**, **validation HTML basique**.
- **Paramètres** : Général (autosave, durée session), **Agence** (address/cgv/vat, email CSS, signature),
  **Templates** (voir ci-dessus).
- **Rôles** : ADMIN (tout), SALES (ventes + lecture planning), CONFIG (ressources/paramètres, lecture ventes).
- **UI** : Sidebar compacte épinglable, icônes colorées partout (menu, recherche, toasts), palette d’actions contextuelles.

## Architecture
- **Client** : Swing + modèles/Services Gateways. `ServiceLocator` oriente Mock/API.
- **Serveur** : Spring Boot. V2 ajoutée pour ne **pas casser** les endpoints V1 existants.
- **Rendu PDF** : `/api/v2/pdf/render` (OpenHTMLtoPDF). Le client envoie un HTML + images inline (`cid:logo` -> `data:`).
- **Templates** : `/api/v2/templates` (in-memory par défaut), scoping par agence via `X-Agency-Id`.
- **Config Agence** : `/api/v2/agency-config` (adresse société, TVA, CGV HTML, **emailCss**, **emailSignatureHtml**).
- **Emails** : `/api/v2/mail/send` avec **to/cc/bcc** + **attachments[]**.

## Démarrage
1. **Backend**
   - `cd server && mvn spring-boot:run`
   - Par défaut, les stores V2 sont **en mémoire** (pas de DB → parfait démo/dev).
2. **Client**
   - Lancer l’app Swing (IDE ou `mvn -pl client exec:java` si configuré).
   - Au démarrage : choisir **Mock** ou **API**, **Agence** et s’authentifier (si activé).
3. **Logo**
   - Placez votre logo dans `client/src/main/resources/branding/logo.png` pour l’inclure dans les PDF.

## Configuration Agence
- Ouvrir **Paramètres → Templates** : section **styles d’emails** (CSS) + **signature HTML** par agence.
- Ouvrir **Paramètres → Général** : autosave, durée session.
- `GET/POST /api/v2/agency-config` stocke :
  - `companyAddressHtml`, `vatRate`, `cgvHtml`
  - **`emailCss`**, **`emailSignatureHtml`**

## Modèles (Templates) & PDF
- **Types** : `QUOTE`, `INVOICE`, `EMAIL`. Clé `default` utilisée comme fallback.
- **Variables** principales disponibles :
  - `agency.*` : `name`, `addressHtml`, `vatRate`, `cgvHtml`
  - `client.*` : `name`, `addressHtml`
  - `quote.*` : `reference`, `date`, `totalHt`, `totalTtc`
  - `invoice.*` : `number`, `date`, `status`, `totalHt`, `totalTtc`
  - **Lignes** : `{{lines.rows}}` (pour PDF) et `{{lines.tableHtml}}` (pour emails)
  - **Logo** : `<img src="{{logo.cdi}}">` (remplacé par une image inline)
- **Éditeur WYSIWYG** : Design ↔ Source, **plein écran**, palette de variables.
- **Prévisualiser PDF** : bouton “Prévisualiser” génère un PDF via `/api/v2/pdf/render`.

## Emails (To/CC/BCC, preview, CSS agence)
- Fenêtre “Envoyer PDF…” :
  - Sélection d’un **modèle EMAIL** ; première ligne `Subject: …` = **sujet auto**.
  - **Preview live** HTML + **aperçu mobile (375px)**.
  - **Validation HTML** basique (balises non fermées, imbrication).
  - **CSS agence** + **signature** injectés automatiquement.
- Envoi via `/api/v2/mail/send` (simulé par log en démo).

## Multi-agences (X-Agency-Id)
- Tous les appels API passent par `ApiSupport` qui ajoute `X-Agency-Id` (et Authorization si activée).
- **Templates** et **Config agence** sont scoppés par entête.

## Sécurité & Rôles
- **Rôles** : ADMIN / SALES / CONFIG.
- Masquage fin des entrées de menu selon rôle + **lecture-seule** forcée dans les dialogues ventes si non autorisé.
- Expiration de session + bouton **Déconnexion**.
- Passage ultérieur à **JWT** possible (non requis pour démo).

## API v2 (endpoints)
- **PDF** : `POST /api/v2/pdf/render`
  - `{"html": "<html>…</html>", "inlineImages": {"logo":"<base64>"}, "baseUrl": "https://..."? }`
  - Response: `application/pdf`
- **Templates** : `GET/POST/DELETE /api/v2/templates`  
  - `TemplateV2{ id, agencyId, type: QUOTE|INVOICE|EMAIL, key, name, content }`
- **Agency config** : `GET/POST /api/v2/agency-config`  
  - `{ companyAddressHtml, vatRate, cgvHtml, emailCss, emailSignatureHtml }`
- **Clients** : `GET /api/v2/clients/{id}` (adresse HTML)
- **Mail** : `POST /api/v2/mail/send`  
  - `{ to:[], cc:[], bcc:[], subject, body, attachments:[{name,contentType,base64}] }`

## Mock vs API
- **Mock** : stores en mémoire pour Templates, Agency Config, Mail (log), PDF (optionnel dummy).
- **API** : back Spring Boot (cf. ci-dessus).
- **Données démo attendues** :
  - **Ressources ~60**, **Utilisateurs ~15**, **Clients ~20×(2–4 contacts)**,
    **Interventions ~60** sur 2 semaines (avec types/icônes).

## Raccourcis & UX
- Palette contextuelle du planning (“Générer devis pour *n* interventions…”, filtre courant, aide `?` avec raccourcis).
- Sidebar compacte épinglable, **icônes colorées** dans le menu/recherche/toasts.
- Fenêtres “édition inline” dans les panneaux (Ressources/Clients/Ventes), tri & filtres par défaut pertinents.

## Roadmap
- Génération de **BL/BC** au même niveau d’automatisation.
- **Signature sur mobile** et synchronisation temps réel des interventions.
- Éditeur de **modèles avancés** avec snippets réutilisables (en-têtes/pieds).
- Support **JWT** et rafraîchissement de token.

---

### Développement
- Java 17+, Maven.
- Client Swing (modèle MVC léger, Gateways HTTP), Serveur Spring Boot.
- **OpenHTMLtoPDF** pour le rendu PDF (pas de dépendance native).

### Contribuer
- PR bienvenues ! Merci de **ne pas casser** les endpoints V1. Toute évolution de contrat passe par **V2**.
