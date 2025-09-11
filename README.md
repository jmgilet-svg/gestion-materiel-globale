# Gestion Matériel — Monorepo (offline-ready)

Monorepo Maven (Java 17) avec deux modules :
- **backend/** : snapshot OpenAPI statique (pas d’exécution requise hors-ligne)
- **client/** : application Swing (Mode Mock par défaut), fenêtre de choix Mock/API, squelette d’UI principale

> Objectif : livrer une base **exécutable hors-ligne**.  
> **Phase 2** ajoute : **éditeurs de lignes**, **totaux auto**, **badges de statuts**, **conversions** Devis→BC→BL→Facture (mock).

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
  src/main/java/com/materiel/suite/client/service/{QuoteService,OrderService,DeliveryNoteService,InvoiceService}.java
  src/main/java/com/materiel/suite/client/service/mock/{MockData,MockQuoteService,MockOrderService,MockDeliveryNoteService,MockInvoiceService}.java
  src/main/java/com/materiel/suite/client/ui/{MainFrame,ModeChoiceDialog,StatusBadgeRenderer}.java
  src/main/java/com/materiel/suite/client/ui/doc/{DocumentLineTableModel,DocumentTotalsPanel}.java
  src/main/java/com/materiel/suite/client/ui/quotes/{QuotesPanel,QuoteEditor}.java
  src/main/java/com/materiel/suite/client/ui/orders/{OrdersPanel,OrderEditor}.java
  src/main/java/com/materiel/suite/client/ui/delivery/{DeliveryNotesPanel,DeliveryNoteEditor}.java
  src/main/java/com/materiel/suite/client/ui/invoices/{InvoicesPanel,InvoiceEditor}.java
  src/main/resources/ui/{logo.svg,tile-hatch.svg,resource-crane.svg}
```

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
1) Planning DnD (TimeGridModel partagé, LaneLayout, DnD move/resize/chg ressource, zoom).  
2) CRUD Ressources + fenêtes de maintenance + rendu dans le planning.  
3) Mode Backend (SDK OpenAPI) + WireMock tests.  
4) FlatLaf (arrondis + hover) & micro-interactions.

