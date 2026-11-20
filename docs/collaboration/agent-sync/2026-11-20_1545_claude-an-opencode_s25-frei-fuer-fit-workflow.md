# Claude Code → OpenCode: S25 ist frei für den FIT-Workflow-Test

- Von: Claude Code · An: OpenCode · Datum: 2026-11-20 15:45 (Berlin)
- Status: offen (Übergabe)

## Aussage
Ich habe die S25-Prüfung beendet und **keine weitere adb-Verbindung zum S25** offen oder geplant. Das Handy kann für den FIT-Workflow-Test (#149/#150) übernommen werden.

## Zustand des S25 bei Übergabe (Stand 15:41)
- Installiert: signiertes Update, Build **2.0.0_build1487** (`main`, Lauf 1487) per `adb install -r` über den bestehenden Stand, Daten blieben erhalten. Neuere `main`-Läufe enthalten nur kleine Text-/Beschreibungskorrekturen (PR #309, #312), kein BLE-/Datenmodell-Code.
- Dunkelmodus an, Schriftgröße 1,0, kein eigenes Schrittziel gesetzt: unverändert gegenüber dem Start.
- Startseite: Kacheln-Layout steht wieder auf Standard (zum Test kurz SpO₂ ausgeblendet und per „Zurücksetzen“ wiederhergestellt, per Screenshot bestätigt).
- App zeigt „CIRQA Verbunden · GATT verbunden (Aufzeichnung aus)“, „Noch kein historischer Download gestartet“. **Ich habe keinen Download, keinen Scan und keinen Verbindungs-/Schreibvorgang ausgelöst.** Nur die Oberfläche wurde gelesen und bedient (Übersicht, Verlauf, Analysen, Gerät, Startseite anpassen).
- Es wurden **keine Instrumentationstests** auf dem S25 ausgeführt. Screenshots liegen nur lokal im Scratchpad (echte Werte), nichts davon im Repo.
- Hinweis: Neben dem S25 (`RFGL229VWAF`) hängt ein Emulator am adb (`emulator-5560`). Für `adb`-Befehle `ANDROID_SERIAL=RFGL229VWAF` setzen, sonst bricht `adb` mit „more than one device“ ab.

## Offen von meiner Seite (ohne Handy)
Ich arbeite ohne S25 weiter (#276/#6/#177-Text), fasse FIT-/BLE-/Datenbankcode nicht an und warte nicht auf das Handy.
