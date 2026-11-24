# ChatGPT → GUI/Projektleitung: fr/es-Sprachprüfung (24.11.2026)

**Gegenstand:** Anfrage zur ausstehenden Muttersprachler-Prüfung; Bezug #365 und A6-43/A6-48. Dies ist ein **KI-gestütztes Sprach- und Terminologie-Review**, **keine Prüfung durch menschliche Muttersprachler**.

## Geprüfte Quellen
- `app/src/main/res/values-fr/strings.xml`, `values-es/strings.xml`, verglichen mit `values-de/strings.xml` und `values/strings.xml`.
- `docs/I18N.md`, insbesondere Glossar und bestehende Freigabegrenzen.
- Agenten-Übergabe `2026-11-22_171500_claude_6_ENDE_a6-43-48-i18n.md`.

## Ergebnis der gezielten A6-43-Prüfung (7 Schlüssel je Sprache)

| Schlüssel | Französisch | Spanisch | Befund |
| --- | --- | --- | --- |
| `main_scan_running` | Recherche en cours (60 secondes) · détection passive … | Buscando (60 segundos) · detección pasiva … | Sinn und Zeitlimit erhalten; fr „Recherche“ und es „Buscando“ verständlich, aber „Recherche Bluetooth“ / „Buscando dispositivos“ ggf. präziser. |
| `main_scan_finished` | Recherche terminée · %1$d appareils | Búsqueda finalizada · %1$d dispositivos | Sinn und numerischer Platzhalter erhalten. |
| `main_scan_failed` | Erreur de recherche : %1$d | Error de búsqueda: %1$d | Inhalt und Platzhalter korrekt; ggf. „Erreur d’analyse Bluetooth“ zur Präzisierung. |
| `main_card_bonded` | Associé à Android | Vinculado con Android | Technisch verständlich; „appairé“ / „emparejado“ sind Bluetooth-spezifischer. |
| `main_card_scan_match` | Même adresse BLE détectée lors de la recherche | Misma dirección BLE detectada en la búsqueda | BLE-Adressvergleich korrekt, keine Behauptung einer aktiven Verbindung. |
| `main_row_match` | ✓ CIRQA : l’adresse BLE correspond à l’association Android | ✓ CIRQA: la dirección BLE coincide con la vinculación de Android | Korrekte, vorsichtige Aussage zur Übereinstimmung mit Android-Kopplung. |
| `main_row_service` | Données de service : %1$s | Datos de servicio: %1$s | Technisch korrekt, Platzhalter erhalten. |

**Fazit:** Keine sinnverändernden Übersetzungsfehler in diesen 14 Texten erkannt. Die vorhandenen A6-43-Formulierungen können als KI-sprachlich geprüft gelten; keine Codeänderung erforderlich. Die übrigen fr/es-Ressourcen sind mit dieser gezielten Prüfung **nicht** vollständig geprüft. Insbesondere medizinische, Datenschutz- und gerätebezogene `REVIEW`-Marker bleiben offen und bedürfen der vorgesehenen inhaltlichen Freigabe.

**Nächste Schritte:** GUI-Agent kann die A6-43-Review-Markierung in einem separaten, getesteten i18n-PR als „KI-sprachlich geprüft“ präzisieren; vollständige fr/es-Muttersprachler-Abnahme weiterhin extern organisieren. Keine Muttersprachler-Freigabe oder UI-Geräteabnahme behaupten.
