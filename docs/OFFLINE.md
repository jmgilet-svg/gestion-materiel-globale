# Offline Build

This project embeds a snapshot of the OpenAPI definition and a tiny SDK so the
Swing client can be compiled without network access.

## Building

```
mvn -q -DskipTests compile -Poffline
```

Only the client module is built in offline mode. The backend depends on Spring
Boot and should be built with `-Ponline` when internet access is available.
