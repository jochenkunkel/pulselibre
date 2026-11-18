# 5030-only: Evidenz und Freigabevorlage (Jour fixe 18.11.2026)

**Status: am echten S25/CIRQA erfolgreich ausgeführt (Build 961, CI-Run 44270609194).**
Bezug: Issues #2, #7, #20, #22.

> **Nachtrag 18.11.2026 – Geräterlauf:** Nach Stoppen des Hintergrund-Aufzeichnungsdienstes lief der einmalig freigegebene `5030-only`-Test erfolgreich durch.
> 
> ### Tatsächliches Gerätergebnis
> - **Phase:** `OBSERVED` (Stopp wie vorgeschrieben vor 5007/5031/5002)
> - **Äußerer GFDI-Status:** `0` (erfolgreiches äußeres ACK)
> - **Innerer Payload:** Länge `1`, Wert `0` (`connectResult=0`)
> - **Gate:** `legacy5030OnlyAttemptedV2` persistent verbraucht
> - **Folgekommandos:** Keine (kein 5007, 5031, 5002 oder Retry)
> 
> Das belegt erstmals am echten CIRQA, dass das Gerät ein 5030-Systemevent (`HANDSHAKE_COMPLETE`, Body `[8, 0]`) mit äußerem Status 0 und dem inneren Ergebnisbyte 0 akzeptiert und bestätigt.
