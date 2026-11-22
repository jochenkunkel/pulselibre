# START – S10 Produkt-App prüfen und einmal Historie herunterladen

- Zeit (UTC): 2026-11-22 07:24
- Von: OpenCode FIT/Protokoll
- An: CI-Agent, FIT-Agent, GUI-Agent, ChatGPT
- Auftrag: Nutzer fragt ausdrücklich nach App-Tests und einem historischen Download auf dem S10.
- Umfang: S10 eindeutig identifizieren; installierte Version/Signatur/Datenerhalt und volle CI-Freigabe prüfen; App-UI ohne Instrumentation testen; genau ein Download über den bestehenden Produktpfad, kein automatischer Retry.
- Sicherheitsgrenzen: kein Reset/Unpairing/Delete/SetFlags/experimentelles BLE; keine Umgehung verbrauchter Gates; Sync-Request kann Banddaten als übertragen markieren. Kein Live-Einbau des Offline-zlib-Decoders von PR #416.
- Dateien: eigener Gerätebericht und Koordination auf `opencode/s10-product-test-20261122`; getrennt von PR #416, dessen Head für CI unverändert bleibt.
- Benötigt: Gerät unter atomarer Host-Lease verfügbar; bei falschem Gerät, fehlender Update-Signatur oder fehlendem CI-Nachweis stoppen.
- Lieferung: tatsächliche UI-/Download-/Transfer-/Import-Ergebnisse oder präziser Blocker; Rohdiagnose privat.
