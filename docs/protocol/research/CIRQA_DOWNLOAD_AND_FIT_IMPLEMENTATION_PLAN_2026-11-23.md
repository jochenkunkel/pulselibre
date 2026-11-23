# Umsetzungsplan: CIRQA-Download und vorhandene FIT-Daten

Stand: 2026-11-23. Dieser Plan bereitet Implementierung vor; er startet keinen
Gerätelauf, ändert keine Vertrauensfreigabe und beansprucht keine vollständige
Historie. Die geparkte Architekturarbeit #496 bleibt unberührt.

## Evidenz und Ausgangslage

- 38 vollständig rekonstruierte Garmin-Transfers sind bytegleich im privaten
  PulsLibre-Archiv; 34 davon sind FIT und wurden unabhängig mit Garmin FIT SDK
  21.222.0 überprüft. Der 39. Pull hatte Erfolg, sein Transfer ist im Mitschnitt
  nicht als vollständig belegt.
- Unser voriger S10-Lauf endete bei 38/39 am 120-s-Limit. Die doppelte
  FIT-Dekodierung ist als Engpass im Code nachgewiesen, aber nicht als einzige
  Ursache des nicht abgeschlossenen Pulls.
- [PR #591](https://github.com/jochenkunkel/pulselibre/pull/591) entfernt den
  zweiten Decode. Auf Head `3a94ac1f` sind Android-Build/JVM/Lint/APK,
  Dependency Review und API-35-Emulatorlauf grün. Der PR ist Draft;
  unabhängiges Review und Geräteabnahme sind nicht belegt.
- Atem- und SpO₂-Kandidaten stimmen bei den verglichenen FIT-Dateien mit dem
  SDK überein, sind aber review-only. Stresszeiten gehen derzeit verloren;
  historische Schlafphasen und Monitoring-Schritte werden nicht abgebildet.
  Details, UTC-Abgrenzung und Grenzen:
  [Capture-Vergleich](CIRQA_CONNECT_S10_CAPTURE_COMPARISON_2026-11-23.md).

## Paket A — Download zuverlässig abschließen

### A1. Bereits entwickelten Doppeldecode-Fix abschließen

1. PR #591 unabhängig auf Decode-/Bytes-/Provenienzbindung, Speichergrenzen,
   Fehlerbehandlung und Regression prüfen. Insbesondere prüfen, dass der neue
   Importpfad ausschließlich das tatsächlich für die geprüften Bytes erzeugte
   Decode-Ergebnis verwendet und keine beliebige veränderbare Fremdausgabe
   als bewiesen ausgibt. Nach Änderungen auf neuem Head erneut volle CI.
2. CI-Ergebnisse für tatsächlichen Head auslesen; erst nach Review und vollem
   grünem Lauf die projektüblichen Merge- und main-CI-Gates einhalten.
3. Kein Versprechen, dass die eingesparte Dekodierung allein das 39. Item oder
   vollständige Historie löst.

**Betroffene Pfade:** `CirqaFitBackfill.kt`, `CirqaMlHandshakeProbe.kt`,
`CirqaFitBackfillTest.kt`; bestehender PR #591.

### A2. Offline-Fehlerfälle und Laufabschluss

- Mit **synthetischen** Protokollfixtures ein Replay der belegten Form bauen:
  erfolgreiche Liste mit 39 Items, 39 erfolgreiche Pullantworten, 38
  konfigurierte und vollständig verifizierte Transfers, ein fehlender
  Transfer. Keine UID, kein Token, kein roher Mitschnitt im Test.
- Tests müssen unterscheiden: Pull-Antwort erfolgreich ≠ Transfer vollständig;
  Transfer ≠ CRC/Archiv ≠ Decode ≠ SQLite-Commit. Fehlende Bytes, Abbruch,
  doppelte/verspätete Callbacks, falsche Größe/CRC, Timeout während Import und
  gültige Fortsetzung jeweils fail-closed und idempotent prüfen.
- Die Hypothese prüfen, ob lokale Arbeit die serielle BLE-Verarbeitung oder
  Deadline blockiert. Falls nach Fix weiterhin erheblich, getrennte,
  begrenzte Verarbeitung nur mit explizitem Ownership-/Cancellation-Konzept:
  zuerst bytegenau verifizieren und dauerhaft archivieren; danach offline
  importieren. Keine unkontrollierte Queue oder verfrühten Erfolgsstatus.
- `runDrained` nur bei tatsächlich abgearbeiteter begrenzter Queue melden;
  `coverage=device-history-unverified` bleibt erhalten. Kein neuer
  `CLOSE_ALL`-, Lösch-, Flag-, Reset- oder anderer mutierender Gerätebefehl.
- Aktuelle 120-s-, Datei-, Byte- und Seitenlimits und bestehende
  BLE-Anfragesequenz bleiben für diesen Fix unverändert. Eine spätere
  Budget-/Sequenzänderung benötigt eine gesonderte begründete Planung,
  Tests, Review und die geltende Freigabe; nicht aus diesem Plan ableiten.

**Betroffene Kandidaten:** `CirqaMlHandshakeProbe.kt`,
`CirqaSyncPipelineLedger.kt`, `CirqaSyncTerminalEvidence.kt`,
`CirqaItemListContinuationPolicy.kt`, ihre Tests. Nur nach spezifischem Befund
ändern; unterschiedliche Item-List-Cursor/Transaction-Semantik nicht aus
einer einzigen Aufnahme verallgemeinern.

### A3. Spätere S10-Abnahme

Nach vollem grünem CI, Review und projektkonformer APK-/Signaturprüfung einen
separaten, begrenzten Versuch planen: Gerät eindeutig zuordnen, vorab Backup
und S10-Lease-Status prüfen, gesamten Zugriff im Lease-Wrapper halten,
bestehende Sequenz und Limits beibehalten. Stop bei Integritätsfehler,
unerwartetem Gerätezustand, fehlender Exklusivität oder bestehendem Stop-Gate.
Vorher/nachher nur lokal-private Daten prüfen; keine Rohdaten veröffentlichen.
**Messgrößen:** angebotene/angeforderte/empfangene/verifizierte/archivierte
Items getrennt, Zeit je Stufe, Grund des Abbruchs und ggf. Queue-Rest.
Keine Aussage über dieselbe 39. Datei allein aufgrund identischer Größe.

**Abnahme A:** vollständige, reproduzierbare Offline-Tests und volles CI;
Gerätetest getrennt dokumentiert. Vollständige Historie nur nach eigenständigem
Abdeckungsnachweis, nicht durch ein leeres Page-Ergebnis behaupten.

## Paket B — vorhandene FIT-Daten nutzbar machen

### B1. Vertrag und SDK-Golden-Tests vor Produktänderung

- Kleine eigens generierte FIT-Fixtures pro Typ mit verifizierbarer Herkunft,
  CRC und erwarteten Ergebnissen erstellen; Tests vergleichen gegen das
  dokumentierte offizielle FIT-Profil/SDK, ohne Garmin-SDK-Code zu übernehmen.
- Je Nachricht Feldnummer, Basistyp, Skalierung, Einheit, Sentinel,
  Zeitstempelherkunft, zulässige Werte und Version explizit dokumentieren.
  Tests für ungültige Werte, fehlende Zeit, Endianness, Kompression/Time16,
  doppelte Einträge und unbekannte Felder.
- Atem/SpO₂: Profilwert und Zeit sind auf den untersuchten Dateien
  unabhängig bestätigt; Modus/Konfidenz, zulässige Anzeige und Qualitätsregeln
  separat prüfen. Stress: `stress_level_time` Feld 1 als FIT-`date_time`
  inklusive Sentinel/Überlauf testen; Status-/Negativwerte nicht als normalen
  Stresswert interpretieren.
- Schlaf: `sleep_level`-Ereignis und Zeit abbilden, nicht pauschal Schlafdauer
  oder Vollständigkeit ableiten. Schritte: Monitoring-Typ, Zeit und
  Akkumulations-/Reset-/Duplikatsemantik prüfen; **keine Summation von vier
  Datensätzen als Tagessumme** ohne nachgewiesene Semantik.

**Betroffene Kandidaten:** `FitHistoricalObservations.kt`,
`FitHistoricalObservationsTest.kt`, synthetische Fixtures. Provenienz immer
Quelle, Decoder-/Profilversion, Record-/Feldidentität und Zeitbasis erhalten.

### B2. Sichere Reanalyse und Migration

- Vor jeder Umsetzung das bestehende Generationsmodell prüfen:
  `replayVerifiedFitSource()` erzeugt derzeit nur
  `decoded.observations`; Review-Kandidaten werden dabei **nicht**
  generationiert. Daher reicht das vorhandene Replay für Kandidaten und
  Stresszeit noch nicht aus.
- Decoder-Änderung versionieren. Ein konkretes, aufgabenbezogenes Migrations-
  und Speicherkonzept mit Datenbank-/Backup-Eigentümern vor Schemaänderung
  abstimmen; #496 bleibt geparkt. Keine alte Kandidatenzeile still
  überschreiben: gleiche Sample-ID mit geändertem Zeitstempel kollidiert
  derzeit absichtlich in `FitObservationCommitter`.
- Aus verifizierten Originalen neue Ausgabe **additiv und transaktional**
  erzeugen, alte Generation und aktiven Zeiger erhalten; erst nach Validierung
  atomar umschalten. Auch Kandidaten müssen versioniert bzw. ihre Migration
  ausdrücklich geprüft werden. Rollback, Abbruch und wiederholter Lauf dürfen
  Originale, gültige Altstände oder andere Quellen nicht beschädigen.
- Tests: synthetische Alt-DB mit null-Stresszeit; Upgrade, exakte Quelldaten,
  veränderte Decoderidentität, Reimport, Abbruch/Rollback, konkurrierender
  Zugriff, Backup/Restore, ältere Backups, Speicherfehler und kein
  Duplikat-/Provenienzverlust. Aktivierung nur nach vollständiger Prüfung.

**Betroffene Kandidaten:** `CirqaDatabase.kt`, `FitObservationCommitter.kt`,
`FitDecoderGeneration.kt`, `DatabaseBackup.kt` und zugehörige Unit-/
Emulatortests. Kein konkreter Schemasprung vor Abstimmung und Migrationsentwurf.

### B3. Ausgabe und UI verlässlich kennzeichnen

- Mit GUI-Agent Verträge für Quelle, Datenstand, Einheit, Zeitzone, Qualität,
  fehlende Werte und explizites „ungeprüft“ abstimmen. Kandidaten nicht
  stillschweigend als gesicherte Gesundheitswerte ausgeben.
- `UnifiedMeasurements` liest aktuell aktive Beobachtungsgenerationen, nicht
  `fit_review_candidates`. Ein klar getrennter Kandidaten-/Prüfpfad oder eine
  nachweislich validierte Freigabe ist daher notwendig; keine Verwechslung von
  technischem Decode-Erfolg mit wissenschaftlicher oder medizinischer
  Aussagekraft.
- Tests für lokale Tagesgrenze/Zeitzone und DST, Statuswerte, fehlende
  Zeitstempel, doppelte Quellen, Schlafintervalle und Schritt-Reset; UI- und
  Dashboard-Dateien gehören dem GUI-Agenten und werden vorher abgestimmt.

**Abnahme B:** synthetische Golden-Fixtures und Migrationstests grün; ein
lokaler, datensparsamer Vergleich der vorhandenen Originale mit SDK und
aktiver Datenbank zeigt je Typ Quelle/Zeit/Wert/Qualität und nachvollziehbare
Abweichungen. Wo verfügbar zusätzlich Connect-UI/Export unabhängig vergleichen;
das bestehende Nutzerzeugnis nicht mit einem bereits erfolgten automatischen
Wertevergleich verwechseln.

## Reihenfolge, Übergabe und Grenzen

1. PR #591 reviewen und gemäß vollem CI-Gate abschließen; A2 offline absichern.
2. B1 synthetisch und SDK-basiert absichern; B2-Migration mit Eigentümern
   spezifizieren, dann implementieren und offline/emulatorbasiert testen.
3. B3 mit GUI-Agent koordinieren; A3 separat nach grünem Build und explizit
   dokumentierter Geräteplanung. A und B können nur dann parallel laufen,
   wenn Zuständigkeit und Dateien nicht kollidieren.
4. Jede Änderung über getrennten Branch/PR; volles Android-CI inklusive
   Emulator und Review vor Merge, volles main-CI danach. Keine
   Instrumentation auf dem S25. Kein Hardwarezugriff ohne korrekte Lease.
5. Private Backups, rohe Logs, Screenshots, IDs, Kopplungsdaten und ungeprüfte
   Traces bleiben außerhalb Git; nur geprüfte synthetische Fixtures einchecken.

Dieser Plan ist eine umsetzbare Reihenfolge, keine Behauptung, dass eine
Implementierung, Migration, UI-Freigabe oder vollständige Historie bereits
abgenommen wäre.
