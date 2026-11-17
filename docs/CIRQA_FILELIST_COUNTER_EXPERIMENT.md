# CIRQA FileList: kontrollierter Zählertest (17.11.2026)

## Status: VORBEREITET, NICHT SENDEREIF

Der alte kompakte FileListRequest `0x8A39` wurde von CIRQA mit `UNSUPPORTED` abgelehnt. `CirqaMlHandshakeProbe` sperrt diesen Sendepfad bewusst. Er bleibt gesperrt.

Die Offline-Kodierung kann jetzt explizite Sequenznummern und Counter verwenden (`GarminFileListRequest.gfdi(sequence, counter)`). Die neue pure Kotlin-Freigabelogik `CirqaFileListCounterGate` verlangt:

- ausdrückliche Nutzerfreigabe für **genau eine** Anfrage,
- bestätigte 5050-Schreibfolge und **frischen** GFDI-5000-ACK,
- tatsächlich beobachtete Sequenz-/Counter-Evidenz aus **derselben** aktuellen Sitzung,
- gültige Werte (Sequence 1..127; Counter 0..65535),
- keine bereits gesendete Anfrage und keine negative Protokollantwort.

Fehlt ein Beleg, ist das Ergebnis **NO_SESSION_EVIDENCE** oder **WRONG_SESSION**; es wird nichts gesendet. Insbesondere sind Startwerte aus GarminHRM600 kein Nachweis für CIRQA. Die aktuellen passiven CIRQA-Logs belegen keinen akzeptierten FileSync-V2-Sitzungszähler.

## Noch erforderlich vor einem APK-Gerätetest

1. Verifizierte Herleitung des aktiven FileSync-Sequenz-/Counter-Zustands aus CIRQA-Nachrichten oder einer CIRQA-spezifisch bestätigten Referenzimplementierung.
2. Verdrahtung der Freigabelogik mit dem bestehenden GATT-Diagnosepfad, ohne den alten gesperrten FileList-Modus zu reaktivieren.
3. Einmaliger, expliziter UI-Opt-in; Timeout, Abbruch bei negativen Antworten und maximal ein Request pro Verbindung.
4. Unit-Tests, Android-Build und bereitgestelltes Test-APK.

**Aktueller Stand:** Nur Offline-Encoder, Entscheidungsgate und Unit-Tests sind implementiert. Es existiert noch **kein freigegebener aktiver Zählertest und keine neue Test-APK**. Eine GATT-Schreibbestätigung allein wäre keine Bestätigung der Protokollakzeptanz.
