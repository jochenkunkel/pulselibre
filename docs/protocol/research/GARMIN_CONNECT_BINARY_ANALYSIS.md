# Garmin Connect Android – Binary-Analyse zur CIRQA-Interoperabilität

**Stand:** 18.11.2026 · **Zweck:** Technische Befunde aus bereits dokumentierter Garmin-Connect-5.31-Untersuchung bündeln; rechtliche und praktische Leitplanken für künftige Interoperabilitätsforschung. **Keine Garmin-Binary, kein dekompilierter Garmin-Quelltext und keine geheimen oder personenbezogenen Daten werden hier abgelegt.**

## Umfang und Herkunft der Erkenntnisse

Die vorhandenen PulsLibre-Berichte beschreiben die Analyse von Garmin Connect **5.31** und obfuskierten DEX-Klassen. Der ältere [detaillierte Reverse-Engineering-Bericht](GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md) dokumentiert **versionCode 11761**, APK-Bundle-SHA-256 `a7cbd2bdde76d4fb804c2a07cee908adb0f9e4846a05f082e92b01f8dea1e4a5` sowie lokale JADX-/DEX- und `dexdump`-Analysen. **Nicht belegt** sind dort die rechtmäßige Bezugsquelle des konkreten APK-Bundles und eine vollständige, reproduzierbare Werkzeug-/Untersuchungschronik. Die Hashangabe stammt aus dem Projektbericht; sie wurde bei dieser redaktionellen Prüfung nicht gegen eine APK-Datei nachgerechnet. Ein dokumentierter Befund bedeutet nicht, dass alle Codepfade auf dem CIRQA funktionieren.

## Dokumentierte technische Befunde

| Bereich | Aus der bisherigen Projektdokumentation | Aussagegrenze |
| --- | --- | --- |
| Handshake 5030 | DEX-Kette `yb60 → vid/aq20 → h6p`, Antwortverarbeitung `gu2/vns`; äußerer GFDI-Status Frameoffset 6, ereignisspezifisches Ergebnisbyte Offset 7 | Bedeutung aller inneren Ergebniswerte nicht aus DEX allein gesichert; Gerätetests separat |
| FileSync-Reihenfolge | Connect 5.31 verwendet laut Projektrecherche 5030 `HANDSHAKE_COMPLETE` vor 5007 und weiteren Legacy-Dateischritten | Keine pauschale Garantie für alle Garmin-Geräte/Firmwareversionen |
| Bluetooth Classic | `BluetoothClassicAcceptor`/`ph4`; `listenUsingRfcommWithServiceRecord("Garmin SPP", uuid)` mit UUID `DEAB91E4-670F-11E1-9ACE-30BC4824019B` | Der CIRQA-RFCOMM-Gerätepfad ist damit nicht nachgewiesen |
| RFCOMM-Datenstrom | `GfdiDeviceCompat.open(inputStream, outputStream, …)`; GFDI/COBS über Stream | BLE- und RFCOMM-Verhalten können abweichen |
| Protobuf 5043 | `connection_ready_notification`; laut dokumentierter Connect-Analyse Transport-ACK, kein zwingendes 5044 | Nachrichtensemantik nur für beobachtete Form |

**Primärdokumente:** [RFCOMM-Vorbereitung](../experiments/CIRQA_RFCOMM_PREPARATION_2026-11-18.md), [Projektstatus](../../PROJECT_STATUS.md), [aktueller Forschungsstand](CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md). Gerätebeobachtungen stets getrennt von Binary-Inferenz ausweisen. Der aktuelle Forschungsstand dokumentiert inzwischen auch einen ersten RFCOMM-V1-Lauf ohne Gegenverbindung; ältere RFCOMM-Vorbereitungsberichte sind daher datierte Snapshots.

## Rechtlicher Rahmen (Deutschland/EU) – keine individuelle Rechtsberatung

**Reverse Engineering ist nicht pauschal verboten oder pauschal erlaubt.** Die Zulässigkeit hängt von Nutzungsberechtigung, Untersuchungsmethode, Umfang, Zweck, Schutzmaßnahmen und Nutzung/Weitergabe der gewonnenen Informationen ab.

- [§ 69d Abs. 3 UrhG](https://www.gesetze-im-internet.de/urhg/__69d.html): Unter den gesetzlichen Voraussetzungen dürfen zur Programmnutzung Berechtigte dessen Funktionsweise beim rechtmäßigen Laden, Anzeigen, Ablaufen, Übertragen oder Speichern beobachten, untersuchen oder testen, um Ideen und Grundsätze zu ermitteln.
- [§ 69e UrhG](https://www.gesetze-im-internet.de/urhg/__69e.html): Die Vervielfältigung/Übersetzung von Code zur Herstellung der Interoperabilität eines unabhängig geschaffenen Computerprogramms ist **nur unter den dort genannten kumulativen Voraussetzungen** zulässig; insbesondere müssen die Informationen nicht bereits ohne Weiteres zugänglich sein und die Handlungen auf notwendige Programmteile beschränkt bleiben. Die gewonnenen Informationen dürfen nicht zweckfremd verwendet oder unzulässig weitergegeben werden.
- [§ 95a UrhG](https://www.gesetze-im-internet.de/urhg/__95a.html): Technische Schutzmaßnahmen können zusätzliche rechtliche Grenzen setzen; deren Umgehung ist gesondert zu beurteilen. Weitere einschlägige Rechte, Vertragsbedingungen und Datenschutzfragen sind im konkreten Fall zu prüfen.

**Projektpraxis:**

1. Interoperabilitätsziel vor einer Analyse festhalten: selbstständige PulsLibre-Implementierung für autorisierten lokalen CIRQA-Datenzugriff, kein Klonen oder Weiterverbreiten von Garmin Connect.
2. Zuerst öffentliche Spezifikationen und eigene Protokollbeobachtungen nutzen. Falls Dekompilierung erforderlich erscheint: Notwendigkeit, Umfang, untersuchte Programmteile und Verfügbarkeit der Information dokumentieren.
3. Ausschließlich eigene Protokollbeschreibungen, unabhängige Kotlin-Implementierungen und nichtpersonenbezogene synthetische Tests veröffentlichen. Keine Garmin-APK, DEX-Dumps, kopierten dekompilierten Programmteile, Zugangsdaten, Geräteschlüssel oder privaten Gesundheitsdaten committen.
4. Herkunft, Version, Zeitpunkt und zulässige Bezugsquelle der untersuchten App intern nachvollziehbar halten; keine vertraulichen oder personenbezogenen Daten in Git veröffentlichen. Geschützte technische Maßnahmen nicht ohne gesonderte rechtliche Prüfung umgehen.
5. Erkenntnisse strikt als **Binary-Befund**, **externe Referenz**, **Offline-Test** oder **am eigenen CIRQA beobachtet** kennzeichnen. Unbekanntes offenlassen; nicht aus einem Klassen-/Methodennamen auf erfolgreich getesteten Datentransfer schließen.
6. Bei konkreten Zweifeln an Zulässigkeit oder Veröffentlichung vor weiteren Schritten qualifizierten Rechtsrat einholen. Diese Richtlinie ist eine Risikobegrenzung, keine nachträgliche Feststellung der Rechtmäßigkeit früherer Handlungen.

## Offene Dokumentationsaufgaben

- Bereits dokumentierte Version 5.31 / versionCode 11761, SHA-256 und JADX-/DEX-Hinweise aus dem [detaillierten Bericht](GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md) übernehmen; konkrete Bezugsquelle und vollständige Reproduktionsschritte **soweit rechtmäßig und tatsächlich belegbar** nachtragen.
- Für Handshake, 5002/5004 und RFCOMM die jeweils notwendigen Methodenaufrufe als **eigene** abstrakte Ablaufdiagramme darstellen; keine dekompilierten Textblöcke.
- Vor jeder Codeübernahme Lizenz und Herkunft der externen Open-Source-Referenz getrennt prüfen.
- Hardware-Evidenz und tatsächlichen `main`-Quellstand nach jedem neuen Gerätetest mit dem [kanonischen Forschungsstand](CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md) abgleichen.
