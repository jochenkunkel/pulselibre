# CIRQA: Vorbereitung des 5030→5007→Directory-Übergangs

> Nachtrag Jour fixe: Die Null-Erfolgshypothese ist überholt. Der Modell-Default
> akzeptiert jetzt keinen inneren Erfolgswert; nur synthetische Tests injizieren
> eine Abbildung. Siehe [5030-Evidenz und Freigabevorlage](CIRQA_5030_ONLY_RELEASE_PROPOSAL_2026-11-18.md).

## Ausgangspunkt und Reichweite

Der einmalige Directory-Nachlauf mit Build 949 ergab einen Eintrag:
`idx=0,type=0,subtype=0,read=false,flags=0x00,size=0`.
Das ist kein Nachweis, dass CIRQA keine historischen Dateien enthält. Beide
Directory-Läufe sendeten nach dem frischen Bootstrap weder 5030 noch 5007.
Der alte 5007-Gate und beide Directory-Gates bleiben verbraucht.

Nutzerauftrag: Ergebnis in Git dokumentieren und weiterarbeiten. Diese Änderung
bereitet den nächsten Übergang ausschließlich offline vor; kein neuer Live-Aufruf,
kein Hintergrundpfad, keine Wiederverwendung bestehender Gates.

## Erneuter Quellenabgleich

Die lokale Connect-5.31-Klasse `yb60.a` baut den Body des Systemevents als
`[eventByte, 0]`, sendet ihn über `xns.b(5030, body, ...)` und wertet das erste
Byte des zurückgegebenen Payloads aus. Das Vorhandensein eines eigenen
Payload-Ergebnisses ist damit belegt; die exakte CIRQA-Antwortform und deren
Ergebniswerte sind noch nicht beobachtet. Ein äußeres GFDI-5000-Status-0-ACK
allein belegt kein positives inneres Systemevent-Ergebnis.

Connect ruft seinen Sync-Listener unter bestimmten Host-Capability-Bedingungen
auch im Fehler-/finally-Pfad auf. PulsLibre übernimmt dies **nicht**.
FULL_SYNC einschließlich Zeitsetzung, Cloud und Archivierung wird ebenfalls
nicht nachgebaut. Es werden keine Capabilitymasken oder Identitäten geändert.
Quellenprovenienz: `docs/protocol/research/GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md`;
proprietäre APK/DEX/Dekompilate verbleiben außerhalb des Repositories.

## Neue Offline-Primitive

- `CirqaLegacy5030RequestCandidate`: ausschließlich GFDI-Rahmen für
  `HANDSHAKE_COMPLETE`, Body `[08 00]`, Länge und CRC-16/ARC; keine BLE-API.
- `CirqaConnectDirectoryTransition`: reine Zustandsmaschine, ohne Preferences,
  DB oder BLE. Beginnt nur mit Bestätigung eines **separaten**, vorher persistent
  verbrauchten Gates und vorhandenem Typcache. Dies ersetzt keine spätere
  atomare Gate-Persistierung in der UI.
- Vollständig bestätigter 5024/5050-Bootstrap und leerer Schreibpfad sind vor
  dem einmaligen Claim für 5030 erforderlich.
- Für jede Stufe müssen GATT-Abschluss und korrelierte CRC-valide Antwort
  vorliegen, unabhängig von ihrer Ankunftsreihenfolge. Weder alleiniger
  Schreibabschluss noch alleinige Geräteantwort gibt die nächste Stufe frei.
- Konservative **Hypothese**, nicht nachgewiesenes CIRQA-Wireformat:
  nichtkompakte 5000-Antwort auf den erwarteten Typ, äußerer Status 0 plus exakt
  ein inneres Ergebnisbyte 0. Fehlendes/zusätzliches Ergebnis, anderer Typ,
  Compact-Format, CRC-Fehler, negativer Status, doppelte oder verfrühte Callbacks
  stoppen dauerhaft. Auch für 5007 ist die innere Form erneut zu prüfen; der
  bisherige aktive Observer klassifiziert nur den äußeren Status.
- Kein Überspringen/Wiederholen einer Stufe, kein Retry, Abbruch und gemeinsames
  Fünf-Minuten-Limit. Der Directory-Claim ist einmalig und hat nur Index 0 zum
  Ziel; der Modellcode lädt selbst keine Datei. Die bisherigen Größen- und
  FIT-Validierungslimits bleiben für einen späteren Adapter verbindlich.

## Nächste Schritte vor einem kombinierten Gerätelauf

1. Die 5030- und 5007-Payload-Ergebnisse bis zum GFDI-Responder erneut statisch
   verfolgen; unbekannte Formen nicht durch toleranteren Parser übergehen.
2. Falls die statische Quelle die CIRQA-Form nicht klärt: zuerst einen separaten
   **5030-only**-Diagnosetest bauen, mit neuem persistentem Einmal-Gate und
   App-Bestätigung; Stop vor 5007/5002, höchstens äußeren Status, Payloadlänge
   und ein Ergebnisbyte melden. Keine Rohframes/Identifier/Zeiten ausgeben.
3. Sessionisolierten ML/COBS-Adapter mit Frame-/Größenlimits und Write-/ACK-
   Korrelation prüfen. Alte Sitzungs-Callbacks dürfen nichts freigeben.
4. Vor Installation `origin/main`, DB-Schemaversion und Signatur-/Versionsfolge
   vergleichen. Die bekannte Startlogik zum Entfernen der alten JSONL-Datei
   muss bei weiterer Gerätearbeit berücksichtigt werden; keine Behauptung,
   sämtliche App-Starts seien nebenwirkungsfrei.

## Validierung

Lokal erfolgreich: Unit-Suite, Debug-APK, Lint und Instrumentation-APK-Build
(`:app:testDebugUnitTest :app:assembleDebug :app:lintDebug
:app:assembleDebugAndroidTest`); Python-HCI-Regression: 8 Tests erfolgreich.
Die Instrumentation-Tests wurden **nicht** auf S25/Emulator ausgeführt.
Neue JVM-Tests verwenden ausschließlich synthetische Frames; sie belegen
Sicherheitsübergänge, nicht CIRQA-Kompatibilität.
