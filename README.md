# Gestion Matériel — Monorepo (offline-ready)

> **Note Maven (parent introuvable)**  \
> Si vous voyez :
> ```
> Could not find artifact com.materiel.suite:gestion-materiel-globale:pom:1.0.0-SNAPSHOT
> ```
> c’est que le **POM parent** n’était pas présent/installé. Solution :
> 1) Assurez-vous d’avoir ce dépôt à la **racine** avec `pom.xml` (parent).  \
> 2) Lancez la build depuis la racine :
> ```bash
> mvn -q -DskipTests install      # ou simplement: mvn -q install
> ```
> Maven résoudra alors les modules `backend` et `client` avec le parent local.

Monorepo Maven (Java 17) avec deux modules :
- **backend/** : **API Spring Boot exécutable** (Ressources, Interventions, Conflits planning, seed mémoire + CORS)
- **client/** : application Swing (Mode Mock par défaut), fenêtre de choix Mock/API, UI ERP + **Planning DnD**

### ❗️Dépendance `com.materiel:gestion-materiel:1.0.0-SNAPSHOT` introuvable
Si vous voyez :
```
Could not resolve dependencies for project com.materiel.suite:backend:jar:1.0.0-SNAPSHOT
dependency: com.materiel:gestion-materiel:jar:1.0.0-SNAPSHOT (compile)
```
Cela signifie que le module **backend** déclare une dépendance vers **lui‑même** ou un ancien artefact externe.  
**Correctif appliqué** : suppression de cette dépendance et renommage de l’artefact backend en `gestion-materiel` pour s’aligner sur les usages historiques.

Rebuild propre :
```bash
mvn -q -DskipTests clean install
```

> Si vous gardez un backend séparé ailleurs, installez‑le d’abord : `cd ../gestion-materiel && mvn -q install`,
> ou référencez‑le comme **module** du parent au lieu d’une dépendance.


## 🚀 Sprint 1 — Backend/Frontend
**Objectif** : rendre le planning exploitable en mode **API** avec détection de conflits côté serveur et **panneau Conflits** côté client.

### Backend (nouveau)
- App Spring Boot (`backend/`) avec endpoints :
  - `GET/POST/PUT/DELETE /api/resources`
  - `GET/POST/PUT/DELETE /api/interventions?from=YYYY-MM-DD&to=YYYY-MM-DD`
  - `GET /api/planning/conflicts?from=YYYY-MM-DD&to=YYYY-MM-DD`
- Stockage **en mémoire** (seed de données) pour un démarrage immédiat.
- **CORS** ouvert sur `/api/**` (dev).

### Frontend
- `ApiPlanningService` **branché** sur l’API (JSON ↔ modèles) avec **fallback** mock.
- Nouvelle méthode `listConflicts(from,to)` dans `PlanningService` (+ implémentations mock & API).
- **Bouton “Conflits (N)”** dans la toolbar du planning : affiche un dialogue listant les conflits par ressource avec des raccourcis d’auto-résolution (+30 min).

Lancer localement :
```bash
mvn -q -pl backend spring-boot:run
mvn -q -pl client -DskipTests exec:java
```

## 🚀 Sprint 2 — Agenda ++ (largeurs fractionnées) & Auto‑résolution
**Objectif** : améliorer la lisibilité de l’Agenda en répartissant les tuiles chevauchées **côte à côte**, et offrir des **actions de résolution** directement depuis le panneau Conflits.

### Backend
- `POST /api/planning/resolve` : actions `shift`, `reassign`, `split` sur une intervention.
  - `shift` : décale début+fin de `minutes` (positif ou négatif).
  - `reassign` : change de ressource (si pas de conflit sur la cible).
  - `split` : coupe l’intervention à `splitAt` (création d’une deuxième intervention).

### Frontend
- **AgendaBoard** : calcul de “lanes” par **jour/ressource** → largeur = 1/N, avec marges, type Google Calendar.
- Panneau **Conflits** : trois boutons d’action — *Décaler +30 min*, *Changer de ressource…*, *Couper à…* — branchés sur l’API (fallback mock si offline).

Mesures de done : tuiles correctement fractionnées lors de chevauchements, actions exécutées sans erreur, Undo/Redo possible côté client pour DnD (inchangé).

---

## 🚀 Sprint 3 — Indispos ressources + Création par glisser + Filtres rapides
**Objectif** : confort d'usage du planning.

### Backend
- **Indisponibilités de ressource** (ex : maintenance, panne) :
  - `GET/POST/DELETE /api/resources/{id}/unavailability?from=&to=`
  - Modèle `ResourceUnavailability { id, resourceId, start, end, reason }`

### Frontend
- **Overlays d'indispos** : bandes hachurées grisées par-dessus les jours/heure concernés (Gantt & Agenda).
- **Création par glisser (Agenda)** : cliquer-glisser sur une plage vide → saisie rapide du libellé → création d'une intervention.
- **Filtres rapides** : champ *Filtrer ressources* dans la toolbar (filtre par nom), toggle *Afficher indispos*.
- **DnD plus “doux”** : seuil de démarrage (6 px), poignées de resize ±6 px, snapping conservé.

Lancer :
```bash
mvn -q -pl backend spring-boot:run
mvn -q -pl client -DskipTests exec:java
```

---

## Quick Wins (UX/Qualité)
Cette livraison ajoute des améliorations ciblées, sans casser l’existant :

### Planning
- **Conflits visuels** : chevauchements par ressource détectés, tuile bordée en **rouge**, badge “⚠ conflit” au survol.
- **Raccourcis** : `N` nouveau, `D` dupliquer, `Delete` supprimer, `←/→` ±15 min, `Shift+←/→` ±60 min sur la sélection.

### ERP
- **Statuts verrouillants** (Devis/BC/BL/Facture) : `BROUILLON → VALIDE → ENVOYE → …`. En dehors du brouillon, les champs sont **non éditables** (sauf actions permises).
- **Export PDF (minimal)** : bouton dans les éditeurs. Utilise **OpenPDF** si dispo, sinon impression système (imprimante PDF).

### Thème & style
- **FlatLaf** clair/sombre (arrondis + hover uniformes). Fallback sur LAF par défaut si FlatLaf indisponible.

> Dépendances optionnelles : `com.formdev:flatlaf` et `com.github.librepdf:openpdf`. Si elles ne sont pas résolues (hors-ligne), l’appli continue en mode dégradé.

> Objectif : livrer une base **exécutable hors-ligne**.  
> **Phase 2** : **éditeurs de lignes**, **totaux auto**, **badges de statuts**, **conversions** Devis→BC→BL→Facture (mock).
> **Phase 3** : **Planning DnD** (glisser-déposer + resize, calcul de voies/chevauchements, hauteur de ligne auto, entêtes alignées), **CRUD minimal Ressources**, **câblage Backend/API (SDK léger)** avec fallback mock si l’API est indisponible.
> **Phase 3.1** : **Précision horaire** (X = jours + heures), snap **15 min**, DnD **resize** bord G/D, drag vertical pour changer de ressource, tuiles arrondies & shadow.
> **Phase 3.2** : **Mode Agenda (heures verticales)** + **Undo/Redo** global et corrections DnD (glissements précis, sans “sauts”).  
>  - Toggle Gantt/Agenda dans la barre d’outils du planning.  
>  - Undo: `Ctrl+Z`, Redo: `Ctrl+Y`.  
>  - DnD corrigé : calcul des minutes par delta de souris (dx/dy) + arrondi 5–60 min ; plus d’écarts catastrophiques.

## Prérequis
- Java 17+
- Maven 3.8+

## Build (hors-ligne)
```bash
mvn -q -DskipTests compile
```

## Lancer le client
Dans votre IDE : `com.materiel.suite.client.Launcher#main`  
Au premier démarrage : choix **Mock** (par défaut) ou **API**.

## Structure
```
backend/
  src/main/resources/openapi/gestion-materiel-v1.yaml   # snapshot OpenAPI (ERP + planning + ressources)
client/
  src/main/java/com/materiel/suite/client/Launcher.java
  src/main/java/com/materiel/suite/client/config/AppConfig.java
  src/main/java/com/materiel/suite/client/net/ServiceFactory.java
  src/main/java/com/materiel/suite/client/model/{DocumentLine,DocumentTotals,Quote,Order,DeliveryNote,Invoice}.java
  src/main/java/com/materiel/suite/client/model/{Resource,Intervention}.java
  src/main/java/com/materiel/suite/client/service/{QuoteService,OrderService,DeliveryNoteService,InvoiceService}.java
  src/main/java/com/materiel/suite/client/service/PlanningService.java
  src/main/java/com/materiel/suite/client/service/mock/{MockData,MockQuoteService,MockOrderService,MockDeliveryNoteService,MockInvoiceService}.java
  src/main/java/com/materiel/suite/client/service/mock/MockPlanningService.java
  src/main/java/com/materiel/suite/client/service/api/{ApiQuoteService,ApiOrderService,ApiDeliveryNoteService,ApiInvoiceService,ApiPlanningService}.java
  src/main/java/com/materiel/suite/client/ui/{MainFrame,ModeChoiceDialog,StatusBadgeRenderer}.java
  src/main/java/com/materiel/suite/client/ui/doc/{DocumentLineTableModel,DocumentTotalsPanel}.java
  src/main/java/com/materiel/suite/client/ui/quotes/{QuotesPanel,QuoteEditor}.java
  src/main/java/com/materiel/suite/client/ui/orders/{OrdersPanel,OrderEditor}.java
  src/main/java/com/materiel/suite/client/ui/delivery/{DeliveryNotesPanel,DeliveryNoteEditor}.java
  src/main/java/com/materiel/suite/client/ui/invoices/{InvoicesPanel,InvoiceEditor}.java
  src/main/java/com/materiel/suite/client/ui/planning/{PlanningPanel,PlanningBoard,DayHeader,LaneLayout}.java
  src/main/java/com/materiel/suite/client/net/{RestClient,SimpleJson}.java
  src/main/resources/ui/{logo.svg,tile-hatch.svg,resource-crane.svg}
```

## Phase 3 — Planning & Backend
- **Planning** : DnD / resize sur tuiles, chevauchements en multiples voies, hauteur de ligne auto, entêtes jours alignées, zoom colonne 60–200 px.
- **Backend/API** : `ServiceFactory` sélectionne **Api*** en mode backend (base URL par défaut `http://localhost:8080`). En cas d’échec réseau/parsing, **fallback** transparent sur **Mock*** pour garder l’UX fluide hors-ligne.

> Le SDK léger repose sur `java.net.http.HttpClient` sans dépendances. `SimpleJson` fournit un parsing minimal pour les champs nécessaires.

## Phase 2 — Utilisation rapide
- **Devis** : onglet Devis → **Nouveau** / **Modifier** → éditez les lignes (désignation, Qté, Unité, PU HT, Remise %, TVA %).  
  Les colonnes **Ligne HT**, **TVA €**, **Ligne TTC** sont calculées. Les totaux **HT/TVA/TTC** s’actualisent.  
  Bouton **Créer BC…** convertit en **Bon de commande**.
- **Commandes** : idem, avec bouton **Générer BL…**.
- **BL** : idem, avec bouton **Créer facture…**.
- **Factures** : idem (sans conversion suivante).

Statuts (badges) : Brouillon, Envoyé, Accepté, Refusé, Expiré, Confirmé, Annulé, Signé, Verrouillé, Envoyée, Partiellement payée, Payée.

> Tout est **en mémoire** (mock). Aucune dépendance réseau nécessaire.

## Étapes suivantes
1) Impression/PDF des documents, en-têtes société, mentions légales.  
2) Planning : filtres par ressource, vue semaine/mois, fondu visuel des indispos.  
3) Backend : authentification, pagination, mapping complet JSON ⇄ modèles.  
4) FlatLaf (arrondis + hover) & micro-interactions.

### Variables d’environnement utiles (optionnel)
- `GM_API_BASE` (ex: `http://localhost:8080`)
- `GM_API_TOKEN` (Bearer)

> Note: Maven build currently requires network access to resolve Spring Boot parent POM.

## Audit des classes non utilisées
```bash
mvn -q -DskipTests package
bash tools/deadcode/find-dead-classes.sh
```
Le rapport apparaît sous `tools/deadcode/report/` :
- `dead-classes-client.txt`
- `dead-classes-backend.txt`

### Windows
```bat
mvn -q -DskipTests package
tools\deadcode\find-dead-classes.bat
```
Les rapports sont dans `tools\deadcode\report\`.

### Exclure des classes légitimes
Ajoutez leur FQN (nom complet) dans `tools/deadcode/excludes.txt`. Les points d’entrée courants (UI, contrôleurs REST) sont déjà exclus.

> L’audit ne supprime rien. Une PR/commit de nettoyage manuel est recommandée après revue.
