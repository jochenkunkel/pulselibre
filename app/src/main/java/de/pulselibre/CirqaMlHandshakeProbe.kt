package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Explicit one-shot Garmin ML/GFDI handle registration test.
 * Registers GFDI and optionally realtime services for bounded diagnostics.
 * The separately gated legacy-5007 mode sends at most one filter request and
 * never performs a file request, sync follow-up, health persistence or retry.
 */
private const val MLR_SERVICE_ID = 8216 // 0x2018, Connect MLRTransportPipe.ML_SERVICE_IDS

class CirqaMlHandshakeProbe(private val context: Context, private val adapter: BluetoothAdapter) {
    private val main = Handler(Looper.getMainLooper())
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var active = false
    @Volatile private var terminationRequested = false
    private var gatt: BluetoothGatt? = null
    private var callback: ((String) -> Unit)? = null
    var onReading: ((Int, GarminRealtime.Reading) -> Unit)? = null
    private var tx: BluetoothGattCharacteristic? = null
    private var selectedRxUuid = "nicht ermittelt"
    private var selectedTxUuid = "nicht ermittelt"
    private var selectedTxProperties = 0
    private var txTrace = CirqaGfdiWriteTrace()
    private var sessionStartedNanos = 0L
    private var session: GarminMlHandshake.Session? = null
    private var requestedCloseAll = false
    private var realtimeMode = false
    private var continuousRecordingMode = false
    private var historyDiagnosticMode = false
    private var legacy5007TestMode = false
    private var legacy5030Only: CirqaLegacy5030OnlySession? = null
    @Volatile private var legacy5030WriteOutstanding = false
    private var historicalSyncV1: CirqaHistoricalSyncV1MultiPhaseSession? = null
    @Volatile private var hsv1WriteOutstanding = false
    private var hsv1Type = -1
    @Volatile private var callbackGeneration: Any = Any()
    private var legacy5007WriteClaimed = false
    private var legacy5007RequestSent = false
    private var legacy5007Response = CirqaLegacy5007ResponseObserver()
    private var legacy5031TestMode = false
    private var legacy5031WriteClaimed = false
    private var legacy5031RequestSent = false
    private var legacy5031Response = CirqaLegacy5031ResponseObserver()
    private var legacyHistoryDownloadMode = false
    private var directoryReadV2Mode = false
    private var directoryReadAfterChain = false
    private var dirReadSession: CirqaDirectoryReadV2Session? = null
    private var fileAccessListMode = false
    private var syncRequestMode = false
    private var syncStopBeforePullMode = false
    private var syncRepeatItemListsMode = false
    private var syncRepeatFirstItems: List<CirqaFileAccessItemListParser.Item>? = null
    private var syncAccepted = false
    private val syncFrameCounts = LinkedHashMap<Int, Int>()
    private var syncProtoShapes = ""
    private var syncOtherLengths = ""
    private var syncItemListSent = false
    private var syncPullSent = false
    private var syncItemSummary = ""
    private var syncPullDiag = ""
    private val syncPendingItems = java.util.ArrayDeque<CirqaFileAccessItemListParser.Item>()
    private var syncCurrentItem: CirqaFileAccessItemListParser.Item? = null
    private var syncNextRequestId = 3
    private var syncCompletedFiles = 0
    private var syncFailedFiles = 0
    private var syncFileResults = ""
    private var syncHasPaginationSession = false
    private var syncHasNextTransactionId = false
    private var syncPageSessionId: Long? = null
    private var syncPageNextTransactionId: Long? = null
    private var syncListPageCount = 0
    private var syncQueuedFiles = 0
    private var syncQueuedBytes = 0L
    private var syncSkippedFiles = 0
    private var syncIncompleteReason: String? = null
    private val syncGdxmlStrings = linkedMapOf<Long, String>()
    private val syncSeenItemUids = hashSetOf<String>()
    private var mlrHandle: Int? = null
    private var mlrRegisterSent = false
    private var mlrDiag = ""
    private var mlrChunks = 0
    private var mlrBytes = 0
    private var mlrConfigureResponseSeen = false
    private var mlrTransferActive = false
    private var syncMlrRegistrationPending = false
    private var syncTransferHandle = 0L
    private var syncExpectedSize = 0L
    private val mlrData = java.io.ByteArrayOutputStream()
    private var syncFinalReport = ""
    private var fileAccessRequestClaimed = false
    private var itemListRepeatabilityMode = false
    private var itemListRepeatabilityFirstPage: CirqaFileAccessItemListParser.ItemListPage? = null
    private val fileAccessFramer = CirqaRfcommStreamFramer()
    private val fileAccessReassembler = GarminProtobufReassembler()
    private var fileAccessDiag = ""
    private var legacy5002RequestWriteClaimed = false
    private var legacy5002RequestSent = false
    private var legacy5002Response = CirqaLegacy5002ResponseObserver()
    private var legacy5004Response = CirqaLegacy5004ResponseObserver()
    private var legacySupportedTypes: Set<Pair<Int, Int>> = emptySet()
    private var legacyFileQueue = ArrayDeque<CirqaLegacyDirectoryParser.Entry>()
    private var legacyCurrentDirectoryEntry: CirqaLegacyDirectoryParser.Entry? = null
    private var legacyCurrentFileIndex: Int? = null
    private var legacyCurrentExpectedSize = 0
    private val legacyRequestedIndices = mutableListOf<Int>()
    private var legacyCurrentAssembler: CirqaLegacyPlainFileAssembler? = null
    private var legacyCurrentCrc = 0
    private var legacyPreviousCrc = 0
    private var legacyLastPayload: ByteArray? = null
    private var legacyPendingCrc: Int? = null
    private var legacyDirectory: CirqaLegacyDirectoryParser.Directory? = null
    private var legacyDirectoryEntriesSeen = 0
    private var legacyFitBytesWritten = 0
    private val legacyDownloadedIndices = mutableListOf<Int>()
    private var legacyCurrentTransferCompleted = false
    private var legacyHistorySessionStartedAt = 0L
    private var legacyHistoryReportStatus = "Noch nicht gestartet"
    private var legacyHistoryReportDirectoryEntries = 0
    private var legacyHistoryReportEligibleFiles = 0
    private var legacyHistoryReportSkippedFiles = 0
    private var legacyHistoryReportCompletedFiles = 0
    private var legacyHistoryReportStoredBytes = 0
    private var legacyHistoryDirectoryEntryDiagnostics = "keine"
    private var legacyHistoryOutputDirectory: File? = null
    private var rawCaptureMode = false
    private var bootstrapAckMode = false
    private var configurationTestMode = false
    private var syntheticSyncDebugMode = false
    private var configurationResponseQueued = false
    private var configurationResponseConfirmed = false
    private var configurationAckReceived = false
    private var configurationObservationExtended = false
    private var configuration5050Baseline = 0
    private var configurationAckBaseline = 0
    private var configuration5037Baseline = 0
    private var configurationMalformed5037Baseline = 0
    private var configurationRejectedBaseline = 0
    private var configurationErrorBaseline = 0
    private var configurationNotificationBaseline = 0
    private var configuration5037ObservationStarted = false
    private var post5050Evidence = Cirqa5050PostWriteEvidence()
    private var passive5050Session = Cirqa5050PassiveSession()
    private var bootstrapAckSent = false
    private var bootstrapAckConfirmed = false
    private var bootstrapAckPending = false
    private var bootstrapAckChunks = ArrayDeque<ByteArray>()
    private var bootstrapBaselineFrames = 0
    private var mlWritePending = false
    private var historyRegistered = false
    private var historyTransport = GarminHistoryTransportInspector()
    private var fileListProbe = GarminFileListProbe()
    private var historyHandle: Int? = null
    private var historyRouted = 0
    private var historyUnrouted = 0
    private val historyLengths = BoundedPacketLengths()
    private var pendingService = 0
    private val realtimeRegistry = GarminRealtime.Registry()
    private var heartRate: GarminRealtime.Reading.HeartRate? = null
    private var steps: GarminRealtime.Reading.Steps? = null
    private var realtimeFrames = 0
    private var expandedMode = false
    private var exploratoryMode = false
    private var mousePodsMode = false
    private var motionHandle: Int? = null
    private var motionPackets = 0
    private var motionStartIssued = false
    private var servicePlan: List<Int> = GarminRealtime.SUPPORTED_SERVICES
    private val exploratoryAccepted = mutableMapOf<Int, Int>()
    private val exploratoryFrames = mutableMapOf<Int, Int>()
    private var registrationIndex = 0
    private val rejectedServices = mutableListOf<Int>()
    private val extraReadings = mutableMapOf<Int, GarminRealtime.Reading>()

    /**
     * Phase-1 diagnostic only: registers the Garmin GFDI ML service and observes
     * notification metadata for 20 seconds. No file-list request is sent yet.
     */
    fun startHistoryDiagnostic(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        historyDiagnosticMode = true
        realtimeMode = false
        start(false, result)
    }

    /** Permanently fail-closed after CIRQA rejected the compact FileSync V2 candidate. */
    fun startFileListQuery(result: (String) -> Unit) {
        result("FileSync V2 ist nach UNSUPPORTED dauerhaft gesperrt; keine Verbindung und kein BLE-Befehl ausgeführt.")
    }

    /** One GFDI registration followed by exactly one 5007 write and a five-second observation. */
    fun startLegacy5007CompatibilityTest(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        legacy5007TestMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        continuousRecordingMode = false
        start(false, result)
        if (!active) {
            legacy5007TestMode = false
            historyDiagnosticMode = false
        }
    }

    /** One-shot 5024/5050 bootstrap; only after its confirmed ACK may one 5007 be sent. */
    fun startLegacy5007AfterBootstrapTest(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        legacy5007TestMode = true
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        continuousRecordingMode = false
        start(false, result)
        if (!active) {
            legacy5007TestMode = false
            bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        }
    }

    /** One-shot bootstrap, one 5007 prerequisite, then one empty 5031 and stop. */
    fun startLegacy5031AfterBootstrapTest(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        legacy5007TestMode = true
        legacy5031TestMode = true
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        continuousRecordingMode = false
        start(false, result)
        if (!active) {
            legacy5007TestMode = false
        legacy5031TestMode = false
        legacyHistoryDownloadMode = false
        bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        }
    }

    /**
     * Prepared but release-locked. New probe instance, dedicated persistent gate,
     * one system event and no file/filter command. Never invoked by a recorder.
     */
    @SuppressLint("MissingPermission")
    fun startLegacy5030OnlyTest(userConfirmed: Boolean, result: (String) -> Unit) {
        if (!CirqaLegacy5030AttemptGate.RELEASED_FOR_DEVICE_TEST || !userConfirmed) {
            result("5030-only gesperrt: gesonderte Gerätefreigabe ausstehend; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || legacy5030Only != null ||
            legacy5007TestMode || legacy5031TestMode || legacyHistoryDownloadMode ||
            realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("5030-only benötigt eine neue, unbenutzte Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("5030-only: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt; kein Gate-Verbrauch.")
            return
        }
        val prefs = context.getSharedPreferences("cirqa_5030_only", Context.MODE_PRIVATE)
        if (!CirqaLegacy5030AttemptGate.consume(
                CirqaLegacy5030AttemptGate.RELEASED_FOR_DEVICE_TEST, userConfirmed,
                { prefs.getBoolean(CirqaLegacy5030AttemptGate.PREFERENCE_KEY, false) },
                { prefs.edit().putBoolean(CirqaLegacy5030AttemptGate.PREFERENCE_KEY, true).commit() }
            )) {
            result("5030-only: Gate verbraucht oder Persistierung fehlgeschlagen; keine Verbindung.")
            return
        }
        val model = CirqaLegacy5030OnlySession()
        legacy5030Only = model
        model.begin(true, android.os.SystemClock.elapsedRealtime())
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        start(false, result)
        if (active) main.postDelayed({
            if (active) {
                model.tick(model.token, android.os.SystemClock.elapsedRealtime())
                finish("5030-only: Gesamtlaufzeit beendet.", gatt)
            }
        }, CirqaLegacy5030OnlySession.SESSION_MILLIS)
    }

    @SuppressLint("MissingPermission")
    fun startHistoricalSyncV1MultiPhaseTest(userConfirmed: Boolean, maxPhase: Int, result: (String) -> Unit) {
        if (!CirqaHistoricalSyncV1MultiPhaseAttemptGate.RELEASED_FOR_DEVICE_TEST || !userConfirmed) {
            result("Historical Sync V1 gesperrt; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || historicalSyncV1 != null ||
            legacy5030Only != null || legacy5007TestMode || legacy5031TestMode ||
            legacyHistoryDownloadMode || realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("Historical Sync V1 benötigt eine neue Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("Historical Sync V1: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt.")
            return
        }
        require(maxPhase in 1..3)
        val prefs = context.getSharedPreferences("cirqa_historical_sync_v1", Context.MODE_PRIVATE)
        val gateKey = CirqaHistoricalSyncV1MultiPhaseAttemptGate.keyForPhase(maxPhase)
        if (!CirqaHistoricalSyncV1MultiPhaseAttemptGate.consume(
                CirqaHistoricalSyncV1MultiPhaseAttemptGate.RELEASED_FOR_DEVICE_TEST, userConfirmed,
                { prefs.getBoolean(gateKey, false) },
                { prefs.edit().putBoolean(gateKey, true).commit() }
            )) {
            result("Historical Sync V1: Gate verbraucht oder Persistierung fehlgeschlagen.")
            return
        }
        val model = CirqaHistoricalSyncV1MultiPhaseSession(maxPhase = maxPhase)
        historicalSyncV1 = model
        model.begin(true, android.os.SystemClock.elapsedRealtime())
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        start(false, result)
        if (active) main.postDelayed({
            if (active) {
                model.tick(model.token, android.os.SystemClock.elapsedRealtime())
                finish("Historical Sync V1: Gesamtlaufzeit beendet.", gatt)
            }
        }, CirqaHistoricalSyncV1MultiPhaseSession.SESSION_MILLIS)
    }

    /**
     * Bounded READ-ONLY directory inspection: bootstrap -> 5002 index 0 ->
     * CRC/offset-validated 5004 chunks with ACK -> parse and report entries.
     * Stops before any FIT file request. Separate persistent one-shot gate.
     */
    @SuppressLint("MissingPermission")
    fun startDirectoryReadV2Test(userConfirmed: Boolean, result: (String) -> Unit) {
        if (!CirqaDirectoryReadV2AttemptGate.RELEASED_FOR_DEVICE_TEST || !userConfirmed) {
            result("Directory-Lesen gesperrt; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || directoryReadV2Mode ||
            historicalSyncV1 != null || legacy5030Only != null ||
            legacy5007TestMode || legacy5031TestMode || legacyHistoryDownloadMode ||
            realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("Directory-Lesen benötigt eine neue Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("Directory-Lesen: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt.")
            return
        }
        val prefs = context.getSharedPreferences("cirqa_directory_read_v2", Context.MODE_PRIVATE)
        if (!CirqaDirectoryReadV2AttemptGate.consume(
                CirqaDirectoryReadV2AttemptGate.RELEASED_FOR_DEVICE_TEST, userConfirmed,
                { prefs.getBoolean(CirqaDirectoryReadV2AttemptGate.PREFERENCE_KEY, false) },
                { prefs.edit().putBoolean(CirqaDirectoryReadV2AttemptGate.PREFERENCE_KEY, true).commit() }
            )) {
            result("Directory-Lesen: Gate verbraucht oder Persistierung fehlgeschlagen.")
            return
        }
        directoryReadV2Mode = true
        legacyHistoryDownloadMode = true
        legacySupportedTypes = emptySet()
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        continuousRecordingMode = false
        start(false, result)
        if (!active) {
            directoryReadV2Mode = false
            legacyHistoryDownloadMode = false
            directoryReadV2Mode = false
            directoryReadAfterChain = false
            dirReadSession = null
            fileAccessListMode = false
            syncRequestMode = false
            syncAccepted = false
            syncFrameCounts.clear()
            syncProtoShapes = ""
            syncOtherLengths = ""
            syncItemListSent = false
            syncPullSent = false
            syncItemSummary = ""
            syncPullDiag = ""
            mlrHandle = null
            mlrRegisterSent = false
            mlrDiag = ""
            mlrChunks = 0
            mlrBytes = 0
            mlrConfigureResponseSeen = false
            mlrTransferActive = false
            syncTransferHandle = 0L
            syncExpectedSize = 0L
            mlrData.reset()
            fileAccessRequestClaimed = false
            bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        }
    }

    /** Fresh bootstrap and one directory transfer plus strictly bounded supported FIT files. */
    fun startLegacyDirectoryAndFitsTest(
        supportedTypes: Set<Pair<Int, Int>>,
        result: (String) -> Unit
    ) {
        if (active) { result("ML-Test läuft bereits."); return }
        if (supportedTypes.isEmpty()) {
            result("Keine gespeicherten, bestätigten 5031-Dateitypen vorhanden; keine Verbindung und kein Request ausgeführt.")
            return
        }
        legacyHistoryDownloadMode = true
        legacySupportedTypes = supportedTypes.toSet()
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        continuousRecordingMode = false
        start(false, result)
        if (!active) {
            legacyHistoryDownloadMode = false
            directoryReadV2Mode = false
            directoryReadAfterChain = false
            dirReadSession = null
            fileAccessListMode = false
            syncRequestMode = false
            syncAccepted = false
            syncFrameCounts.clear()
            syncProtoShapes = ""
            syncOtherLengths = ""
            syncItemListSent = false
            syncPullSent = false
            syncItemSummary = ""
            syncPullDiag = ""
            mlrHandle = null
            mlrRegisterSent = false
            mlrDiag = ""
            mlrChunks = 0
            mlrBytes = 0
            mlrConfigureResponseSeen = false
            mlrTransferActive = false
            syncTransferHandle = 0L
            syncExpectedSize = 0L
            mlrData.reset()
            fileAccessRequestClaimed = false
            bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        }
    }

    /** Explicit opt-in: one minimal identity-free 5024 ACK candidate, no FileSync commands. */
    fun startBootstrapAckTest(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        bootstrapAckMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        start(false, result)
    }

    /**
     * Explicit one-shot active configuration test: 5024 ACK, then 5050 ACK
     * and a conservative 15-byte zero-capability 5050 response.
     * Never downloads files or runs from background recording.
     */
    fun startConfigurationTest(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        configurationTestMode = true
        bootstrapAckMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        start(false, result)
    }

    /**
     * Explicit foreground-only synthetic-device SYNC negotiation diagnostic.
     * Sends a single 5050 capability response advertising only bit 3.
     * Does not claim to download files; file transfer requires verified protocol.
     */
    fun startSyntheticSyncDebug(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        syntheticSyncDebugMode = true
        configurationTestMode = true
        bootstrapAckMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        start(false, result)
    }

    /** Explicit consent: retain at most 32 CRC-verified GFDI frames in the final report. */
    fun startBootstrapRawCapture(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        rawCaptureMode = true
        bootstrapAckMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        start(false, result)
    }

    /** Opt-in bounded read-only discovery; never used by overnight recording. */
    fun startMotionDiscovery(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        mousePodsMode = false
        exploratoryMode = true
        expandedMode = true
        servicePlan = GarminServiceDiscovery.CANDIDATES
        startRealtime(result)
    }

    /** Separate opt-in comparison: REGISTER(16), then 0x01 on its assigned handle. */
    fun startMousePodsMotion(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        mousePodsMode = true
        exploratoryMode = true
        expandedMode = true
        servicePlan = GarminServiceDiscovery.CANDIDATES
        startRealtime(result)
    }

    /** Recorder-only: retain the established GATT subscription until stop or disconnect. */
    fun startContinuousAllMetrics(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        continuousRecordingMode = true
        startAllMetrics(result)
    }

    fun startAllMetrics(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        mousePodsMode = false
        expandedMode = true
        startRealtime(result)
    }

    private fun nextRealtimeService(g: BluetoothGatt) {
        registrationIndex++
        if (registrationIndex < servicePlan.size) {
            pendingService = servicePlan[registrationIndex]
            mark("Registriere Garmin-Service $pendingService")
            send(g, GarminMlHandshake.registerRequest(pendingService))
        } else {
            pendingService = -1
            mark("Registrierung beendet; warte auf echte Messwerte")
        }
    }

    fun startRealtime(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        realtimeMode = true
        start(false, result)
    }
    private val notificationShapes = mutableListOf<String>()
    @Volatile private var phase = "Noch nicht gestartet"
    @Volatile private var notificationsReceived = 0
    @Volatile private var writeConfirmed = false
    private fun mark(value: String) { phase = value }

    @SuppressLint("MissingPermission")
    fun start(closeAll: Boolean, result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        val device = try {
            adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
        } catch (_: SecurityException) { result("Bluetooth-Berechtigung fehlt."); return }
        if (device == null) { result("CIRQA nicht in Android-Kopplungen gefunden."); return }
        if (!CirqaBleSessionGate.acquire(this)) { result("CIRQA Bluetooth belegt: Bitte laufende Messung abwarten und erneut versuchen."); return }
        val generation = Any()
        callbackGeneration = generation
        terminationRequested = false
        active = true
        sessionStartedNanos = System.nanoTime()
        txTrace = CirqaGfdiWriteTrace()
        selectedRxUuid = "nicht ermittelt"
        selectedTxUuid = "nicht ermittelt"
        selectedTxProperties = 0
        historyRegistered = false
        legacy5007WriteClaimed = false
        legacy5007RequestSent = false
        legacy5007Response = CirqaLegacy5007ResponseObserver()
        legacy5031WriteClaimed = false
        legacy5031RequestSent = false
        legacy5031Response = CirqaLegacy5031ResponseObserver()
        legacy5002RequestSent = false
        legacy5002RequestWriteClaimed = false
        legacyRequestedIndices.clear()
        legacy5002Response = CirqaLegacy5002ResponseObserver()
        legacy5004Response = CirqaLegacy5004ResponseObserver()
        legacyFileQueue.clear()
        legacyCurrentDirectoryEntry = null
        legacyCurrentFileIndex = null
        legacyCurrentExpectedSize = 0
        legacyCurrentAssembler = null
        legacyCurrentCrc = 0
        legacyPreviousCrc = 0
        legacyLastPayload = null
        legacyPendingCrc = null
        legacyDirectory = null
        legacyDirectoryEntriesSeen = 0
        legacyFitBytesWritten = 0
        legacyDownloadedIndices.clear()
        legacyCurrentTransferCompleted = false
        legacyHistorySessionStartedAt = 0L
        legacyHistoryReportStatus = "Noch nicht gestartet"
        legacyHistoryReportDirectoryEntries = 0
        legacyHistoryReportEligibleFiles = 0
        legacyHistoryReportSkippedFiles = 0
        legacyHistoryReportCompletedFiles = 0
        legacyHistoryReportStoredBytes = 0
        legacyHistoryDirectoryEntryDiagnostics = "keine"
        legacyHistoryOutputDirectory = null
        bootstrapAckSent = false
        configurationResponseQueued = false
        configurationAckReceived = false
        configurationObservationExtended = false
        configuration5050Baseline = 0
        configurationAckBaseline = 0
        configuration5037Baseline = 0
        configurationMalformed5037Baseline = 0
        configurationRejectedBaseline = 0
        configurationErrorBaseline = 0
        configurationNotificationBaseline = 0
        configuration5037ObservationStarted = false
        post5050Evidence = Cirqa5050PostWriteEvidence()
        passive5050Session = Cirqa5050PassiveSession()
        configurationResponseConfirmed = false
        bootstrapAckConfirmed = false
        bootstrapAckPending = false
        bootstrapAckChunks.clear()
        bootstrapBaselineFrames = 0
        mlWritePending = false
        historyTransport = GarminHistoryTransportInspector()
        fileListProbe = GarminFileListProbe()
        fileListProbe.captureRawFrames = rawCaptureMode
        historyHandle = null
        historyRouted = 0
        historyUnrouted = 0
        historyLengths.clear()
        if (closeAll) realtimeMode = false
        if (!realtimeMode) expandedMode = false
        pendingService = 0
        motionHandle = null
        motionPackets = 0
        motionStartIssued = false
        realtimeRegistry.clear()
        heartRate = null
        steps = null
        realtimeFrames = 0
        registrationIndex = 0
        if (!exploratoryMode) servicePlan = GarminRealtime.SUPPORTED_SERVICES
        exploratoryAccepted.clear()
        exploratoryFrames.clear()
        rejectedServices.clear()
        extraReadings.clear()
        notificationsReceived = 0
        notificationShapes.clear()
        writeConfirmed = false
        mark("GATT-Verbindung angefordert")
        callback = result
        requestedCloseAll = closeAll
        session = GarminMlHandshake.Session(closeAll)
        result("ML-Test: Verbinde und suche Garmin-Dienste …")
        if (bootstrapAckMode || configurationTestMode) {
            result(Cirqa5024AndroidHostEvidence.inspect(context).summary())
        }
        try {
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
                    if (callbackGeneration !== generation) { safeClose(g); return }
                    if (!active || terminationRequested) { safeClose(g); return }
                    if (status != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED) {
                        finish("ML-Verbindung beendet (GATT $status, Zustand $state). Letzte Phase: $phase; Notifications: $notificationsReceived; GATT-Schreiben bestätigt: $writeConfirmed; Protokollzustand: ${session?.stage}; Antwortformen: ${notificationShapes.joinToString(" | ")}.", g)
                    } else if (state == BluetoothProfile.STATE_CONNECTED) {
                        mark("GATT verbunden; Dienstsuche läuft")
                        try { if (!g.discoverServices()) finish("GATT-Dienstsuche konnte nicht starten.", g) }
                        catch (_: SecurityException) { finish("Bluetooth-Berechtigung fehlt.", g) }
                    }
                }
                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    if (callbackGeneration !== generation) return
                    if (!active || terminationRequested) return
                    if (status != BluetoothGatt.GATT_SUCCESS) { finish("Dienstsuche fehlgeschlagen: $status", g); return }
                    val service = g.services.orEmpty().firstOrNull {
                        it.uuid.toString().equals(GarminMlOffline.SERVICE_UUID, true)
                    }
                    if (service == null) {
                        finish("Garmin-ML-GATT-Dienst ${GarminMlOffline.SERVICE_UUID} nicht gefunden.", g)
                        return
                    }
                    val characteristics = service.characteristics.orEmpty()
                    val pairs = GarminMlOffline.characteristicPairCandidates(
                        characteristics.map { it.uuid.toString() }
                    )
                    if (pairs.isEmpty()) {
                        finish("Im Garmin-ML-Dienst keine unterstützte Daten-Characteristic (0x2810–0x2819) gefunden.", g)
                        return
                    }
                    val selected = pairs.firstNotNullOfOrNull { pair ->
                        val candidateRx = service.getCharacteristic(UUID.fromString(pair.rx))
                        val candidateTx = service.getCharacteristic(UUID.fromString(pair.tx))
                        if (candidateRx != null && candidateTx != null &&
                            candidateRx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 &&
                            candidateRx.getDescriptor(cccd) != null &&
                            candidateTx.properties and (
                                BluetoothGattCharacteristic.PROPERTY_WRITE or
                                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                                ) != 0
                        ) pair to (candidateRx to candidateTx) else null
                    }
                    if (selected == null) {
                        finish("Garmin-ML-Daten-Characteristics gefunden, aber kein Notify-/Write-kompatibler Connect-Kandidat.", g)
                        return
                    }
                    val (rx, selectedTx) = selected.second
                    tx = selectedTx
                    selectedRxUuid = rx?.uuid?.toString() ?: "nicht ermittelt"
                    selectedTxUuid = tx?.uuid?.toString() ?: "nicht ermittelt"
                    selectedTxProperties = tx?.properties ?: 0
                    mark("ML RX/TX gefunden; CCCD wird aktiviert")
                    val descriptor = rx.getDescriptor(cccd)
                    if (descriptor == null) { finish("Garmin ML CCCD fehlt; kein Handshake gesendet.", g); return }
                    try {
                        if (!g.setCharacteristicNotification(rx, true)) {
                            finish("Garmin ML Notifications konnten nicht aktiviert werden.", g); return
                        }
                        val ok = if (Build.VERSION.SDK_INT >= 33)
                            g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                        else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            g.writeDescriptor(descriptor)
                        }
                        if (!ok) finish("CCCD-Schreibauftrag abgelehnt.", g)
                    } catch (_: SecurityException) { finish("Bluetooth-Berechtigung beim CCCD fehlt.", g) }
                }
                override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                    if (callbackGeneration !== generation) return
                    if (!active || descriptor.uuid != cccd) return
                    if (status != BluetoothGatt.GATT_SUCCESS) { finish("CCCD-Fehler: $status", g); return }
                    mark("CCCD bestätigt; erster ML-Befehl wird gesendet")
                    send(g, session!!.begin())
                }
                @Deprecated("Android compatibility callback")
                override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    if (callbackGeneration !== generation) return
                    @Suppress("DEPRECATION")
                    receive(g, characteristic.value?.clone() ?: byteArrayOf())
                }
                override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    if (callbackGeneration !== generation) return
                    receive(g, value)
                }
                override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (callbackGeneration !== generation) return
                    if (!active || terminationRequested) return
                    txTrace.completePendingCallback(status)
                    val hsv1 = historicalSyncV1
                    if (hsv1 != null && hsv1WriteOutstanding) {
                        hsv1WriteOutstanding = false
                        mlWritePending = false
                        hsv1.writeCompleted(hsv1.token, hsv1Type, status == BluetoothGatt.GATT_SUCCESS,
                            android.os.SystemClock.elapsedRealtime())
                        finishHsv1IfTerminal(g)
                        return
                    }
                    val eventSession = legacy5030Only
                    if (eventSession != null && legacy5030WriteOutstanding) {
                        legacy5030WriteOutstanding = false
                        mlWritePending = false
                        eventSession.writeCompleted(eventSession.token,
                            status == BluetoothGatt.GATT_SUCCESS, android.os.SystemClock.elapsedRealtime())
                        finish5030IfTerminal(g)
                        return
                    }
                    if (status != BluetoothGatt.GATT_SUCCESS) finish("ML-Schreibfehler: $status", g)
                    else {
                        writeConfirmed = true
                        mlWritePending = false
                        drainDeferredAcks(g)
                        if (bootstrapAckPending) {
                            bootstrapAckPending = false
                            if (bootstrapAckChunks.isEmpty()) bootstrapAckConfirmed = true
                            if (configurationResponseQueued && bootstrapAckChunks.isEmpty() &&
                                !configurationResponseConfirmed) {
                                configurationResponseConfirmed = true
                                passive5050Session.writeConfirmed()
                                // ACK can arrive before the final GATT callback.
                                // Re-evaluate existing evidence at write completion.
                                if (Cirqa5050AckGate.isConfirmed(
                                        configurationResponseQueued,
                                        configurationResponseConfirmed,
                                        Cirqa5050AckEvidence.isNewAck(configurationAckBaseline, fileListProbe.responseStatuses)
                                    )) {
                                    if (!configurationAckReceived) passive5050Session.freshAck()
                                    configurationAckReceived = true
                                    if (!configuration5037ObservationStarted) {
                                        configuration5037Baseline = fileListProbe.validSynchronizationShapes
                                        configuration5037ObservationStarted = true
                                    }
                                }
                                post5050Evidence.onWriteConfirmed(fileListProbe.frameTypes)
                                // Allow a full 10 seconds after the last confirmed write.
                                // This replaces, rather than duplicates, the original timeout.
                                if (configurationTestMode && !configurationObservationExtended) {
                                    configurationObservationExtended = true
                                    main.removeCallbacks(configurationTimeout)
                                    main.postDelayed(configurationTimeout, 10_000L)
                                }
                                phase = "5050-Schreibfolge bestätigt; beobachte Antworten 10 Sekunden"
                                mark(phase)
                            }
                        }
                        if (bootstrapAckMode && bootstrapAckSent && bootstrapAckChunks.isNotEmpty()) {
                            main.postDelayed({ if (active) sendNextBootstrapAckChunk(g) }, 80L)
                        } else if (legacyHistoryDownloadMode && legacyCurrentTransferCompleted) {
                            main.post {
                                if (active && legacyHistoryDownloadMode) completeLegacyTransferAndContinue(g)
                            }
                        } else {
                            maybeSendLegacy5030AfterBootstrap(g)
                            maybeSendLegacy5007AfterBootstrap(g)
                            maybeSendFileAccessList(g)
                            maybeSendLegacy5002AfterBootstrap(g)
                            maybeAdvanceHistoricalSyncV1(g)
                            if (!legacy5007WriteClaimed) mark("ML-Schreiben bestätigt; warte auf Protokollantwort")
                        }
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE)
            if (configurationTestMode && historicalSyncV1 == null) main.postDelayed(configurationTimeout, 20_000L)
            /* Legacy timeout for non-configuration modes. */
            if (!configurationTestMode && !continuousRecordingMode) main.postDelayed({ if (active) finish(if (historyDiagnosticMode) "CIRQA Historien-Diagnose: Beobachtungsfenster beendet." else if (realtimeMode) realtimeSummary("Zeitlimit (20 Sekunden)") else "ML-Handshake: Zeitlimit (20 Sekunden). Letzte Phase: $phase; Notifications: $notificationsReceived; GATT-Schreiben bestätigt: $writeConfirmed; Protokollzustand: ${session?.stage}; Antwortformen: ${notificationShapes.joinToString(" | ")}.", gatt) }, 20_000)
        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung fehlt.", gatt) }
        catch (e: IllegalArgumentException) { finish("Verbindungsfehler: ${e.javaClass.simpleName}", gatt) }
    }

    private val configurationTimeout = Runnable {
        if (active) finish("CIRQA Historien-Diagnose: Beobachtungsfenster nach 5050-Schreibbestätigung beendet.", gatt)
    }
    private val legacy5007Timeout = Runnable {
        if (active && legacy5007TestMode) {
            finish("5007-Test: Fünf-Sekunden-Beobachtungsfenster ohne passende Antwort beendet.", gatt)
        }
    }
    private val legacyHistoryTimeout = Runnable {
        if (active && legacyHistoryDownloadMode) {
            finish("Begrenzter Verzeichnis-/FIT-Transfer nach fünf Minuten beendet.", gatt)
        }
    }

    private fun sendNextBootstrapAckChunk(g: BluetoothGatt) {
        if (!active || terminationRequested || !historyRegistered || mlWritePending || bootstrapAckChunks.isEmpty()) return
        bootstrapAckPending = true
        mark(if (configurationResponseQueued) "5050-Test: sende konfigurierte Antwortfolge" else "Bootstrap-Test: sende einmaliges DeviceInformation-ACK")
        send(g, bootstrapAckChunks.removeFirst())
    }

    /** Serializes transport ACKs (5004 chunks and 5043 protobuf chunks). */
    private val deferredAckWrites = java.util.ArrayDeque<ByteArray>()
    private var acks5004Queued = 0
    private var acks5043Queued = 0
    private var ack5004Hex = ""

    private fun queueAck(g: BluetoothGatt, bytes: ByteArray) {
        deferredAckWrites.addLast(bytes)
        drainDeferredAcks(g)
        main.postDelayed({ if (active) drainDeferredAcks(g) }, 80L)
    }

    private fun configureMlrTransfer(g: BluetoothGatt, handle: Int) {
        if (syncTransferHandle !in 1..0xffff_ffffL) {
            finish("Ungültiger MLR-Transfer-Handle; Transfer gestoppt.", g)
            return
        }
        mlrConfigureResponseSeen = false
        mlrTransferActive = false
        val transfer = syncTransferHandle
        // Connect's MLRTransportPipe configure frame:
        // [routing=handle, response-kind=0, general-status=0, transferHandle=u32LE].
        queueAck(g, byteArrayOf(
            handle.toByte(), 0, 0,
            transfer.toByte(), (transfer ushr 8).toByte(),
            (transfer ushr 16).toByte(), (transfer ushr 24).toByte()
        ))
        main.post { mark("MLR-Pipe konfiguriert: handle=$transfer") }
    }

    private fun sendNextSyncFile(g: BluetoothGatt) {
        if (!active || terminationRequested) return
        val handle = historyHandle
        if (handle == null) {
            finish("GFDI-Handle fehlt vor dem nächsten Pull.", g)
            return
        }
        val item = syncPendingItems.pollFirst()
        if (item == null) {
            requestNextSyncItemListPage(g)
            return
        }
        if (item.dataSize !in 1..CirqaVerifiedRawFileStore.MAX_FILE_BYTES) {
            syncFailedFiles++
            syncFileResults = (syncFileResults + "size-limit,").take(1200)
            sendNextSyncFile(g)
            return
        }
        syncCurrentItem = item
        syncExpectedSize = item.dataSize
        mlrData.reset()
        mlrConfigureResponseSeen = false
        mlrTransferActive = false
        val requestId = syncNextRequestId
        syncNextRequestId = if (syncNextRequestId >= 0xffff) 1 else syncNextRequestId + 1
        syncPullSent = true
        syncPullDiag = (syncPullDiag + "|pull#$requestId:size=${item.dataSize}")
            .take(1000)
        queueAck(g, CirqaFileAccessPullRequest.mlValue(handle, requestId, item))
        main.post {
            mark("FileAccess-Pull $requestId gesendet; Größe=${item.dataSize}, Typ=" +
                CirqaFileAccessItemListParser.safeTypeLabel(item, syncGdxmlStrings))
        }
    }

    private fun completeSyncFile(g: BluetoothGatt) {
        val item = syncCurrentItem ?: return
        val result = mlrValidation()
        val successful = CirqaSyncFileCompletion.isSuccessful(result)
        if (successful) syncCompletedFiles++ else syncFailedFiles++
        syncFileResults = (syncFileResults + "file${syncCompletedFiles + syncFailedFiles}[" +
            CirqaFileAccessItemListParser.safeTypeLabel(item, syncGdxmlStrings) + ":$result];")
            .take(1800)
        syncCurrentItem = null
        syncExpectedSize = 0L
        mlrData.reset()
        mlrTransferActive = false
        mlrConfigureResponseSeen = false
        mlrHandle = null
        mlrRegisterSent = false
        syncMlrRegistrationPending = false
        if (syncPendingItems.isEmpty()) requestNextSyncItemListPage(g) else sendNextSyncFile(g)
    }

    private fun requestNextSyncItemListPage(g: BluetoothGatt) {
        if (!active || terminationRequested) return
        if (syncQueuedFiles >= CirqaSyntheticDownloadLimits.MAX_FILES ||
            syncQueuedBytes >= CirqaSyntheticDownloadLimits.MAX_TOTAL_BYTES
        ) {
            syncIncompleteReason = syncIncompleteReason ?: "download-budget-exhausted"
            finish("Downloadlimit erreicht; keine Vollständigkeit behauptet.", g)
            return
        }
        if (syncListPageCount >= MAX_SYNC_ITEM_LIST_PAGES) {
            syncIncompleteReason = syncIncompleteReason ?: "item-list-page-limit"
            finish("Item-List-Seitenlimit erreicht; keine Vollständigkeit behauptet.", g)
            return
        }
        val sessionId = syncPageSessionId
        if (sessionId == null) {
            finish("Item-List vollständig; Datei-Queue abgearbeitet.", g)
            return
        }
        val handle = historyHandle ?: run {
            finish("GFDI-Handle fehlt vor der Item-List-Fortsetzung.", g)
            return
        }
        val requestId = syncNextRequestId
        syncNextRequestId = if (syncNextRequestId >= 0xffff) 1 else syncNextRequestId + 1
        syncPageSessionId = null
        syncPageNextTransactionId = null
        syncPullDiag = (syncPullDiag + "|list#$requestId:continuation=true").take(1000)
        queueAck(g, CirqaFileAccessListRequest.mlValue(
            handle, requestId, sessionId = sessionId
        ))
        main.post { mark("FileAccess-Item-List-Fortsetzung angefordert (read-only)") }
    }

    private fun appendSyncItemListPage(g: BluetoothGatt, page: CirqaFileAccessItemListParser.ItemListPage) {
        if (page.status != ITEM_LIST_SUCCESS_STATUS) {
            syncIncompleteReason = "item-list-status"
            finish("Item-List meldete Status ${page.status ?: "fehlt"}; Fortsetzung gestoppt.", g)
            return
        }
        syncListPageCount++
        syncPageSessionId = CirqaFileAccessListRequest.nextPageSessionId(page)
        syncPageNextTransactionId = page.nextTransactionId
        syncHasPaginationSession = page.sessionId != null
        syncHasNextTransactionId = page.nextTransactionId != null
        if (syncStopBeforePullMode) {
            val firstItems = syncRepeatFirstItems
            if (syncRepeatItemListsMode && firstItems == null) {
                syncRepeatFirstItems = page.items
                syncItemSummary = "firstItems=${page.items.size}; session=${page.sessionId != null}; " +
                    "nextTransaction=${page.nextTransactionId != null}"
                val handle = historyHandle ?: run {
                    finish("Erste Item-List empfangen, aber GFDI-Handle fehlt; Vergleich gestoppt.", g)
                    return
                }
                fileAccessReassembler.reset()
                val requestId = syncNextRequestId
                syncNextRequestId = if (syncNextRequestId >= 0xffff) 1 else syncNextRequestId + 1
                queueAck(g, CirqaFileAccessListRequest.mlValue(handle, requestId))
                main.post { mark("Zweite Item-List nach sync_request angefordert; kein Pull") }
                return
            }
            if (syncRepeatItemListsMode && firstItems != null) {
                val comparison = CirqaItemListRepeatability.compare(firstItems, page.items)
                syncItemSummary =
                    "erste=${comparison.firstCount}, zweite=${comparison.secondCount}, " +
                        "UID+Metadaten unverändert=${comparison.unchanged}, " +
                        "Metadaten geändert=${comparison.changedMetadata}, neu=${comparison.added}, " +
                        "fehlend=${comparison.missing}, doppelte UIDs=" +
                        "${comparison.duplicateUidsFirst}/${comparison.duplicateUidsSecond}"
                finish(
                    "Zwei Item-Listen nach sync_request verglichen; Stopp vor Datei-Pulls.",
                    g
                )
                return
            }
            syncItemSummary = "items=${page.items.size}; session=${page.sessionId != null}; " +
                "nextTransaction=${page.nextTransactionId != null}"
            finish(
                "Item-List nach sync_request empfangen; sicherer Stopp vor Datei-Pulls.",
                g
            )
            return
        }
        syncGdxmlStrings.putAll(page.gdxmlStrings)
        val maxTotal = CirqaSyntheticDownloadLimits.MAX_TOTAL_BYTES
        var queuedNow = 0
        var bytesNow = 0L
        for (item in page.items) {
            val uidKey = java.security.MessageDigest.getInstance("SHA-256")
                .digest(item.uid).joinToString("") { "%02x".format(it.toInt() and 255) }
            if (!syncSeenItemUids.add(uidKey)) {
                syncSkippedFiles++
                continue
            }
            if (syncQueuedFiles >= CirqaSyntheticDownloadLimits.MAX_FILES ||
                item.dataSize !in 1..CirqaVerifiedRawFileStore.MAX_FILE_BYTES ||
                item.dataSize > maxTotal - syncQueuedBytes
            ) {
                syncSkippedFiles++
                syncIncompleteReason = when {
                    syncQueuedFiles >= CirqaSyntheticDownloadLimits.MAX_FILES -> "file-count-budget"
                    item.dataSize !in 1..CirqaVerifiedRawFileStore.MAX_FILE_BYTES -> "file-size-out-of-range"
                    else -> "aggregate-byte-budget"
                }
                finish("Downloadbudget/Dateigrößenlimit verhindert vollständige Item-List; " +
                    "keine Vollständigkeit behauptet.", g)
                return
            }
            syncPendingItems.addLast(item)
            syncQueuedFiles++
            syncQueuedBytes += item.dataSize
            queuedNow++
            bytesNow += item.dataSize
        }
        val pageSummary = CirqaFileAccessItemListParser.redactedSummary(page.items, syncGdxmlStrings) +
            "; queued=$queuedNow; bytes=$bytesNow; session=${page.sessionId != null};" +
            "nextTransaction=${page.nextTransactionId != null}"
        syncItemSummary = (if (syncItemSummary.isEmpty()) "" else "$syncItemSummary|") +
            pageSummary
        syncItemSummary = syncItemSummary.take(1800)
        if (syncPendingItems.isEmpty()) requestNextSyncItemListPage(g) else sendNextSyncFile(g)
    }

    private fun drainDeferredAcks(g: BluetoothGatt) {
        if (mlWritePending || deferredAckWrites.isEmpty()) return
        send(g, deferredAckWrites.removeFirst())
    }

    @SuppressLint("MissingPermission")
    private fun send(g: BluetoothGatt, bytes: ByteArray) {
        if (!active || terminationRequested) return
        val char = tx ?: run { finish("ML-TX fehlt.", g); return }
        val mode = if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        if (mlWritePending) {
            finish("ML-Schreibvorgang noch aktiv; neuer Schreibauftrag verhindert.", g)
            return
        }
        mark(
            when {
                legacyHistoryDownloadMode && legacy5002RequestWriteClaimed -> "Sende genau einen 5002-Request für Index ${legacyCurrentFileIndex ?: 0}"
                legacyHistoryDownloadMode -> "Sende 5004-Daten-ACK"
                legacy5031TestMode && legacy5031WriteClaimed -> "Sende genau eine GFDI-5031-Anfrage"
                legacy5007TestMode && legacy5007WriteClaimed -> "Sende genau eine GFDI-5007-Anfrage"
                session?.stage == GarminMlHandshake.Stage.WAIT_CLOSE -> "Sende CLOSE_ALL"
                pendingService > 0 -> "Registriere Garmin-Service $pendingService"
                else -> "Sende GFDI-Registrierung"
            }
        )
        mlWritePending = true
        try {
            val ok = if (Build.VERSION.SDK_INT >= 33)
                g.writeCharacteristic(char, bytes, mode) == BluetoothStatusCodes.SUCCESS
            else {
                char.writeType = mode
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                g.writeCharacteristic(char)
            }
            if (!ok) {
                mlWritePending = false
                finish("ML-Schreibauftrag abgelehnt.", g)
            } else {
                txTrace.record(
                    bytes,
                    if (mode == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                        "WITHOUT_RESPONSE (API-Auftrag angenommen)"
                    else "WITH_RESPONSE (API-Auftrag angenommen)",
                    (System.nanoTime() - sessionStartedNanos) / 1_000_000
                )
                if (legacy5007TestMode && legacy5007WriteClaimed && !legacyHistoryDownloadMode) {
                    legacy5007RequestSent = true
                    main.removeCallbacks(legacy5007Timeout)
                    main.postDelayed(legacy5007Timeout, 5_000L)
                }
                if (legacy5031TestMode && legacy5031WriteClaimed && !legacyHistoryDownloadMode) {
                    legacy5031RequestSent = true
                    main.removeCallbacks(legacy5007Timeout)
                    main.postDelayed(legacy5007Timeout, 5_000L)
                }
                if (legacyHistoryDownloadMode && legacy5002RequestWriteClaimed) {
                    legacy5002RequestSent = true
                    legacy5002RequestWriteClaimed = false
                    legacyCurrentFileIndex?.let(legacyRequestedIndices::add)
                }
            }
        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung beim Schreiben fehlt.", g) }
    }

    private fun receive(g: BluetoothGatt, bytes: ByteArray) {
        if (!active || terminationRequested) return
        notificationsReceived++
        if (configurationTestMode && configurationResponseQueued &&
            Cirqa5050NotificationFloodGuard.shouldAbort(
                configurationNotificationBaseline, notificationsReceived
            )) {
            finish("Sicherheitsabbruch: zu viele BLE-Notifications nach 5050.", g)
            return
        }
        if (historyDiagnosticMode) {
            historyTransport.observe(bytes)
            val registration = GarminMlOffline.inspectRegisterResponse(bytes)
            if (registration?.accepted == true && registration.serviceId == MLR_SERVICE_ID) {
                mlrHandle = registration.handle
                syncMlrRegistrationPending = false
                mlrDiag = "mlrService=$MLR_SERVICE_ID,handle=${registration.handle}"
                configureMlrTransfer(g, registration.handle)
            } else if (registration?.serviceId == MLR_SERVICE_ID && syncRequestMode) {
                finish("MLR-Service-Registrierung abgelehnt; weitere Transfers gestoppt.", g)
                return
            } else if (registration?.serviceId == GarminMlHandshake.GFDI_SERVICE && registration.accepted) {
                historyHandle = registration.handle
                // The dynamic handle only exists after the ML register response;
                // trigger the one-shot FileAccess request here, not from earlier writes.
                maybeSendFileAccessList(g)
            } else {
                val first = bytes.firstOrNull()?.toInt()?.and(255)
                if (first != null && first == mlrHandle && syncRequestMode) {
                    // Connect's MLRTransportPipe consumes the first MLR callback
                    // as [response-kind, general-status, configure-status], not
                    // as file data. Never append this control response to the file.
                    val payload = bytes.copyOfRange(1, bytes.size)
                    if (!mlrConfigureResponseSeen) {
                        if (!CirqaMlrConfigureResponse.isSuccessful(payload)) {
                            mlrDiag += ";configureResponse=invalid"
                            finish("MLR-Konfigurationsantwort ungültig; Transfer gestoppt.", g)
                            return
                        }
                        mlrConfigureResponseSeen = true
                        mlrTransferActive = true
                        mlrDiag += ";configureResponse=success"
                        return
                    }
                    if (!mlrTransferActive || syncCurrentItem == null) return
                    // Subsequent callbacks are raw transfer bytes, passed
                    // unchanged by Connect's MLRTransportPipe to its data sink.
                    mlrChunks++
                    mlrBytes += payload.size
                    if (syncExpectedSize <= 0 ||
                        mlrData.size().toLong() + payload.size > syncExpectedSize
                    ) {
                        mlrDiag += ";dataOverflow=true"
                        finish("MLR-Daten überschreiten die erwartete Dateigröße; Transfer gestoppt.", g)
                        return
                    }
                    mlrData.write(payload)
                    if (mlrData.size().toLong() == syncExpectedSize) {
                        completeSyncFile(g)
                    }
                    return
                }
                if (first != null && first == historyHandle) {
                    historyRouted++
                    val hsv1 = historicalSyncV1
                    if (hsv1 != null && hsv1.phase in setOf(
                            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5030_RESPONSE,
                            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5007_RESPONSE,
                            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5031_RESPONSE,
                            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_DIR_RESPONSE)) {
                        hsv1.receive(hsv1.token, bytes.copyOfRange(1, bytes.size),
                            android.os.SystemClock.elapsedRealtime())
                        val nextPhase = hsv1.phase
                        if (nextPhase in setOf(
                                CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5007_WRITE,
                                CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5031_WRITE,
                                CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_DIR_WRITE)) {
                            claimAndSendHsv1(g, hsv1, nextPhase)
                        }
                        finishHsv1IfTerminal(g)
                        return
                    }
                    val eventSession = legacy5030Only
                    if (eventSession?.phase == CirqaLegacy5030OnlySession.Phase.WAITING) {
                        eventSession.receive(eventSession.token, bytes.copyOfRange(1, bytes.size),
                            android.os.SystemClock.elapsedRealtime())
                        finish5030IfTerminal(g)
                        return
                    }
                    if (fileAccessListMode) {
                        handleFileAccessResponse(g, bytes.copyOfRange(1, bytes.size))
                        return
                    }
                    // Full-chain directory-body read: route 5004 chunks to the dedicated session.
                    dirReadSession?.let { d ->
                        val beforeDataFrames = d.dataFramesProcessed
                        val result = d.onNotification(bytes.copyOfRange(1, bytes.size))
                        val pbAcks = d.drainProtobufAcks()
                        val handle = historyHandle
                        if (handle == null) { finish("Directory-Body: Handle fehlt.", g); return }
                        // Connect acknowledges every 5043 transport chunk with a 5000 ACK
                        // (responseTo=5043). This is a transport ACK only, not a 5044 answer
                        // and not a device mutation. Explicitly released for this bounded run.
                        for ((requestId, offset) in pbAcks) {
                            acks5043Queued++
                            queueAck(g, CirqaLegacy5043AckCandidate.mlGattValueCandidate(handle, requestId, offset))
                        }
                        when (result) {
                            CirqaDirectoryReadV2Session.Result.ACCEPTED,
                            CirqaDirectoryReadV2Session.Result.DUPLICATE -> {
                                // Control-only notifications need no 5004 ACK. Re-ACK only for a
                                // real 5004 data frame (accepted or retry).
                                if (d.dataFramesProcessed != beforeDataFrames) {
                                    val ackOffset = d.nextAckOffset() ?: return
                                    acks5004Queued++
                                    val ack = CirqaLegacy5004AckCandidate.mlGattValueCandidate(handle, ackOffset)
                                    if (ack5004Hex.isEmpty()) ack5004Hex =
                                        ack.joinToString("") { "%02x".format(it) }
                                    queueAck(g, ack)
                                }
                                return
                            }
                            CirqaDirectoryReadV2Session.Result.COMPLETE -> {
                                val report = d.summary()
                                val dir = d.completedBytes()?.let(CirqaLegacyDirectoryParser::parseForDownload)
                                val detail = dir?.let { parsed ->
                                    val rows = parsed.entries.take(24).map { e ->
                                        "idx=${e.fileIndex},type=${e.type},sub=${e.subtype},read=${e.readable}," +
                                            "flags=0x${e.attributes.toString(16).padStart(2, '0')},size=${e.size}"
                                    }
                                    "entries=${parsed.entries.size}; [${rows.joinToString("; ")}]" +
                                        if (parsed.entries.size > 24) "; more=${parsed.entries.size - 24}" else ""
                                } ?: "directory-parse-failed"
                                finish("Directory vollständig gelesen (read-only). $report; $detail", g)
                                return
                            }
                            else -> {
                                finish("Directory-Body abgebrochen: ${d.summary()}", g)
                                return
                            }
                        }
                    }
                    if (legacyHistoryDownloadMode && legacy5002RequestSent) {
                        receiveLegacyDirectoryOrFitData(g, bytes.copyOfRange(1, bytes.size))
                        return
                    }
                    if (legacy5007TestMode && legacy5007WriteClaimed) {
                        if (legacy5031TestMode && legacy5031RequestSent) {
                            when (legacy5031Response.observeMlPayload(bytes.copyOfRange(1, bytes.size))) {
                                CirqaLegacy5031ResponseObserver.Result.PENDING -> Unit
                                CirqaLegacy5031ResponseObserver.Result.SUPPORTED ->
                                    finish("5031-Antwort CRC-validiert; Status 0; Dateitypen=${legacy5031Response.fileTypes?.size ?: 0}. Stopp vor 5002.", g)
                                CirqaLegacy5031ResponseObserver.Result.NOT_SUPPORTED ->
                                    finish("5031-Antwort CRC-validiert; negativer Status ${legacy5031Response.responseStatus}. Stopp vor 5002.", g)
                                CirqaLegacy5031ResponseObserver.Result.UNEXPECTED ->
                                    finish("5031-Test abgebrochen: unerwarteter GFDI-Antworttyp oder mehrere Frames. Stopp vor 5002.", g)
                                CirqaLegacy5031ResponseObserver.Result.MALFORMED ->
                                    finish("5031-Test abgebrochen: Antwort-COBS/GFDI/CRC/Dateitypen ungültig. Stopp vor 5002.", g)
                            }
                            return
                        }
                        when (legacy5007Response.observeMlPayload(bytes.copyOfRange(1, bytes.size))) {
                            CirqaLegacy5007ResponseObserver.Result.PENDING -> Unit
                            CirqaLegacy5007ResponseObserver.Result.SUPPORTED -> {
                                if (legacy5031TestMode) {
                                    val handle = historyHandle
                                    if (handle == null || legacy5031WriteClaimed || mlWritePending) {
                                        finish("5007 bestätigt, aber 5031 nicht gesendet: Handle fehlt oder Schreibpfad belegt. Stopp vor 5002.", g)
                                    } else {
                                        legacy5031WriteClaimed = true
                                        mark("5007 Status 0 bestätigt; sende genau einen leeren 5031-Request")
                                        send(g, CirqaLegacy5031RequestCandidate.mlGattValueCandidate(handle))
                                    }
                                } else {
                                    finish("5007-Antwort strukturell passend; Status 0 (Connect-Vertrag: unterstützt, CIRQA-Kompatibilität beobachtet).", g)
                                }
                            }
                            CirqaLegacy5007ResponseObserver.Result.NOT_SUPPORTED ->
                                finish("5007-Antwort strukturell passend; negativer Status ${legacy5007Response.responseStatus}.", g)
                            CirqaLegacy5007ResponseObserver.Result.UNEXPECTED ->
                                finish("5007-Test abgebrochen: unerwarteter GFDI-Antworttyp oder mehrere Frames.", g)
                            CirqaLegacy5007ResponseObserver.Result.MALFORMED ->
                                finish("5007-Test abgebrochen: Antwort-COBS/GFDI/CRC ungültig.", g)
                        }
                        return
                    }
                    fileListProbe.observeMlPayload(bytes.copyOfRange(1, bytes.size))
                    if (configurationTestMode) post5050Evidence.observe(fileListProbe.frameTypes)
                    if (configurationTestMode && configurationResponseQueued &&
                        Cirqa5050Malformed5037Guard.shouldAbort(
                            configurationMalformed5037Baseline, fileListProbe.malformedSynchronizationShapes
                        )) {
                        finish("Sicherheitsabbruch: ungültige 5037-Synchronisationsstruktur nach 5050.", g)
                        return
                    }
                    if (configurationTestMode && configurationResponseQueued &&
                        Cirqa5050MalformedFrameGuard.shouldAbort(
                            configurationRejectedBaseline, fileListProbe.rejectedFrames
                        )) {
                        finish("Sicherheitsabbruch: mindestens drei neue ungültige GFDI-Frames nach 5050.", g)
                        return
                    }
                    // A negative protocol response must stop the existing zero-mask
                    // configuration diagnostic immediately, not only FileList probes.
                    // This does not enable the experimental SYNC capability.
                    if (configurationTestMode &&
                        ((!configurationResponseQueued && fileListProbe.responseErrors > 0) ||
                            (configurationResponseQueued &&
                                Cirqa5050NegativeResponseGuard.shouldAbort(
                                    configurationErrorBaseline, fileListProbe.responseErrors
                                )))) {
                        finish("Sicherheitsabbruch: negative GFDI-Antwort während 5050-Diagnose.", g)
                        return
                    }
                    // A second device 5050 after our response was queued is an
                    // unverified retry; stop instead of negotiating repeatedly.
                    if (configurationTestMode && configurationResponseQueued &&
                        Cirqa5050RetryGuard.hasRetry(configuration5050Baseline, fileListProbe.frameTypes["gfdi-5050"] ?: 0)) {
                        finish("Sicherheitsabbruch: erneute 5050-Anfrage nach geplanter Antwort.", g)
                        return
                    }
                    if (configurationTestMode && configuration5037ObservationStarted &&
                        Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(
                            configuration5037Baseline, fileListProbe.validSynchronizationShapes,
                            configurationResponseConfirmed, configurationAckReceived
                        )) passive5050Session.valid5037()
                    if (configurationTestMode && Cirqa5050AckGate.isConfirmed(
                            configurationResponseQueued,
                            configurationResponseConfirmed,
                            Cirqa5050AckEvidence.isNewAck(configurationAckBaseline, fileListProbe.responseStatuses)
                        )) {
                        if (!configurationAckReceived) passive5050Session.freshAck()
                        configurationAckReceived = true
                        if (!configuration5037ObservationStarted) {
                            configuration5037Baseline = fileListProbe.validSynchronizationShapes
                            configuration5037ObservationStarted = true
                        }
                        phase = "GFDI-5000-ACK für 5050 empfangen; beobachte weitere Antworten"
                        maybeSendLegacy5030AfterBootstrap(g)
                        maybeSendLegacy5007AfterBootstrap(g)
                        maybeSendFileAccessList(g)
                        maybeSendLegacy5002AfterBootstrap(g)
                        maybeAdvanceHistoricalSyncV1(g)
                    }
                    if (bootstrapAckMode && historyRegistered && !bootstrapAckSent &&
                        fileListProbe.bootstrap.deviceInformationFrames > 0 &&
                        GarminDeviceInformationAckCandidate.validCandidate()) {
                        val handle = historyHandle
                        if (handle != null) {
                            bootstrapAckSent = true
                            fileListProbe.ackAttempted = true
                            bootstrapBaselineFrames = fileListProbe.frames
                            val framed = GarminFileListRequest.cobs(GarminDeviceInformationAckCandidate.frame())
                            framed.toList().chunked(19).forEach { part ->
                                bootstrapAckChunks.addLast(byteArrayOf(handle.toByte()) + part.toByteArray())
                            }
                            main.post { if (active) sendNextBootstrapAckChunk(g) }
                        }
                    }
                    if (configurationTestMode && !configurationResponseQueued &&
                        bootstrapAckConfirmed && !bootstrapAckPending && bootstrapAckChunks.isEmpty() &&
                        (fileListProbe.frameTypes["gfdi-5050"] ?: 0) > 0) {
                        configuration5050Baseline = fileListProbe.frameTypes["gfdi-5050"] ?: 0
                        configurationAckBaseline = Cirqa5050AckEvidence.count(fileListProbe.responseStatuses)
                        configurationMalformed5037Baseline = fileListProbe.malformedSynchronizationShapes
                        configurationRejectedBaseline = fileListProbe.rejectedFrames
                        configurationErrorBaseline = fileListProbe.responseErrors
                        configurationNotificationBaseline = notificationsReceived
                        configurationResponseQueued = true
                        passive5050Session.queued()
                        val handle = historyHandle
                        if (handle != null) {
                            val candidates = listOf(
                                CirqaOfflineGfdiStatusCandidate.ackFrame(5050),
                                Cirqa5050OfflineHostCapabilities.offlineFrame(
                                    advertiseSyncForOfflineComparison = syntheticSyncDebugMode, byteCount = 15
                                )
                            )
                            for (candidate in candidates) {
                                GarminFileListRequest.cobs(candidate).toList().chunked(19).forEach { part ->
                                    bootstrapAckChunks.addLast(byteArrayOf(handle.toByte()) + part.toByteArray())
                                }
                            }
                            mark("5050 empfangen; sende einmalig ACK und konservative 15-Byte-Hostmaske")
                            main.post { if (active) sendNextBootstrapAckChunk(g) }
                        }
                    }
                    // Fail closed on repetitive protocol traffic: do not let a read-only
                    // diagnostic keep the device in an unacknowledged response loop.
                } else {
                    historyUnrouted++
                    if (legacy5007TestMode) {
                        finish("5007-Test abgebrochen: unerwartete ML-Control-Nachricht oder dynamischer Handle; 5007 nur nach abgeschlossenem Bootstrap.", g)
                        return
                    }
                }
                historyLengths.record(bytes.size)
            }
        }
        if (notificationShapes.size < 12) {
            val first = bytes.firstOrNull()?.toInt()?.and(255)
            val second = bytes.getOrNull(1)?.toInt()?.and(255)
            val control = first == 0
            val parsed = GarminMlOffline.inspectRegisterResponse(bytes)
            notificationShapes.add("L=${bytes.size}, control=$control, type=${if (control) second else "-"}, reg=${if (parsed == null) "nein" else "service:${parsed.serviceId}/status:${parsed.status}/handle:${parsed.handle}"}")
        }
        mark("ML-Notification empfangen; Protokollantwort wird geprüft")
        if (realtimeMode && pendingService != 0) {
            val response = GarminMlOffline.inspectRegisterResponse(bytes)
            if (response != null && response.serviceId == pendingService) {
                if (!response.accepted) {
                    if (!expandedMode) {
                        finish(realtimeSummary("Service $pendingService abgelehnt (Status ${response.status})"), g)
                        return
                    }
                    rejectedServices.add(pendingService)
                } else {
                    if (exploratoryMode) {
                        exploratoryAccepted[pendingService] = response.handle
                        if (mousePodsMode && pendingService == 16) motionHandle = response.handle
                    }
                    else realtimeRegistry.register(pendingService, response.handle)
                }
                if (expandedMode) {
                    if (mousePodsMode && pendingService == 16 && response.accepted) {
                        // MousePods' documented service-specific start byte, framed by the dynamic ML handle.
                        val handle = motionHandle!!
                        motionStartIssued = true
                        mark("MousePods: Service 16 registriert; sende Start 0x01")
                        send(g, byteArrayOf(handle.toByte(), 0x01))
                    }
                    nextRealtimeService(g)
                }
                else if (pendingService == GarminRealtime.HEART_RATE_SERVICE) {
                    pendingService = GarminRealtime.STEPS_SERVICE
                    mark("Puls registriert; registriere Schritte")
                    send(g, GarminMlHandshake.registerRequest(pendingService))
                } else {
                    pendingService = -1
                    mark("Puls und Schritte registriert; warte auf Messwerte")
                }
                return
            }
            if (exploratoryMode) {
                val routed = GarminRealtime.route(bytes)
                val id = exploratoryAccepted.entries.firstOrNull { it.value == routed?.handle }?.key
                if (id != null) {
                    exploratoryFrames[id] = (exploratoryFrames[id] ?: 0) + 1
                    if (id == 16) motionPackets++
                }
                return
            }
            val reading = realtimeRegistry.receive(bytes)
            if (reading != null) {
                realtimeFrames++
                val serviceCode = when (reading) {
                    is GarminRealtime.Reading.HeartRate -> GarminRealtime.HEART_RATE_SERVICE
                    is GarminRealtime.Reading.Steps -> GarminRealtime.STEPS_SERVICE
                    is GarminRealtime.Reading.Calories -> GarminRealtime.CALORIES_SERVICE
                    is GarminRealtime.Reading.Intensity -> GarminRealtime.INTENSITY_SERVICE
                    is GarminRealtime.Reading.Hrv -> GarminRealtime.HRV_SERVICE
                    is GarminRealtime.Reading.Stress -> GarminRealtime.STRESS_SERVICE
                    is GarminRealtime.Reading.SpO2 -> GarminRealtime.SPO2_SERVICE
                    is GarminRealtime.Reading.BodyBattery -> GarminRealtime.BODY_BATTERY_SERVICE
                    is GarminRealtime.Reading.Respiration -> GarminRealtime.RESPIRATION_SERVICE
                }
                onReading?.invoke(serviceCode, reading)
                when (reading) {
                    is GarminRealtime.Reading.HeartRate -> heartRate = reading
                    is GarminRealtime.Reading.Steps -> steps = reading
                    else -> {
                        extraReadings[when (reading) {
                            is GarminRealtime.Reading.SpO2 -> GarminRealtime.SPO2_SERVICE
                            is GarminRealtime.Reading.Hrv -> GarminRealtime.HRV_SERVICE
                            is GarminRealtime.Reading.Stress -> GarminRealtime.STRESS_SERVICE
                            is GarminRealtime.Reading.BodyBattery -> GarminRealtime.BODY_BATTERY_SERVICE
                            is GarminRealtime.Reading.Respiration -> GarminRealtime.RESPIRATION_SERVICE
                            is GarminRealtime.Reading.Calories -> GarminRealtime.CALORIES_SERVICE
                            is GarminRealtime.Reading.Intensity -> GarminRealtime.INTENSITY_SERVICE
                            else -> -1
                        }] = reading
                    }
                }
                if (!expandedMode && heartRate != null && steps != null)
                    finish(realtimeSummary("Beide Messwerte empfangen"), g)
            }
            return
        }
        val state = session ?: return
        val next = state.onNotification(bytes)
        if (next != null) { mark("CLOSE_ALL bestätigt; sende GFDI-Registrierung"); send(g, next); return }
        when (state.stage) {
            GarminMlHandshake.Stage.COMPLETE -> {
                if (realtimeMode) {
                    pendingService = if (expandedMode) servicePlan.first() else GarminRealtime.HEART_RATE_SERVICE
                    mark("GFDI registriert; registriere Service $pendingService")
                    send(g, GarminMlHandshake.registerRequest(pendingService))
                } else if (historyDiagnosticMode && !historyRegistered) {
                    historyRegistered = true
                    if (legacy5007TestMode && !bootstrapAckMode) {
                        val handle = historyHandle
                        if (handle == null || legacy5007WriteClaimed) {
                            finish("5007-Test abgebrochen: dynamischer GFDI-Handle fehlt oder Request bereits gesendet.", g)
                        } else {
                            legacy5007WriteClaimed = true
                            send(g, CirqaLegacy5007RequestCandidate.mlGattValueCandidate(handle))
                        }
                    } else {
                        mark(if (bootstrapAckMode && legacy5007TestMode) {
                            "GFDI registriert; warte auf 5024/5050-Handshake vor 5007"
                        } else if (bootstrapAckMode) {
                            "GFDI registriert; warte auf DeviceInformation für einmaliges ACK"
                        } else "GFDI registriert; beobachte Metadaten bis zum Zeitlimit")
                    }
                } else if (!historyDiagnosticMode) finish("Garmin ML: GFDI-Service erfolgreich registriert! ${if (requestedCloseAll) "CLOSE_ALL wurde zuvor bestätigt." else "Ohne CLOSE_ALL."} Verbindung beendet; keine Gesundheitsdaten gelesen.", g)
            }
            GarminMlHandshake.Stage.FAILED ->
                finish("Garmin ML: GFDI-Registrierung vom Gerät abgelehnt.", g)
            else -> Unit
        }
    }

    private fun maybeSendLegacy5030AfterBootstrap(g: BluetoothGatt) {
        val model = legacy5030Only ?: return
        if (model.phase != CirqaLegacy5030OnlySession.Phase.BOOTSTRAP) return
        val ready = CirqaLegacy5007BootstrapGate.maySend5007(
            bootstrapAckSent, bootstrapAckConfirmed, configurationResponseQueued,
            configurationResponseConfirmed, configurationAckReceived, mlWritePending,
            bootstrapAckChunks.isNotEmpty()
        )
        val now = android.os.SystemClock.elapsedRealtime()
        if (!model.bootstrapReady(model.token, ready, now)) {
            finish5030IfTerminal(g)
            return
        }
        val handle = historyHandle
        if (handle == null || handle !in 1..127 || mlWritePending) {
            model.abort(model.token)
            finish("5030-only: kein eindeutiger sendefreier GFDI-Transport.", g)
            return
        }
        if (!model.claimRequest(model.token, now)) {
            finish5030IfTerminal(g)
            return
        }
        main.removeCallbacks(configurationTimeout)
        legacy5030WriteOutstanding = true
        val value = byteArrayOf(handle.toByte()) +
            GarminFileListRequest.cobs(CirqaLegacy5030RequestCandidate.gfdiFrame())
        main.postDelayed({
            if (active && legacy5030Only === model) {
                model.tick(model.token, android.os.SystemClock.elapsedRealtime())
                finish("5030-only: Antwort-/Write-Zeitlimit, kein Retry.", g)
            }
        }, CirqaLegacy5030OnlySession.RESPONSE_MILLIS)
        send(g, value)
    }

    private fun maybeAdvanceHistoricalSyncV1(g: BluetoothGatt) {
        val s = historicalSyncV1 ?: return
        val now = android.os.SystemClock.elapsedRealtime()
        val ready = CirqaLegacy5007BootstrapGate.maySend5007(
            bootstrapAckSent, bootstrapAckConfirmed, configurationResponseQueued,
            configurationResponseConfirmed, configurationAckReceived, mlWritePending,
            bootstrapAckChunks.isNotEmpty())
        // Capture the phase BEFORE claim: claim5030() advances the state to
        // WAITING_5030_RESPONSE, but sendHsv1Frame needs the request phase.
        val requestPhase = s.phase
        when (requestPhase) {
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.BOOTSTRAP -> {
                if (!s.bootstrapReady(s.token, ready, now)) { finishHsv1IfTerminal(g); return }
                if (!s.claim5030(s.token, now)) { finishHsv1IfTerminal(g); return }
            }
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5030_WRITE -> {
                if (!s.claim5030(s.token, now)) { finishHsv1IfTerminal(g); return }
            }
            // Already awaiting a response (e.g. a second trigger from the write callback)
            // or a later phase -> nothing to send now.
            else -> return
        }
        sendHsv1Frame(g, CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5030_WRITE)
    }

    private fun sendHsv1Frame(g: BluetoothGatt, phase: CirqaHistoricalSyncV1MultiPhaseSession.Phase) {
        val s = historicalSyncV1 ?: return
        val handle = historyHandle
        if (handle == null || handle !in 1..127 || mlWritePending) {
            s.abort(s.token); finish("Historical Sync V1: Transport nicht bereit.", g); return
        }
        val (frame, type) = when (phase) {
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5030_WRITE ->
                CirqaHistoricalSyncV1Wire.systemEventComplete(handle) to 5030
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5007_WRITE ->
                CirqaHistoricalSyncV1Wire.filter(handle, CirqaHistoricalSyncV1Wire.DIRECTORY_FILTER) to 5007
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5031_WRITE ->
                CirqaHistoricalSyncV1Wire.supportedFileTypes(handle) to 5031
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_DIR_WRITE ->
                CirqaHistoricalSyncV1Wire.directoryRead(handle, 0) to 5002
            else -> { finish("Historical Sync V1: unerwartete Sendephase $phase.", g); return }
        }
        main.removeCallbacks(configurationTimeout)
        hsv1WriteOutstanding = true
        hsv1Type = type
        main.postDelayed({
            if (active && historicalSyncV1 === s) {
                s.tick(s.token, android.os.SystemClock.elapsedRealtime())
                finish("Historical Sync V1: Antwort-Timeout, kein Retry.", g)
            }
        }, 5_000L)
        // The preceding 5050 write is already confirmed at this point; clear the
        // generic pending flag so send() cannot reject this request as overlapping.
        mlWritePending = false
        send(g, frame)
        // With WRITE_TYPE_NO_RESPONSE Android delivers no onCharacteristicWrite callback.
        // Treat the accepted API call as write-confirmed, exactly like the working 5030-only path.
        if (tx?.properties?.and(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            hsv1WriteOutstanding = false
            s.writeCompleted(s.token, type, true, android.os.SystemClock.elapsedRealtime())
            finishHsv1IfTerminal(g)
        }
    }

    private fun startDirectoryBodyRead(g: BluetoothGatt, size: Int): Boolean {
        val session = CirqaDirectoryReadV2Session()
        if (!session.begin(size)) return false
        dirReadSession = session
        acks5004Queued = 0
        acks5043Queued = 0
        ack5004Hex = ""
        deferredAckWrites.clear()
        configurationTestMode = false
        // Cancel the stale 5 s response timer from the 5002 request; the body stream
        // has its own 20 s budget below.
        historicalSyncV1 = null
        main.removeCallbacks(configurationTimeout)
        main.postDelayed({
            if (active && dirReadSession === session && session.phase == CirqaDirectoryReadV2Session.Phase.RECEIVING) {
                finish("Directory-Body Zeitlimit; kein Retry. ${session.summary()}", g)
            }
        }, 20_000L)
        mark("Directory-Body: warte auf CRC-validierte 5004-Chunks (${size} B)")
        return true
    }

    /** One Connect-style 5043 sync_request after a fresh bootstrap. */
    @SuppressLint("MissingPermission")
    fun startSyncRequestTest(
        userConfirmed: Boolean,
        stopBeforePull: Boolean = false,
        repeatItemLists: Boolean = false,
        result: (String) -> Unit
    ) = startSyncRequest(
        userConfirmed = userConfirmed,
        stopBeforePull = stopBeforePull,
        repeatItemLists = repeatItemLists,
        requireDeviceTestGate = true,
        result = result
    )

    /** User-initiated read-only historical file download for the product UI. */
    @SuppressLint("MissingPermission")
    fun startHistoricalDownload(
        userConfirmed: Boolean,
        result: (String) -> Unit
    ) = startSyncRequest(
        userConfirmed = userConfirmed,
        stopBeforePull = false,
        repeatItemLists = false,
        requireDeviceTestGate = false,
        result = result
    )

    @SuppressLint("MissingPermission")
    private fun startSyncRequest(
        userConfirmed: Boolean,
        stopBeforePull: Boolean,
        repeatItemLists: Boolean,
        requireDeviceTestGate: Boolean,
        result: (String) -> Unit
    ) {
        if ((requireDeviceTestGate &&
                ((!stopBeforePull && !CirqaSyncAttemptGate.RELEASED_FOR_DEVICE_TEST) ||
                    (stopBeforePull && !CirqaSyncItemListNoPullAttemptGate.RELEASED_FOR_DEVICE_TEST))) ||
            !userConfirmed
        ) {
            result("Historischer Sync nicht freigegeben/bestätigt; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || historicalSyncV1 != null || dirReadSession != null ||
            directoryReadAfterChain || directoryReadV2Mode || legacy5030Only != null ||
            legacy5007TestMode || legacy5031TestMode || legacyHistoryDownloadMode ||
            realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("Sync-Request benötigt eine neue Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("Sync-Request: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt.")
            return
        }
        if (requireDeviceTestGate) {
            val prefs = if (stopBeforePull) {
                context.getSharedPreferences(
                    CirqaSyncItemListNoPullAttemptGate.PREFS_NAME, Context.MODE_PRIVATE
                )
            } else {
                context.getSharedPreferences(CirqaSyncAttemptGate.PREFS_NAME, Context.MODE_PRIVATE)
            }
            val consumed = if (stopBeforePull) {
                CirqaSyncItemListNoPullAttemptGate.consume(prefs)
            } else {
                CirqaSyncAttemptGate.consume(prefs)
            }
            if (!consumed) {
                result("Sync-Request: Versuchslimit verbraucht oder Persistierung fehlgeschlagen.")
                return
            }
        }
        syncRequestMode = true
        syncStopBeforePullMode = stopBeforePull
        syncRepeatItemListsMode = stopBeforePull && repeatItemLists
        syncRepeatFirstItems = null
        syncAccepted = false
        syncProtoShapes = ""
        syncOtherLengths = ""
        syncFinalReport = ""
        syncItemListSent = false
        syncPullSent = false
        syncItemSummary = ""
        syncPullDiag = ""
        syncPendingItems.clear()
        syncCurrentItem = null
        syncNextRequestId = 3
        syncCompletedFiles = 0
        syncFailedFiles = 0
        syncFileResults = ""
        syncHasPaginationSession = false
        syncHasNextTransactionId = false
        syncPageSessionId = null
        syncPageNextTransactionId = null
        syncListPageCount = 0
        syncQueuedFiles = 0
        syncQueuedBytes = 0L
        syncSkippedFiles = 0
        syncIncompleteReason = null
        syncGdxmlStrings.clear()
        syncSeenItemUids.clear()
        mlrHandle = null
        mlrRegisterSent = false
        syncMlrRegistrationPending = false
        syncFrameCounts.clear()
        fileAccessListMode = true
        fileAccessRequestClaimed = false
        fileAccessDiag = ""
        fileAccessFramer.reset()
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        start(false, result)
        if (!active) {
            syncRequestMode = false
            syncStopBeforePullMode = false
            syncRepeatItemListsMode = false
            syncRepeatFirstItems = null
            fileAccessListMode = false
            bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        } else {
            main.postDelayed({
                if (active && fileAccessListMode) {
                    // finish() builds the single canonical report (including validation).
                    finish("${if (stopBeforePull) "40-s" else "120-s"}-Zeitlimit; $fileAccessDiag", gatt)
                }
            }, if (stopBeforePull) 40_000L else CirqaSyntheticDownloadLimits.MAX_SESSION_MILLIS)
        }
    }

    /** One read-only 5043 FileAccess item-list request after a fresh bootstrap. */
    @SuppressLint("MissingPermission")
    fun startFileAccessListTest(userConfirmed: Boolean, result: (String) -> Unit) {
        if (!CirqaFileAccessAttemptGate.RELEASED_FOR_DEVICE_TEST || !userConfirmed) {
            result("FileAccess-Liste gesperrt; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || historicalSyncV1 != null || dirReadSession != null ||
            directoryReadAfterChain || directoryReadV2Mode || legacy5030Only != null ||
            legacy5007TestMode || legacy5031TestMode || legacyHistoryDownloadMode ||
            realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("FileAccess-Liste benötigt eine neue Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("FileAccess-Liste: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt.")
            return
        }
        val prefs = context.getSharedPreferences(CirqaFileAccessAttemptGate.PREFS_NAME, Context.MODE_PRIVATE)
        if (!CirqaFileAccessAttemptGate.consume(prefs)) {
            result("FileAccess-Liste: Gate verbraucht oder Persistierung fehlgeschlagen.")
            return
        }
        fileAccessListMode = true
        fileAccessRequestClaimed = false
        fileAccessDiag = ""
        fileAccessFramer.reset()
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        start(false, result)
        if (!active) {
            fileAccessListMode = false
            bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        } else {
            main.postDelayed({
                if (active && fileAccessListMode) {
                    finish("FileAccess-Liste: 20-s-Zeitlimit ohne vollständige 5044-Antwort.$fileAccessDiag", gatt)
                }
            }, 20_000L)
        }
    }

    /** Two read-only item-list requests in one session; never pulls file bytes. */
    @SuppressLint("MissingPermission")
    fun startItemListRepeatabilityTest(userConfirmed: Boolean, result: (String) -> Unit) {
        if (!CirqaItemListRepeatabilityAttemptGate.RELEASED_FOR_DEVICE_TEST || !userConfirmed) {
            result("Item-List-Wiederholbarkeit gesperrt; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || historicalSyncV1 != null || dirReadSession != null ||
            directoryReadAfterChain || directoryReadV2Mode || legacy5030Only != null ||
            legacy5007TestMode || legacy5031TestMode || legacyHistoryDownloadMode ||
            realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("Item-List-Wiederholbarkeit benötigt eine neue Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("Item-List-Wiederholbarkeit: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt.")
            return
        }
        val prefs = context.getSharedPreferences(
            CirqaItemListRepeatabilityAttemptGate.PREFS_NAME, Context.MODE_PRIVATE
        )
        if (!CirqaItemListRepeatabilityAttemptGate.consume(prefs)) {
            result("Item-List-Wiederholbarkeit: Einmal-Gate verbraucht oder Persistierung fehlgeschlagen.")
            return
        }
        itemListRepeatabilityMode = true
        itemListRepeatabilityFirstPage = null
        fileAccessListMode = true
        fileAccessRequestClaimed = false
        fileAccessDiag = ""
        fileAccessFramer.reset()
        fileAccessReassembler.reset()
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        start(false, result)
        if (!active) {
            itemListRepeatabilityMode = false
            fileAccessListMode = false
            bootstrapAckMode = false
            configurationTestMode = false
            historyDiagnosticMode = false
        } else {
            main.postDelayed({
                if (active && itemListRepeatabilityMode) {
                    finish("Item-List-Wiederholbarkeit: Zeitlimit; kein Retry. $fileAccessDiag", gatt)
                }
            }, 40_000L)
        }
    }

    @SuppressLint("MissingPermission")
    private fun maybeSendFileAccessList(g: BluetoothGatt) {
        if (!fileAccessListMode || fileAccessRequestClaimed || mlWritePending) return
        val handle = historyHandle
        if (handle == null) return // wait for the ML register response
        fileAccessRequestClaimed = true
        configurationTestMode = false
        main.removeCallbacks(configurationTimeout)
        if (syncRequestMode) {
            mark("Sync-Request: sende genau einen Connect-kompatiblen 5043-sync_request")
            send(g, CirqaSyncRequestCandidate.mlValue(handle, 1))
        } else {
            mark("FileAccess-Liste: sende genau eine 5043-Item-List-Anfrage (read-only)")
            send(g, CirqaFileAccessListRequest.mlValue(handle, 1))
        }
    }

    private fun handleFileAccessResponse(g: BluetoothGatt, payload: ByteArray) {
        for (frame in fileAccessFramer.accept(payload)) {
            val verified = GarminFrameValidator.inspectVerified(frame) ?: continue
            if (syncRequestMode) {
                syncFrameCounts[verified.messageType] = (syncFrameCounts[verified.messageType] ?: 0) + 1
                if (verified.messageType != 5044 && verified.messageType != 5000 &&
                    syncOtherLengths.length < 260) {
                    val chunk = CirqaLegacyTransferDecoder.plainChunk(
                        frame.copyOfRange(4, frame.size - 2)
                    )
                    val detail = if (chunk != null) "[off=${chunk.offset},len=${chunk.payload.size}]" else ""
                    syncOtherLengths += "${verified.messageType}:${frame.size}$detail,"
                }
            }
            if (verified.messageType == 5000) {
                val h = GarminGfdiHeader.inspect(frame)
                if (h?.responseTo == 5043) {
                    val ack = CirqaLegacy5043AckObservation.parse(frame)
                    fileAccessDiag = if (ack != null) {
                        "5043-Transport-ACK(gfdiAck=${ack.gfdiAck},requestId=${ack.requestId}," +
                            "offset=${ack.receivedOffset},successFlag=${ack.successFlag},status=${ack.status}); " +
                            "dies bestätigt nur den Transport, keine Item-List-Antwort"
                    } else {
                        "5000[to=5043,gfdiAck=${h.responseStatus},frameLen=${frame.size}," +
                            "ACK-Form=nicht verifiziert]"
                    }
                    main.post { mark("FileAccess: $fileAccessDiag") }
                }
                continue
            }
            if (verified.messageType != 5044) {
                continue
            }
            if (syncRequestMode) {
                // Connect's ProtobufHandler ACKs incoming 5044 chunks exactly like
                // 5043 chunks. Without this the device retries three times and stops.
                val handle = historyHandle
                val body = frame.copyOfRange(4, frame.size - 2)
                if (handle != null && body.size >= 14) {
                    val id = (body[0].toInt() and 255) or ((body[1].toInt() and 255) shl 8)
                    val offset = (0..3).fold(0L) { n, k -> n or ((body[2 + k].toLong() and 255L) shl (8 * k)) }
                    queueAck(g, CirqaLegacy5043AckCandidate.mlGattValueCandidate(handle, id, offset, 5044))
                }
            }
            fileAccessReassembler.accept(frame)?.let { msg ->
                val report = "5044 vollständig, len=${msg.size}, " +
                    "shape=${CirqaProtobufShapeInspector.nestedShape(msg)}"
                if (itemListRepeatabilityMode) {
                    val page = CirqaFileAccessItemListParser.parsePage(msg)
                    if (page == null || page.status != ITEM_LIST_SUCCESS_STATUS) {
                        finish("Item-List-Wiederholbarkeit: ungültige/fehlgeschlagene Antwort; keine Wiederholung.", g)
                        return
                    }
                    val first = itemListRepeatabilityFirstPage
                    if (first == null) {
                        itemListRepeatabilityFirstPage = page
                        val handle = historyHandle ?: run {
                            finish("Item-List-Wiederholbarkeit: GFDI-Handle fehlt; keine Wiederholung.", g)
                            return
                        }
                        fileAccessReassembler.reset()
                        queueAck(g, CirqaFileAccessListRequest.mlValue(handle, 2))
                        main.post { mark("Item-List-Wiederholbarkeit: zweite read-only Item-List angefordert; kein Pull") }
                    } else {
                        val comparison = CirqaItemListRepeatability.compare(first.items, page.items)
                        val reportText =
                            "Item-List-Wiederholbarkeit abgeschlossen: " +
                                "erste=${comparison.firstCount}, zweite=${comparison.secondCount}, " +
                                "UID+Metadaten unverändert=${comparison.unchanged}, " +
                                "Metadaten geändert=${comparison.changedMetadata}, " +
                                "neu=${comparison.added}, fehlend=${comparison.missing}, " +
                                "doppelte UIDs=${comparison.duplicateUidsFirst}/${comparison.duplicateUidsSecond}; " +
                                "nur zwei Listen im selben BLE-Lauf, keine Datei-Pulls; " +
                                "sessionId=${page.sessionId != null} (keine Pagination); " +
                                "UID-Stabilität über getrennte Sitzungen und Archivvollständigkeit ungeprüft."
                        finish(reportText, g)
                    }
                    return
                }
                if (syncRequestMode) {
                    // Connect keeps the connection open after sync_response: the device
                    // begins delivering buffered files only afterwards.
                    syncAccepted = true
                    syncProtoShapes = (syncProtoShapes + "${CirqaProtobufShapeInspector.nestedShape(msg)};").take(400)
                    // An item_list_response carries real, readable items. Parse it and
                    // request the first one; UIDs stay in memory and are never reported.
                    CirqaFileAccessItemListParser.parsePullResult(msg)?.let { pull ->
                        if (syncStopBeforePullMode) {
                            syncPullDiag = "unexpectedPullResponse=true; no MLR registration or file pull"
                            finish("Unerwartete Pull-Antwort; vor MLR und Datei-Pull gestoppt.", g)
                            return
                        }
                        syncPullDiag = (syncPullDiag + "|result=${pull.resultCode},transport=${pull.transport}," +
                            "handle=${pull.transferHandle},path=${pull.hasFilePath}," +
                            "window=${pull.compressionWindow}]").take(1000)
                        syncTransferHandle = pull.transferHandle
                        if (pull.resultCode != 0L || pull.transport != 0L ||
                            pull.transferHandle !in 1..0xffff_ffffL
                        ) {
                            syncFailedFiles++
                            syncFileResults = (syncFileResults + "pull-rejected;").take(1800)
                            finish("Pull-Antwort nicht erfolgreich; weitere Abrufe gestoppt.", g)
                        } else if (syncMlrRegistrationPending || mlrConfigureResponseSeen) {
                            // Duplicate pull responses must not allocate/configure a
                            // second MLR pipe while the current transfer is active.
                        } else if (!mlrRegisterSent) {
                            mlrRegisterSent = true
                            syncMlrRegistrationPending = true
                            // Connect registers one of its MLR services (0x2018) to
                            // receive the raw transfer bytes for each transfer pipe.
                            queueAck(g, GarminMlHandshake.registerRequest(MLR_SERVICE_ID))
                            main.post { mark("MLR-Service 0x2018 angefordert") }
                        } else {
                            val registeredHandle = mlrHandle
                            if (registeredHandle == null) {
                                finish("MLR-Handle fehlt für Folge-Pull.", g)
                            } else {
                                configureMlrTransfer(g, registeredHandle)
                            }
                        }
                    }
                    CirqaFileAccessItemListParser.parsePage(msg)?.let { page ->
                        appendSyncItemListPage(g, page)
                        if (syncStopBeforePullMode) return
                    }
                    // Connect requests the pending file list only AFTER sync_response.
                    val handle = historyHandle
                    if (handle != null && !syncItemListSent) {
                        syncItemListSent = true
                        queueAck(g, CirqaFileAccessListRequest.mlValue(handle, 2))
                        main.post { mark("Sync akzeptiert; FileAccess-Item-List nachgesendet") }
                    } else {
                        main.post { mark("Sync akzeptiert ($report); beobachte weitere Frames") }
                    }
                } else {
                    finish("FileAccess ItemList: $report. ${fileAccessReassembler.summary()}", g)
                    return
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDirectoryAfterChainTest(userConfirmed: Boolean, result: (String) -> Unit) {
        if (!CirqaDirectoryAfterChainGate.RELEASED_FOR_DEVICE_TEST || !userConfirmed) {
            result("Directory-nach-Kette gesperrt; keine Verbindung.")
            return
        }
        if (active || sessionStartedNanos != 0L || historicalSyncV1 != null || dirReadSession != null ||
            directoryReadAfterChain || directoryReadV2Mode || legacy5030Only != null ||
            legacy5007TestMode || legacy5031TestMode || legacyHistoryDownloadMode ||
            realtimeMode || continuousRecordingMode || rawCaptureMode) {
            result("Directory-nach-Kette benötigt eine neue Probeinstanz; keine Verbindung.")
            return
        }
        val uniqueTarget = try {
            adapter.isEnabled && adapter.bondedDevices.orEmpty().count {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            } == 1
        } catch (_: SecurityException) { false }
        if (!uniqueTarget || CirqaBleSessionGate.isBusy()) {
            result("Directory-nach-Kette: Bluetooth aus, Ziel nicht eindeutig oder Sitzung belegt.")
            return
        }
        val prefs = context.getSharedPreferences("cirqa_directory_after_chain", Context.MODE_PRIVATE)
        if (!CirqaDirectoryAfterChainGate.consume(
                CirqaDirectoryAfterChainGate.RELEASED_FOR_DEVICE_TEST, userConfirmed,
                { prefs.getBoolean(CirqaDirectoryAfterChainGate.PREFERENCE_KEY, false) },
                { prefs.edit().putBoolean(CirqaDirectoryAfterChainGate.PREFERENCE_KEY, true).commit() }
            )) {
            result("Directory-nach-Kette: Gate verbraucht oder Persistierung fehlgeschlagen.")
            return
        }
        directoryReadAfterChain = true
        val model = CirqaHistoricalSyncV1MultiPhaseSession(maxPhase = 3)
        historicalSyncV1 = model
        model.begin(true, android.os.SystemClock.elapsedRealtime())
        bootstrapAckMode = true
        configurationTestMode = true
        historyDiagnosticMode = true
        start(false, result)
        if (active) main.postDelayed({
            if (active) {
                model.tick(model.token, android.os.SystemClock.elapsedRealtime())
                finish("Directory-nach-Kette: Gesamtlaufzeit beendet.", gatt)
            }
        }, CirqaHistoricalSyncV1MultiPhaseSession.SESSION_MILLIS)
    }

    private fun claimAndSendHsv1(
        g: BluetoothGatt,
        s: CirqaHistoricalSyncV1MultiPhaseSession,
        writePhase: CirqaHistoricalSyncV1MultiPhaseSession.Phase
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        val claimed = when (writePhase) {
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5007_WRITE -> s.claim5007(s.token, now)
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5031_WRITE -> s.claim5031(s.token, now)
            CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_DIR_WRITE -> s.claimDirectory(s.token, now)
            else -> false
        }
        if (claimed) sendHsv1Frame(g, writePhase)
    }

    private fun finishHsv1IfTerminal(g: BluetoothGatt) {
        val s = historicalSyncV1 ?: return
        if (s.phase == CirqaHistoricalSyncV1MultiPhaseSession.Phase.PHASE_A_COMPLETED) {
            if (s.maxPhase <= 1) {
                finish("Historical Sync V1 Phase A abgeschlossen; Stopp vor 5031. ${s.summary()}", g); return
            }
            if (s.resumeToPhaseB(s.token, android.os.SystemClock.elapsedRealtime())) {
                claimAndSendHsv1(g, s, CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5031_WRITE)
            }
            return
        }
        if (s.phase == CirqaHistoricalSyncV1MultiPhaseSession.Phase.PHASE_B_COMPLETED) {
            if (s.maxPhase <= 2) {
                finish("Historical Sync V1 Phase B abgeschlossen; Stopp vor Directory. ${s.summary()}", g); return
            }
            if (s.resumeToPhaseC(s.token, android.os.SystemClock.elapsedRealtime())) {
                claimAndSendHsv1(g, s, CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_DIR_WRITE)
            }
            return
        }
        if (s.phase == CirqaHistoricalSyncV1MultiPhaseSession.Phase.PHASE_C_COMPLETED) {
            if (directoryReadAfterChain) {
                // The 5002 directory size is known; start reading its bytes via 5004 in the SAME connection.
                val size = s.directorySize
                if (size == null || size !in 16..CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES ||
                    !startDirectoryBodyRead(g, size)) {
                    finish("Directory-Body-Lese konnte nicht gestartet werden. ${s.summary()}", g)
                }
                return
            }
            finish("Historical Sync V1 Phase C abgeschlossen. ${s.summary()}", g); return
        }
        if (s.phase == CirqaHistoricalSyncV1MultiPhaseSession.Phase.STOPPED) {
            finish("Historical Sync V1 abgebrochen. ${s.summary()}", g)
        }
    }

    private fun finish5030IfTerminal(g: BluetoothGatt) {
        val model = legacy5030Only ?: return
        if (model.phase == CirqaLegacy5030OnlySession.Phase.OBSERVED ||
            model.phase == CirqaLegacy5030OnlySession.Phase.STOPPED) {
            finish("5030-only abgeschlossen; Stopp vor 5007/5031/5002.", g)
        }
    }

    private fun maybeSendLegacy5007AfterBootstrap(g: BluetoothGatt) {
        if (!legacy5007TestMode || legacy5007WriteClaimed ||
            !CirqaLegacy5007BootstrapGate.maySend5007(
                bootstrapAckSent,
                bootstrapAckConfirmed,
                configurationResponseQueued,
                configurationResponseConfirmed,
                configurationAckReceived,
                mlWritePending,
                bootstrapAckChunks.isNotEmpty()
            )) return
        val handle = historyHandle
        if (handle == null) {
            finish("Bootstrap abgeschlossen, aber dynamischer GFDI-Handle fehlt; 5007 nicht gesendet.", g)
            return
        }
        legacy5007WriteClaimed = true
        mark("5024/5050 bestätigt; sende genau einen 5007-Request")
        send(g, CirqaLegacy5007RequestCandidate.mlGattValueCandidate(handle))
    }

    private fun maybeSendLegacy5002AfterBootstrap(g: BluetoothGatt) {
        if (!legacyHistoryDownloadMode || legacy5002RequestSent || legacy5002RequestWriteClaimed ||
            legacyHistorySessionStartedAt != 0L ||
            !CirqaLegacy5007BootstrapGate.maySend5007(
                bootstrapAckSent,
                bootstrapAckConfirmed,
                configurationResponseQueued,
                configurationResponseConfirmed,
                configurationAckReceived,
                mlWritePending,
                bootstrapAckChunks.isNotEmpty()
            )) return
        val handle = historyHandle
        if (handle == null) {
            finish("Bootstrap vollständig, aber dynamischer GFDI-Handle fehlt; 5002 nicht gesendet.", g)
            return
        }
        configurationTestMode = false
        legacyHistorySessionStartedAt = System.nanoTime()
        main.removeCallbacks(configurationTimeout)
        main.removeCallbacks(legacy5007Timeout)
        main.postDelayed(legacyHistoryTimeout, CirqaLegacyDownloadLimits.MAX_SESSION_MILLIS)
        legacyCurrentFileIndex = 0
        legacy5002Response = CirqaLegacy5002ResponseObserver()
        legacy5004Response = CirqaLegacy5004ResponseObserver()
        legacyCurrentFileIndex = 0
        legacyCurrentAssembler = null
        legacy5002RequestWriteClaimed = true
        legacyHistoryReportStatus = "5002 Index 0 gesendet; warte auf begrenzte Directory-Übertragung"
        mark("Bootstrap bestätigt; sende einen unkomprimierten 5002-Request für Directory-Index 0")
        send(g, CirqaLegacy5002RequestCandidate.mlGattValueCandidate(
            dynamicHandle = handle,
            fileIndex = 0,
            compressed = false
        ))
    }

    private fun receiveLegacyDirectoryOrFitData(g: BluetoothGatt, payload: ByteArray) {
        if (legacyCurrentAssembler == null) {
            when (legacy5002Response.observeMlPayload(payload)) {
                CirqaLegacy5002ResponseObserver.Result.PENDING -> Unit
                CirqaLegacy5002ResponseObserver.Result.SUCCESS -> {
                    val index = legacyCurrentFileIndex
                    val response = legacy5002Response.response
                    val size = response?.unsignedValue
                    val maxSize = if (index == 0) CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES
                    else minOf(
                        CirqaLegacyDownloadLimits.MAX_FIT_FILE_BYTES,
                        CirqaLegacyDownloadLimits.MAX_TOTAL_FIT_BYTES - legacyFitBytesWritten
                    )
                    if (index == null || size == null || size !in 1..maxSize.toLong()) {
                        legacyHistoryReportStatus = "5002 success size fehlt oder überschreitet das festgelegte Limit"
                        finish(legacyHistoryReportStatus, g)
                        return
                    }
                    val directoryEntry = legacyCurrentDirectoryEntry
                    if (index != 0 && (directoryEntry?.fileIndex != index ||
                            directoryEntry.size != size)) {
                        finish("5002-Größe für FIT-Index $index stimmt nicht mit dem Verzeichniseintrag überein; Datei-Transfer gestoppt.", g)
                        return
                    }
                    legacyCurrentExpectedSize = size.toInt()
                    legacyCurrentCrc = 0
                    legacyPreviousCrc = 0
                    legacyLastPayload = null
                    legacyLastOffset = null
                    legacyPendingCrc = null
                    legacyCurrentAssembler = CirqaLegacyPlainFileAssembler(
                        expectedSize = legacyCurrentExpectedSize,
                        verifyPayloadCrc = ::verifyLegacyChunkCrc,
                        maxFileBytes = maxSize
                    )
                    legacy5004Response = CirqaLegacy5004ResponseObserver()
                    legacyHistoryReportStatus =
                        "5002 Index $index erfolgreich; Größe=${legacyCurrentExpectedSize} Bytes; warte auf CRC-validierte 5004-Chunks"
                    mark(legacyHistoryReportStatus)
                }
                CirqaLegacy5002ResponseObserver.Result.STATUS_ERROR -> {
                    val status = legacy5002Response.response?.status?.name
                        ?: "GFDI-${legacy5002Response.gfdiStatus ?: "unbekannt"}"
                    legacyHistoryReportStatus = "5002 Index ${legacyCurrentFileIndex ?: "?"} abgelehnt: $status"
                    finish(legacyHistoryReportStatus, g)
                }
                CirqaLegacy5002ResponseObserver.Result.UNEXPECTED ->
                    finish("5002-Test abgebrochen: unerwartete GFDI-Antwort oder mehrere Frames; kein Retry.", g)
                CirqaLegacy5002ResponseObserver.Result.MALFORMED ->
                    finish("5002-Test abgebrochen: Antwort-COBS/GFDI/CRC/Response-Body ungültig; kein Retry.", g)
            }
            return
        }

        when (legacy5004Response.observeMlPayload(payload)) {
            CirqaLegacy5004ResponseObserver.Result.PENDING -> Unit
            CirqaLegacy5004ResponseObserver.Result.UNEXPECTED ->
                finish("Transfer abgebrochen: unerwarteter oder zusätzlicher Frame während 5004; kein Retry.", g)
            CirqaLegacy5004ResponseObserver.Result.MALFORMED ->
                finish("Transfer abgebrochen: ungültiger COBS/GFDI/CRC-Frame; kein ACK und kein Retry.", g)
            CirqaLegacy5004ResponseObserver.Result.DATA -> {
                val body = legacy5004Response.body ?: run {
                    finish("Transfer abgebrochen: 5004-Body fehlt.", g)
                    return
                }
                val chunk = CirqaLegacyTransferDecoder.plainChunk(body) ?: run {
                    finish("Transfer abgebrochen: 5004-Chunkheader ungültig; kein ACK.", g)
                    return
                }
                legacyCandidateChunkOffset = chunk.offset
                legacyPendingCrc = null
                val accepted = legacyCurrentAssembler?.accept(body)
                when (accepted) {
                    CirqaLegacyPlainFileAssembler.Result.ACCEPTED,
                    CirqaLegacyPlainFileAssembler.Result.COMPLETE -> {
                        val crc = legacyPendingCrc
                        if (crc == null) {
                            finish("Transfer abgebrochen: CRC-Akkumulator nicht konsistent; kein ACK.", g)
                            return
                        }
                        legacyPreviousCrc = legacyCurrentCrc
                        legacyCurrentCrc = crc
                        legacyLastPayload = chunk.payload.copyOf()
                        legacyLastOffset = chunk.offset
                    }
                    CirqaLegacyPlainFileAssembler.Result.DUPLICATE -> Unit
                    else -> {
                        finish("Transfer abgebrochen: 5004-Chunk nicht CRC-/Offset-validiert ($accepted); kein ACK oder Retry.", g)
                        return
                    }
                }
                legacyCurrentTransferCompleted =
                    accepted == CirqaLegacyPlainFileAssembler.Result.COMPLETE
                legacy5004Response = CirqaLegacy5004ResponseObserver()
                val handle = historyHandle ?: run {
                    finish("Transfer abgebrochen: dynamischer GFDI-Handle fehlt; kein ACK.", g)
                    return
                }
                val nextOffset = legacyCurrentAssembler?.receivedBytes ?: run {
                    finish("Transfer abgebrochen: Assembler fehlt; kein ACK.", g)
                    return
                }
                legacyHistoryReportStatus =
                    "5004-Chunk CRC/Offset-validiert; nächster Offset=$nextOffset/${legacyCurrentExpectedSize}"
                mark(legacyHistoryReportStatus)
                send(g, CirqaLegacy5004AckCandidate.mlGattValueCandidate(handle, nextOffset))
            }
        }
    }

    private var legacyCandidateChunkOffset = -1L
    private var legacyLastOffset: Long? = null

    private fun verifyLegacyChunkCrc(payload: ByteArray, expected: Int): Boolean {
        if (legacyCandidateChunkOffset == legacyLastOffset &&
            legacyLastPayload?.contentEquals(payload) == true &&
            CirqaLegacyCrc16.compute(payload, legacyPreviousCrc) == expected) {
            legacyPendingCrc = null
            return true
        }
        val updated = CirqaLegacyCrc16.compute(payload, legacyCurrentCrc)
        if (updated != expected) {
            legacyPendingCrc = null
            return false
        }
        legacyPendingCrc = updated
        return true
    }

    private fun completeLegacyTransferAndContinue(g: BluetoothGatt) {
        val bytes = legacyCurrentAssembler?.completedBytes() ?: run {
            finish("Transferabschluss ungültig; vollständige CRC-validierte Daten fehlen.", g)
            return
        }
        val index = legacyCurrentFileIndex ?: run {
            finish("Transferabschluss ohne bekannten Dateiindex.", g)
            return
        }
        legacyCurrentAssembler = null
        legacyCurrentExpectedSize = 0
        legacyCurrentTransferCompleted = false

        if (index == 0) {
            val directory = CirqaLegacyDirectoryParser.parseForDownload(bytes) ?: run {
                finish("Directory-Index 0 vollständig übertragen, aber strikt nicht parsebar; FIT-Requests gestoppt.", g)
                return
            }
            legacyDirectory = directory
            legacyDirectoryEntriesSeen = directory.entries.size
            legacyHistoryReportDirectoryEntries = directory.entries.size
            val initialDecisions = CirqaLegacyDownloadLimits.classifyEntries(
                directory.entries, legacySupportedTypes
            )
            var selectedCount = 0
            var selectedBytes = legacyFitBytesWritten
            val finalDecisions = initialDecisions.map { decision ->
                if (!decision.eligible) return@map decision to decision.reason
                val entry = decision.entry
                val reason = when {
                    selectedCount >= CirqaLegacyDownloadLimits.MAX_FIT_FILES -> "file-count-limit"
                    entry.size >
                        (CirqaLegacyDownloadLimits.MAX_TOTAL_FIT_BYTES - selectedBytes).toLong() ->
                        "total-size-limit"
                    else -> {
                        selectedCount++
                        selectedBytes += entry.size.toInt()
                        "selected"
                    }
                }
                decision to reason
            }
            val bounded = finalDecisions.filter { it.second == "selected" }.map { it.first.entry }
            legacyHistoryReportEligibleFiles = bounded.size
            legacyHistoryReportSkippedFiles = directory.entries.size - bounded.size
            val reasonCounts = finalDecisions.groupingBy { it.second }.eachCount()
            val rows = finalDecisions.take(MAX_DIRECTORY_DIAGNOSTIC_ENTRIES).map { (decision, reason) ->
                val entry = decision.entry
                "idx=${entry.fileIndex},type=${entry.type},subtype=${entry.subtype}," +
                    "read=${entry.readable},flags=0x${entry.attributes.toString(16).padStart(2, '0')}," +
                    "size=${entry.size},decision=$reason"
            }
            val remainingEntries = finalDecisions.size - rows.size
            legacyHistoryDirectoryEntryDiagnostics =
                "reasonCounts=$reasonCounts; ${rows.joinToString("; ")}" +
                    if (remainingEntries > 0) "; weitere=$remainingEntries" else ""
            if (directoryReadV2Mode) {
                legacyHistoryReportStatus =
                    "Directory gelesen (${directory.entries.size} Einträge, " +
                        "${bounded.size} FIT-Kandidaten). Stopp vor FIT-Download. " +
                        legacyHistoryDirectoryEntryDiagnostics
                finish(legacyHistoryReportStatus, g)
                return
            }
            legacyFileQueue.clear()
            bounded.forEach(legacyFileQueue::addLast)
            if (legacyFileQueue.isEmpty()) {
                legacyHistoryReportStatus = "Directory gelesen; keine passenden lesbaren, unterstützten FIT-Einträge innerhalb der Limits."
                finish(legacyHistoryReportStatus, g)
                return
            }
            issueNextLegacyFitRequest(g)
            return
        }

        FitFileHeader.inspectVerified(bytes) ?: run {
            finish("FIT-Index $index vollständig übertragen, aber FIT-Header/CRC ungültig; nächste Datei gestoppt.", g)
            return
        }
        if (!persistVerifiedLegacyFit(index, bytes)) {
            finish("FIT-Index $index verifiziert, aber sichere lokale Speicherung schlug fehl; nächste Datei gestoppt.", g)
            return
        }
        legacyDownloadedIndices += index
        legacyFitBytesWritten += bytes.size
        legacyHistoryReportCompletedFiles = legacyDownloadedIndices.size
        legacyHistoryReportStoredBytes = legacyFitBytesWritten
        legacyHistoryReportStatus =
            "FIT-Index $index lokal gespeichert; Größe=${bytes.size}; verifizierter Header CRC=OK"
        if (legacyFileQueue.isEmpty()) {
            finish("Verzeichnis und ${legacyDownloadedIndices.size} FIT-Datei(en) erfolgreich und begrenzt lokal gespeichert.", g)
        } else {
            issueNextLegacyFitRequest(g)
        }
    }

    private fun issueNextLegacyFitRequest(g: BluetoothGatt) {
        if (!active || terminationRequested) return
        val entry = if (legacyFileQueue.isEmpty()) null else legacyFileQueue.removeFirst()
        if (entry == null) {
            finish("Verzeichnis und FIT-Auswahl abgeschlossen.", g)
            return
        }
        val handle = historyHandle ?: run {
            finish("FIT-Transfer gestoppt: dynamischer GFDI-Handle fehlt.", g)
            return
        }
        if (legacyDownloadedIndices.size >= CirqaLegacyDownloadLimits.MAX_FIT_FILES ||
            legacyFitBytesWritten + entry.size > CirqaLegacyDownloadLimits.MAX_TOTAL_FIT_BYTES) {
            finish("FIT-Auswahl-Limit erreicht; keine weitere Datei angefordert.", g)
            return
        }
        legacyCurrentFileIndex = entry.fileIndex
        legacyCurrentDirectoryEntry = entry
        legacy5002Response = CirqaLegacy5002ResponseObserver()
        legacy5004Response = CirqaLegacy5004ResponseObserver()
        legacy5002RequestWriteClaimed = true
        legacyHistoryReportStatus =
            "Sende einen unkomprimierten 5002-Request für lesbaren, unterstützten FIT-Index ${entry.fileIndex}"
        mark(legacyHistoryReportStatus)
        send(g, CirqaLegacy5002RequestCandidate.mlGattValueCandidate(
            dynamicHandle = handle,
            fileIndex = entry.fileIndex,
            compressed = false
        ))
    }

    private fun persistVerifiedLegacyFit(index: Int, bytes: ByteArray): Boolean {
        return try {
            val parent = File(context.filesDir, "cirqa_fit_history")
            if (!parent.exists() && !parent.mkdirs()) return false
            val directory = legacyHistoryOutputDirectory ?: File(
                parent, "session_${System.currentTimeMillis()}"
            ).also {
                if (!it.mkdir()) return false
                legacyHistoryOutputDirectory = it
            }
            val target = File(directory, "fit_index_${index}.fit")
            if (target.exists()) return false
            val temporary = File.createTempFile("fit_${index}_", ".part", directory)
            try {
                FileOutputStream(temporary).use { stream ->
                    stream.write(bytes)
                    stream.fd.sync()
                }
                if (!temporary.renameTo(target)) return false
                true
            } finally {
                if (temporary.exists()) temporary.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun legacyHistoryDownloadReport(reason: String): String =
        "PulsLibre · CIRQA Legacy-Verzeichnis-/FIT-Einzeltest\n" +
            "Ergebnis: $reason\n" +
            "Bootstrap: 5024-ACK=${bootstrapAckConfirmed}; 5050-Antwort=${configurationResponseConfirmed}; 5050-ACK=${configurationAckReceived}.\n" +
            "5007/5031 wurden in diesem Lauf nicht wiederholt; verwendet wurden nur die zuvor CRC-validierten 5031-Typen. " +
            "Der geschlossene 5007-Gate 5/5 blieb unverändert.\n" +
            "5002-Indizes angefordert: ${legacyRequestedIndices.joinToString().ifEmpty { "keine" }}; Retry=0.\n" +
            "Directory-Einträge=${legacyHistoryReportDirectoryEntries}; geeignete FITs=${legacyHistoryReportEligibleFiles}; " +
            "wegen Limits/Filter übersprungen=${legacyHistoryReportSkippedFiles}.\n" +
            "Directory-Eintrag-Metadaten (ohne Zeitstempel, Identifierbytes oder Rohdaten): $legacyHistoryDirectoryEntryDiagnostics.\n" +
            "Vollständig validierte und lokal gespeicherte FIT-Dateien=${legacyDownloadedIndices.joinToString().ifEmpty { "keine" }}; " +
            "Dateien=${legacyHistoryReportCompletedFiles}; Bytes=$legacyHistoryReportStoredBytes; " +
            "Privater App-Speicher=${legacyHistoryOutputDirectory?.name ?: "kein FIT gespeichert"}.\n" +
            "Grenzen: max. Directory ${CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES} B; " +
            "max. ${CirqaLegacyDownloadLimits.MAX_FIT_FILES} Dateien, je ${CirqaLegacyDownloadLimits.MAX_FIT_FILE_BYTES} B, " +
            "gesamt ${CirqaLegacyDownloadLimits.MAX_TOTAL_FIT_BYTES} B; Laufzeit höchstens 5 min.\n" +
            "Nur lesbare, als FIT (type 128) und in der gespeicherten 5031-Liste unterstützte Einträge. " +
            "Unkomprimierte 5004-Chunks nur nach CRC/Offset-Validierung positiv bestätigt.\n" +
            "Kein 5008/Archive/Erase/SetFlags, CLOSE_ALL, Retry oder sonstiges Sync-Folgekommando. " +
            "Dateiinhalte/Messwerte nicht in Bericht oder SQLite übernommen.\n" +
            "GFDI-Handle=${historyHandle ?: "unbekannt"}; RX=$selectedRxUuid; TX=$selectedTxUuid.\n" +
            txTrace.summary()

    companion object {
        private const val MAX_DIRECTORY_DIAGNOSTIC_ENTRIES = 12
        private const val MAX_SYNC_ITEM_LIST_PAGES = 64
        // ItemListStatus.SUCCESS is 0; 1 means FAIL_UNKNOWN_SESSION_ID.
        private const val ITEM_LIST_SUCCESS_STATUS = 0L
    }

    private fun legacy5007Report(reason: String): String =
        "PulsLibre · CIRQA ${if (legacy5031TestMode) "GFDI-5031-Einzeltest" else "GFDI-5007-Einzeltest"}\n" +
            "Ergebnis: $reason\n" +
            (if (bootstrapAckMode) "Vorbedingung: 5024-ACK gesendet=${bootstrapAckSent}; GATT bestätigt=$bootstrapAckConfirmed; 5050-Hostantwort bestätigt=$configurationResponseConfirmed; 5000-ACK für 5050 empfangen=$configurationAckReceived.\n" else "") +
            "5007-Request gesendet: $legacy5007RequestSent (höchstens ein Versuch)\n" +
            (if (legacy5031TestMode) "5031-Request gesendet: $legacy5031RequestSent (höchstens ein leerer Request); Status=${legacy5031Response.responseStatus ?: "unbekannt"}; erkannte Dateitypen=${legacy5031Response.fileTypes?.joinToString { "${it.type}/${it.subtype}:${it.name}" }?.take(512) ?: "keine"}; 5002=NICHT GESENDET.\n" else "") +
            "Connect-Kandidat: Typ 5007, Body 03, GFDI/CRC/COBS offline geprüft.\n" +
            "GFDI-Handle: ${historyHandle ?: "unbekannt"}; RX=$selectedRxUuid; TX=$selectedTxUuid; " +
            "TX-Properties=$selectedTxProperties.\n" +
            "Antwort: ${legacy5007Response.responseType?.toString() ?: "keine klassifizierte Antwort"}; " +
            "Status=${legacy5007Response.responseStatus?.toString() ?: "unbekannt"}; " +
            "CRC-validierte Ergebnisframes=${if (legacy5007Response.responseType == null) 0 else 1}.\n" +
            txTrace.summary() + "\n" +
            "Keine Folgekommandos, Rohframes, Dateiinhalte oder Messwerte erfasst."

    private fun historyReport(reason: String): String =
        "PulsLibre · CIRQA Historien-Diagnose (Phase 1)\n" +
        "Ergebnis: $reason\n" +
        (if (bootstrapAckMode) "Bootstrap-ACK-Test: Kandidat gesendet=$bootstrapAckSent; GATT bestätigt=$bootstrapAckConfirmed; GFDI-Frames nach ACK-Start=${(fileListProbe.frames - bootstrapBaselineFrames).coerceAtLeast(0)}; Dateibefehle=0\n" else "") +
        (if (configurationTestMode) "5050-Konfigurationstest: Antwort eingeplant=$configurationResponseQueued; GATT-Schreibfolge bestätigt=$configurationResponseConfirmed; GFDI-5000-ACK für 5050 empfangen=$configurationAckReceived; Hostmaske=15 Bytes/${if (syntheticSyncDebugMode) "nur SYNC-Bit 3" else "alle Bits 0"}; Dateibefehle=0\n" else "") +
        (if (configurationTestMode) "5050-Evidenzstatus: ${Cirqa5050Outcome.classify(configurationResponseQueued, configurationResponseConfirmed, configurationAckReceived, configuration5037ObservationStarted && Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(configuration5037Baseline, fileListProbe.validSynchronizationShapes, configurationResponseConfirmed, configurationAckReceived))}; kein Beleg für Dateiliste oder Historien-Download.\n" else "") +
        (if (configurationTestMode) "5050-Passivzustand vor Abbruch: ${passive5050Session.stateBeforeStop ?: passive5050Session.state}; 5037-Signal beobachtet=${passive5050Session.signalObserved}; ${if (syntheticSyncDebugMode) "synthetischer SYNC-Debugmodus aktiv; kein Dateiabruf" else "keine aktive SYNC-Freigabe"}.\n" else "") +
        (if (configurationTestMode) post5050Evidence.summary() + "\n" else "") +
        (if (configurationTestMode) "Passiv beobachtete 5037-Frames: ${fileListProbe.frameTypes["gfdi-5037"] ?: 0}; gültige 5037-Strukturen: ${fileListProbe.validSynchronizationShapes}; ungültige 5037-Strukturen: ${fileListProbe.malformedSynchronizationShapes}; frisches 5037-Signal nach bestätigtem 5050: ${(configuration5037ObservationStarted && Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(configuration5037Baseline, fileListProbe.validSynchronizationShapes, configurationResponseConfirmed, configurationAckReceived))}; keine aktive Sync-Anforderung.\n" else "") +
        "GFDI-Registrierung: ${if (historyRegistered) "bestätigt" else "nicht bestätigt"}\n" +
        "Letzte Phase: $phase\n" +
        "GATT-Schreiben bestätigt: $writeConfirmed\n" +
        "Notifications: $notificationsReceived\n" +
        "Protokollzustand: ${session?.stage}\n" +
        "Transportanalyse: ${historyTransport.summary()}\n" +
        "Passiver GFDI/COBS/FileSync-Decoder: ${fileListProbe.summary()}\n" +
        "GATT-Merkmale: RX=$selectedRxUuid; TX=$selectedTxUuid; TX-Properties=$selectedTxProperties; MTU nicht aktiv ausgehandelt.\n" +
        txTrace.summary() + "\n" +
        (if (configurationTestMode) "Hinweis: Der passive Decoder meldet seinen eigenen 5050-Sendestatus; die aktive Testsequenz wird oben separat ausgewiesen. Ein 5000-ACK bestätigt keine Historienfreigabe.\n" else "") +
        "GFDI-Handle: ${historyHandle ?: "unbekannt"}; zugeordnete Pakete: $historyRouted; übrige Pakete: $historyUnrouted\n" +
        "Paketlängen (ohne Registrierung): ${historyLengths.summary()}\n" +
        (if (rawCaptureMode) fileListProbe.rawCaptureReport() + "\n" else "") +
        "Antwort-Metadaten (max. 12): ${notificationShapes.joinToString(" | ")}\n" +
        "FileSync-Anfrage: NICHT angefordert; Historienzugriff ${if (fileListProbe.validLists > 0) "durch Antwort bestätigt" else "NICHT nachgewiesen"}.\n" +
        (if (rawCaptureMode)
            "Hinweis: Dieser ausdrücklich angeforderte Rohdatenbericht kann Geräte-/Gesundheitsdaten enthalten; nicht öffentlich teilen."
        else
            "Keine Dateiübertragung, kein SetFlags, kein CLOSE_ALL, keine Rohframes oder Geräteadressen im Standardbericht.")

    private fun realtimeSummary(reason: String): String =
        "CIRQA Echtzeit-Test: $reason. " +
        "Puls: ${heartRate?.bpm?.toString() ?: "nicht empfangen"}; " +
        "Ruhepuls: ${heartRate?.restingBpm?.toString() ?: "unbekannt"}; " +
        "Schritte: ${steps?.count?.toString() ?: "nicht empfangen"}; " +
        "Ziel: ${steps?.goal?.toString() ?: "unbekannt"}; " +
        "gültige Messpakete: $realtimeFrames; " +
        (if (exploratoryMode) "Modus: ${if (mousePodsMode) "MousePods Start 0x01" else "Nur Registrierung"}; Start gesendet: $motionStartIssued; Service-16-Pakete: $motionPackets; Discovery: ${GarminServiceDiscovery.describe(exploratoryAccepted, rejectedServices, exploratoryFrames)}; " else if (expandedMode) "Weitere Werte: ${extraReadings.values.joinToString()}; abgelehnte Dienste: ${rejectedServices.joinToString()}; " else "") +
        "Phase: $phase. Keine Daten gespeichert.\n" +
        "GATT-Merkmale: RX=$selectedRxUuid; TX=$selectedTxUuid; TX-Properties=$selectedTxProperties; MTU nicht aktiv ausgehandelt.\n" +
        txTrace.summary()

    /** Redacted integrity summary of the received MLR transfer. */
    private fun mlrValidation(): String {
        val data = mlrData.toByteArray()
        if (data.isEmpty()) return "no-data"
        val item = syncCurrentItem
        val label = item?.let { CirqaFileAccessItemListParser.safeTypeLabel(it, syncGdxmlStrings) }
            ?: "unknown"
        val magic = data.copyOfRange(0, minOf(4, data.size))
            .joinToString("") { "%02x".format(it) }
        if (item == null || data.size.toLong() != syncExpectedSize ||
            data.size.toLong() != item.dataSize
        ) {
            return "len=${data.size};expected=$syncExpectedSize;type=$label;exactSize=false"
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(data)
        val sha = digest.copyOfRange(0, 6).joinToString("") { "%02x".format(it) }
        val looksFit = data.size >= 12 && data[8] == '.'.code.toByte() &&
            data[9] == 'F'.code.toByte() && data[10] == 'I'.code.toByte() &&
            data[11] == 'T'.code.toByte()
        val declaredFit = label.startsWith("FIT_TYPE_")
        if (!declaredFit && !looksFit) {
            val saved = try {
                CirqaVerifiedRawFileStore(java.io.File(context.filesDir, "verified_raw"))
                    .saveExact(data, item.dataSize)
            } catch (e: Exception) {
                return "len=${data.size};expected=${item.dataSize};type=$label;sha256=$sha;" +
                    "privateRawSaveFailed=${e.javaClass.simpleName}"
            }
            val metadata = CirqaRawDeviceMetadataParser.inspect(data)
            val metadataIndex = if (metadata == null) {
                "metadata=notDeviceXml"
            } else {
                try {
                    CirqaDatabase(context).use { store ->
                        store.storeRawDeviceMetadata(RawDeviceMetadataRecord(
                            sourceSha256 = saved.sha256,
                            fileBytes = data.size.toLong(),
                            verifiedAtEpochMillis = System.currentTimeMillis(),
                            parserVersion = CirqaRawDeviceMetadataParser.VERSION,
                            softwareVersion = metadata.softwareVersion,
                            massStorageMode = metadata.massStorageMode,
                            transferDirectionElementCount = metadata.transferDirectionElementCount
                        ))
                    }
                    "metadata=indexed"
                } catch (e: Exception) {
                    "metadataIndexFailed=${e.javaClass.simpleName}"
                }
            }
            return "len=${data.size};expected=${item.dataSize};type=$label;exactSize=true;" +
                "sha256=$sha;privateRaw=${if (saved.alreadyPresent) "ALREADY_PRESENT" else "CREATED"};$metadataIndex"
        }
        if (FitFileHeader.inspectVerified(data) == null) {
            val savedRaw = try {
                CirqaVerifiedRawFileStore(java.io.File(context.filesDir, "verified_raw"))
                    .saveExact(data, item.dataSize)
            } catch (e: Exception) {
                return "len=${data.size};expected=${item.dataSize};type=$label;magic=$magic;" +
                    "fitHeader=false;fitCrc=false;privateRawSaveFailed=${e.javaClass.simpleName}"
            }
            return "len=${data.size};expected=${item.dataSize};type=$label;magic=$magic;" +
                "exactSize=true;fitHeader=false;fitCrc=false;" +
                "privateRaw=${if (savedRaw.alreadyPresent) "ALREADY_PRESENT" else "CREATED"}"
        }
        val saved = try {
            CirqaVerifiedFitStore(java.io.File(context.filesDir, "verified_fit"))
                .saveVerified(data)
        } catch (e: Exception) {
            return "len=${data.size};expected=${item.dataSize};type=$label;fitHeader=true;" +
                "sha256=$sha;privateSaveFailed=${e.javaClass.simpleName}"
        }
        val decoded = FitHistoricalObservations.decode(data)
            ?: return "len=${data.size};expected=${item.dataSize};type=$label;fitHeader=true;" +
                "sha256=$sha;privateFit=${saved.outcome};decode=unsupported"
        val importable = decoded.observations.filter {
            it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                it.timestampEpochMillis != null
        }
        val imported = try {
            CirqaDatabase(context).use { store ->
                "ok:${CirqaFitBackfill.importVerifiedFile(saved.file, store) ?: 0}"
            }
        } catch (e: Exception) {
            "failed:${e.javaClass.simpleName}"
        }
        return "len=${data.size};expected=${item.dataSize};type=$label;fitHeader=true;" +
            "sha256=$sha;privateFit=${saved.outcome};observations=${decoded.observations.size};" +
            "importable=${importable.size};sqlite=$imported"
    }

    @SuppressLint("MissingPermission")
    private fun safeClose(g: BluetoothGatt?) {
        if (g == null) return
        try { g.disconnect() } catch (_: Exception) {}
        try { g.close() } catch (_: Exception) {}
    }
    private fun finish(message: String, g: BluetoothGatt?) {
        if (!active || terminationRequested) return
        if (syncRequestMode && syncFinalReport.isEmpty()) {
            val counts = syncFrameCounts.entries.sortedBy { it.key }
                .joinToString(",") { "${it.key}x${it.value}" }
            val runDrained = syncAccepted && syncIncompleteReason == null &&
                syncFailedFiles == 0 && syncPendingItems.isEmpty() && syncCurrentItem == null &&
                syncPageSessionId == null
            syncFinalReport = "Sync-Beobachtung: $message; runDrained=$runDrained; " +
                "coverage=device-history-unverified; " +
                "incompleteReason=${syncIncompleteReason ?: when {
                    !syncAccepted -> "sync-not-accepted"
                    syncFailedFiles > 0 -> "file-import-failed"
                    syncStopBeforePullMode -> "diagnostic-stop-before-pull"
                    !runDrained -> "transfer-not-drained"
                    else -> "none"
                }}; " +
                "accepted=$syncAccepted; " +
                "itemListSent=$syncItemListSent; pullSent=$syncPullSent; items=[$syncItemSummary]; " +
                "$syncPullDiag; $mlrDiag; mlr=[chunks=$mlrChunks,bytes=$mlrBytes]; " +
                "files=[completed=$syncCompletedFiles,failed=$syncFailedFiles,queued=${syncPendingItems.size}," +
                "selected=$syncQueuedFiles,selectedBytes=$syncQueuedBytes,skipped=$syncSkippedFiles," +
                "pages=$syncListPageCount,currentBytes=${mlrData.size()},details=$syncFileResults]; " +
                "validate=[${if (syncCurrentItem != null) mlrValidation() else "complete"}]; " +
                "frames=$counts; proto=[$syncProtoShapes]"
        }
        terminationRequested = true
        legacy5030Only?.let { it.abort(it.token) }
        passive5050Session.stop()
        main.post {
            if (!active) return@post
            active = false
            main.removeCallbacksAndMessages(null)
            // Keep the session gate until GATT has actually been closed.
            // Best-effort MousePods stop command before GATT disconnect; never CLOSE_ALL.
            val stopHandle = if (mousePodsMode && motionStartIssued) motionHandle else null
            if (stopHandle != null && g != null && tx != null &&
                (Build.VERSION.SDK_INT < 31 ||
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
                try {
                    val characteristic = tx!!
                    val stop = byteArrayOf(stopHandle.toByte(), 0x00)
                    if (Build.VERSION.SDK_INT >= 33) {
                        g.writeCharacteristic(characteristic, stop,
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        @Suppress("DEPRECATION")
                        characteristic.value = stop
                        @Suppress("DEPRECATION")
                        g.writeCharacteristic(characteristic)
                    }
                } catch (_: Exception) { /* Disconnect remains mandatory even if stop fails. */ }
                main.postDelayed({
                    safeClose(g)
                    CirqaBleSessionGate.release(this)
                }, 400L)
            } else {
                safeClose(g)
                CirqaBleSessionGate.release(this)
            }
            gatt = null
            callback?.invoke(
                if (syncFinalReport.isNotEmpty()) syncFinalReport
                else if (fileAccessListMode) "${if (syncRequestMode) "SyncRequest" else "FileAccess-ItemList"}: $message; " +
                    "${fileAccessReassembler.summary()}; requestSent=$fileAccessRequestClaimed; " +
                    (if (syncRequestMode) "syncAccepted=$syncAccepted; " else "") +
                    "proto=[$syncProtoShapes]; other=[$syncOtherLengths]; " +
                    "response=$fileAccessDiag"
                else if (dirReadSession != null) dirReadSession!!.summary() +
                    " [acks5004=$acks5004Queued,acks5043=$acks5043Queued," +
                    "pendingWrites=${deferredAckWrites.size},writePending=$mlWritePending," +
                    "ack5004=$ack5004Hex,filter=${CirqaHistoricalSyncV1Wire.DIRECTORY_FILTER}]" +
                    " [finish=$message]"
                else if (historicalSyncV1 != null) historicalSyncV1!!.summary() + " [finish=$message]"
                else if (legacy5030Only != null) legacy5030Only!!.summary()
                else if (legacyHistoryDownloadMode) legacyHistoryDownloadReport(message)
                else if (legacy5007TestMode) legacy5007Report(message)
                else if (historyDiagnosticMode) historyReport(message)
                else message
            )
            callback = null
            tx = null
            session = null
            realtimeMode = false
            continuousRecordingMode = false
            historyDiagnosticMode = false
            legacy5007TestMode = false
            legacy5031TestMode = false
            legacyHistoryDownloadMode = false
            directoryReadV2Mode = false
            directoryReadAfterChain = false
            dirReadSession = null
            fileAccessListMode = false
            itemListRepeatabilityMode = false
            itemListRepeatabilityFirstPage = null
            syncRequestMode = false
            syncStopBeforePullMode = false
            syncRepeatItemListsMode = false
            syncRepeatFirstItems = null
            syncAccepted = false
            syncFrameCounts.clear()
            syncProtoShapes = ""
            syncOtherLengths = ""
            syncItemListSent = false
            syncPullSent = false
            syncItemSummary = ""
            syncPullDiag = ""
            syncPendingItems.clear()
            syncCurrentItem = null
            syncNextRequestId = 3
            syncCompletedFiles = 0
            syncFailedFiles = 0
            syncFileResults = ""
            syncHasPaginationSession = false
            syncHasNextTransactionId = false
            syncPageSessionId = null
            syncPageNextTransactionId = null
            syncListPageCount = 0
            syncQueuedFiles = 0
            syncQueuedBytes = 0L
            syncSkippedFiles = 0
            syncIncompleteReason = null
            syncGdxmlStrings.clear()
            syncSeenItemUids.clear()
            syncMlrRegistrationPending = false
            mlrHandle = null
            mlrRegisterSent = false
            mlrDiag = ""
            mlrChunks = 0
            mlrBytes = 0
            mlrConfigureResponseSeen = false
            mlrTransferActive = false
            syncTransferHandle = 0L
            syncExpectedSize = 0L
            mlrData.reset()
            fileAccessRequestClaimed = false
            bootstrapAckMode = false
            configurationTestMode = false
            rawCaptureMode = false
            expandedMode = false
            exploratoryMode = false
            mousePodsMode = false
        }
    }
    fun close() {
        terminationRequested = true
        active = false
        main.removeCallbacksAndMessages(null)
        safeClose(gatt)
        CirqaBleSessionGate.release(this)
        gatt = null
        callback = null
        session = null
        realtimeMode = false
        continuousRecordingMode = false
        historyDiagnosticMode = false
        legacy5007TestMode = false
        legacy5031TestMode = false
        legacyHistoryDownloadMode = false
        bootstrapAckMode = false
        configurationTestMode = false
        rawCaptureMode = false
        expandedMode = false
        exploratoryMode = false
    }
}
