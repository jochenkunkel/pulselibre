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

## Passive RX-Zählerdiagnose (17.11.2026)

`CirqaCompactSessionObserver` ist nun in `GarminFileListProbe` integriert. Der Diagnosebericht zeigt CRC-geprüfte kompakte **RX**-Frames, die zuletzt beobachtete Sequenz/Counter/Kennung sowie Duplikate und Diskontinuitäten. Der Observer speichert keine Protobuf-Nutzdaten. Unit-Tests prüfen CRC-Ablehnung, Wraparound, Duplikate und Sprünge.

**Wesentliche Einschränkung:** Empfangene Geräte-Zähler sind nicht gleichbedeutend mit dem Host-Sendezähler. Der Observer speist das Freigabegate ausdrücklich **nicht**; der aktive FileList-Sendepfad bleibt deaktiviert. Erst CIRQA-spezifisch belegte TX-Zähler erlauben eine belastbare Freigabe.

## Realgerät: passive Historien-Diagnose (17.11.2026)

Nutzerbericht: GFDI-Registrierung bestätigt; 5 Notifications (1 Registrierung, 4 GFDI); alle vier GFDI-Frames sind CRC-gültige 5024-DeviceInformation, drei Wiederholungen; 5024-ACK nicht gesendet; 5050=0, 5037=0, FileListResponse=0; kompakte RX-Frames=0, TX-Session-Zähler unbekannt. Keine negativen Antworten und keine ungültigen Frames.

**Interpretation:** Der passive Modus erreicht ohne 5024-ACK keine spätere Bootstrap-Phase. Null kompakte Frames ist kein Beweis für falsche Counter oder fehlende FileSync-V2-Unterstützung. Dieser Test darf nicht als Zählertest gewertet werden. Vor einer aktiven FileList-Probe zuerst den bereits implementierten expliziten 5024+5050-Diagnosepfad nutzen, um nach bestätigter 5050-Antwort passiv nach kompakten Frames zu suchen. Selbst dann bleibt RX ≠ TX; der Sendepfad ist weiter gesperrt.

## Realgerät: 5024+5050 mit Null-Hostmaske (17.11.2026)

Nutzerbericht: GFDI registriert (Handle 63), 5024 einmal, 5050 einmal, GFDI-5000-ACK für 5050 einmal (request=5050/status=ACK), keine negativen Antworten, keine ungültigen Frames. 5050-Antwort 15 Bytes mit **allen Host-Bits 0**, GATT-Schreibfolge bestätigt. Nach Schreibbestätigung genau ein 5000-Frame; kein 5037, 5011, FileListResponse oder kompakter RX-Frame. Ergebnis `ACK_WITHOUT_5037`.

Dieser Test ist **nicht** der frühere SYNC-Bit-3-Test. Nullmaske und SYNC-Bit 3 dürfen in Vergleichen nicht vermischt werden. Das ACK beweist keine Historienfreigabe. Der passive Decoder zeigt `5050-Antwort=NICHT_GESENDET`, obwohl der aktive Test sie sendete; diese Felder betreffen unterschiedliche Komponenten und sind nicht als widersprechende BLE-Evidenz zu interpretieren. RX- und TX-Counter bleiben unbekannt; der FileList-Sendepfad bleibt deaktiviert.
