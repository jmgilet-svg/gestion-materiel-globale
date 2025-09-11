# Gestion Matériel Globale

Monorepo Maven Java 17 with two modules:

- **backend** – Spring Boot REST API exposing simple quote management.
- **client** – Swing desktop application using FlatLaf.

## Build

```
mvn -q -DskipTests compile
```

To run backend:
```
cd backend
mvn spring-boot:run
```
The server logs `BACKEND_READY_API` when started.

To run client:
```
cd client
mvn -q exec:java -Dexec.mainClass=com.materiel.suite.client.Launcher
```
The UI shows a window and prints `CLIENT_READY_UI` in the console.

This repository only implements a minimal skeleton of the full ERP described in the specification.
