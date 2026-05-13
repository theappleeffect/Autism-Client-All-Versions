package autismclient.util;

import autismclient.AutismClientAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import autismclient.util.lan.*;
import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.protocol.Packet;

@SuppressWarnings("deprecation")
public class PackUtilLANSync {
    private static PackUtilLANSync instance;
    private static final int MACRO_INLINE_UTF_LIMIT = 60_000;
    private static final int MACRO_CHUNK_SIZE = 32_000;
    private static final long MACRO_CHUNK_TIMEOUT_MS = 120_000L;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private String sessionId;
    private boolean isHost = false;
    private String myUsername = "";
    private final Map<String, ClientInfo> connectedClients = new ConcurrentHashMap<>();
    private final Map<String, String> discoveredSessions = new ConcurrentHashMap<>();

    private final Set<String> clientsReady = ConcurrentHashMap.newKeySet();

    private volatile boolean isSearching = false;
    private int searchTicksRemaining = 0;
    private static final int SEARCH_DURATION_TICKS = 400;
    private static final int SEARCH_BROADCAST_INTERVAL = 5;

    private Runnable onClientJoined;
    private Runnable onClientLeft;
    private Runnable onCountdown;
    private Runnable onSendNow;
    private Runnable onSessionStateChanged;
    private Runnable onSyncStateChanged;
    private Runnable onSpreadCalculated;
    private Runnable onPeerStatusChanged;

    private volatile ConcurrentHashMap<String, Map<String, PackUtilMacro>> clientMacroLists = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, Map<String, PackUtilMacro>> remoteMeteorMacros = new ConcurrentHashMap<>();
    private Map<String, String> lastBroadcastMeteorMacros = new ConcurrentHashMap<>();
    private final Map<String, PendingMacroTransfer> pendingMacroTransfers = new ConcurrentHashMap<>();

    private volatile Map<String, String> syncedAssignments = new ConcurrentHashMap<>();
    private volatile String assignmentSetBy = "";
    private Runnable onAssignmentsChanged;

    private volatile SpreadResult lastSpreadResult = null;
    private final CopyOnWriteArrayList<SpreadResult> spreadHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_SPREAD_HISTORY = 10;

    public enum SyncState {
        IDLE, PREPARING, ALL_READY, DISPATCHING_GO, EXECUTING, REPORTING, DONE
    }

    public static class SyncContext {
        public final long executionId;
        public final boolean isMacro;
        public final String macroName;
        public final String command;
        public final PackUtilMacro macro;
        public final boolean isInitiator;

        public SyncContext(long executionId, boolean isMacro, String macroName,
                           String command, PackUtilMacro macro, boolean isInitiator) {
            this.executionId = executionId;
            this.isMacro = isMacro;
            this.macroName = macroName;
            this.command = command;
            this.macro = macro;
            this.isInitiator = isInitiator;
        }
    }

    private static final class PendingMacroTransfer {
        private final long createdAtMs = System.currentTimeMillis();
        private final byte[][] chunks;
        private int receivedChunks;
        private int totalBytes;

        private PendingMacroTransfer(int totalChunks) {
            this.chunks = new byte[totalChunks][];
        }

        private synchronized byte[] addChunk(int index, byte[] data) {
            if (index < 0 || index >= chunks.length || data == null) return null;
            if (chunks[index] == null) {
                chunks[index] = data;
                receivedChunks++;
                totalBytes += data.length;
            }
            if (receivedChunks != chunks.length) return null;

            byte[] complete = new byte[totalBytes];
            int offset = 0;
            for (byte[] chunk : chunks) {
                if (chunk == null) return null;
                System.arraycopy(chunk, 0, complete, offset, chunk.length);
                offset += chunk.length;
            }
            return complete;
        }
    }

    private volatile SyncContext activeSyncCtx = null;
    private volatile SyncState syncState = SyncState.IDLE;

    private final Map<String, Long> executionTimestamps = new ConcurrentHashMap<>();

    private final Map<String, Long> ackReceiptTimes = new ConcurrentHashMap<>();

    private final Map<String, Integer> peerStepProgress = new ConcurrentHashMap<>();
    private volatile Runnable onStepProgressChanged;

    private static final int TCP_PORT = 25568;
    private ServerSocket tcpServer;
    private Thread tcpAcceptThread;
    private final Map<String, Socket> tcpClients = new ConcurrentHashMap<>();
    private Socket tcpHostSocket;
    private Thread tcpReadThread;

    private final Object tcpHostWriteLock = new Object();
    private final Map<String, Object> tcpClientWriteLocks = new ConcurrentHashMap<>();

    private final Map<String, Long> lastHeartbeatTime = new ConcurrentHashMap<>();
    private long lastHeartbeatSentMs = 0;
    private static final long HEARTBEAT_INTERVAL_MS = 9000;
    private static final long HEARTBEAT_STALE_MS = 15000;
    private static final long HEARTBEAT_DEAD_MS = 30000;

    private volatile boolean reconnecting = false;
    private volatile String lastHostIp = null;
    private volatile String lastSessionId = null;

    private PackUtilLANSync() {}

    public static PackUtilLANSync getInstance() {
        if (instance == null) {
            instance = new PackUtilLANSync();
        }
        return instance;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§aLAN Sync started (TCP mode on port " + TCP_PORT + ").");
        AutismClientAddon.LOG.info("[PackUtil-LAN] Started in TCP-only mode on port {}", TCP_PORT);
    }

    public void stop() {
        if (!running.get()) return;
        if (sessionId != null) leaveSession();
        running.set(false);
        clearSession();
        discoveredSessions.clear();
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§eLAN Sync stopped.");
    }

    public void createSession() {
        if (!running.get()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cStart LAN Sync first.");
            return;
        }

        cancelSearch();
        discoveredSessions.clear();

        if (sessionId != null) leaveSession();
        stopTcp();

        sessionId = generateSessionId();
        isHost = true;
        myUsername = getUsername();
        if (myUsername.isEmpty()) myUsername = "Host";
        connectedClients.put(myUsername, new ClientInfo(myUsername, true));

        resetSyncState();
        startTcpServer();

        PackUtilClientMessaging.sendPrefixed("Ã‚Â§aSession created: " + sessionId + " [TCP Sync]");
        fireCallback(onSessionStateChanged);
    }

    public void joinSession(String targetSessionId) {
        joinSession(targetSessionId, "127.0.0.1");
    }

    public void joinSession(String targetSessionId, String hostIp) {
        if (!running.get()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cStart LAN Sync first.");
            return;
        }
        if (sessionId != null && sessionId.equals(targetSessionId)) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cAlready in session " + sessionId);
            return;
        }

        cancelSearch();
        discoveredSessions.clear();

        if (sessionId != null) leaveSession();
        stopTcp();

        sessionId = targetSessionId;
        lastSessionId = targetSessionId;
        isHost = false;
        myUsername = getUsername();
        if (myUsername.isEmpty()) myUsername = "Player";
        connectedClients.put(myUsername, new ClientInfo(myUsername, false));
        resetSyncState();

        lastHostIp = (hostIp != null && !hostIp.isBlank()) ? hostIp : "127.0.0.1";
        connectToTcpServer(lastHostIp);
        AutismClientAddon.LOG.info("[TCP] Connecting to {}:{}", lastHostIp, TCP_PORT);

        fireCallback(onClientJoined);
        fireCallback(onSessionStateChanged);
    }

    public void leaveSession() {
        if (sessionId == null) return;

        reconnecting = false;
        lastHostIp = null;
        lastSessionId = null;
        sendTcpPacket(new LanPacket.LeavePacket(sessionId, getUsername()));
        clearSession();
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§eLeft session.");
        fireCallback(onSessionStateChanged);
    }

    private void clearSession() {
        sessionId = null;
        isHost = false;
        myUsername = "";
        reconnecting = false;
        lastHostIp = null;
        lastSessionId = null;
        connectedClients.clear();
        clientMacroLists.clear();
        remoteMeteorMacros.clear();
        lastBroadcastMeteorMacros.clear();
        syncedAssignments.clear();
        assignmentSetBy = "";
        isSearching = false;
        searchTicksRemaining = 0;
        lastSpreadResult = null;
        resetSyncState();
        lastHeartbeatTime.clear();
        stopTcp();
    }

    private void resetSyncState() {
        activeSyncCtx = null;
        setSyncState(SyncState.IDLE);
        executionTimestamps.clear();
        ackReceiptTimes.clear();
        clientsReady.clear();
        peerStepProgress.clear();
    }

    private void setSyncState(SyncState state) {
        this.syncState = state;
        fireCallback(onSyncStateChanged);
    }

    private void initiateGoSync(boolean isMacro, String macroName, String command) {
        initiateGoSync(isMacro, macroName, command, null);
    }

    private void initiateGoSync(boolean isMacro, String macroName, String command, Map<String, String> macroAssignments) {
        long executionId = System.nanoTime();

        if (isMacro) {
            if (macroAssignments != null && !macroAssignments.isEmpty()) {
                Map<String, String> missingAssignments = getMissingAssignedMacros(macroAssignments);
                if (!missingAssignments.isEmpty()) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMissing assigned macros on: " + summarizeAssignments(missingAssignments));
                    return;
                }
            } else if (macroName != null && !macroName.isBlank()) {
                List<String> missingPeers = getPeersMissingMacro(macroName);
                if (!missingPeers.isEmpty()) {
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMissing '" + macroName + "' on: " + summarizeNames(missingPeers));
                    return;
                }
            }
        }

        String myMacroName = macroName;
        if (macroAssignments != null && macroAssignments.containsKey(myUsername)) {
            myMacroName = macroAssignments.get(myUsername);
        }

        PackUtilMacro macro = null;
        if (isMacro && myMacroName != null) {
            macro = PackUtilMacroManager.get().get(myMacroName);
            if (macro != null) {
                macro.regenerateAllPackets();
            } else {
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMacro not found: " + myMacroName);
                return;
            }
        }

        activeSyncCtx = new SyncContext(executionId, isMacro, myMacroName, command, macro, true);
        clientsReady.clear();
        executionTimestamps.clear();
        ackReceiptTimes.clear();
        peerStepProgress.clear();
        clientsReady.add(myUsername);
        setSyncState(SyncState.PREPARING);

        AutismClientAddon.LOG.info("[Sync] Initiating GO-sync: execId={}", executionId);

        String assignmentStr = "";
        if (macroAssignments != null && !macroAssignments.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : macroAssignments.entrySet()) {
                if (sb.length() > 0) sb.append('\t');
                sb.append(entry.getKey()).append('\t').append(entry.getValue());
            }
            assignmentStr = sb.toString();
        }

        LanPacket.PrepareExecutionPacket preparePacket = new LanPacket.PrepareExecutionPacket(
            sessionId, executionId, isMacro, macroName != null ? macroName : "", command, myUsername);
        preparePacket.macroAssignments = assignmentStr;
        sendTcpPacket(preparePacket);

        broadcastSyncState(SyncState.PREPARING, clientsReady.size() + "/" + connectedClients.size() + " ready");

        new Thread(() -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
                long waitStart = System.currentTimeMillis();
                int expectedClients = connectedClients.size();

                while (clientsReady.size() < expectedClients) {
                    if (System.currentTimeMillis() - waitStart > 3000) {
                        AutismClientAddon.LOG.warn("[Sync] Timeout waiting for clients. Got {}/{}",
                            clientsReady.size(), expectedClients);
                        break;
                    }
                    java.util.concurrent.locks.LockSupport.parkNanos(500_000);
                }

                setSyncState(SyncState.ALL_READY);
                broadcastSyncState(SyncState.ALL_READY, clientsReady.size() + "/" + expectedClients + " ready");
                AutismClientAddon.LOG.info("[Sync] All {} clients ready!", clientsReady.size());

                setSyncState(SyncState.DISPATCHING_GO);
                byte[] goPayload = serializePacket(new LanPacket.GoPacket(sessionId, executionId));

                setSyncState(SyncState.EXECUTING);
                int hostOffset = getPlayerDelayOffset(myUsername);
                if (hostOffset > 0) {
                    try { Thread.sleep(hostOffset); } catch (InterruptedException ignored) {}
                }

                long hostExecNanos = System.nanoTime();
                ackReceiptTimes.put(myUsername, hostExecNanos);
                executeAction();

                for (Map.Entry<String, Socket> entry : tcpClients.entrySet()) {
                    Socket clientSocket = entry.getValue();
                    String clientName = entry.getKey();
                    try {
                        if (!clientSocket.isClosed()) {
                            Object lock = tcpClientWriteLocks.computeIfAbsent(clientName, k -> new Object());
                            synchronized (lock) {
                                OutputStream out = clientSocket.getOutputStream();
                                out.write(goPayload);
                                out.flush();
                            }
                        }
                    } catch (IOException e) {
                        AutismClientAddon.LOG.warn("[Sync] Failed to send GO to {}", clientName);
                    }
                }

            } catch (Exception e) {
                AutismClientAddon.LOG.error("[Sync] Error in GO-sync: ", e);
                setSyncState(SyncState.IDLE);
            }
        }, "Sync-GO-Initiator").start();
    }

    private void initiateGoSyncForQueue() {
        long executionId = System.nanoTime();

        activeSyncCtx = new SyncContext(executionId, false, null, "__QUEUE_FLUSH__", null, true);
        clientsReady.clear();
        executionTimestamps.clear();
        ackReceiptTimes.clear();
        clientsReady.add(myUsername);
        setSyncState(SyncState.PREPARING);

        AutismClientAddon.LOG.info("[Sync] Initiating queue flush GO-sync: execId={}", executionId);

        sendTcpPacket(new LanPacket.PrepareExecutionPacket(sessionId, executionId, false,
            "", "__QUEUE_FLUSH__", myUsername));

        broadcastSyncState(SyncState.PREPARING, clientsReady.size() + "/" + connectedClients.size() + " ready");

        new Thread(() -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
                long waitStart = System.currentTimeMillis();
                int expectedClients = connectedClients.size();

                while (clientsReady.size() < expectedClients) {
                    if (System.currentTimeMillis() - waitStart > 3000) {
                        AutismClientAddon.LOG.warn("[Sync] Timeout waiting for clients. Got {}/{}",
                            clientsReady.size(), expectedClients);
                        break;
                    }
                    java.util.concurrent.locks.LockSupport.parkNanos(500_000);
                }

                setSyncState(SyncState.ALL_READY);
                broadcastSyncState(SyncState.ALL_READY, clientsReady.size() + "/" + expectedClients + " ready");

                setSyncState(SyncState.DISPATCHING_GO);
                byte[] goPayload = serializePacket(new LanPacket.GoPacket(sessionId, executionId));

                setSyncState(SyncState.EXECUTING);
                int hostOffset = getPlayerDelayOffset(myUsername);
                if (hostOffset > 0) {
                    try { Thread.sleep(hostOffset); } catch (InterruptedException ignored) {}
                }

                long hostExecNanos = System.nanoTime();
                ackReceiptTimes.put(myUsername, hostExecNanos);
                executeAction();

                for (Map.Entry<String, Socket> entry : tcpClients.entrySet()) {
                    Socket clientSocket = entry.getValue();
                    String clientName = entry.getKey();
                    try {
                        if (!clientSocket.isClosed()) {
                            Object lock = tcpClientWriteLocks.computeIfAbsent(clientName, k -> new Object());
                            synchronized (lock) {
                                OutputStream out = clientSocket.getOutputStream();
                                out.write(goPayload);
                                out.flush();
                            }
                        }
                    } catch (IOException e) {
                        AutismClientAddon.LOG.warn("[Sync] Failed to send GO to {}", clientName);
                    }
                }

            } catch (Exception e) {
                AutismClientAddon.LOG.error("[Sync] Error in queue GO-sync: ", e);
                setSyncState(SyncState.IDLE);
            }
        }, "Sync-GO-QueueFlush").start();
    }

    private void handlePrepare(long executionId, boolean isMacro, String macroName, String command,
                               String senderUsername, String macroAssignments) {
        if (senderUsername != null && senderUsername.equals(myUsername)) return;

        AutismClientAddon.LOG.info("[Sync] Received PREPARE from {}", senderUsername);
        peerStepProgress.clear();

        String myMacroName = macroName;
        if (macroAssignments != null && !macroAssignments.isEmpty()) {
            String[] parts = macroAssignments.split("\t");
            for (int pi = 0; pi + 1 < parts.length; pi += 2) {
                if (parts[pi].equals(myUsername)) {
                    myMacroName = parts[pi + 1];
                    break;
                }
            }
        }

        PackUtilMacro macro = null;
        if (isMacro && myMacroName != null && !myMacroName.isEmpty()) {
            macro = PackUtilMacroManager.get().get(myMacroName);
            if (macro != null) {
                macro.regenerateAllPackets();
            }
        } else if ("__QUEUE_FLUSH__".equals(command)) {
            PackUtilSharedState.get().regenerateQueue();
        }

        activeSyncCtx = new SyncContext(executionId, isMacro, myMacroName, command, macro, false);
        setSyncState(SyncState.PREPARING);

        sendTcpPacket(new LanPacket.ClientReadyPacket(sessionId, executionId, myUsername));
        AutismClientAddon.LOG.info("[Sync] Sent READY signal");
    }

    private void handleClientReady(long executionId, String username) {
        SyncContext ctx = activeSyncCtx;
        if (ctx != null && executionId == ctx.executionId) {
            clientsReady.add(username);
            broadcastSyncState(SyncState.PREPARING, clientsReady.size() + "/" + connectedClients.size() + " ready");
            AutismClientAddon.LOG.info("[Sync] Client ready: {} (total: {})", username, clientsReady.size());
        }
    }

    private void handleGo(long executionId) {
        SyncContext ctx = activeSyncCtx;
        if (ctx == null || executionId != ctx.executionId) {
            AutismClientAddon.LOG.warn("[Sync] GO for wrong execId, ignoring");
            return;
        }
        if (ctx.isInitiator) return;

        int offset = getPlayerDelayOffset(myUsername);
        if (offset > 0) {
            AutismClientAddon.LOG.info("[Sync] Received GO! Applying {}ms offset before executing.", offset);
            setSyncState(SyncState.EXECUTING);
            try { Thread.sleep(offset); } catch (InterruptedException ignored) {}
            executeAction();
        } else {
            AutismClientAddon.LOG.info("[Sync] Received GO! Executing immediately.");
            setSyncState(SyncState.EXECUTING);
            executeAction();
        }
    }

    private void executeAction() {
        final long executedAtMs = System.currentTimeMillis();
        final SyncContext ctx = activeSyncCtx;
        if (ctx == null) return;

        Minecraft mc = Minecraft.getInstance();

        if ("__QUEUE_FLUSH__".equals(ctx.command)) {
            mc.execute(() -> {
                ClientPacketListener nh = mc.getConnection();
                if (nh != null) {
                    int count = PackUtilSharedState.get().flushDelayedPackets(nh);
                    AutismClientAddon.LOG.info("[Sync] Queue flushed! {} packets sent", count);
                }
            });
        } else if (ctx.command != null && ctx.command.startsWith("__CHAT__\t")) {
            String chatMsg = ctx.command.substring("__CHAT__\t".length());
            mc.execute(() -> {
                ClientPacketListener nh = mc.getConnection();
                if (nh != null) {
                    if (chatMsg.startsWith("/")) {
                        nh.sendCommand(chatMsg.substring(1));
                    } else {
                        nh.sendChat(chatMsg);
                    }
                    AutismClientAddon.LOG.info("[Sync] Chat sent: {}", chatMsg);
                }
            });
        } else if (ctx.isMacro && ctx.macro != null) {
            mc.execute(() -> ctx.macro.execute(false));
        }

        AutismClientAddon.LOG.info("[Sync] EXECUTED at {}", executedAtMs);

        executionTimestamps.put(myUsername, executedAtMs);
        sendTcpPacket(new LanPacket.TcpCommandAckPacket(sessionId, myUsername, executedAtMs));

        setSyncState(SyncState.REPORTING);

        if (ctx.isInitiator) {
            new Thread(() -> {
                try {

                    int expected = connectedClients.size();
                    long start = System.currentTimeMillis();
                    while (ackReceiptTimes.size() < expected && System.currentTimeMillis() - start < 500) {
                        java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                    }
                    calculateAndDisplaySpread();
                } catch (Exception e) {
                    AutismClientAddon.LOG.error("[Sync] Error calculating spread", e);
                }
            }, "Sync-SpreadCalc").start();
        }
    }

    private void handleExecutionTimestamp(String username, long timestampMs) {
        executionTimestamps.put(username, timestampMs);

        ackReceiptTimes.put(username, System.nanoTime());
        AutismClientAddon.LOG.info("[Sync] ACK from {}: remoteMs={}, hostReceiptNano={}", username, timestampMs, ackReceiptTimes.get(username));
    }

    private void calculateAndDisplaySpread() {

        Map<String, Long> timings = ackReceiptTimes.isEmpty() ? executionTimestamps : ackReceiptTimes;

        if (timings.size() < 2) {
            setSyncState(SyncState.DONE);
            return;
        }

        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        String earliestClient = "";
        String latestClient = "";

        for (Map.Entry<String, Long> entry : timings.entrySet()) {
            long ts = entry.getValue();
            if (ts < earliest) { earliest = ts; earliestClient = entry.getKey(); }
            if (ts > latest) { latest = ts; latestClient = entry.getKey(); }
        }

        boolean isNanos = !ackReceiptTimes.isEmpty();
        long spreadNanos = latest - earliest;
        long totalSpreadMs = isNanos ? spreadNanos / 1_000_000 : (latest - earliest);

        lastSpreadResult = new SpreadResult(isNanos ? spreadNanos : totalSpreadMs * 1_000_000, latestClient, timings.size());

        spreadHistory.add(lastSpreadResult);
        while (spreadHistory.size() > MAX_SPREAD_HISTORY) {
            spreadHistory.remove(0);
        }

        String report;
        if (totalSpreadMs == 0) {
            report = "Ã‚Â§aPerfect sync!";
        } else {
            report = "Ã‚Â§e" + latestClient + " Ã‚Â§7was Ã‚Â§c+" + totalSpreadMs + "ms Ã‚Â§7late";
        }

        PackUtilClientMessaging.sendPrefixed(report);

        sendTcpPacket(new LanPacket.ChatMessagePacket(sessionId, "__SYNC__", report));

        sendTcpPacket(new LanPacket.ChatMessagePacket(sessionId, "__SPREAD_DATA__",
            spreadNanos + "\t" + latestClient + "\t" + timings.size()));

        broadcastSyncState(SyncState.DONE, totalSpreadMs + "ms spread");
        setSyncState(SyncState.DONE);
        fireCallback(onSpreadCalculated);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            if (syncState == SyncState.DONE) setSyncState(SyncState.IDLE);
        }, "Sync-ResetIdle").start();
    }

    public void sendQueuedPackets() {
        if (sessionId == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cJoin a session first.");
            return;
        }
        PackUtilSharedState.get().regenerateQueue();

        if (isHost) {
            initiateGoSyncForQueue();
        } else {

            sendTcpPacket(new LanPacket.RequestSyncPacket(sessionId, myUsername, false, "", "__QUEUE_FLUSH__"));
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§eRequested sync execution from host...");
        }
    }

    public void executeMacroSynchronized(String macroName) {
        if (sessionId == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cJoin a session first.");
            return;
        }

        if (isHost) {
            initiateGoSync(true, macroName, "");
        } else {
            sendTcpPacket(new LanPacket.RequestSyncPacket(sessionId, myUsername, true, macroName, ""));
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§eRequested sync execution from host...");
        }
    }

    public void executeMacrosSynchronized(Map<String, String> assignments) {
        if (sessionId == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cJoin a session first.");
            return;
        }
        if (assignments == null || assignments.isEmpty()) return;

        String defaultMacro = assignments.values().iterator().next();

        if (isHost) {
            initiateGoSync(true, defaultMacro, "", assignments);
        } else {

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : assignments.entrySet()) {
                if (sb.length() > 0) sb.append('\t');
                sb.append(entry.getKey()).append('\t').append(entry.getValue());
            }
            LanPacket.RequestSyncPacket rsp = new LanPacket.RequestSyncPacket(
                sessionId, myUsername, true, defaultMacro, "__PER_USER__\t" + sb);
            sendTcpPacket(rsp);
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§eRequested per-user sync from host...");
        }
    }

    public static class SpreadResult {
        public final long spreadNanos;
        public final String lateClient;
        public final int clientCount;

        public SpreadResult(long spreadNanos, String lateClient, int clientCount) {
            this.spreadNanos = spreadNanos;
            this.lateClient = lateClient;
            this.clientCount = clientCount;
        }

        public double getSpreadMs() { return spreadNanos / 1_000_000.0; }
    }

    public void startSearching() {
        if (!running.get()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cStart LAN Sync first.");
            return;
        }
        if (isInSession()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cAlready in session.");
            return;
        }
        if (myUsername.isEmpty()) myUsername = getUsername();

        discoveredSessions.clear();
        isSearching = true;
        searchTicksRemaining = SEARCH_DURATION_TICKS;

        new Thread(() -> {
            while (isSearching && searchTicksRemaining > 0) {
                try {
                    Socket searchSocket = new Socket();
                    searchSocket.connect(new InetSocketAddress("127.0.0.1", TCP_PORT), 500);
                    searchSocket.setTcpNoDelay(true);

                    DataOutputStream out = new DataOutputStream(searchSocket.getOutputStream());
                    DataInputStream in = new DataInputStream(searchSocket.getInputStream());

                    out.writeInt(LanPacketType.SEARCH_REQUEST.getId());
                    out.writeUTF("");
                    out.flush();

                    int typeId = in.readInt();
                    String foundSessionId = in.readUTF();

                    if (typeId == LanPacketType.SESSION.getId()) {
                        LanPacket.SessionPacket session = new LanPacket.SessionPacket();
                        session.setSessionId(foundSessionId);
                        session.read(in);
                        discoveredSessions.put(foundSessionId, session.hostName);
                    }
                    searchSocket.close();
                    Thread.sleep(1000);
                } catch (IOException e) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "TCP-Search-Loop").start();
    }

    public void cancelSearch() {
        isSearching = false;
        searchTicksRemaining = 0;
    }

    public void tick() {
        if (!running.get() && sessionId == null) return;

        if (sessionId != null) {
            long nowMs = System.currentTimeMillis();
            if (nowMs - lastHeartbeatSentMs >= HEARTBEAT_INTERVAL_MS) {
                lastHeartbeatSentMs = nowMs;
                sendTcpPacket(new LanPacket.HeartbeatPacket(sessionId, myUsername, nowMs));
                checkPeerHealth();
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long currentTick = mc.level.getGameTime();

        if (sessionId != null && currentTick % 40 == 10) {
            checkAndBroadcastMacroListChanges();
        }

        if (isSearching && searchTicksRemaining > 0) {
            searchTicksRemaining--;

            if (!discoveredSessions.isEmpty()) {
                String firstSessionId = discoveredSessions.keySet().iterator().next();
                String hostName = discoveredSessions.get(firstSessionId);
                isSearching = false;
                searchTicksRemaining = 0;
                joinSession(firstSessionId);
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§aJoined session " + firstSessionId + " (Host: " + hostName + ").");
                return;
            }

            if (searchTicksRemaining == 0) {
                isSearching = false;
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cNo sessions found.");
            }
        }
    }

    private void checkPeerHealth() {
        long now = System.currentTimeMillis();
        List<String> deadPeers = new ArrayList<>();

        for (Map.Entry<String, Long> entry : lastHeartbeatTime.entrySet()) {
            String peer = entry.getKey();
            if (peer.equals(myUsername)) continue;
            long elapsed = now - entry.getValue();

            if (elapsed > HEARTBEAT_DEAD_MS) {
                deadPeers.add(peer);
            }
        }

        for (String dead : deadPeers) {
            connectedClients.remove(dead);
            lastHeartbeatTime.remove(dead);
            clientMacroLists.remove(dead);
            remoteMeteorMacros.remove(dead);
            peerStepProgress.remove(dead);

            if (isHost) {
                Socket s = tcpClients.remove(dead);
                if (s != null) try { s.close(); } catch (IOException ignored) {}
                tcpClientWriteLocks.remove(dead);
            }

            PackUtilClientMessaging.sendPrefixed("Ã‚Â§e" + dead + " timed out.");
            AutismClientAddon.LOG.info("[Heartbeat] Peer timed out: {}", dead);
            fireCallback(onClientLeft);
            fireCallback(onPeerStatusChanged);
            autismclient.util.macro.MacroConditionRegistry.onLanStepProgress();
        }

        if (isHost && !deadPeers.isEmpty()) {
            broadcastClientList();
        }
    }

    public PeerStatus getPeerStatus(String username) {
        if (username.equals(myUsername)) return PeerStatus.HEALTHY;
        Long lastBeat = lastHeartbeatTime.get(username);
        if (lastBeat == null) return PeerStatus.UNKNOWN;
        long elapsed = System.currentTimeMillis() - lastBeat;
        if (elapsed > HEARTBEAT_STALE_MS) return PeerStatus.STALE;
        return PeerStatus.HEALTHY;
    }

    public enum PeerStatus { HEALTHY, STALE, UNKNOWN }

    private void handlePacket(LanPacket packet) {
        try {
            switch (packet.getType()) {
                case SEARCH_REQUEST:
                    break;

                case SESSION:
                    if (packet instanceof LanPacket.SessionPacket) {
                        LanPacket.SessionPacket p = (LanPacket.SessionPacket) packet;
                        String sid = p.getSessionId();
                        String host = p.hostName;
                        if (!host.equals(myUsername) && !isInSession()) {
                            boolean isNew = !discoveredSessions.containsKey(sid);
                            discoveredSessions.put(sid, host);
                            if (isNew) {
                                PackUtilClientMessaging.sendPrefixed("Ã‚Â§eFound session " + sid + " (Host: " + host + ").");
                            }
                        }
                    }
                    break;

                case JOIN:
                    if (packet instanceof LanPacket.JoinPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.JoinPacket p = (LanPacket.JoinPacket) packet;
                        String username = p.username;
                        if (username != null && !username.isEmpty()
                                && !username.equals(myUsername) && !connectedClients.containsKey(username)) {
                            connectedClients.put(username, new ClientInfo(username, false));
                            lastHeartbeatTime.put(username, System.currentTimeMillis());
                            PackUtilClientMessaging.sendPrefixed("Ã‚Â§e" + username + " joined.");
                            executionTimestamps.clear();
                            if (isHost) {
                                broadcastClientList();
                                broadcastMacroList();
                            }
                            fireCallback(onClientJoined);
                            fireCallback(onPeerStatusChanged);
                        }
                    }
                    break;

                case CLIENT_LIST:
                    if (packet instanceof LanPacket.ClientListPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.ClientListPacket p = (LanPacket.ClientListPacket) packet;
                        if (!p.clients.isEmpty()) {
                            ClientInfo ourInfo = connectedClients.get(myUsername);
                            connectedClients.clear();
                            long now = System.currentTimeMillis();
                            java.util.Set<String> listedPeers = new java.util.HashSet<>();
                            for (LanPacket.ClientListPacket.ClientEntry entry : p.clients) {
                                connectedClients.put(entry.name, new ClientInfo(entry.name, entry.isHost));
                                if (!entry.name.equals(myUsername)) {

                                    lastHeartbeatTime.put(entry.name, now);
                                    listedPeers.add(entry.name);
                                }
                            }

                            lastHeartbeatTime.keySet().retainAll(listedPeers);
                            if (!connectedClients.containsKey(myUsername) && ourInfo != null) {
                                connectedClients.put(myUsername, ourInfo);
                            }
                        }
                    }
                    break;

                case LEAVE:
                    if (packet instanceof LanPacket.LeavePacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.LeavePacket p = (LanPacket.LeavePacket) packet;
                        String username = p.username;
                        if (username != null && !username.isEmpty() && !username.equals(myUsername)) {
                            ClientInfo leavingClient = connectedClients.get(username);
                            boolean leavingWasHost = leavingClient != null && leavingClient.isHost;
                            connectedClients.remove(username);
                            lastHeartbeatTime.remove(username);
                            clientMacroLists.remove(username);
                            remoteMeteorMacros.remove(username);
                            peerStepProgress.remove(username);
                            PackUtilClientMessaging.sendPrefixed("Ã‚Â§e" + username + " left.");

                            if (isHost) {

                                Socket s = tcpClients.remove(username);
                                if (s != null) try { s.close(); } catch (IOException ignored) {}
                                tcpClientWriteLocks.remove(username);

                                relayTcpPacketToClients(packet, username);
                                broadcastClientList();
                            }

                            if (leavingWasHost && !connectedClients.isEmpty()) {
                                String newHostUsername = connectedClients.keySet().stream()
                                    .sorted().findFirst().orElse(null);
                                if (newHostUsername != null && newHostUsername.equals(myUsername)) {
                                    isHost = true;
                                    connectedClients.put(myUsername, new ClientInfo(myUsername, true));
                                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§aYou are now the host!");
                                    executionTimestamps.clear();
                                    broadcastClientList();
                                }
                            }
                            fireCallback(onClientLeft);
                            fireCallback(onPeerStatusChanged);
                            autismclient.util.macro.MacroConditionRegistry.onLanStepProgress();
                        }
                    }
                    break;

                case PLAYER_OFFSETS:

                    break;

                case CLIENT_OFFSET_UPDATE:

                    break;

                case REQUEST_CLIENT_LIST:
                    if (isHost) broadcastClientList();
                    break;

                case REQUEST_MACRO:
                    handleMacroRequest(packet);
                    break;

                case MACRO_DATA:
                    handleMacroData(packet);
                    break;

                case MACRO_DATA_CHUNK:
                    handleMacroDataChunk(packet);
                    break;

                case MACRO_DELETE:
                    handleMacroDelete(packet);
                    break;

                case PRESET_DATA:
                    handlePresetData(packet);
                    break;

                case QUEUE_SYNC:
                    if (packet instanceof LanPacket.QueueSyncPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.QueueSyncPacket qp = (LanPacket.QueueSyncPacket) packet;
                        handleQueueSync(qp.queueData, qp.senderUsername);
                    }
                    break;

                case CHAT_MESSAGE:
                    if (packet instanceof LanPacket.ChatMessagePacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.ChatMessagePacket cp = (LanPacket.ChatMessagePacket) packet;
                        handleChatMessage(cp.senderUsername, cp.message);
                    }
                    break;

                case PREPARE_EXECUTION:
                    if (packet instanceof LanPacket.PrepareExecutionPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.PrepareExecutionPacket p = (LanPacket.PrepareExecutionPacket) packet;
                        handlePrepare(p.executionId, p.isMacro, p.targetName, p.queueData, p.senderUsername, p.macroAssignments);
                    }
                    break;

                case CLIENT_READY:
                    if (packet instanceof LanPacket.ClientReadyPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.ClientReadyPacket p = (LanPacket.ClientReadyPacket) packet;
                        handleClientReady(p.executionId, p.senderUsername);
                    }
                    break;

                case EXECUTE_NOW:

                    break;

                case GO:
                    if (packet instanceof LanPacket.GoPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.GoPacket p = (LanPacket.GoPacket) packet;
                        handleGo(p.executionId);
                    }
                    break;

                case TCP_COMMAND_ACK:
                    if (packet instanceof LanPacket.TcpCommandAckPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.TcpCommandAckPacket p = (LanPacket.TcpCommandAckPacket) packet;
                        handleExecutionTimestamp(p.senderUsername, p.executeNanoTime);
                    }
                    break;

                case HEARTBEAT:
                    if (packet instanceof LanPacket.HeartbeatPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.HeartbeatPacket p = (LanPacket.HeartbeatPacket) packet;
                        if (!p.senderUsername.equals(myUsername)) {
                            lastHeartbeatTime.put(p.senderUsername, System.currentTimeMillis());

                            if (isHost) {
                                relayTcpPacketToClients(packet, p.senderUsername);
                            }
                        }
                    }
                    break;

                case REQUEST_SYNC:
                    if (packet instanceof LanPacket.RequestSyncPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.RequestSyncPacket p = (LanPacket.RequestSyncPacket) packet;
                        handleRequestSync(p.senderUsername, p.isMacro, p.macroName, p.command);
                    }
                    break;

                case OFFSET_SYNC:
                    if (packet instanceof LanPacket.OffsetSyncPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.OffsetSyncPacket p = (LanPacket.OffsetSyncPacket) packet;
                        handleOffsetSync(p.senderUsername, p.targetUsername, p.offsetMs);
                    }
                    break;

                case MACRO_STEP_PROGRESS:
                    if (packet instanceof LanPacket.MacroStepProgressPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.MacroStepProgressPacket p = (LanPacket.MacroStepProgressPacket) packet;
                        handleStepProgress(p.senderUsername, p.completedStep, p.totalSteps, p.macroName);
                    }
                    break;

                case SYNC_STATE_UPDATE:
                    if (packet instanceof LanPacket.SyncStateUpdatePacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.SyncStateUpdatePacket p = (LanPacket.SyncStateUpdatePacket) packet;
                        SyncState[] states = SyncState.values();
                        if (p.stateOrdinal >= 0 && p.stateOrdinal < states.length) {

                            SyncContext ctx = activeSyncCtx;
                            if (ctx == null || !ctx.isInitiator) {
                                syncState = states[p.stateOrdinal];
                                fireCallback(onSyncStateChanged);
                            }
                        }
                    }
                    break;

                case MACRO_ASSIGNMENT_SYNC:
                    if (packet instanceof LanPacket.MacroAssignmentSyncPacket && packet.getSessionId().equals(sessionId)) {
                        LanPacket.MacroAssignmentSyncPacket p = (LanPacket.MacroAssignmentSyncPacket) packet;
                        if (!p.senderUsername.equals(myUsername)) {
                            handleAssignmentSync(p.senderUsername, p.assignments);
                        }
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            AutismClientAddon.LOG.error("[PackUtil-LAN] Error handling packet: {}", packet.getType(), e);
        }
    }

    private void startTcpServer() {
        try {
            tcpServer = new ServerSocket(TCP_PORT);
            tcpServer.setReuseAddress(true);

            tcpAcceptThread = new Thread(() -> {
                while (running.get() && isHost && tcpServer != null && !tcpServer.isClosed()) {
                    try {
                        Socket clientSocket = tcpServer.accept();
                        clientSocket.setTcpNoDelay(true);
                        handleNewTcpClient(clientSocket);
                    } catch (IOException e) {
                        if (running.get() && isHost) {
                            AutismClientAddon.LOG.warn("[TCP] Accept error: {}", e.getMessage());
                        }
                    }
                }
            }, "TCP-Accept");
            tcpAcceptThread.setDaemon(true);
            tcpAcceptThread.start();

            AutismClientAddon.LOG.info("[TCP] Server started on port {}", TCP_PORT);
        } catch (IOException e) {
            AutismClientAddon.LOG.error("[TCP] Failed to start server: {}", e.getMessage());
        }
    }

    private void handleNewTcpClient(Socket clientSocket) {
        new Thread(() -> {
            try {
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                int packetType = in.readInt();
                String packetSession = in.readUTF();

                if (packetType == LanPacketType.SEARCH_REQUEST.getId()) {
                    AutismClientAddon.LOG.info("[TCP] Received search request, responding with session info");
                    out.writeInt(LanPacketType.SESSION.getId());
                    out.writeUTF(sessionId);
                    new LanPacket.SessionPacket(sessionId, 0, myUsername).write(out);
                    out.flush();
                    clientSocket.close();
                    return;
                }

                if (packetType == LanPacketType.JOIN.getId()) {
                    LanPacket.JoinPacket joinPacket = new LanPacket.JoinPacket();
                    joinPacket.setSessionId(packetSession);
                    joinPacket.read(in);

                    String clientUsername = joinPacket.username;

                    if (clientUsername == null || clientUsername.isBlank()) {
                        AutismClientAddon.LOG.warn("[TCP] Rejected client with empty username");
                        clientSocket.close();
                        return;
                    }

                    Socket oldSocket = tcpClients.put(clientUsername, clientSocket);
                    if (oldSocket != null && oldSocket != clientSocket) {
                        try { oldSocket.close(); } catch (IOException ignored) {}
                    }
                    tcpClientWriteLocks.put(clientUsername, new Object());
                    lastHeartbeatTime.put(clientUsername, System.currentTimeMillis());

                    if (!clientUsername.equals(myUsername) && !connectedClients.containsKey(clientUsername)) {
                        connectedClients.put(clientUsername, new ClientInfo(clientUsername, false));
                        PackUtilClientMessaging.sendPrefixed("Ã‚Â§e" + clientUsername + " joined.");
                        executionTimestamps.clear();

                        broadcastClientList();
                        sendMacroListToClient(clientSocket, clientUsername);

                        fireCallback(onClientJoined);
                        fireCallback(onPeerStatusChanged);
                    }

                    AutismClientAddon.LOG.info("[TCP] Client joined: {}", clientUsername);

                    while (running.get() && !clientSocket.isClosed()) {
                        int nextPacketType = in.readInt();
                        String nextPacketSession = in.readUTF();

                        LanPacketType type = LanPacketType.fromId(nextPacketType);
                        if (type == null) continue;
                        LanPacket packet = LanPacket.create(type, nextPacketSession);
                        if (packet != null) {
                            packet.read(in);
                            handlePacket(packet);
                        }
                    }
                }
            } catch (IOException e) {

            } finally {
                try { clientSocket.close(); } catch (IOException ignored) {}

                if (running.get() && isHost) {
                    String disconnectedUser = null;
                    for (Map.Entry<String, Socket> entry : tcpClients.entrySet()) {
                        if (entry.getValue() == clientSocket) {
                            disconnectedUser = entry.getKey();
                            break;
                        }
                    }

                    if (disconnectedUser != null) {
                        tcpClients.remove(disconnectedUser);
                        tcpClientWriteLocks.remove(disconnectedUser);
                        connectedClients.remove(disconnectedUser);
                        clientMacroLists.remove(disconnectedUser);
                        remoteMeteorMacros.remove(disconnectedUser);
                        lastHeartbeatTime.remove(disconnectedUser);
                        peerStepProgress.remove(disconnectedUser);

                        PackUtilClientMessaging.sendPrefixed("Ã‚Â§e" + disconnectedUser + " disconnected.");
                        AutismClientAddon.LOG.info("[TCP] Client disconnected: {}", disconnectedUser);

                        broadcastClientList();
                        fireCallback(onClientLeft);

                        autismclient.util.macro.MacroConditionRegistry.onLanStepProgress();
                    }
                }
            }
        }, "TCP-Client-Handler").start();
    }

    private void sendClientListToClient(Socket clientSocket, String clientUsername) {
        try {
            List<LanPacket.ClientListPacket.ClientEntry> entries = new ArrayList<>();
            for (Map.Entry<String, ClientInfo> entry : connectedClients.entrySet()) {
                entries.add(new LanPacket.ClientListPacket.ClientEntry(entry.getKey(), entry.getValue().isHost));
            }

            LanPacket.ClientListPacket packet = new LanPacket.ClientListPacket(sessionId, entries);
            byte[] data = serializePacket(packet);

            Object lock = tcpClientWriteLocks.computeIfAbsent(clientUsername, k -> new Object());
            synchronized (lock) {
                clientSocket.getOutputStream().write(data);
                clientSocket.getOutputStream().flush();
            }
        } catch (IOException e) {
            AutismClientAddon.LOG.warn("[TCP] Failed to send client list: {}", e.getMessage());
        }
    }

    private void sendMacroListToClient(Socket clientSocket, String clientUsername) {
        try {
            List<PackUtilMacro> localMacros = PackUtilMacroManager.get().getAll();
            Object lock = tcpClientWriteLocks.computeIfAbsent(clientUsername, k -> new Object());

            for (PackUtilMacro macro : localMacros) {
                writeMacroDataPacketsToStream(clientSocket.getOutputStream(), lock, myUsername, macro, (byte)0);
            }
        } catch (IOException e) {
            AutismClientAddon.LOG.warn("[TCP] Failed to send macro list: {}", e.getMessage());
        }
    }

    private void connectToTcpServer(String hostIp) {
        new Thread(() -> {
            try {
                tcpHostSocket = new Socket(hostIp, TCP_PORT);
                tcpHostSocket.setTcpNoDelay(true);
                DataOutputStream out = new DataOutputStream(tcpHostSocket.getOutputStream());

                LanPacket.JoinPacket joinPacket = new LanPacket.JoinPacket(sessionId, myUsername);
                synchronized (tcpHostWriteLock) {
                    out.writeInt(joinPacket.getType().getId());
                    out.writeUTF(joinPacket.getSessionId());
                    joinPacket.write(out);
                    out.flush();
                }

                AutismClientAddon.LOG.info("[TCP] Connected to host at {}:{}", hostIp, TCP_PORT);
                reconnecting = false;

                sendMacroListViaConnection(out);

                tcpReadThread = new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(tcpHostSocket.getInputStream());
                        while (running.get() && tcpHostSocket != null && !tcpHostSocket.isClosed()) {
                            int packetType = in.readInt();
                            String packetSession = in.readUTF();

                            LanPacketType type = LanPacketType.fromId(packetType);
                            if (type == null) continue;
                            LanPacket packet = LanPacket.create(type, packetSession);
                            if (packet != null) {
                                packet.read(in);
                                handlePacket(packet);
                            }
                        }
                    } catch (IOException e) {
                        AutismClientAddon.LOG.warn("[TCP] Disconnected from host");
                        attemptReconnect();
                    }
                }, "TCP-Reader");
                tcpReadThread.setDaemon(true);
                tcpReadThread.start();

            } catch (IOException e) {
                AutismClientAddon.LOG.warn("[TCP] Failed to connect to host: {}", e.getMessage());
                attemptReconnect();
            }
        }, "TCP-Connect").start();
    }

    private void attemptReconnect() {
        if (reconnecting || !running.get() || lastHostIp == null || lastSessionId == null) return;
        if (isHost) return;

        reconnecting = true;
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§eConnection lost. Reconnecting...");

        new Thread(() -> {
            long backoffMs = 1000;
            int attempt = 0;

            while (reconnecting && running.get() && lastHostIp != null) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    return;
                }

                if (!reconnecting || !running.get() || lastHostIp == null) return;

                attempt++;
                AutismClientAddon.LOG.info("[TCP] Reconnect attempt {}...", attempt);

                try {
                    Socket testSocket = new Socket();
                    testSocket.connect(new InetSocketAddress(lastHostIp, TCP_PORT), 2000);
                    testSocket.setTcpNoDelay(true);
                    testSocket.close();

                    sessionId = lastSessionId;
                    if (myUsername.isEmpty()) myUsername = getUsername();
                    connectedClients.putIfAbsent(myUsername, new ClientInfo(myUsername, false));
                    connectToTcpServer(lastHostIp);
                    PackUtilClientMessaging.sendPrefixed("Ã‚Â§aReconnected to session!");
                    return;
                } catch (IOException e) {
                    AutismClientAddon.LOG.warn("[TCP] Reconnect attempt {} failed", attempt);
                }

                backoffMs = Math.min(backoffMs * 2, 16_000);
            }

            reconnecting = false;
        }, "TCP-Reconnect").start();
    }

    private void sendMacroListViaConnection(DataOutputStream out) {
        try {
            List<PackUtilMacro> localMacros = PackUtilMacroManager.get().getAll();
            synchronized (tcpHostWriteLock) {
                for (PackUtilMacro macro : localMacros) {
                    writeMacroDataPacketsToStream(out, null, myUsername, macro, (byte)0);
                }
            }
        } catch (IOException e) {
            AutismClientAddon.LOG.warn("[TCP] Failed to send macro list: {}", e.getMessage());
        }
    }

    private byte[] serializePacket(LanPacket packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(packet.getType().getId());
        out.writeUTF(packet.getSessionId());
        packet.write(out);
        return baos.toByteArray();
    }

    private void writeMacroDataPacketsToStream(OutputStream out, Object lock, String senderUsername,
                                               PackUtilMacro macro, byte sourceType) throws IOException {
        if (macro == null) return;
        String nbtString = macro.toTag().toString();
        List<LanPacket> packets = createMacroDataPackets(senderUsername, macro.name, nbtString, sourceType);
        for (LanPacket packet : packets) {
            byte[] data = serializePacket(packet);
            if (lock != null) {
                synchronized (lock) {
                    out.write(data);
                    out.flush();
                }
            } else {
                out.write(data);
                out.flush();
            }
        }
    }

    private void sendTcpPacket(LanPacket packet) {
        try {
            byte[] data = serializePacket(packet);

            if (isHost) {
                for (Map.Entry<String, Socket> entry : tcpClients.entrySet()) {
                    Socket client = entry.getValue();
                    String clientName = entry.getKey();
                    try {
                        if (!client.isClosed()) {
                            Object lock = tcpClientWriteLocks.computeIfAbsent(clientName, k -> new Object());
                            synchronized (lock) {
                                client.getOutputStream().write(data);
                                client.getOutputStream().flush();
                            }
                        }
                    } catch (IOException e) {

                    }
                }
            } else {
                synchronized (tcpHostWriteLock) {
                    if (tcpHostSocket != null && !tcpHostSocket.isClosed()) {
                        tcpHostSocket.getOutputStream().write(data);
                        tcpHostSocket.getOutputStream().flush();
                    }
                }
            }
        } catch (IOException e) {
            AutismClientAddon.LOG.warn("[TCP] Failed to send packet: {}", e.getMessage());
        }
    }

    private void relayTcpPacketToClients(LanPacket packet, String excludedUsername) {
        if (!isHost) return;
        try {
            byte[] data = serializePacket(packet);
            for (Map.Entry<String, Socket> entry : tcpClients.entrySet()) {
                String clientName = entry.getKey();
                if (excludedUsername != null && excludedUsername.equals(clientName)) continue;

                Socket client = entry.getValue();
                try {
                    if (!client.isClosed()) {
                        Object lock = tcpClientWriteLocks.computeIfAbsent(clientName, k -> new Object());
                        synchronized (lock) {
                            client.getOutputStream().write(data);
                            client.getOutputStream().flush();
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException e) {
            AutismClientAddon.LOG.warn("[TCP] Failed to relay packet: {}", e.getMessage());
        }
    }

    private void stopTcp() {
        try {
            if (tcpServer != null) {
                tcpServer.close();
                tcpServer = null;
            }
            if (tcpHostSocket != null) {
                tcpHostSocket.close();
                tcpHostSocket = null;
            }
            for (Socket s : tcpClients.values()) {
                try { s.close(); } catch (IOException ignored) {}
            }
            tcpClients.clear();
            tcpClientWriteLocks.clear();
            pendingMacroTransfers.clear();
        } catch (IOException e) {
            AutismClientAddon.LOG.warn("[TCP] Stop error: {}", e.getMessage());
        }
    }

    private void broadcastClientList() {
        if (!isHost) return;
        List<LanPacket.ClientListPacket.ClientEntry> entries = new ArrayList<>();
        for (Map.Entry<String, ClientInfo> entry : connectedClients.entrySet()) {
            entries.add(new LanPacket.ClientListPacket.ClientEntry(entry.getKey(), entry.getValue().isHost));
        }
        sendTcpPacket(new LanPacket.ClientListPacket(sessionId, entries));
    }

    private void broadcastSyncState(SyncState state, String detail) {
        if (isHost) {
            sendTcpPacket(new LanPacket.SyncStateUpdatePacket(sessionId, state.ordinal(), detail));
        }
    }

    private void checkAndBroadcastMacroListChanges() {
        try {
            Map<String, String> currentMeteorMacros = snapshotMeteorMacros();
            boolean changed = currentMeteorMacros.size() != lastBroadcastMeteorMacros.size();

            if (!changed) {
                for (Map.Entry<String, String> entry : currentMeteorMacros.entrySet()) {
                    String lastValue = lastBroadcastMeteorMacros.get(entry.getKey());
                    if (!entry.getValue().equals(lastValue)) {
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                for (String previousName : new java.util.ArrayList<>(lastBroadcastMeteorMacros.keySet())) {
                    if (!currentMeteorMacros.containsKey(previousName)) {
                        broadcastMacroDeletion(previousName);
                    }
                }
                lastBroadcastMeteorMacros = new ConcurrentHashMap<>(currentMeteorMacros);
                broadcastMacroList();
            }
        } catch (Exception e) {
            AutismClientAddon.LOG.error("[PackUtil-LAN] Failed to check macro list changes", e);
        }
    }

    public void broadcastMacroList() {
        if (sessionId == null) return;

        List<PackUtilMacro> localMacros = PackUtilMacroManager.get().getAll();
        for (PackUtilMacro macro : localMacros) {
            sendMacroData(myUsername, macro, (byte)0);
        }

        List<PackUtilMacro> meteorMacros = MeteorMacroAdapter.getMeteorMacros();
        for (PackUtilMacro macro : meteorMacros) {
            sendMacroData(myUsername, macro, (byte)1);
        }

        lastBroadcastMeteorMacros = new ConcurrentHashMap<>(snapshotMeteorMacros());
    }

    private void sendMacroData(String senderUsername, PackUtilMacro macro, byte sourceType) {
        if (macro == null || sessionId == null) return;
        String nbtString = macro.toTag().toString();
        for (LanPacket packet : createMacroDataPackets(senderUsername, macro.name, nbtString, sourceType)) {
            sendTcpPacket(packet);
        }
    }

    private List<LanPacket> createMacroDataPackets(String senderUsername, String macroName, String nbtData, byte sourceType) {
        String safeSender = senderUsername != null ? senderUsername : "";
        String safeName = macroName != null ? macroName : "";
        String safeNbt = nbtData != null ? nbtData : "";
        List<LanPacket> packets = new ArrayList<>();
        if (modifiedUtfLength(safeNbt) <= MACRO_INLINE_UTF_LIMIT) {
            packets.add(new LanPacket.MacroDataPacket(sessionId, safeSender, safeName, safeNbt, sourceType));
            return packets;
        }

        byte[] bytes = safeNbt.getBytes(StandardCharsets.UTF_8);
        int totalChunks = Math.max(1, (bytes.length + MACRO_CHUNK_SIZE - 1) / MACRO_CHUNK_SIZE);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * MACRO_CHUNK_SIZE;
            int len = Math.min(MACRO_CHUNK_SIZE, bytes.length - start);
            byte[] chunk = new byte[len];
            System.arraycopy(bytes, start, chunk, 0, len);
            packets.add(new LanPacket.MacroDataChunkPacket(sessionId, safeSender, safeName, sourceType, i, totalChunks, chunk));
        }
        return packets;
    }

    private static int modifiedUtfLength(String value) {
        if (value == null) return 0;
        int length = 0;
        for (int i = 0; i < value.length(); i++) {
            int c = value.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                length += 1;
            } else if (c <= 0x07FF) {
                length += 2;
            } else {
                length += 3;
            }
        }
        return length;
    }

    private Map<String, String> snapshotMeteorMacros() {
        Map<String, String> snapshot = new java.util.LinkedHashMap<>();
        for (PackUtilMacro macro : MeteorMacroAdapter.getMeteorMacros()) {
            if (macro == null || macro.name == null || macro.name.isBlank()) continue;
            snapshot.put(macro.name, macro.toTag().toString());
        }
        return snapshot;
    }

    private void handleMacroData(LanPacket packet) {
        handleMacroData(packet, true);
    }

    private void handleMacroData(LanPacket packet, boolean relayIfHost) {
        if (!(packet instanceof LanPacket.MacroDataPacket) || !packet.getSessionId().equals(sessionId)) return;
        LanPacket.MacroDataPacket p = (LanPacket.MacroDataPacket) packet;
        String sender = p.senderUsername;
        if (sender != null && sender.equals(myUsername)) return;

        try {
            net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.TagParser.parseCompoundFully(p.nbtData);
            PackUtilMacro macro = new PackUtilMacro();
            macro.fromTag(nbt);

            if (p.sourceType == 1) {
                remoteMeteorMacros.putIfAbsent(sender, new ConcurrentHashMap<>());
                remoteMeteorMacros.get(sender).put(macro.name, macro);
            } else {
                clientMacroLists.putIfAbsent(sender, new ConcurrentHashMap<>());
                clientMacroLists.get(sender).put(macro.name, macro);
            }

            if (p.sourceType == 2) {
                importSharedMacroIfMissing(macro, sender);
            }

            if (relayIfHost && isHost && sender != null && !sender.isBlank()) {
                relayTcpPacketToClients(new LanPacket.MacroDataPacket(sessionId, sender, macro.name, p.nbtData, p.sourceType), sender);
            }
        } catch (Exception e) {
            AutismClientAddon.LOG.error("[PackUtil-LAN] Failed to deserialize macro", e);
        }
    }

    private void handleMacroDataChunk(LanPacket packet) {
        if (!(packet instanceof LanPacket.MacroDataChunkPacket) || !packet.getSessionId().equals(sessionId)) return;
        LanPacket.MacroDataChunkPacket p = (LanPacket.MacroDataChunkPacket) packet;
        String sender = p.senderUsername;
        if (sender != null && sender.equals(myUsername)) return;
        if (p.totalChunks <= 0 || p.totalChunks > 4096 || p.chunkIndex < 0 || p.chunkIndex >= p.totalChunks) return;
        if (p.chunkData == null || p.chunkData.length == 0 || p.chunkData.length > MACRO_CHUNK_SIZE) return;

        if (isHost && sender != null && !sender.isBlank()) {
            relayTcpPacketToClients(new LanPacket.MacroDataChunkPacket(
                sessionId, sender, p.macroName, p.sourceType, p.chunkIndex, p.totalChunks, p.chunkData
            ), sender);
        }

        cleanupStaleMacroTransfers();
        String key = sender + "\u0000" + p.macroName + "\u0000" + p.sourceType + "\u0000" + p.totalChunks;
        PendingMacroTransfer transfer = pendingMacroTransfers.computeIfAbsent(key, ignored -> new PendingMacroTransfer(p.totalChunks));
        byte[] complete = transfer.addChunk(p.chunkIndex, p.chunkData);
        if (complete == null) return;

        pendingMacroTransfers.remove(key);
        String nbtData = new String(complete, StandardCharsets.UTF_8);
        handleMacroData(new LanPacket.MacroDataPacket(sessionId, sender, p.macroName, nbtData, p.sourceType), false);
    }

    private void cleanupStaleMacroTransfers() {
        long now = System.currentTimeMillis();
        pendingMacroTransfers.entrySet().removeIf(entry -> now - entry.getValue().createdAtMs > MACRO_CHUNK_TIMEOUT_MS);
    }

    private void importSharedMacroIfMissing(PackUtilMacro macro, String sender) {
        if (macro == null || macro.name == null || macro.name.isBlank()) return;

        PackUtilMacroManager manager = PackUtilMacroManager.get();
        PackUtilMacro existing = manager.get(macro.name);
        if (existing != null) {

            PackUtilMacro fresh = macro.deepCopy(existing.name);
            existing.actions.clear();
            existing.actions.addAll(fresh.actions);
            existing.loop = fresh.loop;
            existing.loopCount = fresh.loopCount;
            manager.save();
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§aUpdated macro from " + sender + ": " + existing.name);
            return;
        }

        PackUtilMacro installed = macro.deepCopy(macro.name);
        manager.add(installed);
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§aReceived macro from " + sender + ": " + installed.name);
    }

    private void handleMacroRequest(LanPacket packet) {
        if (!(packet instanceof LanPacket.RequestMacroPacket) || !packet.getSessionId().equals(sessionId)) return;
        LanPacket.RequestMacroPacket p = (LanPacket.RequestMacroPacket) packet;
        String requestedName = p.macroName == null ? "" : p.macroName.trim();
        if (requestedName.isEmpty()) return;

        PackUtilMacro macro = PackUtilMacroManager.get().get(requestedName);
        if (macro != null) {
            sendMacroData(myUsername, macro, (byte)2);
            return;
        }

        for (PackUtilMacro meteorMacro : MeteorMacroAdapter.getMeteorMacros()) {
            if (meteorMacro != null && meteorMacro.name != null && meteorMacro.name.equalsIgnoreCase(requestedName)) {
                sendMacroData(myUsername, meteorMacro, (byte)2);
                return;
            }
        }
    }

    private void handlePresetData(LanPacket packet) {
        if (!(packet instanceof LanPacket.PresetDataPacket) || !packet.getSessionId().equals(sessionId)) return;
        LanPacket.PresetDataPacket p = (LanPacket.PresetDataPacket) packet;
        if (p.senderUsername != null && p.senderUsername.equals(myUsername)) return;

        if (PackUtilPresetManager.get().importSharedPreset(p.presetName, p.presetJson, p.senderUsername)) {
            if (isHost && p.senderUsername != null && !p.senderUsername.isBlank()) {
                relayTcpPacketToClients(new LanPacket.PresetDataPacket(sessionId, p.senderUsername, p.presetName, p.presetJson), p.senderUsername);
            }
        }
    }

    private void handleMacroDelete(LanPacket packet) {
        if (!(packet instanceof LanPacket.MacroDeletePacket) || !packet.getSessionId().equals(sessionId)) return;
        LanPacket.MacroDeletePacket p = (LanPacket.MacroDeletePacket) packet;
        if (p.senderUsername != null && p.senderUsername.equals(myUsername)) return;

        if (clientMacroLists.containsKey(p.senderUsername)) {
            clientMacroLists.get(p.senderUsername).remove(p.macroName);
        }
        if (remoteMeteorMacros.containsKey(p.senderUsername)) {
            remoteMeteorMacros.get(p.senderUsername).remove(p.macroName);
        }

        if (isHost && p.senderUsername != null && !p.senderUsername.isBlank()) {
            relayTcpPacketToClients(new LanPacket.MacroDeletePacket(sessionId, p.senderUsername, p.macroName), p.senderUsername);
        }
    }

    private void handleQueueSync(String queueData, String senderUsername) {
        try {
            if (senderUsername != null && senderUsername.equals(myUsername)) return;

            PackUtilSharedState.QueuedPacket.resetIdCounter();
            List<PackUtilSharedState.QueuedPacket> receivedQueue =
                PackUtilClipboardHelper.deserializeQueueFromBase64(queueData);

            if (receivedQueue == null || receivedQueue.isEmpty()) {
                PackUtilClientMessaging.sendPrefixed("Ã‚Â§cReceived empty queue");
                return;
            }

            PackUtilSharedState.get().setDelayedPackets(receivedQueue);
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§aReceived queue: " + receivedQueue.size() + " packets");
        } catch (Exception e) {
            AutismClientAddon.LOG.error("[PackUtil-LAN] Failed to process queue sync", e);
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to receive queue");
        }
    }

    private void handleChatMessage(String senderUsername, String message) {
        String safeSender = senderUsername == null || senderUsername.isBlank() ? "Peer" : senderUsername;
        String safeMessage = message == null ? "" : message.trim();
        if (safeMessage.isEmpty()) return;

        if ("__SYNC__".equals(safeSender)) {
            PackUtilClientMessaging.sendPrefixed(safeMessage);
            return;
        }

        if ("__STOP_ALL__".equals(safeSender)) {
            autismclient.util.macro.MacroExecutor.stop();
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§c[LAN] Macros stopped by " + safeMessage);
            return;
        }

        if ("__SPREAD_DATA__".equals(safeSender)) {
            try {
                String[] parts = safeMessage.split("\t");
                if (parts.length >= 3) {
                    long nanos = Long.parseLong(parts[0]);
                    String lateClient = parts[1];
                    int count = Integer.parseInt(parts[2]);
                    lastSpreadResult = new SpreadResult(nanos, lateClient, count);
                    spreadHistory.add(lastSpreadResult);
                    while (spreadHistory.size() > MAX_SPREAD_HISTORY) spreadHistory.remove(0);
                    fireCallback(onSpreadCalculated);
                }
            } catch (NumberFormatException ignored) {}
            return;
        }

        if (safeSender.equals(myUsername)) return;

        PackUtilClientMessaging.send("[LAN] <" + safeSender + "> " + safeMessage);
    }

    private void handleRequestSync(String senderUsername, boolean isMacro, String macroName, String command) {
        if (senderUsername == null || senderUsername.equals(myUsername)) return;

        if (!isHost) {
            AutismClientAddon.LOG.warn("[Sync] Non-host received REQUEST_SYNC from {} Ã¢â‚¬â€ ignoring", senderUsername);
            return;
        }

        AutismClientAddon.LOG.info("[Sync] Received sync request from {}: isMacro={}, name={}, cmd={}",
            senderUsername, isMacro, macroName, command);

        if ("__QUEUE_FLUSH__".equals(command)) {
            initiateGoSyncForQueue();
        } else if (command != null && command.startsWith("__PER_USER__\t")) {

            String assignStr = command.substring("__PER_USER__\t".length());
            String[] parts = assignStr.split("\t");
            Map<String, String> assignments = new java.util.LinkedHashMap<>();
            for (int pi = 0; pi + 1 < parts.length; pi += 2) {
                assignments.put(parts[pi], parts[pi + 1]);
            }
            initiateGoSync(isMacro, macroName, "", assignments);
        } else {
            initiateGoSync(isMacro, macroName, command);
        }
    }

    private void handleStepProgress(String senderUsername, int completedStep, int totalSteps, String macroName) {
        if (senderUsername == null || senderUsername.equals(myUsername)) return;

        if (isHost) {
            sendTcpPacket(new LanPacket.MacroStepProgressPacket(sessionId, senderUsername, completedStep, totalSteps, macroName));
        }

        if (completedStep < 0) {

            peerStepProgress.remove(senderUsername);
        } else {
            peerStepProgress.put(senderUsername, completedStep);
        }

        autismclient.util.macro.MacroConditionRegistry.onLanStepProgress();

        Runnable listener = onStepProgressChanged;
        if (listener != null) listener.run();
    }

    private void handleOffsetSync(String senderUsername, String targetUsername, int offsetMs) {
        if (senderUsername == null || senderUsername.equals(myUsername)) return;

        ClientInfo info = connectedClients.get(targetUsername);
        if (info != null) {
            info.delayOffsetMs = Math.max(0, offsetMs);
            AutismClientAddon.LOG.info("[Sync] Offset for {} set to {}ms by {}", targetUsername, offsetMs, senderUsername);
        }

        if (isHost) {
            sendTcpPacket(new LanPacket.OffsetSyncPacket(sessionId, senderUsername, targetUsername, offsetMs));
        }
    }

    public boolean isRunning() { return running.get(); }
    public boolean hasTickWork() { return running.get() && (sessionId != null || isSearching || reconnecting); }
    public boolean isInSession() { return sessionId != null; }

    public void onGameJoined() {
        if (sessionId == null) return;
        if (isHost) {

            broadcastClientList();
        } else {

            sendTcpPacket(new LanPacket.RequestClientListPacket(sessionId));
        }
    }
    public boolean isHost() { return isHost; }
    public String getSessionId() { return sessionId; }
    public int getConnectedCount() { return connectedClients.size(); }
    public boolean isSearching() { return isSearching; }
    public int getSearchSecondsRemaining() { return searchTicksRemaining / 20; }
    public boolean isReconnecting() { return reconnecting; }
    public SyncState getSyncState() { return syncState; }
    public SyncContext getActiveSyncContext() { return activeSyncCtx; }
    public SpreadResult getLastSpreadResult() { return lastSpreadResult; }
    public List<SpreadResult> getSpreadHistory() { return new ArrayList<>(spreadHistory); }
    public String getMyUsername() { return myUsername; }

    public Map<String, ClientInfo> getConnectedClients() { return new ConcurrentHashMap<>(connectedClients); }

    public void broadcastMacroDeletion(String macroName) {
        if (sessionId == null) return;
        sendTcpPacket(new LanPacket.MacroDeletePacket(sessionId, myUsername, macroName));
    }

    public void broadcastQueueSync(String queueData) {
        if (!isInSession()) return;
        sendTcpPacket(new LanPacket.QueueSyncPacket(sessionId, queueData, myUsername));
    }

    public void broadcastStopAll() {
        if (!isInSession()) return;

        autismclient.util.macro.MacroExecutor.stop();
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§c[LAN] Macros stopped by " + myUsername);

        sendTcpPacket(new LanPacket.ChatMessagePacket(sessionId, "__STOP_ALL__", myUsername));
    }

    public boolean sendChatMessage(String message) {
        String safeMessage = message == null ? "" : message.trim();
        if (safeMessage.isEmpty()) return false;
        if (!isInSession()) {
            PackUtilClientMessaging.sendPrefixed("Join a LAN Sync session first.");
            return false;
        }

        String command = "__CHAT__\t" + safeMessage;
        if (isHost) {
            initiateGoSync(false, null, command);
        } else {
            sendTcpPacket(new LanPacket.RequestSyncPacket(sessionId, myUsername, false, "", command));
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§eRequested sync chat from host...");
        }
        return true;
    }

    public int getConnectedClientCount() { return connectedClients.size(); }

    public void broadcastStepProgress(int completedStep, int totalSteps, String macroName) {
        if (sessionId == null) return;
        if (completedStep < 0) {
            peerStepProgress.remove(myUsername);
        } else {
            peerStepProgress.put(myUsername, completedStep);
        }
        sendTcpPacket(new LanPacket.MacroStepProgressPacket(sessionId, myUsername, completedStep, totalSteps,
                macroName != null ? macroName : ""));

        autismclient.util.macro.MacroConditionRegistry.onLanStepProgress();
    }

    public int getPeerStep(String username) {
        return peerStepProgress.getOrDefault(username, 0);
    }

    public Map<String, Integer> getAllPeerSteps() {
        return new ConcurrentHashMap<>(peerStepProgress);
    }

    public void clearStepProgress() {
        peerStepProgress.clear();
    }

    public void setOnStepProgressChanged(Runnable callback) { this.onStepProgressChanged = callback; }

    public Map<String, Map<String, PackUtilMacro>> getAllRemoteMacros() { return new java.util.HashMap<>(clientMacroLists); }
    public Map<String, Map<String, PackUtilMacro>> getAllRemoteMeteorMacros() { return new java.util.HashMap<>(remoteMeteorMacros); }

    public List<String> getPeersMissingMacro(String macroName) {
        List<String> missing = new ArrayList<>();
        if (macroName == null || macroName.isBlank()) return missing;

        Map<String, Map<String, PackUtilMacro>> remote = getAllRemoteMacros();
        java.util.TreeSet<String> peers = new java.util.TreeSet<>(connectedClients.keySet());
        for (String peer : peers) {
            if (peer.equals(myUsername)) continue;
            Map<String, PackUtilMacro> peerMacros = remote.get(peer);
            if (peerMacros == null || !peerMacros.containsKey(macroName)) {
                missing.add(peer);
            }
        }
        return missing;
    }

    public Map<String, String> getMissingAssignedMacros(Map<String, String> assignments) {
        Map<String, String> missing = new java.util.LinkedHashMap<>();
        if (assignments == null || assignments.isEmpty()) return missing;

        Map<String, Map<String, PackUtilMacro>> remote = getAllRemoteMacros();
        for (Map.Entry<String, String> entry : new java.util.LinkedHashMap<>(assignments).entrySet()) {
            String username = entry.getKey();
            String macroName = entry.getValue();
            if (username == null || username.isBlank() || macroName == null || macroName.isBlank()) continue;

            if (username.equals(myUsername)) {
                if (PackUtilMacroManager.get().get(macroName) == null) {
                    missing.put(username, macroName);
                }
                continue;
            }

            Map<String, PackUtilMacro> peerMacros = remote.get(username);
            if (peerMacros == null || !peerMacros.containsKey(macroName)) {
                missing.put(username, macroName);
            }
        }
        return missing;
    }

    public List<String> getRemoteMacros() {
        Set<String> remoteMacros = new java.util.HashSet<>();
        Set<String> ownMacros = new java.util.HashSet<>();

        ownMacros.addAll(PackUtilCompatManager.getMeteorMacroNames());

        for (Map.Entry<String, Map<String, PackUtilMacro>> entry : clientMacroLists.entrySet()) {
            if (!entry.getKey().equals(myUsername)) {
                for (String macroName : entry.getValue().keySet()) {
                    if (!ownMacros.contains(macroName)) {
                        remoteMacros.add(macroName);
                    }
                }
            }
        }

        return new ArrayList<>(remoteMacros);
    }

    public void importMacro(String clientUsername, String macroName) {
        Map<String, PackUtilMacro> clientMacros = clientMacroLists.get(clientUsername);
        if (clientMacros == null || !clientMacros.containsKey(macroName)) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMacro not found: " + macroName);
            return;
        }

        PackUtilMacro remoteMacro = clientMacros.get(macroName);
        String newName = macroName;
        int suffix = 1;
        while (PackUtilMacroManager.get().get(newName) != null) {
            newName = macroName + " (" + suffix + ")";
            suffix++;
        }

        PackUtilMacro newMacro = new PackUtilMacro(newName);
        newMacro.actions.addAll(remoteMacro.actions);
        PackUtilMacroManager.get().add(newMacro);
        PackUtilMacroManager.get().save();

        PackUtilClientMessaging.sendPrefixed("Ã‚Â§aImported macro: " + newName);
        AutismClientAddon.LOG.info("[PackUtil-LAN] Imported macro '{}' from {}", newName, clientUsername);
    }

    public void broadcastAssignments(Map<String, String> assignments) {
        if (sessionId == null || !isInSession()) return;
        syncedAssignments.clear();
        syncedAssignments.putAll(assignments);
        assignmentSetBy = myUsername;

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            if (sb.length() > 0) sb.append('\t');
            sb.append(entry.getKey()).append('\t').append(entry.getValue());
        }
        sendTcpPacket(new LanPacket.MacroAssignmentSyncPacket(sessionId, myUsername, sb.toString()));
    }

    private void handleAssignmentSync(String sender, String encoded) {
        syncedAssignments.clear();
        assignmentSetBy = sender;
        if (encoded != null && !encoded.isEmpty()) {
            String[] parts = encoded.split("\t");
            for (int i = 0; i + 1 < parts.length; i += 2) {
                syncedAssignments.put(parts[i], parts[i + 1]);
            }
        }
        fireCallback(onAssignmentsChanged);
    }

    public Map<String, String> getSyncedAssignments() {
        return new java.util.LinkedHashMap<>(syncedAssignments);
    }

    public String getAssignmentSetBy() { return assignmentSetBy; }

    public void setOnAssignmentsChanged(Runnable callback) { this.onAssignmentsChanged = callback; }

    public List<String> getRemoteMacroNamesForPeer(String peerUsername) {
        Map<String, PackUtilMacro> peerMacros = clientMacroLists.get(peerUsername);
        if (peerMacros == null || peerMacros.isEmpty()) return java.util.Collections.emptyList();
        return new ArrayList<>(peerMacros.keySet());
    }

    public boolean shareMacroWithPeers(String macroName) {
        if (sessionId == null || !isInSession()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cJoin a session first.");
            return false;
        }
        if (macroName == null || macroName.isBlank()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cSelect a macro first.");
            return false;
        }

        PackUtilMacro macro = PackUtilMacroManager.get().get(macroName);
        if (macro == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cMacro not found: " + macroName);
            return false;
        }

        sendMacroData(myUsername, macro, (byte)2);
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§aSent macro to peers: " + macro.name);
        return true;
    }

    public void sharePreset(String presetName) {
        if (sessionId == null) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cNot in session.");
            return;
        }

        java.io.File file = new java.io.File(new java.io.File(AutismClientAddon.FOLDER, "presets"), presetName + ".json");
        if (!file.exists()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cPreset not found: " + presetName);
            return;
        }

        try {
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            sendTcpPacket(new LanPacket.PresetDataPacket(sessionId, myUsername, presetName, json));
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§aShared preset '" + presetName + "' with LAN.");
        } catch (IOException e) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cFailed to read preset: " + e.getMessage());
        }
    }

    public void requestMacro(String macroName) {
        if (sessionId == null || !isInSession()) {
            PackUtilClientMessaging.sendPrefixed("Ã‚Â§cNot in a LAN Sync session!");
            return;
        }
        sendTcpPacket(new LanPacket.RequestMacroPacket(sessionId, macroName));
        PackUtilClientMessaging.sendPrefixed("Ã‚Â§eRequesting macro: " + macroName);
    }

    public void startTwoPhaseExecution(String targetName, boolean isMacro) {
        if (!isInSession()) return;
        if (isMacro) executeMacroSynchronized(targetName);
        else sendQueuedPackets();
    }

    private String summarizeNames(List<String> names) {
        if (names == null || names.isEmpty()) return "";
        if (names.size() <= 3) return String.join(", ", names);
        return String.join(", ", names.subList(0, 3)) + " +" + (names.size() - 3) + " more";
    }

    private String summarizeAssignments(Map<String, String> assignments) {
        if (assignments == null || assignments.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
            if (parts.size() == 3 && assignments.size() > 3) break;
        }
        if (assignments.size() > 3) {
            parts.add("+" + (assignments.size() - 3) + " more");
        }
        return String.join(", ", parts);
    }

    public void setOnClientJoined(Runnable callback) { this.onClientJoined = callback; }
    public void setOnClientLeft(Runnable callback) { this.onClientLeft = callback; }
    public void setOnCountdown(Runnable callback) { this.onCountdown = callback; }
    public void setOnSendNow(Runnable callback) { this.onSendNow = callback; }
    public void setOnSessionStateChanged(Runnable callback) { this.onSessionStateChanged = callback; }
    public void setOnSyncStateChanged(Runnable callback) { this.onSyncStateChanged = callback; }
    public void setOnSpreadCalculated(Runnable callback) { this.onSpreadCalculated = callback; }
    public void setOnPeerStatusChanged(Runnable callback) { this.onPeerStatusChanged = callback; }

    private void fireCallback(Runnable callback) {
        if (callback != null) {
            Minecraft.getInstance().execute(callback);
        }
    }

    private String generateSessionId() {
        return String.format("%04d", (int)(Math.random() * 10000));
    }

    private String getUsername() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            return mc.player.getName().getString();
        }
        return "Player";
    }

    public static class ClientInfo {
        public String username;
        public boolean isHost;
        public long lastSeen;
        public int delayOffsetMs;

        ClientInfo(String username, boolean isHost) {
            this.username = username;
            this.isHost = isHost;
            this.lastSeen = System.currentTimeMillis();
            this.delayOffsetMs = 0;
        }
    }

    public void setPlayerDelayOffset(String username, int offsetMs) {
        ClientInfo info = connectedClients.get(username);
        if (info != null) {
            int clamped = Math.max(0, offsetMs);
            info.delayOffsetMs = clamped;

            if (sessionId != null) {
                sendTcpPacket(new LanPacket.OffsetSyncPacket(sessionId, myUsername, username, clamped));
            }
        }
    }

    public int getPlayerDelayOffset(String username) {
        ClientInfo info = connectedClients.get(username);
        return info != null ? info.delayOffsetMs : 0;
    }

    public void sendChat(String message) {
        sendChatMessage(message);
    }
}
