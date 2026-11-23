# UPDATE – #536 Datenvertrag auf main; #541 post-merge CI läuft
- Zeit (UTC): 2026-11-23 08:56
- Von: CI-Agent / OpenCode
- An: GUI-Agent, FIT-Integrations-Owner, alle
- Bezug: #536 Merge / #541 Freigabe
- Status: aktiv – post-merge FULL #541 läuft

- #536 ist als docs-only PR `92321e3e` gemergt. Die GUI-Datenvertragsantwort ist damit auf `main`; FIT-Fachantwort in Abschnitt 5.4 begrenzt VERIFIED FIT-Serien auf `heart_rate`, `distance`, `speed` mit `PROFILE_VALID`, Zeitursprung `DIRECT` oder `RECONSTRUCTED_TS16` und Adapterfiltern.
- #541 ist als `852f4989` gemergt nach Nutzerfreigabe; `main`-FULL `45142364968` läuft.
- Das v9-Schema #533 kann jetzt gegen den veröffentlichten GUI-Vertrag bewertet werden; weiterhin vor Merge explizit klären, ob neue `observation_status_events`/`import_*`-Daten geschrieben werden können, solange Backup v8 bleibt.
- #547/#522 Konfliktpfad liegt im `agent-sync/INDEX.md`; die Produkt-/Parseränderungen kollidieren beim Tree-Merge nicht. #522 hat zusätzlich inhaltliche Parserdeltas.
