# Nutzerbeobachtung: Garmin Connect lädt fehlende Tagesdaten nach (21.11.2026)

**An:** OpenCode, Claude Code, CI-Agent und alle CIRQA-Historien-Agenten  
**Quelle:** direkte Beobachtung und Klarstellung des Nutzers im Chat am 21.11.2026. Kein unabhängiger Paketmitschnitt.

## Vom Nutzer berichtete Reihenfolge

1. Garmin Connect war nach Aussage des Nutzers den ganzen Tag deaktiviert und zwischenzeitlich deinstalliert; daher war nach seiner Angabe kein App-Cache vorhanden.
2. Nach dem Start synchronisierte Garmin Connect **zuerst mit der Cloud**. **Alle Daten von heute fehlten danach weiterhin**.
3. Erst bei **aktivem Bluetooth und einer Synchronisation mit dem Armband** wurden die fehlenden Daten in Garmin Connect sichtbar.
4. Diese Armband-Synchronisation dauerte nach Beobachtung des Nutzers **ungefähr eine Minute**. Der Test fand während einer U-Bahn-Fahrt statt.

## Technische Bedeutung – Beobachtung von Interpretation trennen

- **Nutzerbeobachtung:** Die Cloud-Synchronisation allein brachte die fehlenden Tagesdaten nicht; nach der Bluetooth-Armband-Synchronisation waren sie in Garmin Connect vorhanden.
- Dies ist konkrete Evidenz dafür, dass der **Armband-Synchronisationsschritt** die fehlenden Tagesdaten für Garmin Connect verfügbar machte.
- **Nicht behaupten:** Die Daten seien bereits vor dem Bluetooth-Sync in der Cloud oder in einem Garmin-Connect-App-Cache verfügbar gewesen. Solche Erklärungen widersprechen dem berichteten Ablauf und sind nicht belegt.
- Ohne Paketmitschnitt ist der interne Transport-/Speichermechanismus von Garmin Connect nicht vollständig technisch nachgewiesen. Das relativiert **nicht** die klare beobachtete Reihenfolge.
- PulseLibre meldete zuvor weiterhin `device-history-unverified`; aus dem Garmin-Connect-Erfolg folgt nicht automatisch, dass PulseLibre dieselben Daten bereits lädt.

## An alle Agenten

Bitte diese korrigierte Nutzerbeobachtung bei #149, #148, ItemList-/nextTransaction-Analyse und Historienlücken berücksichtigen. Die frühere pauschale Spekulation „Armband, Cache oder Cloud“ **ist durch die Nutzerklarstellung für diesen Test überholt**. Keine neuen BLE-Geräteversuche oder Produktänderungen ohne gesonderte Freigabe; ChatGPT bleibt beim Auftrag Repository-Hygiene.

**Status:** Korrigierte Nutzerbeobachtung dokumentiert; keine zusätzlichen Geräteaktionen.
