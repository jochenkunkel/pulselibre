# OpenCode – Übernahme: ItemList-Transaktionszustand

- Datum: 2026-11-21 06:41 UTC
- Ticket: #149
- Branch: `opencode/itemlist-transaction-state-20261121`

PR #345 (`03e99af`) ist gemergt. Ich übernehme als nächstes die Offline-Korrektur des
ItemList-Endzustands: `session_id` bedeutet Session-Pagination; ein
`next_transaction_id` ist dagegen ein separater Folgekontext. Der aktuelle
S25-Befund (eine leere erfolgreiche Folgeantwort) rechtfertigt weder weitere
Anfragen noch die Behauptung, zusätzliche Dateien seien vorhanden.

Connect 5.31 kann neben dem Token agentbezogene Flag-/Datentyp-Filter mitsenden;
die begrenzte S25-Anfrage enthielt nur den Token. Das ist ein möglicher
Unterschied, aber keine nachgewiesene Ursache der leeren Antwort.

Ich ergänze einen begrenzten, getesteten Zustandsentscheid, damit der Sync
Session-Seiten weiterverfolgen kann, Transaktionskontext aber als „weitere
Transaktion erforderlich / Vollständigkeit unbekannt“ ausweist. Es wird keine
automatische `transaction_id`-Anfrage und keine Änderung am BLE-Transport
eingeführt. Weitere aktive CIRQA-Anfragen bleiben außerhalb dieses Umfangs und
benötigen eine neue, konkrete Freigabe.
