# Dead code audit

Outil léger **offline** pour repérer les classes **non référencées** à l'exécution.

### Linux / macOS
```bash
mvn -q -DskipTests package
bash tools/deadcode/find-dead-classes.sh
```

### Windows
```bat
mvn -q -DskipTests package
tools\deadcode\find-dead-classes.bat
```
Les rapports sont générés dans `tools/deadcode/report/`:
- `dead-classes-client.txt`
- `dead-classes-backend.txt`

Pour exclure des classes légitimes, ajoutez leur FQN dans `tools/deadcode/excludes.txt`.
