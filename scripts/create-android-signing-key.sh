#!/usr/bin/env bash
set -euo pipefail

# Run locally on a trusted machine, NEVER in CI and NEVER commit generated files.
# Output is kept outside this repository by default.
OUT="${1:-$HOME/pulselibre-private-signing}"
mkdir -p "$OUT"
chmod 700 "$OUT"
KEYSTORE="$OUT/pulselibre-ci.keystore"
if [[ -e "$KEYSTORE" ]]; then
  echo "Refusing to overwrite existing signing key: $KEYSTORE" >&2
  exit 1
fi
echo "Creating a dedicated PulseLibre update signing key."
echo "Choose and safely back up a strong keystore password and key password."
keytool -genkeypair -v -keystore "$KEYSTORE" -alias pulselibre-ci \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=PulseLibre Offline Android Updates"
chmod 600 "$KEYSTORE"
base64 "$KEYSTORE" | tr -d '\n' > "$OUT/pulselibre-ci.keystore.base64"
chmod 600 "$OUT/pulselibre-ci.keystore.base64"
echo
echo "Created: $KEYSTORE"
echo "Created: $OUT/pulselibre-ci.keystore.base64"
echo "Set GitHub Actions secrets:"
echo "  PULSELIBRE_SIGNING_KEY_B64: contents of .base64 file"
echo "  PULSELIBRE_SIGNING_STORE_PASSWORD: password entered above"
echo "  PULSELIBRE_SIGNING_KEY_ALIAS: pulselibre-ci"
echo "  PULSELIBRE_SIGNING_KEY_PASSWORD: key password entered above"
echo "Back up the keystore and passwords OFFLINE. Do not send them in chat or commit them."
