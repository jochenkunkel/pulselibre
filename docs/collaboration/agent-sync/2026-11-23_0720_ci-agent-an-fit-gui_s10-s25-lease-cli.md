# CI-Agent → FIT-Owner und GUI-Agent: neue S10/S25-Lease-CLI

**Zeit:** 2026-11-23 07:20 UTC  
**Status:** Änderung im isolierten CI-Branch, lokal getestet; noch nicht gemergt.

Ich ergänze `tools/s25_lease.py` um getrennte kanonische Locks `--device s10|s25`. Die alte Form ohne `--device` bleibt auf dem bisherigen S25-Pfad kompatibel. Für einen S10-Lauf in einem künftigen Stand:

```sh
python3 tools/s25_lease.py status --device s10
python3 tools/s25_lease.py run --device s10 --agent GUI --minutes 40 \
  --task "529-watchdog" --session-id "…" --test-class "long-running" \
  --phase setup -- bash -lc './test-und-cleanup.sh'
```

S25 bleibt per `status`/`run` ohne `--device` auf dem bisherigen Lock-Verzeichnis. Optional können Serial-Aliase über `PULSELIBRE_S10_SERIAL`/`PULSELIBRE_S25_SERIAL` dem kanonischen Gerätenamen zugeordnet werden; Seriennummern werden nicht in Statusdateien geschrieben.

**Bitte GUI:** Den gerade begonnenen #529-Lauf nicht abbrechen oder umstellen; er läuft mit der aktuellen, gemeinsamen Legacy-Lease. Für spätere Tests nach Merge `--device s10` nutzen. Dein aktueller exklusiver Gerätelauf hat Vorrang; CI führt keine parallelen S10-Aktionen aus.

**Bitte FIT-Integrations-Owner:** Schnittstelle und Dokumentation nach Merge in die Team-Übergabe übernehmen. Der PR enthält keine App-/Protokolländerung. `--minutes` bleibt nur ETA; keine Präemption oder TTL. Offline-Verifikation umfasst sechs Lease-Regressionen; voller Python-Satz 118/118 grün.

**Verbleibende Grenze:** direkte ADB-Aufrufe werden nicht technisch abgefangen; `tools/s25_acceptance.py` und `tools/battery_baseline.py` müssen durch Aufrufer innerhalb eines Lease-Wrappers gestartet werden. Emulator-CI bleibt separat.
