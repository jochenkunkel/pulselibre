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

## Ergebnis des freigegebenen Laufs – 2026-11-17

Build 931; separater Einmal-Gate wurde vor Verbindungsaufbau verbraucht. CI-Build und alle 423 JVM-Unit-Tests waren erfolgreich. Installationsupdate war signaturkompatibel und erhielt App-Daten.

- Bootstrap vollständig: 5024-ACK geschrieben und GATT-bestätigt; 5050-Hostantwort geschrieben und GATT-bestätigt; CRC-valides GFDI-5000-ACK zu 5050 empfangen.
- Genau ein 5007-Request; CRC-valides GFDI-5000-ACK mit Status 0.
- Genau ein leerer 5031-Request; CRC-valide GFDI-5000-Antwort mit Status 0. Der Body wurde strikt geparst; CIRQA meldete:
  - FIT: `128/4`, `128/32`, `128/44`, `128/41`, `128/49`, `128/68`, `128/70`, `128/73`, `128/72`, `128/74`
  - weitere gemeldete Typen: `255/245` (`ErrorShutdownReports`), `255/247` (`ULFLogs`)
- Kein zweiter Antwortframe, kein Retry und **kein 5002**. Keine Dateiliste oder FIT-Datei angefordert.
- Report: dynamischer GFDI-Handle 111; RX/TX waren das bekannte `2810`/`2820`-Paar. TX-Metadaten enthielten sieben GATT-Schreibaufträge mit Callback `SUCCESS`, einschließlich eines 5007- und eines 5031-Requests. Geräteannahme wird durch die jeweiligen CRC-validen GFDI-Antworten belegt, nicht durch den GATT-Callback allein.
- Abschließend Bluetooth getrennt; 5007-Gate blieb 5/5 geschlossen; der 5031-Einmal-Gate ist verbraucht.

**Einordnung:** CIRQA hat den leeren Supported-File-Types-Request angenommen und 12 Typen gemeldet. Das ist keine Dateiliste, kein Beleg für vorhandene Einträge und kein FIT-Historienabruf. Der nächste Connect-Schritt wäre 5002 für Verzeichniseintrag Index 0; dafür liegt keine Freigabe vor und es wurde nichts gesendet.
