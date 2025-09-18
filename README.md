# Gestion Matériel — README

Application **planning & exploitation** pour sociétés de levage/transport/manutention :
ressources (grues, camions, chariots…), **interventions** multi-ressources, **devis/ventes**,
paramétrage fin (icônes, types…), **sécurité par rôles**, et gros **jeu de données mock**.

> ✅ **Contrat v1 préservé**. Les ajouts sont introduits en **v2** (DTO/contrôleurs) pour éviter toute régression.

---

## 🧭 Sommaire
1. [Aperçu des fonctionnalités](#aperçu-des-fonctionnalités)
2. [Sécurité & rôles](#sécurité--rôles)
3. [Données mock enrichies](#données-mock-enrichies)
4. [UX — Navigation & écrans](#ux--navigation--écrans)
5. [Interventions (v2)](#interventions-v2)
6. [Ressources & types](#ressources--types)
7. [Paramétrage](#paramétrage)
8. [Ventes (devis/BC/BL/factures)](#ventes-devisbcblfactures)
9. [API & contrat](#api--contrat)
10. [Build & Run](#build--run)
11. [Roadmap](#roadmap)

---

## Aperçu des fonctionnalités

- **Planning** d’interventions avec **drag & drop** (déplacement), **filtre de période** (semaine/mois) et **refresh auto** après enregistrement.
- **Interventions multi-ressources** : sélection de plusieurs ressources de **tout type**, avec :
  - **Type d’intervention** paramétrable (icône, ordre d’affichage), **tri** & **duplication rapide**,
  - **Description**, **note interne**, **note de fin**,
  - **Horaires planifiés** et **horaires effectifs** (début/fin),
  - **Contacts client** multiples (filtrés par client sélectionné),
  - Pré-devis rapide : **lignes par ressource** avec **PU HT** (prix porté par la *ressource*).
- **Ressources** : vue type “Clients” avec édition **inline** dans le même panneau, filtre par **type**, tri par **type**, **indisponibilités** (date début/fin).
- **Icônes SVG** en **couleur** et **catalogue partagé** (ressources, types, recherche globale, toasts/notifications, tuiles d’intervention).
- **Paramètres** : icônes, types d’intervention (**ordre par DnD**, duplication, édition inline), général (ex. **durée de session**).
- **Sécurité** : login (avec **agence**), rôles, **masquage** fin du menu, **lecture seule** sur écrans/boîtes de dialogue en fonction des droits,
  **expiration de session** par inactivité, **changement de mot de passe**, **administration des utilisateurs**.
- **Mock** riche pour la démo : ~**60 ressources**, **20 clients** × **2–4 contacts**, **15 utilisateurs**, ~**60 interventions** sur **2 semaines**.

---

## Sécurité & rôles

Au lancement (après choix **Mock** / **API**), une fenêtre de **connexion** s’ouvre :
sélection de **l’agence**, **login/mot de passe** (en mock : `admin/admin`, `sales/sales`, `config/config` et variantes).

**Rôles :**
- **ADMIN** : tous les droits.
- **SALES** : lecture planning + **édition** ventes (Devis/BC/BL/Factures), pas de configuration.
- **CONFIG** : lecture générale + **édition** **Ressources** et **Paramètres**.

**Comportements clés :**
- Menu latéral **masqué** par droit (planning, ventes, ressources, paramètres).
- **Interventions** en lecture seule pour non-ADMIN.
- Ventes en lecture seule pour non-SALES/ADMIN (boutons Nouveau/Modifier/Supprimer/Enregistrer **désactivés**).
- **Header** : bouton **Déconnexion** + **“Mot de passe…”** (changement du mot de passe utilisateur).
- **Session** : expiration par inactivité (par défaut **30 min**, **paramétrable**).
- **Pré-câblage JWT** : le mock renvoie un `token` (client l’envoie en `Authorization: Bearer …`) — prêt pour durcir côté API.

**Administration des comptes (ADMIN seulement)** :
- Onglet **Paramètres → Comptes utilisateurs** : liste, **créer**, **modifier**, **supprimer**, **définir mot de passe**.

---

## Données mock enrichies

- **Ressources (~60)** : grues, camions, chariots, conteneurs, quelques convois “spéciaux”, états variés (DISPONIBLE / OCCUPÉE / EN_MAINTENANCE), **PU HT réalistes**.
- **Clients (20)** : adresses FR plausibles, **tri par nom**.
- **Contacts (2–4 par client)** : email/portable cohérents, **contact principal** marqué (si le modèle le supporte).
- **Utilisateurs (15)** : répartition ADMIN/SALES/CONFIG sur 2 agences, mots de passe mock alignés sur les logins.
- **Interventions (~60 / 2 semaines)** : titres, adresses (client ou “chantier”), **types** (icônes), **ressources multiples**, **contacts**, notes, horaires planifiés/logiques.

> Les seeds mock sont **déterministes** (graine) pour des tests stables.

---

## UX — Navigation & écrans

- **Menu latéral** compact **épingle**/**auto-repli** (icône + libellé au survol).
- Icônes en couleur partout (menu, recherche globale, toasts, tuiles).
- **Ressources** : même **pattern que Clients** (édition dans le panneau – pas de dialog), **filtre** par type, tri par défaut **par type**.
- **InterventionDialog** : réorganisée et **tabulée** (ex. *Général*, *Intervention*, *Facturation*), **plein écran** pour les dispatchers, sélection **ergonomique** des ressources/contacts (listes + filtres).
- **Types d’intervention** : tableau avec **drag & drop** pour l’ordre (`orderIndex`), **duplication**, **édition inline** (F2), persistance par **ID**.

---

## Interventions (v2)

Caractéristiques principales :
- Plusieurs **ressources hétérogènes** affectées.
- **Type d’intervention** (icône configurable, ordre personnalisable).
- **Horaires planifiés** et **effectifs** (début/fin).
- **Contacts client** multiples (et **filtrage** par client).
- **Notes** (interne + fin).
- **Pré-devis** : génération initiale des lignes avec **PU** de chaque ressource.
- **Signature** PNG (champ prévu côté service; utilisé si présent).

**Planning :**
- **Drag & drop** pour déplacer un créneau; filtre **Semaine/Mois**; rechargement auto après `save()`.
- Ouverture depuis le planning (double-clic / menu contextuel) ; en lecture seule si l’utilisateur n’a pas le droit d’éditer.

---

## Ressources & types

- **Ressource = prix unitaire (PU HT)** porté par la *ressource* (pas par le type).
- **Indisponibilités** stockées en paires **date début** / **date fin**.
- **Type de ressource** : porte l’**icône** (catalogue SVG couleur commun).
- Édition **inline** des ressources dans le panneau (à la “Clients”).

---

## Paramétrage

- **Icônes** : catalogue SVG couleur mutualisé (ressources, types, recherche globale, toasts).
- **Types d’intervention** : ordre **DnD**, **duplication**, **édition inline** (F2), tri persistant (`orderIndex`).
- **Général** : **durée d’inactivité** (minutes) avant déconnexion — *appliquée à chaud*.
- **Comptes utilisateurs (ADMIN)** : CRUD utilisateurs + définition de mot de passe.

---

## Ventes (Devis/BC/BL/Factures)

- Accès **lecture/édition** contrôlé par rôle (**SALES** ou **ADMIN** pour l’édition).
- **Dialogs** de ventes forcent la **lecture seule** si l’utilisateur n’a pas les droits (boutons d’action désactivés, champs non éditables).
- Conversion/flux à venir (pré-devis depuis l’intervention déjà amorcé via lignes ressources).

---

## API & contrat

### Compatibilité
- **OpenAPI v1** conservée pour les endpoints historiques (ex : `#/components/schemas/Quote` existants).
- Les nouveautés sont **versionnées en v2** (DTO/contrôleurs dédiés).

### Endpoints v2 ajoutés

Authentification & agences :
- `GET /api/v2/agencies` — liste des agences.
- `POST /api/v2/auth/login` — **login** (retourne `UserV2` + éventuel `token` mock).

Administration utilisateurs :
- `GET /api/v2/users` — lister.
- `POST /api/v2/users` — créer (payload `UserCreateRequest` avec mot de passe).
- `PUT /api/v2/users/{id}` — mettre à jour.
- `DELETE /api/v2/users/{id}` — supprimer.
- `POST /api/v2/users/{id}/password` — définir le mot de passe.

Types d’intervention :
- `GET /api/v2/intervention-types` — renvoie la liste **triée** par `orderIndex`.
- `POST /api/v2/intervention-types` / `PUT /api/v2/intervention-types/{id}` — création/maj (gèrent `orderIndex`).

> **Casing JSON** : attention aux propriétés `totalTtc` (client/serveur alignés via `@JsonProperty("totalTtc")` le cas échéant).

### Auth côté client
- Le client reçoit un `token` (mock) au login et l’envoie automatiquement via `Authorization: Bearer …`.
- Le backend **n’exige pas** encore le JWT : prêt pour un durcissement futur (filtre/decoder).

---

## Build & Run

### Backend
```bash
mvn -pl backend -am spring-boot:run
```
Expose les endpoints **v1** historiques et les ajouts **v2** ci-dessus. Spécification OpenAPI dans
`backend/src/main/resources/openapi/gestion-materiel-v1.yaml` (inclut les schémas v2).

### Client (Swing)
```bash
mvn -pl client -am package
java -jar client/target/gestion-materiel-client.jar
```
Au démarrage :
1. Choisir **Mock** ou **API**,
2. **Login** (sélection d’agence + identifiants).

**Comptes mock** utiles :
- `admin / admin` (plein accès),
- `sales / sales` (ventes éditables),
- `config / config` (paramètres & ressources éditables).

---

## Roadmap

- Génération **PDF** (bons d’intervention / BL / factures) avec les icônes & signatures.
- **Mobile** terrain : app dédiée pour chauffeurs/grutiers (liste du jour, démarrage & fin effectifs, signature client).
- **JWT** côté backend (validation, rôles/autorités) + rafraîchissement de token.
- Import/Export **JSON** des types d’intervention & icônes.
- Optimisation calendrier : **regroupement** par ressource, **détection de conflits** (chevauchements/indispos).

---

## Licence

Voir le fichier `LICENSE` le cas échéant.
