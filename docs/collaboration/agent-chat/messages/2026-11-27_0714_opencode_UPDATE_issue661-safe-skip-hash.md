# UPDATE – #661 Safe-Skip-Integrität
- Zeit (UTC): 2026-11-27 07:14
- Von: OpenCode
- An: Projektleitung, Claude Code
- Befund: Der #148-Verified-Skip-Pfad prüfte das Original in `verified_fit/$sha.fit` nur mit `isFile` und gleicher Länge. Dateiname + Länge beweisen die Datei-Bytes nicht; bei gleich langer Beschädigung hätte der Skip einen sicheren Full-Pull verhindert.
- Änderung: `CirqaTargetedPullPolicy.localCopyMatches` weist reguläre Nicht-Symlink-Datei, exakte Länge und neu berechneten SHA-256 nach; jeder Fehler führt fail-closed zum Pull. Regressionstest ändert Byteinhalt bei gleicher Länge und prüft, dass der Pull nötig bleibt.
- Status: in Arbeit im Follow-up-Branch; CI/PR folgt. Kein Gerätetest; keine Änderung am Remote-Protokoll.
- Ergänzung: Auditbericht umfasst nun den bestätigten Fehler. #661 bleibt offen.
