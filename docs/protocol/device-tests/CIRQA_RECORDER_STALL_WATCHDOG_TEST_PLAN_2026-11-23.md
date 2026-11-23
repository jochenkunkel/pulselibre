# Testplan: Stillstands-Wächter der Daueraufzeichnung (#529, #25)

Status: **Freigabe durch den Nutzer am 23.11.2026 (gilt für den Tag); erster Lauf 06:54–07:04 UTC abgebrochen (Fremdinstallation ohne Lease, danach Freigabe des Geräts auf Nutzerwunsch), nach ca. 5 min, ohne Ergebnis. #529 bleibt offen; Wiederholung unter Lease (ETA 40 min) steht aus.** Gerät aktuell FREI.
Gerät: S10 (`RF8M826126Z`) mit dem Band; Build: signierter Build von `main` mit PR #525.

## 1. Was getestet wird
Der Wächter beendet eine offene Verbindung, die 10 Minuten lang keinen Messwert liefert, und baut sie über den vorhandenen, begrenzten Reconnect-Pfad (2/4/8/16/32/60 s) neu auf. Er sendet keine neuen BLE-Kommandos.

## 2. Ablauf (rein über vorhandene App-Funktionen)
1. Vorbedingungen: Band getragen und im Funkbereich, Bluetooth an, Bluetooth-Berechtigung erteilt, Akku ≥ 50 %, kein Garmin Connect im Vordergrund.
2. Reiter „Gerät“: „Starten“ (Daueraufzeichnung). Zeit notieren (T0).
3. 30 Minuten beobachten (Gerätebildschirm nicht sperren oder Akku-Optimierung unverändert lassen; es wird nichts umgestellt).
4. Bei T0 + 30 min: „Stoppen“. Ende.
Es werden **keine** Tests auf dem Gerät ausgeführt, kein Pairing/Unpairing, kein Reset, keine Historie geladen, keine Instrumentationstests.

## 3. Was aufgezeichnet wird und wo es liegt
- Wie im normalen Betrieb: Live-Messwerte (Puls, SpO₂ usw.) und Ereignisse der Aufzeichnung (`attempt_start`, `attempt`, `reconnect_scheduled`, neu: `stall_detected`) in der lokalen SQLite-Datenbank der App (Tabelle `events`). Keine Rohpayloads, keine MAC-/Geräte-IDs (so ist `CirqaLocalLog` gebaut).
- Für die Auswertung: nach dem Test legt der Nutzer über „Daten → Backup erstellen“ ein Backup an **oder** gibt die Freigabe für `adb`-Lesezugriff auf dessen Datei; der Agent zählt nur Ereignisarten und Zeitabstände und schreibt **keine Gesundheitswerte** in Tickets oder Dokumente.
- Ohne Backup: sichtbar bleiben „Sitzungen heute (Starts, Wiederverbindungen)“ und „Letzter Empfang je Messgröße“ im Reiter „Gerät“.

## 4. Dauer
30 Minuten reine Beobachtung (Wächter-Schwelle 10 min plus Reconnect-Wartezeit und Sicherheitsabstand), zuzüglich ca. 10 Minuten Vor- und Nachbereitung.

## 5. Erwartete Auswirkungen
- Eine dauerhafte GATT-Verbindung von PulsLibre zum Band (wie bei der vorhandenen Daueraufzeichnung). Garmin Connect kann das Band währenddessen nicht gleichzeitig verbinden; ein laufendes Garmin-Connect-Sync wird verdrängt. Nach „Stoppen“ ist die Verbindung frei.
- Mehr Bluetooth-Last und Akkuverbrauch am Telefon und Band während der 30 Minuten (Foreground-Benachrichtigung sichtbar).
- Keine Änderung an Kopplung, Einstellungen oder gespeicherten Daten des Bandes.

## 6. Beenden und Abbruch
- Regulär: Reiter „Gerät“ → „Stoppen“ (oder Stopp in der Benachrichtigung).
- Sofortiger Abbruch: Bluetooth ausschalten, oder App aus den Einstellungen beenden. Der Dienst räumt seine Flags auf (siehe `CirqaRecordingServiceLifecycleTest`).
- Abbruchgründe: Absturz, Berechtigungsdialog, Telefon-/Gerätewarnung, Band meldet Fehler, Nutzer wünscht Ende.

## 7. Erfolgskriterien
1. Dienst läuft 30 Minuten durch (Heartbeat im Reiter „Gerät“ aktuell, kein Absturz).
2. **Keine Fehlauslösung:** Solange Messwerte regelmäßig ankommen, kein `stall_detected`.
3. **Bei echtem Stillstand** (Verbindung offen, ≥ 10 min kein Messwert): `stall_detected` mit `silent_ms` ≥ 600000, danach innerhalb von 90 s `reconnect_scheduled` und ein neuer `attempt_start`.
4. Nach „Stoppen“: Dienst beendet, Status „Aufzeichnung aus“, Verbindung frei.
5. Es wurden nur die oben genannten Daten geschrieben.

## 8. Grenzen des Tests
Ein echter Stillstand lässt sich nicht auf Knopfdruck erzeugen (Band weit weg führt zu einer Trennung, nicht zu einem Stillstand). Der Test belegt daher sicher Kriterium 2, 4 und 5; Kriterium 3 nur, wenn in den 30 Minuten ein natürlicher Stillstand auftritt. Andernfalls bleibt der Wächter durch den JVM-Test (`RecorderStallTest`) und die Emulator-Tests belegt, und #529 bleibt für den Dauerbetrieb offen.
