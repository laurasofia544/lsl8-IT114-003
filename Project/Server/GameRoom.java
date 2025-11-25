package Project.Server;

import java.util.*;
import Project.Common.*;
import Project.Common.TextFX.Color;

// lsl8 | 11/24/25
// GameRoom adds round logic, ready check, picks, elimination, points, and sessions
public class GameRoom extends Room {

    // who is ready for the NEXT session
    private final Set<Long> readySet = new HashSet<>();

    // choices for the current round (null = not picked yet)
    private final Map<Long, RpsChoice> choices = new HashMap<>();

    // eliminated players are spectators until session resets
    private final Set<Long> eliminated = new HashSet<>();

    // points tracked across rounds within a session
    private final Map<Long, Integer> points = new HashMap<>();

    private GamePhase phase = GamePhase.ENDED;
    private Timer roundTimer;

    public GameRoom(String name) {
        super(name);
    }
    public synchronized void handleReady(ServerThread sender) {
        long id = sender.getClientId();

        if (eliminated.contains(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot /ready.");
            return;
        }

        readySet.add(id);

        // broadcast ready notice
        broadcastNotice(String.format("%s is ready", nameOf(id)));

        int alive = countActivePlayers();
        if (alive >= 2 && readySet.size() == alive) {
            readySet.clear();
            startRound();
        } else {
            int needed = alive - readySet.size();
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    String.format("Waiting for %d more player(s) to /ready…", needed));
        }
    }

    /**
     * /pick r|p|s logic during choosing phase
     */
    public synchronized void handlePick(ServerThread sender, String arg) {
        if (phase != GamePhase.CHOOSING) return;

        long id = sender.getClientId();
        if (eliminated.contains(id)) return; // spectators can't pick

        RpsChoice pick = parse(arg);
        if (pick == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid pick. Use /pick r|p|s");
            return;
        }

        choices.put(id, pick);

        // tell everyone without revealing actual pick
        broadcastNotice(String.format("%s picked their choice", nameOf(id)));

        // Condition 2: if all active players picked, end round early
        if (allActivePicked()) {
            endRound();
        }
    }

    public synchronized void handleScoreRequest(ServerThread sender) {
        syncAllPoints();
    }

    private void startRound() {
        choices.clear();
        phase = GamePhase.CHOOSING;

        if (roundTimer != null) roundTimer.cancel();
        roundTimer = new Timer(true);
        roundTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (GameRoom.this) {
                    endRound(); // Condition 1: timer expires
                }
            }
        }, 15000); // 15 second pick window

        clientsInRoom.values().forEach(ServerThread::sendRoundStart);
    }
    private void endRound() {
        if (phase != GamePhase.CHOOSING) return;
        phase = GamePhase.RESOLVING;

        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }

        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id) && !choices.containsKey(id)) {
                eliminated.add(id);
            }
        }

        processBattles();
        syncAllPoints();

        int active = countActivePlayers();
        if (active == 1) {
            long winner = remainingPlayerId();
            gameOver("Winner: " + nameOf(winner));
            resetSession();
        } else if (active == 0) {
            gameOver("No players remain. It's a tie.");
            resetSession();
        } else {
            phase = GamePhase.ENDED;
            startRound(); // next round automatically
        }
    }
    private void processBattles() {
        List<Long> alive = aliveIds();
        int n = alive.size();
        if (n < 2) return;

        for (int i = 0; i < n; i++) {
            long attacker = alive.get(i);
            long defender = alive.get((i + 1) % n);
            resolveAttack(attacker, defender);
        }
    }
    private void resolveAttack(long attacker, long defender) {
        RpsChoice aPick = choices.get(attacker);
        RpsChoice dPick = choices.get(defender);
        if (aPick == null || dPick == null) return; // one didn't pick

        int result = beats(aPick, dPick); // 1 attacker wins, -1 defender wins, 0 tie

        String msg = String.format("%s(%s) attacks %s(%s) -> %s",
                nameOf(attacker), aPick,
                nameOf(defender), dPick,
                result > 0 ? nameOf(attacker) + " wins"
                        : result < 0 ? nameOf(defender) + " wins"
                        : "tie");

        broadcastBattle(msg);

        if (result > 0) {
            awardPoint(attacker);
        } else if (result < 0) {
            awardPoint(defender);
            eliminated.add(attacker);  // attacker eliminated on attack-loss
        }
        // tie: no points, no elimination
    }
    private void broadcastNotice(String msg) {
        clientsInRoom.values().forEach(st -> st.sendPickedNotice(msg));
    }

    private void broadcastBattle(String msg) {
        clientsInRoom.values().forEach(st -> st.sendBattleResult(msg));
    }

    private void syncAllPoints() {
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();

            PointsPayload p = new PointsPayload();
            p.setPayloadType(PayloadType.POINTS_SYNC);
            p.setClientId(Constants.DEFAULT_CLIENT_ID);
            p.setTargetClientId(id);
            p.setPoints(points.getOrDefault(id, 0));

            st.sendToClient(p);
        }
    }

    private void gameOver(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg).append("\nFinal scores:\n");

        points.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(String.format("%s: %d\n", nameOf(e.getKey()), e.getValue())));

        String text = sb.toString();
        clientsInRoom.values().forEach(st -> st.sendGameOver(text));
    }
    private void resetSession() {
        choices.clear();
        eliminated.clear();
        points.clear();
        readySet.clear();
        phase = GamePhase.ENDED;
        syncAllPoints();
    }

    private boolean allActivePicked() {
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id) && !choices.containsKey(id)) {
                return false;
            }
        }
        return countActivePlayers() > 0;
    }

    private int countActivePlayers() {
        int count = 0;
        for (ServerThread st : clientsInRoom.values()) {
            if (!eliminated.contains(st.getClientId())) count++;
        }
        return count;
    }

    private List<Long> aliveIds() {
        List<Long> ids = new ArrayList<>();
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)) ids.add(id);
        }
        return ids;
    }

    private long remainingPlayerId() {
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)) return id;
        }
        return Constants.DEFAULT_CLIENT_ID;
    }

    private String nameOf(long id) {
        ServerThread st = clientsInRoom.get(id);
        return st == null ? ("#" + id) : st.getDisplayName();
    }

    private void awardPoint(long id) {
        points.put(id, points.getOrDefault(id, 0) + 1);
    }

    private int beats(RpsChoice a, RpsChoice b) {
        if (a == b) return 0;
        if (a == RpsChoice.ROCK && b == RpsChoice.SCISSORS) return 1;
        if (a == RpsChoice.PAPER && b == RpsChoice.ROCK) return 1;
        if (a == RpsChoice.SCISSORS && b == RpsChoice.PAPER) return 1;
        return -1;
    }

    private RpsChoice parse(String arg) {
        if (arg == null) return null;
        switch (arg.trim().toLowerCase()) {
            case "r": return RpsChoice.ROCK;
            case "p": return RpsChoice.PAPER;
            case "s": return RpsChoice.SCISSORS;
            default: return null;
        }
    }
}
