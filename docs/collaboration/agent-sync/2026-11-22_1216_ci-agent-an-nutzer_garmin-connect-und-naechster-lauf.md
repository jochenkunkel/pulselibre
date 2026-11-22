# Garmin Connect für einen nächsten Lauf: präzise Freigabe nötig

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:16 UTC

## Was passiert ist

Der freigegebene erste Download lief von 12:09:56 UTC (Nutzermeldung) bis zur 5-Minuten-Frist 12:14:56 UTC. Die UI blieb ohne lesbaren Statustext (`uiautomator dump`: `could not get idle state`). Um 12:15:34 UTC wurde die App mit `am force-stop de.pulselibre` beendet. Kein Retry. Kein DB-Import und kein Downloadabschluss nachgewiesen. Keine BLE-/GATT-/FileSync-Kommandos von mir, kein Reset, keine Entkopplung, keine Löschung. Ein zweiter SAF-Export zum Vergleich steht noch aus.

## Nutzeranfrage 12:13 UTC

Der Nutzer fragte: „kannst du evt garmin connect beim nächsten lauf deaktivieren?"

Befund (offline/lesend): PulsLibre hat **keinen** In-App-Schalter für Garmin Connect, nur einen Hinweistext („Garmin Connect kann die Bluetooth-Verbindung beeinflussen"). Auf dem Gerät ist `com.garmin.android.apps.connectmobile` installiert. Ich habe **nichts** deaktiviert.

## Warum eine neue, präzise Freigabe nötig ist

„Deaktivieren" kann Folgendes bedeuten – mit sehr unterschiedlicher Wirkung:

1. `am force-stop com.garmin.android.apps.connectmobile` – temporär, beim nächsten Start der App wieder aktiv; geringstes Risiko.
2. `pm disable-user --user 0 com.garmin.android.apps.connectmobile` – dauerhaft deaktiviert, muss aktiv wieder aktiviert werden; systemweiter Eingriff in eine fremde App.
3. Entkopplung/`CLOSE_ALL`/Reset am Band – nach AGENTS.md ausdrücklich **nicht** ohne gesonderte Freigabe und Stop-Gates; ich schlage das nicht vor.

Bitte konkret wählen, falls ein weiterer Lauf gewünscht ist. Wegen des Retry-Verbots im aktuellen Plan wäre das ein **neuer, separat freizugebender Test** mit eigenem Umfang und eigenen Stop-Bedingungen – nicht eine Fortsetzung dieses Laufs.

## Grenzen

`coverage=device-history-unverified` bleibt gültig. Keine Rohdaten, Gerätekennungen oder Gesundheitswerte in GitHub.
