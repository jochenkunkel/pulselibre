package de.pulselibre

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

/**
 * Explicit one-shot Garmin ML/GFDI handle registration test.
 * Registers GFDI and optionally realtime services for bounded diagnostic reads.
 * No GFDI application messages, file sync, health persistence or persistent connection.
 */
class CirqaMlHandshakeProbe(private val context: Context, private val adapter: BluetoothAdapter) {
    private val main = Handler(Looper.getMainLooper())
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var active = false
    @Volatile private var terminationRequested = false
    private var gatt: BluetoothGatt? = null
    private var callback: ((String) -> Unit)? = null
    var onReading: ((Int, GarminRealtime.Reading) -> Unit)? = null
    private var tx: BluetoothGattCharacteristic? = null
    private var session: GarminMlHandshake.Session? = null
    private var requestedCloseAll = false
    private var realtimeMode = false
    private var historyDiagnosticMode = false
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
    private var fileListRequestMode = false
    private var fileListRequestSent = false
    private var fileListWritePending = false
    private var fileListWriteConfirmed = false
    private var mlWritePending = false
    private var fileListChunks = ArrayDeque<ByteArray>()
    private var historyRegistered = false
    private var historyTransport = GarminHistoryTransportInspector()
    private var fileListProbe = GarminFileListProbe()
    private var historyHandle: Int? = null
    private var historyRouted = 0
    private var historyBurstStopped = false
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

    /** Explicit one-shot FileSync V2 list query; does not download or modify files. */
    fun startFileListQuery(result: (String) -> Unit) {
        if (active) { result("ML-Test läuft bereits."); return }
        fileListRequestMode = true
        historyDiagnosticMode = true
        realtimeMode = false
        start(false, result)
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
        terminationRequested = false
        active = true
        historyRegistered = false
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
        fileListRequestSent = false
        fileListWritePending = false
        fileListWriteConfirmed = false
        mlWritePending = false
        fileListChunks.clear()
        historyTransport = GarminHistoryTransportInspector()
        fileListProbe = GarminFileListProbe()
        fileListProbe.captureRawFrames = rawCaptureMode
        historyHandle = null
        historyRouted = 0
        historyBurstStopped = false
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
        try {
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
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
                    if (!active || terminationRequested) return
                    if (status != BluetoothGatt.GATT_SUCCESS) { finish("Dienstsuche fehlgeschlagen: $status", g); return }
                    val service = g.services.orEmpty().firstOrNull {
                        it.uuid.toString().equals(GarminMlOffline.SERVICE_UUID, true)
                    }
                    val pair = GarminMlOffline.findCharacteristicPair(
                        service?.characteristics.orEmpty().map { it.uuid.toString() })
                    if (service == null || pair == null) {
                        finish("Garmin ML RX/TX-Paar im ML-Dienst nicht vorhanden.", g); return
                    }
                    val rx = service.getCharacteristic(UUID.fromString(pair.rx))
                    tx = service.getCharacteristic(UUID.fromString(pair.tx))
                    if (rx == null || tx == null || rx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0 ||
                        tx!!.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
                        finish("Garmin ML Characteristics unterstützen Notify/Write nicht.", g); return
                    }
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
                    if (!active || descriptor.uuid != cccd) return
                    if (status != BluetoothGatt.GATT_SUCCESS) { finish("CCCD-Fehler: $status", g); return }
                    mark("CCCD bestätigt; erster ML-Befehl wird gesendet")
                    send(g, session!!.begin())
                }
                @Deprecated("Android compatibility callback")
                override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    @Suppress("DEPRECATION")
                    receive(g, characteristic.value?.clone() ?: byteArrayOf())
                }
                override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    receive(g, value)
                }
                override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (!active || terminationRequested) return
                    if (status != BluetoothGatt.GATT_SUCCESS) finish("ML-Schreibfehler: $status", g)
                    else {
                        writeConfirmed = true
                        mlWritePending = false
                        if (fileListWritePending) {
                            fileListWritePending = false
                            if (fileListChunks.isEmpty()) fileListWriteConfirmed = true
                        }
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
                        } else if (fileListRequestMode && historyRegistered && fileListChunks.isNotEmpty()) {
                            main.postDelayed({ if (active) sendNextFileListChunk(g) }, 80L)
                        } else mark("ML-Schreiben bestätigt; warte auf Protokollantwort")
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE)
            if (configurationTestMode) main.postDelayed(configurationTimeout, 20_000L)
            /* Legacy timeout for non-configuration modes. */
            if (!configurationTestMode) main.postDelayed({ if (active) finish(if (historyDiagnosticMode) "CIRQA Historien-Diagnose: Beobachtungsfenster beendet." else if (realtimeMode) realtimeSummary("Zeitlimit (20 Sekunden)") else "ML-Handshake: Zeitlimit (20 Sekunden). Letzte Phase: $phase; Notifications: $notificationsReceived; GATT-Schreiben bestätigt: $writeConfirmed; Protokollzustand: ${session?.stage}; Antwortformen: ${notificationShapes.joinToString(" | ")}.", gatt) }, 20_000)
        } catch (_: SecurityException) { finish("Bluetooth-Berechtigung fehlt.", gatt) }
        catch (e: IllegalArgumentException) { finish("Verbindungsfehler: ${e.javaClass.simpleName}", gatt) }
    }

    private val configurationTimeout = Runnable {
        if (active) finish("CIRQA Historien-Diagnose: Beobachtungsfenster nach 5050-Schreibbestätigung beendet.", gatt)
    }

    private fun sendNextBootstrapAckChunk(g: BluetoothGatt) {
        if (!active || terminationRequested || !historyRegistered || mlWritePending || bootstrapAckChunks.isEmpty()) return
        bootstrapAckPending = true
        mark(if (configurationResponseQueued) "5050-Test: sende konfigurierte Antwortfolge" else "Bootstrap-Test: sende einmaliges DeviceInformation-ACK")
        send(g, bootstrapAckChunks.removeFirst())
    }

    private fun sendNextFileListChunk(g: BluetoothGatt) {
        if (!active || terminationRequested || !historyRegistered || mlWritePending || fileListChunks.isEmpty()) return
        val chunk = fileListChunks.removeFirst()
        fileListWritePending = true
        mark("Sende FileSync-V2-Dateilistenanfrage (${chunk.size} Bytes)")
        send(g, chunk)
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
        if (!fileListWritePending) mark(if (session?.stage == GarminMlHandshake.Stage.WAIT_CLOSE) "Sende CLOSE_ALL" else if (pendingService > 0) "Registriere Garmin-Service $pendingService" else "Sende GFDI-Registrierung")
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
                finish("ML-Schreibauftrag abgelehnt (${if (fileListWritePending) "FileSync V2" else "Registrierung"}).", g)
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
            if (registration?.serviceId == GarminMlHandshake.GFDI_SERVICE && registration.accepted) {
                historyHandle = registration.handle
            } else {
                val first = bytes.firstOrNull()?.toInt()?.and(255)
                if (first != null && first == historyHandle) {
                    historyRouted++
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
                    if (fileListRequestMode && !historyBurstStopped &&
                        fileListProbe.responseErrors >= 3) {
                        historyBurstStopped = true
                        finish("Sicherheitsabbruch: mindestens drei negative GFDI-Protokollantworten; siehe GFDI-5000-Status.", g)
                        return
                    }
                    if (fileListRequestMode && !historyBurstStopped &&
                        fileListProbe.frames >= 60 && fileListProbe.validLists == 0) {
                        historyBurstStopped = true
                        finish("Sicherheitsabbruch: mindestens 60 GFDI-Frames ohne Dateilistenantwort; möglicher Wiederholungsverkehr.", g)
                        return
                    }
                } else historyUnrouted++
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
                    if (fileListRequestMode) {
                        val handle = historyHandle
                        if (handle == null) {
                            finish("GFDI-Handle fehlt; Dateiliste nicht angefordert.", g)
                        } else {
                            // CIRQA returned UNSUPPORTED for the current compact 0x8A39
                            // request. Do not repeat it until the bootstrap is validated.
                            mark("FileSync V2 deaktiviert: CIRQA meldete UNSUPPORTED; nur passive Analyse")
                            main.postDelayed({
                                if (active) finish("Dateilistenanfrage sicherheitshalber deaktiviert: CIRQA hat 0x8A39 mit UNSUPPORTED abgelehnt.", g)
                            }, 500L)
                        }
                    } else mark(if (bootstrapAckMode) "GFDI registriert; warte auf DeviceInformation für einmaliges ACK" else "GFDI registriert; beobachte Metadaten bis zum Zeitlimit")
                } else if (!historyDiagnosticMode) finish("Garmin ML: GFDI-Service erfolgreich registriert! ${if (requestedCloseAll) "CLOSE_ALL wurde zuvor bestätigt." else "Ohne CLOSE_ALL."} Verbindung beendet; keine Gesundheitsdaten gelesen.", g)
            }
            GarminMlHandshake.Stage.FAILED ->
                finish("Garmin ML: GFDI-Registrierung vom Gerät abgelehnt.", g)
            else -> Unit
        }
    }

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
        (if (configurationTestMode) "Hinweis: Der passive Decoder meldet seinen eigenen 5050-Sendestatus; die aktive Testsequenz wird oben separat ausgewiesen. Ein 5000-ACK bestätigt keine Historienfreigabe.\n" else "") +
        "GFDI-Handle: ${historyHandle ?: "unbekannt"}; zugeordnete Pakete: $historyRouted; übrige Pakete: $historyUnrouted\n" +
        "Paketlängen (ohne Registrierung): ${historyLengths.summary()}\n" +
        (if (rawCaptureMode) fileListProbe.rawCaptureReport() + "\n" else "") +
        "Antwort-Metadaten (max. 12): ${notificationShapes.joinToString(" | ")}\n" +
        "Dateiliste: ${if (fileListWriteConfirmed) "FileSync V2 GATT-Schreiben bestätigt" else if (fileListRequestSent) "FileSync V2 Sendeversuch, nicht vollständig bestätigt" else "NICHT angefordert"}; Historienzugriff ${if (fileListProbe.validLists > 0) "durch Antwort bestätigt" else "NICHT nachgewiesen"}.\n" +
        "Keine Dateiübertragung, kein SetFlags, kein CLOSE_ALL, keine Gesundheitswerte oder Geräteadressen im Bericht."

    private fun realtimeSummary(reason: String): String =
        "CIRQA Echtzeit-Test: $reason. " +
        "Puls: ${heartRate?.bpm?.toString() ?: "nicht empfangen"}; " +
        "Ruhepuls: ${heartRate?.restingBpm?.toString() ?: "unbekannt"}; " +
        "Schritte: ${steps?.count?.toString() ?: "nicht empfangen"}; " +
        "Ziel: ${steps?.goal?.toString() ?: "unbekannt"}; " +
        "gültige Messpakete: $realtimeFrames; " +
        (if (exploratoryMode) "Modus: ${if (mousePodsMode) "MousePods Start 0x01" else "Nur Registrierung"}; Start gesendet: $motionStartIssued; Service-16-Pakete: $motionPackets; Discovery: ${GarminServiceDiscovery.describe(exploratoryAccepted, rejectedServices, exploratoryFrames)}; " else if (expandedMode) "Weitere Werte: ${extraReadings.values.joinToString()}; abgelehnte Dienste: ${rejectedServices.joinToString()}; " else "") +
        "Phase: $phase. Keine Daten gespeichert."

    @SuppressLint("MissingPermission")
    private fun safeClose(g: BluetoothGatt?) {
        if (g == null) return
        try { g.disconnect() } catch (_: Exception) {}
        try { g.close() } catch (_: Exception) {}
    }
    private fun finish(message: String, g: BluetoothGatt?) {
        if (!active || terminationRequested) return
        terminationRequested = true
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
            callback?.invoke(if (historyDiagnosticMode) historyReport(message) else message)
            callback = null
            tx = null
            session = null
            realtimeMode = false
            historyDiagnosticMode = false
            fileListRequestMode = false
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
        bootstrapAckMode = false
        configurationTestMode = false
        rawCaptureMode = false
        expandedMode = false
        exploratoryMode = false
    }
}
