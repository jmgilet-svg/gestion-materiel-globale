# Gestion Matériel Globale
ERP & planning multi-agences pour location de grues/camions/remorques avec chauffeur — monorepo Java (Spring Boot + Swing).

## Table des matières
- [Aperçu du monorepo](#aperçu-du-monorepo)
- [Fonctionnalités (vue d’ensemble)](#fonctionnalités-vue-densemble)
- [Démarrage rapide (dev)](#démarrage-rapide-dev)
- [API & contrats](#api--contrats)
- [Guide utilisateur (client Swing)](#guide-utilisateur-client-swing)
- [Architecture & décisions clés](#architecture--décisions-clés)
- [Roadmap (lots priorisés)](#roadmap-lots-priorisés)
- [Contribuer / Dev notes](#contribuer--dev-notes)
- [Licences & mentions](#licences--mentions)

## Aperçu du monorepo
- `backend/` – API Spring Boot 3 (Java 17)
- `client/` – Application desktop Java Swing (Java 17)
- `seeds/` – Données d’exemple (devis, commandes) pour tester le pipeline commercial
- `docs/` – Notes techniques (offline build, OpenAPI vendored)

Statut : expérimental mais exploitable en dev.

## Fonctionnalités (vue d’ensemble)
### Planning & Agenda
- ✅ DnD des interventions, redimensionnement, snapping 15 min
- 🚧 Panneau “Conflits (N)” + actions d’auto-résolution (shift/reassign/split) côté client et endpoints côté serveur
- 🚧 Lanes parallèles (affichage côte à côte en cas de chevauchements)
- 🚧 Indisponibilités ressources (overlays, CRUD API)
- ✅ Toolbar : bascule Semaine/Jour, densité, filtres rapides
- ✅ Raccourcis clavier (N, D, Suppr, ←/→, Shift+←/→)
- 🚧 Undo/Redo unifié (mouvements, resize, assignation)

### Ressources & indispos
- 🚧 Endpoints GET/POST/DELETE `/api/resources/{id}/unavailability` + overlay UI

### Documents commerciaux (Devis → Bon de commande → BL → Facture)
- 🚧 Pipeline statutaire (Brouillon→Validé→Envoyé→…)
- 🚧 Totaux automatiques (multi-TVA), modèles de lignes (heure/jour/demi-journée, arrondis ¼ h)
- 🚧 PDF multi-tenant (logo, palette, CGV, mentions), séquences (ex. FAC-00001)

### Exports et conformité
- 🧭 FEC export strict + ZIP avec SHA-256
- 🧭 Exports CSV/XLSX
- 🧭 Mapping comptable en base + mini admin UI

### Sécurité & multi-tenant
- 🚧 JWT (`/auth/login`) + Bearer sur `/api/**`
- 🚧 En-tête `X-Tenant` bout-en-bout
- ✅ SSE `/api/system/ping` (~15 s) pour keep-alive (client + serveur)

### Offline & fiabilité
- 🚧 File d’ordres locale (queue), retry avec backoff, reprise au démarrage
- ✅ Mode Mock vs API sélectionnable au démarrage du client

### Qualité & CI
- 🚧 Tests unitaires/services (conflits, résolution, indispos)
- 🚧 CI Maven, packaging JAR, scripts d’exécution

## Démarrage rapide (dev)
### Prérequis
- Java 17
- Maven 3.9+
- (Optionnel) Docker pour un Postgres local
- Lire `docs/OFFLINE.md` pour compiler le client en mode hors-ligne (`-Poffline`)

### Backend
```bash
cd backend
mvn spring-boot:run
```

- Profil `dev` (défaut) : `application.yml` → H2 en mémoire + console H2 activée.
- Profil `pg` (en cours) : future `application-pg.yml` à activer via `--spring.profiles.active=pg` ; en attendant, injecter `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.
- Variables d’environnement utiles : `JWT_SECRET`, `TENANT_DEFAULT`, `ALLOWED_ORIGINS`.
- URL dev typique : http://localhost:8080.

### Client (Swing)
```bash
cd client
mvn -q exec:java
```

- Classe main alternative : `com.materiel.suite.client.Launcher`.
- Choix Mock/API : effectué sur l’écran d’accueil (base URL, token, tenant).

> **Mode Mock vs API**  
> Mock = données locales, pas de détection de conflits serveur ni de persistance.  
> API = appelle le backend REST ; nécessite un token JWT (en cours) et l’en-tête `X-Tenant`.  
> Le mode peut être rebasculé via `Paramètres → Connexion` ; une file offline (🚧) mettra en cache les actions lors des coupures réseau.

### Seeds & données d’essai
- Le backend charge un jeu d’essai en mémoire (`InMemoryStore`) : ressources “Grue A/B”, interventions de la semaine courante.
- Jeux complémentaires : `seeds/quotes.json`, `seeds/orders.json` pour préparer les écrans Devis/Commandes.
- Adapter les seeds lors du passage en multi-tenant (prévoir colonnes `tenantCode`).

## API & contrats
- OpenAPI versionnée : `backend/src/main/resources/openapi/gestion-materiel-v1.yaml` (copie vendored dans `client/openapi/` pour build offline).
- Contrats mis à jour à chaque livraison backend ; les clients Swing consomment un SDK généré à partir de ce fichier.

Endpoints clés :
- POST `/auth/login` → JWT (🧭 roadmap)
- GET `/api/system/ping` (SSE, dev) → keep-alive et surveillance des pannes
- GET `/api/resources` / GET `/api/interventions` (dev)
- GET `/api/planning/conflicts?from=&to=` (🚧)
- POST `/api/planning/resolve` avec `action: shift|reassign|split` (🚧)
- GET|POST|DELETE `/api/resources/{id}/unavailability` (🚧)

Exemples `curl` :
```bash
curl -N http://localhost:8080/api/system/ping
curl -H "X-Tenant: DEMO" http://localhost:8080/api/planning/conflicts?from=2025-09-15T00:00:00&to=2025-09-21T23:59:59
```

En-têtes à fournir :
- `Authorization: Bearer <token>` (JWT, 🚧)
- `X-Tenant: <code_agence>` (multi-tenant, 🚧)

## Guide utilisateur (client Swing)
- Vue Planning : bascule Semaine/Jour, drag & drop horizontal/vertical, resize avec pas de 15 min, densité d’affichage.
- Vue Agenda (15 min) : colonnes par ressource, création rapide par double-clic, navigation clavier (←/→ pour ±15 min).
- Panneau Conflits : liste des collisions, boutons **Décaler**, **Réassigner**, **Scinder** (API en cours d’activation).
- Overlays d’indisponibilités : affichage (🚧) avec toggle dans la toolbar ; CRUD relié au backend en cours.
- Documents commerciaux : création d’un Devis, conversions Devis→BC→BL→Facture (🚧), totaux multi-TVA et modèles de lignes (🚧).
- Export PDF : thèmes par tenant, logos et CGV (🚧, utilise OpenPDF si dispo).

Pipeline statuts (personnalisable) :

| Étape | Description |
| --- | --- |
| Brouillon | édition libre + duplication |
| Validé | verrouillage partiel, prêt pour envoi |
| Envoyé | suivi client, relances |
| Facturé | déclenche la facturation & export comptable |

## Architecture & décisions clés
- Monorepo Maven : `backend/` (API) vs `client/` (Swing) avec parent `pom.xml` commun.
- OpenAPI embarqué pour garantir le build offline et éviter les divergences de contrat.
- Multi-tenant via en-tête `X-Tenant` (🚧) + colonnes tenant dans les futures tables.
- Authentification JWT (🚧) : filter Spring Security à activer, stockage des refresh tokens à définir.
- SSE `/api/system/ping` : keep-alive 15 s pour détecter les coupures réseau (implémentation minimaliste côté serveur/client).
- File offline (🚧) : ordonnancement local et reprise après reconnexion.
- Documents commerciaux : pipeline Devis→BC→BL→Facture et PDF multi-tenant (🚧) avec séquences par agence.
- Exports comptables : FEC/CSV/XLSX + archive ZIP signée SHA-256 (🧭).

## Roadmap (lots priorisés)
- **Lot A – Conflits (serveur & client)** : finaliser les endpoints `/api/planning/conflicts` & `/api/planning/resolve`, panneau Conflits interactif.
- **Lot B – Indispos** : CRUD complet des indisponibilités + overlay UI et filtres.
- **Lot C – Agenda pro** : lanes parallèles, snapping consolidé, DnD vertical/horizontal stabilisé.
- **Lot D – Docs commerciaux** : statuts & conversions complètes, totaux multi-TVA, modèles de lignes, PDF multi-tenant & séquences.
- **Lot E – Sécurité & multi-tenant** : auth JWT, propagation `X-Tenant`, SSE robuste, file offline & retry.

## Contribuer / Dev notes
- Branche principale : `main`. Utiliser des PRs courtes ; pas de branches longues non rebases.
- Conventions de commit : Conventional Commits (`feat:`, `fix:`, `docs:`…).
- Style Java : Google Java Style simplifié (imports groupés, pas de `*`).
- Tests à compléter : services de détection de conflits, règles de résolution, indisponibilités, conversions Devis→Facture.
- Build local : `mvn -q -DskipTests install` depuis la racine ; exécuter `mvn -pl backend test` avant PR dès que les suites seront ajoutées.
- Scripts de lancement dédiés (`run-backend.sh`, `run-client.sh`) : TODO.
- Pense-bête multi-tenant : toujours inclure `X-Tenant` dans les appels API et filtrer côté repo/service.

## Licences & mentions
- Licence : MIT (`LICENSE`).
- Dépendances clés : Spring Boot 3, Spring Data JPA, OkHttp, Jackson, FlatLaf (UI), OpenPDF (PDF). Tous compatibles Java 17.
- Packages tiers embarqués : SDK client généré depuis l’OpenAPI, thèmes FlatLaf clair/sombre.
- Mentions légales : aligner CGV et identité visuelle par tenant ; signature électronique avancée (eIDAS) en 🧭 roadmap.
- Sécurité : audit JWT/tenants à planifier avant déploiement prod ; vérifier les hachages SHA-256 sur les exports.
