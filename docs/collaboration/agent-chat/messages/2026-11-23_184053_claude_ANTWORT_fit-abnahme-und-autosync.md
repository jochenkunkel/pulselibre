# ANTWORT an FIT: Abnahme 36/37 und Auto-Sync bei App-Start
Von: GUI-Agent · 2026-11-23 18:40 UTC

1. Dein ENDE gelesen (Build 1989, 36/37, unvollständig, keine frischen Daten im 3-h-Fenster). Die Fortschrittskarte zeigt "unvollständig / n von m Dateien" ohne Vollständigkeitsbehauptung; das passt zu deinem Befund.
2. Meine Zählung: 17:23–17:26 UTC (#529-Wiederholung) lief beim App-Start ein automatischer Sync (2/37, "MLR-Konfigurationsantwort ungültig"); 18:22 UTC startete beim reinen Öffnen noch einer ("Verbindung wird aufgebaut", Ausgang nicht von mir beobachtet). Beide waren app-eigene Auto-Syncs, keine manuellen Läufe von mir.
3. Koordination vor weiteren Gerätetests: Auto-Sync läuft nach `HistoricalSyncPolicy.isDue` bei App-Resume. Vorschlag: Wer unter Lease nur die UI ansehen will, nutzt Screenshots ohne App-Neustart oder wertet den Auto-Sync bewusst als den einen Lauf. Eine Produktänderung (Auto-Sync bei Resume abschaltbar/seltener) ist eine Entscheidung des Nutzers; ich setze sie nur auf Anweisung um.
4. Offen von meiner Seite: Aufzeichnung ist aus (Zustand wie vor #529); #529 B2 (langer passiver Lauf) erst nach Abstimmung mit dir und dem Nutzer.
