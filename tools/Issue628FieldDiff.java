import de.pulselibre.FitHistoricalObservations;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/**
 * Private offline per-occurrence comparison input for #628. Writes hashed
 * identities and attributes locally, not raw values; the derived hashes remain
 * sensitive and must never be committed or shared.
 */
public class Issue628FieldDiff {
    private static String digest(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws Exception {
        ArrayList<String> rows = new ArrayList<>();
        int files = 0, fields = 0, observations = 0;
        try (Stream<Path> paths = Files.list(Path.of(args[0]))) {
            for (Path path : paths.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".fit")).sorted().toList()) {
                var result = FitHistoricalObservations.INSTANCE.decode(Files.readAllBytes(path));
                if (result == null) continue;
                files++;
                for (var field : result.getDecodedFields()) {
                    fields++;
                    String key = field.getSourceFileSha256() + ":" + field.getRecordOrdinal() + ":" +
                        field.getGlobalMessageNumber() + ":" + field.getFieldKind() + ":" +
                        field.getFieldNumber() + ":" + field.getDeveloperDataIndex();
                    String raw = field.getBaseTypeCode() + ":" + field.getFieldSize() + ":" +
                        field.getBigEndian() + ":" + Base64.getEncoder().encodeToString(field.getRawBytes());
                    rows.add("F " + digest(key) + " " + digest(raw) + " " +
                        digest(String.valueOf(field.getTimestampEpochMillis())) + " " +
                        digest(field.getTimestampOrigin().name()));
                }
                for (var observation : result.getObservations()) {
                    observations++;
                    String raw = observation.getMetric().name() + ":" +
                        Double.doubleToLongBits(observation.getValue()) + ":" + observation.getUnit() +
                        ":" + observation.getQuality().name();
                    rows.add("O " + digest(observation.getIdempotencyKey()) + " " + digest(raw) + " " +
                        digest(String.valueOf(observation.getTimestampEpochMillis())) + " " +
                        digest(observation.getTimestampOrigin().name()));
                }
            }
        }
        Files.write(Path.of(args[1]), rows, StandardOpenOption.CREATE_NEW);
        System.out.printf("decoded_files=%d fields=%d observations=%d%n", files, fields, observations);
    }
}
