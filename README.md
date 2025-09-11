# Gestion Matériel Globale

Monorepo Maven Java 17 with two modules:

- **backend** – Spring Boot REST API exposing simple quote management.
- **client** – Swing desktop application using FlatLaf.

## Build

```
mvn -q -DskipTests compile -Poffline
```

Only the Swing client module is compiled in offline mode. The backend requires
Spring Boot and should be built with `-Ponline` once dependencies are
available.

To launch the client:
```
cd client
mvn -q exec:java -Dexec.mainClass=com.materiel.suite.client.Launcher
```
The application prints `CLIENT_READY_UI_OFFLINE` on startup.

This repository still contains a minimal skeleton of the much larger ERP
described in the specification.
