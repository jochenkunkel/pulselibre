# CIRQA – passiver Garmin-Connect-Mitschnitt (2026-11-20)

## Ablauf und Datenschutz

Auf dem bereits gekoppelten Galaxy S25 wurde Garmins sichtbare manuelle Synchronisierung auf der CIRQA-Geräteseite ausgelöst. PulsLibre wurde dabei nicht verbunden und es wurde kein PulsLibre-Gerätekommando gesendet. Direkt danach wurde ein Android-Bugreport lokal gesichert. Bugreport und dekodierter Rohmitschnitt bleiben privat und außerhalb des Repositories.

## Offline-Befund

Der Bugreport enthält einen komprimierten BTSNOOZ-v2-Block. Nach lokaler Konvertierung ins btsnoop-Format ergab der vorhandene Metadaten-Validator:

- 6.990 HCI-Datensätze: 540 Commands und 6.450 Events
- **0 ACL-Datensätze**, 0 vollständige ATT-Kandidaten
- daher keine GATT-/ATT-/GFDI-Aussage möglich

Die Aufnahme belegt somit keinen Garmin-Connect-Dateitransfer und erlaubt keine Aussage, ob die manuelle Synchronisierung fachlich erfolgreich war. Die UI blieb auf der Connect-Geräteseite; der Connect-Sync-Dienst war aktiv. Ein aktiver Dienst allein ist kein Sync-Erfolgsnachweis.

## Konsequenz

Issue [#7](https://github.com/jochenkunkel/pulselibre/issues/7) bleibt offen: Der Android-Bugreport-Weg lieferte auch bei diesem unmittelbar nach der manuellen Synchronisierung erstellten Bericht keine ACL-Daten. Der vorhandene btsnoop-Validator funktioniert für den dekodierten Trace, kann fehlende ACL-Daten aber nicht ersetzen. Keine Rohdaten, Kennungen oder personenbezogenen Informationen wurden veröffentlicht.
