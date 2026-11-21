# Nutzerbeobachtung: Garmin Connect synchronisiert fehlende Historie (21.11.2026)

**An:** OpenCode, Claude Code, CI-Agent und alle CIRQA-Historien-Agenten
**Quelle:** direkte Nutzerbeobachtung im Chat, 21.11.2026; kein unabhängiger Trace.

## Beobachtung
Der Nutzer war unterwegs in der U-Bahn und hat Garmin Connect getestet. **Garmin Connect synchronisierte ungefähr eine Minute lang und zeigte anschließend alle zuvor fehlenden Daten in der App an.**

## Einordnung
- Dies ist ein wichtiger Gegenbefund zur Vermutung, die fehlende Historie sei grundsätzlich nicht verfügbar.
- Nicht bewiesen ist, ob die Daten vom Armband, aus lokalem Cache oder aus der Cloud kamen; auch Zeitraum, Datenarten und Netzwerkweg sind nicht erfasst.
- PulseLibre-Historienabdeckung bleibt unbestätigt (device-history-unverified).
- Keine Änderung an Gerätebefehlen, Daten oder Code aus dieser Beobachtung ableiten.

## Nachricht an alle Agenten
Bitte diese Beobachtung bei den bestehenden Untersuchungen zu #149, #148, Dateilisten und nextTransaction berücksichtigen. Vorhandene Berichte und die tatsächliche PulseLibre-Abdeckung vergleichen, aber keine unbelegten Protokollannahmen treffen. Neue BLE-Gerätetests und Produktentwicklung benötigen weiterhin ausdrückliche Nutzerfreigabe. Der Auftrag des stündlichen ChatGPT-Dienstes bleibt ausschließlich Repository-Hygiene.

**Status:** Nutzerbeobachtung dokumentiert; technische Ursache offen.
