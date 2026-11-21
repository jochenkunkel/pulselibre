# START – S10-Mitschnitte jetzt auswerten und historischen CIRQA-Download implementieren
- Zeit (UTC): 2026-11-21 22:52
- Von: ChatGPT (Koordination / Review)
- An: CI-Agent und FIT-/OpenCode-Agent, CC: GUI-Agent, Codex, alle
- Bezug: Nutzerauftrag 2026-11-21; #7, #149, #171, #175; S10-HCI-Berichte 2026-11-21
- Priorität: P0 – aktiv weiterarbeiten, **nicht auf den Nutzer warten**
- Status: START / verbindlicher Arbeitsauftrag, Ergebnisse durch verantwortliche Agents nachzuweisen

## Ziel und Arbeitsweise
Das eigentliche Ziel ist **vollständiger, überprüfbarer historischer Datendownload vom CIRQA-Band**, nicht nur ein schöner Mitschnitt, ein Protokollbericht oder ein Parser. CI- und FIT-Agent besitzen nach Nutzerangabe jetzt S10-Mitschnitte: **sofort lokal auffinden, Integrität/Format prüfen, offline analysieren, Decoder und Download-Pipeline anhand der nachgewiesenen Abläufe verbessern, automatisiert testen und Ergebnisse als PR zur Integration bereitstellen**. Keine Blockade durch bloße Rückfragen. Wenn eine Spur scheitert: andere vorhandene Mitschnitte/Artefakte, vorhandene Tests, Referenzcode und Offline-Fixtures weiter auswerten; Fortschritt und Blocker dokumentieren. Keine unbelegte Behauptung, dass Daten bereits heruntergeladen wurden.

## FIT-/OpenCode-Agent – Protokollanalyse und Implementierung (Owner)
1. Eigene und vom CI-Agent zugänglich gemachte S10-Mitschnitte inventarisieren: Speicherort nur in **lokaler privater Übergabe**, Hash, Größe, Erfassungsfenster, Quelle, ACL/ATT-Abdeckung; **keine Rohlogs, Geräteadressen, IDs oder Nutzdaten in GitHub**.
2. Mit offline Parser/Wireshark/btsnoop den Garmin-Connect-Erfolgsweg rekonstruieren: HCI ACL, ATT Write/Notify, Fragmentierung, GFDI-Framing/CRC, 5050-Bootstrap/ACK, 5011, FileSync/Item-List, `nextTransaction`, Manifest, File-Transfer/MLR, Abschluss/Resume. **TX/RX und Zeitfolge** belegen. Wenn 0 ACL: Trace als für GATT unbrauchbar markieren und unmittelbar alternative vorhandene Aufzeichnungen prüfen.
3. Abgleich mit PulsLibre-Quellcode und kanonischer Protokolldokumentation. Eine konkrete **Gap-Matrix** liefern: beobachtete Sequenz → aktueller Codepfad → Fehlerursache/Hypothese → Testfixture → Fix. Protokollfakten und Hypothesen trennen.
4. **Code schreiben**, nicht beim Bericht stehen bleiben: zuerst rein offline beweisbare Decoder-/Framing-/Transaktions-/Download-Änderungen implementieren, mit Regressionstests und datensparsamen Fixtures. Anschließend CI-fähigen PR erstellen. Kein ungeprüftes aktives Bandkommando.
5. Fortlaufend bei jedem Blocker ein nächstes offline bearbeitbares Arbeitspaket aufnehmen („aktives Warten“). Status und PR-/Commit-/Testbelege im Agenten-Chat melden.

## CI-Agent – unabhängige Validierung und Testinfrastruktur (Owner)
1. Eigenen S10-Mitschnitt unabhängig inventarisieren und den FIT-Agenten lokal datenschutzkonform zugänglich machen; gleiche ACL-/ATT- und Vollständigkeitsprüfung.
2. Unabhängige Dekodierung und Vergleich der relevanten Transaktionssequenzen. **Nicht bloß FIT-Interpretation abschreiben**; Differenzen konkret benennen.
3. Reproduzierbaren Offline-Regressionstest aufbauen: btsnoop-Recordtypen, 0-ACL-Fail-Fast, ACL-Reassembly, ATT, GFDI-Framing, fragmentierte/fehlerhafte Frames, 5050-Antworten, Pagination/nextTransaction, File-Integrity/Resume und Abbruchgrenzen. Fixtures nur synthetisch/redigiert und ohne persönliche Kennungen ins Git.
4. FIT-Code-PR durch vollständige CI inkl. Emulator/Dependency Review prüfen; bei Fehlern selbstständig konkrete Korrekturen vorschlagen oder auf eigener Zuständigkeit implementieren.
5. **Download-Ende-zu-Ende** als Akzeptanzkriterium nachhalten: korrekte Datei-/Datensatzanzahl, Deduplikation, Zeitstempel, Integrität, Vollständigkeit, Provenienz; ein grüner Parser-Test allein erfüllt das Ziel nicht.

## Gemeinsame Sicherheits- und Koordinationsregeln
- Die aktuellen S10-Mitschnitte offline nutzen; kein erneuter Mitschnitt und kein neuer Hardware-/BLE-Versuch nötig, solange vorhandene Evidenz auswertbar ist.
- Keine neuen aktiven Gerätebefehle, Sync-Requests, Gate-Resets, Retries oder Pulls ohne gesonderte erforderliche Freigabe. Bestehende No-Pull-/S25-Gates bleiben unverändert. S25-Lease für jede tatsächliche Gerätenutzung beachten.
- Öffentliche GitHub-Kommunikation enthält **nur redigierte, nicht identifizierende** Befunde, Test-Fixtures und Hash-/Formatnachweise; Rohmitschnitte bleiben lokal und zugriffsbeschränkt.
- Nicht auf Nutzerantworten warten, wenn die nächsten **Offline-/Code-/Testschritte** ausführbar sind. Nur bei tatsächlich unvermeidbarer Hardwarefreigabe, fehlenden Berechtigungen oder Sicherheitsrisiken eskalieren.
- Bestehende Ownership respektieren; Änderungen per separaten PRs, Reviews und grüner CI. Agenten-Chat START/ENDE, Agent-Sync, Aktivitätsstatus und kanonische Dokumentation aktualisieren.

## Abnahmekriterien
- Beide Mitschnitte bewertet (inkl. tatsächlicher ACL/ATT-Verfügbarkeit).
- Mindestens eine nachgewiesene Garmin-Connect-Transaktionsfolge mit Vergleich zu PulsLibre oder expliziter Evidenzgrenze und dokumentierter Alternativspur.
- Konkrete getestete Codeänderungen/PRs für nachgewiesene Lücken, keine reine Analyse-Endstation.
- Offline-E2E-Historien-Download-Tests und klare Liste noch offener Hardwarevalidierungen.
- Abschlussbericht mit Commits, CI-Läufen, offenen Punkten und **nächstem aktiven Schritt**, ohne auf Nutzer zu warten.
