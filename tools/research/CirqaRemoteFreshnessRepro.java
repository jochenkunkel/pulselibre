import de.pulselibre.CirqaFileAccessItemListParser;
import de.pulselibre.CirqaTargetedPullManifest;
import de.pulselibre.CirqaTargetedPullPolicy;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Offline characterization of missing remote freshness validation.
 * Uses only generated synthetic metadata and the actual compiled product classes.
 * REPRODUCED means the defect exists, NOT that the selection policy is correct.
 */
public final class CirqaRemoteFreshnessRepro {
    static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (var part : parts) out.writeBytes(part);
        return out.toByteArray();
    }
    static byte[] varint(long value) {
        var out = new ByteArrayOutputStream();
        do {
            int b = (int) (value & 127);
            value >>>= 7;
            out.write(value == 0 ? b : b | 128);
        } while (value != 0);
        return out.toByteArray();
    }
    static byte[] scalar(int field, long value) {
        return concat(varint(field << 3), varint(value));
    }
    static byte[] message(int field, byte[] value) {
        return concat(varint((field << 3) | 2), varint(value.length), value);
    }
    static CirqaFileAccessItemListParser.Item item(long transaction, long modified, long size) {
        byte[] uid = {1, 2, 3, 4}; // synthetic, not a device identifier
        byte[] reference = concat(
            message(1, uid), message(2, scalar(1, 1)), scalar(3, size),
            scalar(4, 0), scalar(5, transaction), scalar(7, modified)
        );
        byte[] smart = message(43, message(10, concat(scalar(1, 0), message(4, reference))));
        var page = CirqaFileAccessItemListParser.INSTANCE.parsePage(smart);
        if (page == null || !page.getComplete()) throw new AssertionError("fixture parse failed");
        return page.getItems().get(0);
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Map<String, String> storage = new HashMap<>();
        var manifest = new CirqaTargetedPullManifest(new CirqaTargetedPullManifest.Storage() {
            public String get(String key) { return storage.get(key); }
            public boolean put(String key, String value) { storage.put(key, value); return true; }
            public boolean remove(String key) { storage.remove(key); return true; }
        });
        var oldItem = item(10, 1000, 128);
        var changedItem = item(11, 2000, 128);
        byte[] oldPayload = new byte[128];
        byte[] newPayload = new byte[128];
        newPayload[64] = 1;
        var digest = MessageDigest.getInstance("SHA-256");
        String oldSha = HexFormat.of().formatHex(digest.digest(oldPayload));
        String newSha = HexFormat.of().formatHex(digest.digest(newPayload));
        require(!oldSha.equals(newSha), "fixture content must differ");
        for (String session : new String[]{"synthetic1", "synthetic2", "synthetic3"}) {
            require(manifest.observe(oldItem, session), "observation failed");
        }
        require(manifest.recordCompletedImport(oldItem, oldSha), "manifest recording failed");
        var oldIdentity = manifest.identity(oldItem);
        var changedIdentity = manifest.identity(changedItem);
        require(oldIdentity != null && changedIdentity != null, "missing identity");
        require(oldIdentity.getKey().equals(changedIdentity.getKey()),
            "characterization changed: remote version is no longer discarded");
        require(manifest.completedImportSha256(changedItem).equals(oldSha),
            "characterization changed: stale import not reused");
        var completed = new CirqaTargetedPullPolicy.CompletedImport(
            oldIdentity.getPolicyIdentity(), oldSha, true, manifest.stabilityEvidence(oldItem));
        boolean pullChanged = CirqaTargetedPullPolicy.INSTANCE.shouldPull(
            changedIdentity.getPolicyIdentity(), Collections.singletonList(completed));
        var changedSize = manifest.identity(item(11, 2000, 129));
        require(changedSize != null, "control identity missing");
        boolean pullChangedSize = CirqaTargetedPullPolicy.INSTANCE.shouldPull(
            changedSize.getPolicyIdentity(), Collections.singletonList(completed));
        require(!pullChanged && pullChangedSize, "characterization changed; review policy");
        System.out.println("remote_content_changed=true; remote_size_changed=false");
        System.out.println("remote_transaction_and_modified_time_changed=true");
        System.out.println("manifest_identity_changed=false; shouldPullChangedContent=false");
        System.out.println("control_changed_size_shouldPull=true");
        System.out.println("REPRODUCED: local integrity + repeated metadata do not prove remote freshness");
    }
}
