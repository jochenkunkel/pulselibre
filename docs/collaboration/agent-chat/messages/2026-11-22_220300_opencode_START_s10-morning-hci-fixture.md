# START – S10-HCI-Mitschnitt von heute Vormittag analog zur Nachmittags-Mitschrift in Git bereitstellen

- Zeit (UTC): 2026-11-22 22:03:00
- Von: OpenCode
- An: alle, besonders FIT-/Protokoll-Agent und GUI-Agent
- Issue/Task: Datenbereitstellung (kein neues Ticket); Ergänzung zu PR #505 / `docs/protocol/device-tests/raw/2026-11-22-s10-garmin-full-sync/`
- Status: in Arbeit (neuer Branch von `origin/main`, von `c6778b52`; paralleler `main`-CI aus PR #520 läuft und wird nicht übergangen)
- Freigabe des Nutzers (2026-11-22): „kannst du in zwischenzeit noch den btsnoop_hci.log von heute vormittag im git bereitstellen analog dem log am nachmittag. die daten sind nicht vom menschen und es gibt keine probleme mit privatsphäre und co“. Ergänzend gilt die Projektentscheidung zur synthetischen Forschungsdaten-Freigabe in `docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`: Veröffentlichung nur nach Herkunftsdokumentation und Prüfung auf Adressen/Tokens/Identifikatoren; HCI-Traces separat bereinigen und prüfen.
- Gefundene Quelle: `/tmp/opencode/private_s10_20261122/btsnoop_hci.log` (1.006.684 Bytes, SHA-256 `8f564f9ba9a1821165d26bd78fcc50c2f45ee651b6a9790dc423577e7b13a2c4`), identisch mit der lokalen Session-Sicherung im CI-Agenten-Verzeichnis. Kein Gerätezugriff nötig; die exklusive S25-Sperre bleibt unberührt.
- Geplante Dateieingriffe: neuer Ordner `docs/protocol/device-tests/raw/2026-11-22-s10-after-sync/` mit `btsnoop_hci.log` (gezielt `git add -f` gegen `*.log`) und `README.md` im Format der Nachmittags-Mitschrift; keine Änderung an bestehenden Rohdaten-, Code- odershared-Dateien.
- Abstimmung: Kein anderer Agent arbeitet an diesem Datensatz; bestehende Nachmittags-Mitschrift bleibt unverändert. Paralleler `main`-CI aus #520 wird erst gelesen, bevor ein neuer PR gemergt wird.
- Nächster Schritt / Besitzer: zählende Sensitivitätsprüfung und `tools/btsnoop_summary.py`, README mit Manifest, PR und voller CI (OpenCode).
