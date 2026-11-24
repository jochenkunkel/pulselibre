import de.pulselibre.FitHistoricalObservations;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.*;

/**
 * Offline aggregate-only FIT replay. Input is read-only. Optional second
 * argument writes sensitive derived fingerprints locally for a set comparison;
 * use a private directory and never publish the fingerprint file.
 */
public class Issue628Replay {
    public static void main(String[] args) throws Exception {
        int seen = 0, unique = 0, decoded = 0, observations = 0, timed = 0, missing = 0, eligible = 0;
        Set<String> files = new HashSet<>(), samples = new HashSet<>(), fingerprints = new HashSet<>();
        Map<String, Integer> metrics = new TreeMap<>();
        try (Stream<Path> paths = Files.list(Path.of(args[0]))) {
            for (Path path : paths.filter(f -> f.toString().toLowerCase(Locale.ROOT).endsWith(".fit")).sorted().toList()) {
                seen++;
                byte[] bytes = Files.readAllBytes(path);
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!files.add(hash)) continue;
                unique++;
                var result = FitHistoricalObservations.INSTANCE.decode(bytes);
                if (result == null) continue;
                decoded++;
                for (var observation : result.getObservations()) {
                    if (!samples.add(observation.getIdempotencyKey())) continue;
                    observations++;
                    if (observation.getTimestampEpochMillis() == null) missing++; else timed++;
                    if (observation.getTimestampEpochMillis() != null &&
                        observation.getQuality() == FitHistoricalObservations.Quality.PROFILE_VALID &&
                        fingerprints.add(observation.getDuplicateFingerprint())) {
                        eligible++;
                        metrics.merge(observation.getMetric().name(), 1, Integer::sum);
                    }
                }
            }
        }
        System.out.printf("seen=%d unique=%d decoded=%d observations=%d timed=%d missing=%d distinct_importable=%d metrics=%s%n",
            seen, unique, decoded, observations, timed, missing, eligible, metrics);
        if (args.length > 1) Files.write(Path.of(args[1]), new TreeSet<>(fingerprints),
            StandardOpenOption.CREATE_NEW);
    }
}
