#!/usr/bin/env bash
# Make /dev/kvm usable for the current user without unnecessary root work.
#
# GitHub-hosted runners need `sudo chmod 666 /dev/kvm`; a properly provisioned
# self-hosted runner user is in the `kvm` group (or has an ACL) and needs nothing.
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

if [ ! -e /dev/kvm ]; then
  ci_die "Hardwarevirtualisierung nicht verfügbar: /dev/kvm fehlt. Emulator-Tests sind ohne KVM nicht möglich."
fi

if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then
  ci_info "KVM bereits nutzbar für $(id -un)"
  exit 0
fi

ci_info "KVM nicht nutzbar für $(id -un); versuche einmalige Rechteanpassung"
if command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
  sudo chmod 666 /dev/kvm
elif command -v sudo >/dev/null 2>&1; then
  sudo chmod 666 /dev/kvm
else
  ci_die "Kein sudo verfügbar. Runner-Benutzer in die Gruppe 'kvm' aufnehmen (docs/ci/HYBRID_CI.md#kvm)."
fi

[ -r /dev/kvm ] && [ -w /dev/kvm ] || ci_die "KVM weiterhin nicht nutzbar nach Rechteanpassung."
ci_info "KVM nutzbar"
