# #785 – Zustände des CIRQA-Bandes: Stand und Testplan (28.11.2026)

## Zustandsmatrix (Stand jetzt)

| Zustand | Auslöser | Beobachtete Nachricht | Connect-Anzeige | PulsLibre-Anzeige | Nachweis |
|---|---|---|---|---|---|
| Aktivität läuft | Start am Band | Smart 30.3 → 1012 → 2 → 1 = **3** (Lauf) bzw. **9** (Gehen); nur nach Abo 30.1 | „Aktivität läuft“, Knopf „speichern und beenden“ | Übersicht-Hinweis, Gerät-Zeile, Knopf „beenden und speichern“ | 3: S10 zweimal (Connect 11:47, PulsLibre 13:50/13:55). 9: einmal (17:00) |
| Angehalten | 56.17 {2:2} oder am Band | 30.3 = **1** | – | gilt als laufend | einmal (Connect-Lauf) |
| Wird gespeichert | 56.17 {2:3} | 30.3 = **5** | – | gilt als laufend | einmal |
| Gespeichert/bereit | nach dem Speichern | 30.3 = **4** | Aktivität verschwindet | kein Hinweis | einmal |
| Keine Aktivität | – | Abo-Antwort 30.2, **kein** 30.3 | – | kein Hinweis | zweimal (14:42, 15:56) |
| Am Handgelenk / abgelegt | An- oder Ablegen | **nicht belegt** | – | keine Anzeige (bewusst) | Mitschnitt 11:31 hatte keine Live-Verbindung |

## Trennung vom PulsLibre-Status
- **Band-Aktivität:** `BandActivityStore`, Quelle: Sync-Bericht `bandActivity=`.
- **Recorder:** `RecorderStatus` (Aufzeichnung an/aus).
- **Download:** `HistorySyncProgress`.
- Die drei Zustände werden getrennt gespeichert und angezeigt. Ein Download pausiert die Aufzeichnung (seit #786), ändert aber den Bandzustand nicht.

## Veralteter Status
- Der Bandzustand trägt einen Zeitstempel.
- „Läuft“ wird höchstens 6 h nach der letzten Meldung angezeigt (`BandActivityStore.MAX_AGE_MS`).
- Jeder neue Download überschreibt den Zustand, auch mit „keine Aktivität“.

## Testplan für den nächsten gemeinsamen Gerätetest
1. **On-/Off-Wrist:**
   - Aufzeichnung an, HCI-Snoop an.
   - Band 2 min tragen, 2 min ablegen, wieder anlegen.
   - Zeiten notieren und Live-Kanäle (Puls, SpO₂, Stress, Atmung) sowie Smart-/5024-Meldungen vergleichen.
   - Gegenprobe mit Connect (Connect zeigt „nicht getragen“?). Connect und PulsLibre nie gleichzeitig verbinden.
2. **Aktivitätsarten:** Lauf, Gehen und eine weitere Aktivität starten und jeweils den Code ablesen. Damit wird die Zuordnung 3/9 geprüft.
3. **Pause/Fortsetzen am Band:** Code beobachten, erwartet 1 bzw. 3/9.
4. **Live-Abo:** Prüfen, ob 30.1 in der Live-Verbindung ohne Sync beantwortet wird. Das ist die Voraussetzung, damit die Positionsspur ohne Öffnen der App startet.

Unbelegte Zustände werden in der App nicht angezeigt.
