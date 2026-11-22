# Temporäre Forschungsdaten-Freigabe für das aktuelle CIRQA-Armband

**Projektentscheidung vom 21.11.2026, bis zum ausdrücklichen Widerruf bzw. zur Umstellung auf reale personenbezogene Betriebsdaten.**

Der Projektverantwortliche erklärt, dass **die derzeit auf dem Testarmband befindlichen Daten keiner Person zugeordnet werden können**. Er erlaubt ausdrücklich **allen beauftragten PulsLibre-Agenten (Codex, Claude Code, OpenCode, ChatGPT und weiteren)**, diese **aktuellen Armband-Rohdaten** für die Entwicklung und Validierung zu nutzen, statt ausschließlich redigierte Summen zu verwenden.

## Was ohne erneute Einzelfreigabe erlaubt und erwünscht ist

- Originale FIT-Dateien, BLE-/GATT-/HCI-Traces, Roh-Payloads, unbekannte Felder, Metadaten und dazugehörige SQLite-Daten **lokal lesen, auswerten, zwischen den autorisierten Entwicklungsagenten teilen und für Decoder-, Sync-, GUI- und Regressionstests verwenden**.
- Nachvollziehbare Rohdatenbeispiele und reproduzierbare Testfixtures **in einem zugriffsbeschränkten Entwicklungsbereich** ablegen, sofern sie nach Prüfung tatsächlich nur Daten dieses Testarmbands enthalten. Für GitHub-Issues/PRs vorzugsweise kleine, zweckgebundene Ausschnitte und aggregierte Befunde.
- Die bereits erteilte Freigabe umfasst die **Datennutzung**, nicht automatisch neue aktive BLE-Kommandos, Reset, Löschung, Firmwareänderung, Entkoppeln oder andere riskante Geräteaktionen.

## Grenzen und Umschaltregel

- Die Aussage „nicht personenbezogen“ ist **die aktuelle Erklärung des Projektverantwortlichen**, keine unabhängig verifizierte rechtliche Einstufung. Auch Gerätekennungen, präzise Zeitreihen, Kopplungsinformationen oder Metadaten können je nach Kontext eine Wiederzuordnung ermöglichen. **Vor öffentlicher Veröffentlichung** (insbesondere in einem öffentlichen GitHub-Repository, Issue, PR, CI-Artefakt oder externen Dienst) daher prüfen, ob der konkrete Datensatz tatsächlich keine Identifizierung oder Rückverknüpfung ermöglicht. Keine Zugangsdaten, Tokens, Schlüssel, fremde Daten oder identifizierende Gerätekennungen veröffentlichen.
- **Die Freigabe hebt die bestehende Gitignore-Regel für FIT-/DB-/Trace-Dateien nicht pauschal auf.** Lokale Nutzung und agenteninterner Austausch sind erlaubt und erwünscht; für eine öffentliche Rohdatenablage ist eine gesonderte Prüfung und bewusste Entscheidung nötig.
- **Sobald echte personenbezogene Daten eingeführt werden oder der Projektverantwortliche dies ausdrücklich ankündigt**, diese Sonderregel nicht mehr auf die neuen Daten anwenden. Dann Datenschutz-/Freigabekonzept vor Verarbeitung, Export oder Veröffentlichung aktualisieren. Bei Unklarheit über die Herkunft eines Datensatzes konservativ behandeln und Rückfrage nur für die konkrete Risikofrage stellen.
- Vor Nutzung des physischen S25 gilt weiterhin die exklusive Gerätesperre; Geräte-/BLE-Sicherheitsgrenzen bleiben bestehen.

**Vorrang:** Diese aktuelle Entscheidung ersetzt ältere pauschale Verbote, die **rein lokale Untersuchung und agenteninternen Austausch der aktuellen Armband-Rohdaten** verhinderten. Sie ersetzt **nicht** Schutzmaßnahmen gegen öffentliche Identifizierbarkeit, Geheimnisabfluss oder riskante Hardwarezugriffe.

## Ergänzung 22.11.2026 – synthetische Daten und reproduzierbare Git-Veröffentlichung

Der Projektverantwortliche stellt ausdrücklich klar: Das aktuell untersuchte Testarmband wird **nicht an einem Menschen getragen**; die Testmessungen sind nach seiner Erklärung **synthetisch**. Er erlaubt und wünscht, dass alle beauftragten Agenten diese Daten so weit wie möglich auswerten und **synthetische, geprüfte Forschungs- und Regressionstestdaten in Git bereitstellen**, um die FIT-Felder gemeinschaftlich entschlüsseln zu können. Diese Freigabe gilt auch für konkrete Rohwerte und Zeitreihen, soweit die **jeweilige veröffentlichte Datei** die folgenden Prüfungen besteht.

**Verbindlicher Veröffentlichungsablauf:**
1. Herkunft pro Datei dokumentieren: `synthetic`, `generated-fixture` oder `source-unverified`. Keine stillschweigende Gleichsetzung von „Testarmband“ und „garantiert frei von Identifikatoren“.
2. Vor öffentlichem Git-Push auf Geräte-/Bluetooth-Adressen, Seriennummern, UUID-/Pairing-Geheimnisse, Tokens, Schlüssel, persönliche Profile, fremde Datensätze und versehentlich enthaltene Kennungen prüfen; nötigenfalls ersetzen, synthetisieren oder auslassen. Bei unklarer Herkunft **nur intern** teilen und zuerst klären.
3. Für freigegebene Datensätze kleine bis vollständige FIT-Dateien, anonymisierte SQLite-/CSV-Beispiele, Feldmatrizen, Decoder-Golden-Fixtures und deterministische Testfälle **bewusst versionieren**; `.gitignore`-Ausnahmen gezielt und nur für überprüfte Fixtures verwenden, nicht global alle FIT-/DB-/Trace-Dateien freischalten.
4. Ein kurzes Manifest je Datensatz beilegen: Ursprung und Synthetik-Erklärung, Prüfverfahren, Format/Profilversion, SHA-256 der publizierten Datei, erwartete Decoder-/Importresultate und Lizenz-/Nutzungsgrenze, soweit bekannt.
5. Roh-HCI-/BLE-Traces können Authentifizierungs-/Kopplungsmaterial enthalten; vor einer öffentlichen Veröffentlichung separat bereinigen und prüfen, auch wenn die enthaltenen Fitnesswerte synthetisch sind.
6. Veröffentlichte Testfixtures sind **keine** Evidenz, dass alle Felder im Produktivdecoder korrekt importiert werden oder die gesamte Gerätehistorie vollständig übertragen wurde. Geräte-Sicherheitsgrenzen und exklusive Gerätesperre gelten unverändert.

Diese Ergänzung erweitert die bisherige bevorzugt interne Nutzung **ausdrücklich um geprüfte öffentliche Git-Testfixtures**. Sie ist keine pauschale Freigabe für ungeprüfte Rohdateien oder Secrets. Agenten sollen die Freigabe aktiv nutzen und nicht aus einem überholten pauschalen Rohdatenverbot ableiten, dass synthetische Fixtures unzulässig seien.

