#!/usr/bin/env python3
"""Offline, metadata-only btsnoop record classifier. Never prints packet contents."""
import argparse
import json
import struct
from collections import Counter
from pathlib import Path

MAGIC = b"btsnoop\x00"
MAX_RECORD_SIZE = 16 * 1024 * 1024
TYPES = {1: "command", 2: "acl", 3: "sco", 4: "event", 5: "iso"}


class InvalidSnoop(ValueError):
    pass


def summarize(path):
    counts = Counter()
    att_candidates = 0
    with open(path, "rb") as stream:
        header = stream.read(16)
        if len(header) != 16 or header[:8] != MAGIC:
            raise InvalidSnoop("Invalid or truncated btsnoop header")
        version, datalink = struct.unpack(">II", header[8:])
        if version != 1 or datalink not in (1001, 1002):
            raise InvalidSnoop("Unsupported btsnoop version or datalink")
        while True:
            record = stream.read(24)
            if not record:
                break
            if len(record) != 24:
                raise InvalidSnoop("Truncated record header")
            original, included, flags, drops, timestamp = struct.unpack(">IIIIQ", record)
            if included > original or included > MAX_RECORD_SIZE:
                raise InvalidSnoop("Invalid record length")
            payload = stream.read(included)
            if len(payload) != included:
                raise InvalidSnoop("Truncated record payload")
            # Android H4 btsnoop records carry a one-byte HCI packet type.
            if datalink == 1002:
                if not payload:
                    raise InvalidSnoop("Empty H4 record")
                packet_type, packet = payload[0], payload[1:]
            else:
                # HCI UART (1001) is not guaranteed to carry H4 framing.
                raise InvalidSnoop("Unsupported unframed datalink 1001")
            counts[TYPES.get(packet_type, "unknown")] += 1
            # Only complete single-fragment L2CAP ATT ACL frames are counted.
            if packet_type == 2:
                if len(packet) < 4:
                    raise InvalidSnoop("Truncated ACL header")
                handle_flags, acl_length = struct.unpack_from("<HH", packet)
                if len(packet) != 4 + acl_length:
                    raise InvalidSnoop("ACL payload length mismatch")
                pb = (handle_flags >> 12) & 3
                if pb in (0, 2) and acl_length >= 4 and len(packet) >= 4 + acl_length:
                    l2_length, cid = struct.unpack_from("<HH", packet, 4)
                    if cid == 4 and l2_length > 0 and acl_length >= 4 + l2_length:
                        att_candidates += 1
    return {
        "records": sum(counts.values()),
        "types": dict(sorted(counts.items())),
        "acl_records": counts["acl"],
        "complete_att_candidates": att_candidates,
        "sufficient_for_att_analysis": counts["acl"] > 0,
        "warning": "No ACL packets; ATT/GATT analysis impossible" if not counts["acl"] else
                   "ACL present; ATT completeness and device attribution not established",
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    args = parser.parse_args()
    try:
        result = summarize(args.capture)
    except (OSError, InvalidSnoop) as error:
        parser.exit(2, f"Invalid capture: {error}\n")
    print(json.dumps(result, sort_keys=True))
    if not result["acl_records"]:
        parser.exit(1)


if __name__ == "__main__":
    main()
