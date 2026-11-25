# OpenCode FIT → Projektleitung: #637 c17 Route-4-Stream-Replay

- **Datum:** 2026-11-25 11:21 UTC
- **Von:** OpenCode FIT
- **An:** Projektleitung
- **Bezug:** `CIRQA_CONNECT_NORMAL_HISTORY_COMPARISON_2026-11-25.md`, PR #653
- **Status:** Aktivitätsdatei-Transfer weiter eingegrenzt; Remote-ItemList-/Terminalabdeckung weiter offen.

## Fakten

- Ein erneuter lokaler Replay des vorhandenen privaten BTSnoop-Captures rekonstruiert 649 Host-Write-Commands und 892 Notifications in c17; unvollständige ACL-Assemblies am Capture-Ende: 0.
- Die c17-ATT-Handles sind nicht in diesem Capture entdeckt; die Zuordnung zu ML-RX/TX bleibt aus vorigen Verbindungen abgeleitet.
- Auf der unter dieser Annahme normalisierten Route 4 liegen 49 eingehende Notifications. Die dokumentierte Konfigurations-/Präfixbehandlung ergibt 6.498 Bytes Material. Der zlib-Decoder konsumiert einen vollständigen Stream von 4.012 Bytes mit EOF und erzeugt 12.981 Bytes; FIT-Signatur, Header-CRC und Datei-CRC sind gültig.
- Weitere 2.486 Bytes folgen nach diesem Stream. Sie bilden nach derselben Framingannahme keinen zweiten vollständigen zlib-Stream und bleiben unklassifiziert.

## Empfehlung / Grenzen

Der Bericht präzisiert nun „ein vollständiger Aktivitäts-FIT-Stream vorhanden“ gegenüber „alle Route-4-Bytes erklärt“. Kein neuer Datei-/Tageshistorienimport, kein ItemList-Match und kein terminaler Transferabschluss wird daraus abgeleitet. Keine Protokolländerung, keine Geräteaktion.

PR #653 wurde konfliktfrei auf den damaligen `main`-Stand `5f26be0b` synchronisiert; `main` rückte danach auf `96e21f0e` vor und ist im PR-Branch integriert. Der Dispatch `45602512062` war auf Zwischen-Head `32c45cb0` erfolgreich (API-35 erwartungsgemäß skipped), ist aber weder auf dem finalen Dokumentations-Head noch als PR-Statuscheck zu werten. PR-Statuschecks bleiben durch docs-only `paths-ignore` aus; Branch Protection verlangt Android Build, Dependency Review und Gradle dependency graph. Kein Merge.
