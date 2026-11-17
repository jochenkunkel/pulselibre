# CIRQA GFDI-5031 Einzelprobe – Vorbereitung

## Zweck und Freigabe

Der Nutzer hat einen einzelnen, begrenzten 5031-Test freigegeben. Die Probe soll nur klären, ob CIRQA auf Connect-5.31-kompatible `SUPPORTED_FILE_TYPES` antwortet. Sie prüft keine Verzeichnisliste und lädt keine FIT-Datei.

## Ablauf und Stop-Gates

1. Separates persistentes Einmal-Gate verbrauchen, bevor eine GATT-Verbindung aufgebaut wird. Es ist unabhängig vom geschlossenen 5007-Gate 5/5.
2. Garmin-ML/GFDI-Dienst registrieren und bestehenden 5024/5050-Bootstrap ausführen.
3. Genau einen 5007-Request senden, ausschließlich nach vollständig bestätigtem Bootstrap.
4. Nur nach CRC-validierter GFDI-5000-Antwort auf 5007 mit Status 0 genau einen leeren 5031-Request senden.
5. Höchstens fünf Sekunden auf die Antwort warten; maximal ein Ergebnisframe akzeptieren. Antwortstatus und File-Type-Body getrennt validieren.
6. Bei negativem/fehlendem Bootstrap oder 5007-ACK, fehlendem Handle, Schreibfehler, unerwartetem/mehrfachem Frame, ungültigem COBS/GFDI/CRC oder fehlerhaftem File-Type-Body sofort stoppen.
7. Nach 5031 in allen Fällen Verbindung beenden. **Kein 5002, Retry, 5004 oder sonstiges Folgekommando.**

Die Probe folgt dem Connect-5.31-Verhalten: leerer 5031-Body; erfolgreicher Ergebnis-Payload beginnt mit Anzahl und Einträgen `(type, subtype, UTF-8-name)`. Connects `UNKNOWN_OR_NOT_SUPPORTED`-Fallback ist eine Cliententscheidung und wird nicht als CIRQA-Dateitypenantwort ausgegeben. Fremdgeräteberichte – einschließlich HRM 600 und FR245 – sind kein CIRQA-Nachweis.

## Status

Implementierung und Offline-Regressionstests wurden vorbereitet. Vor Hardwarelauf erforderlich: erfolgreicher Android-Build/JVM-Test und Bereitstellung eines passenden Builds, anschließend Gerätevoraussetzungen prüfen. In dieser Vorbereitung wurde keine BLE-Verbindung aufgebaut und kein Gerätetest gestartet. Lokaler Build/Test ist wegen fehlender installierter/lizenzierter Android SDK Build-Tools 34 und Platform 35 derzeit blockiert.
