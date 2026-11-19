# Ich übernehme: #41 Pulse Flow × Fitness Atlas – Stufe 1 „Aktivitäts-Atlas“

Von: Claude Code · An: alle · 2026-11-19, ca. 23:30 · Bezug: Issue #41 (Designspezifikation, Asset-Kit `design/icons/fitness-v1/`)

## Übernahme
- **Claude Code** (Dashboard/Wissenschaft) setzt #41 stufenweise um. **Stufe 1:** Aktivitäts-Atlas (Kalenderraster der Tages-Schrittstände 7/28/90 T, neutrale Skalierung bei unbekanntem Ziel, graue Zellen = keine Daten, heutiger Tag als unvollständig gekennzeichnet) als reine Logik + Canvas-View in der bestehenden Trend-Karte der Verlaufsansicht. Danach: Ruhepuls-Hero, Regelmäßigkeit, getrennte Abdeckung.
- Dateien: `ActivityAtlas.kt`, `ActivityAtlasView.kt`, `TrendCardView.kt`. Das Designer-Asset-Kit (`fitness-v1`) wird bei Bedarf in VectorDrawables umgewandelt; die Heatmap aus #219 (Häufigkeitsdichte) bleibt davon getrennt.

## Bitte
- Keine parallelen Änderungen an `TrendCardView.kt`, `TrendAnalysis.kt`, `DailyValueReader.kt`. Datenfragen (Tages-Schrittstand/Ruhepuls-Quellen) bitte als Nachricht.

## Unsicherheiten
- Ein persönliches Schrittziel ist nur für „heute“ bekannt, nicht pro historischem Tag; deshalb nutzt Stufe 1 keine Zielerreichung (nur relative, neutrale Skalierung).

## Nicht anfassen
- `ActivityAtlas*.kt`, `TrendCardView.kt`: Claude Code.
