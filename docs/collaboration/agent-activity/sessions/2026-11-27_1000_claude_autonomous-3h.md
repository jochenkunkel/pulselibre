# Claude Code – autonomes 3-h-Fenster (27.11.2026, ca. 09:45–12:45 UTC)

- **Agent/Umgebung:** Claude Code, einziger ausführender Agent. Haupt-IDE; S10 per Lease; CIRQA-Armband gesperrt, S10-Bluetooth auf `BLE_ON`.
- **Auftrag:** Owner-Auftrag „3 Stunden autonom“. P0: #744 abschließen, Migration und Reimport am S10 prüfen, Tageshistorie in SQLite nachweisen. P1: Dashboard, Backup/Konfiguration, weitere Aufgaben.

## Ergebnisse

**#744 (Schema 12, monitoring_b-Import)**
- Der erste FULL-Lauf scheiterte: 10 Migrationstests erwarteten user_version 11, und ein INSERT…SELECT kompilierte gegen den Fremdschlüssel einer v8-Datenbank nicht.
- Beides ist behoben; der zweite FULL-Lauf war grün, danach wurde gemergt (`888ccc92`).

**S10-Gerätenachweis**
- Es wurden ausschließlich In-place-Updates installiert.
- Vor jedem Datenbankwechsel lag ein geprüftes Backup vor (Manifest-SHA 24/24), gesichert unter `04_src/backup/s10-2026-11-27/`.
- Build 2515 (#744):
  - Stress 0 → 755, SpO₂ 0 → 791, Atmung 0 → 787, Ganztagespuls +749. Die Werte sind identisch zur SDK-geprüften Offline-Erwartung.
  - Keine Regression bei HRV, Schlaf und Haut.
  - 0 neue Importfehler, Provenienz vollständig, keine Duplikate.
  - Der CSV-Export ist identisch.
- Build 2525 (#745–#748):
  - Aktivitätszusammenfassungen (Decoder 8) sind sichtbar.
  - Die Analysen-Texte sind korrigiert.
  - Die Wochenkarte zeigt Ø Stress aus dem Band.

**PRs**
- Gemergt:
  - #744: monitoring_b-Import
  - #745: Analysen-Texte
  - #746: Grund eines gescheiterten Commits im Sync-Bericht
  - #747: Decoder 8 mit Aktivitätszusammenfassung
  - #748: Wochenkarte Ø Stress
  - #749: PROJECT_STATUS
- Offen:
  - #750: Erfassungskarte zählt kanonisierte Kandidaten nicht mehr als „nicht gezählt“; FULL läuft.

**Konfigurations-Backup:** Die Bestandsaufnahme ist ohne Lücke.
- Gesichert sind 5 Nutzer-Stores (Profil inklusive SpO₂-Grenzen und Schrittziel, Design und Sprache, Aktivitätserkennung, Aufzeichnung, Dashboard).
- Technische Stores werden bewusst nicht gesichert: Sync-Policy, Recorder-Zustand, Diagnose, Berechtigungsflag, Protokoll-Gates und das Pull-Manifest. Ein Skip setzt zusätzlich das verifizierte lokale Original voraus.

**#329:** Kalorien und Intensitätsminuten bleiben ohne Tageswert. Die Zähler sind je Aktivitätstyp kumulativ, der Tageswechsel ist nicht beobachtet; die Regel ist im Issue dokumentiert.

## Grenzen und Offenes
- Kein Bandkontakt (Owner-Vorgabe).
- `integrity_check` direkt am Gerät ist nicht möglich: Das Release-Build erlaubt kein `run-as`.
- Ältere Agenten-Doku-PRs (#653, #658, #667, #669, #697, #723, #738) sind teils konfliktbehaftet und nicht bearbeitet.
