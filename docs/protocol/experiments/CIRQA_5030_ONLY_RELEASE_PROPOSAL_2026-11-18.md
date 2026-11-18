# 5030-only & 5030→5007 Vorbereitung (Jour fixe 18.11.2026)

**Status: 5030-only am echten S25/CIRQA erfolgreich ausgeführt (Build 961, CI-Run 44270609194).**
Bezug: Issues #2, #7, #20, #22.

> **Gerätebefund 5030-only:**
> - **Phase:** `OBSERVED` (Stopp wie vorgeschrieben vor 5007/5031/5002)
> - **Äußerer GFDI-Status:** `0` (erfolgreiches äußeres ACK)
> - **Innerer Payload:** Länge `1`, Wert `0` (`connectResult=0`)
> - **Gate:** `legacy5030OnlyAttemptedV2` persistent verbraucht
> - **Folgekommandos:** Keine (kein 5007, 5031, 5002 oder Retry)
> 
> Das belegt erstmals am echten CIRQA, dass das Gerät ein 5030-Systemevent (`HANDSHAKE_COMPLETE`, Body `[8, 0]`) mit äußerem Status 0 und dem inneren Ergebnisbyte 0 akzeptiert und bestätigt.

## Nächster Kombinierter Gerätetest: 5030 → 5007

### Protokollkontrollfluss
1. **Bootstrap:** Standard 5024/5050-Handshake mit 15-Byte-Nullmaske und Bestätigung des 5050-ACKs.
2. **Systemevent 5030:** Senden des `HANDSHAKE_COMPLETE`-Rahmens. Warten auf äußeren Status 0 und inneres Ergebnisbyte 0.
3. **Filter-Request 5007:** Senden von `PENDING_UPLOADS_ONLY` (Body `03`) über denselben GFDI-Kanal.
4. **Auswertung:** Erwartet wird eine CRC-valide GFDI-5000-Antwort mit Status 0 (Unterstützt).
5. **Stopp:** Nach erfolgreicher oder fehlerhafter 5007-Auswertung sofortiger Stopp. Kein 5031, kein 5002, kein FIT-Download.

### Sicherheits- und Stop-Regeln
- Separates Einmal-Gate (`legacy5030To5007Attempted`).
- Gesamtlaufzeit maximal 30 Sekunden; 5 Sekunden Timeout je Schreib-/Antwortschritt.
- Kein automatischer Retry, kein Archivieren, Löschen, Flag-Ändern, Entkoppeln oder `CLOSE_ALL`.
