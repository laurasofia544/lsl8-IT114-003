package Project.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import Project.Common.Constants;
import Project.Common.GamePhase;
import Project.Common.PointsPayload;
import Project.Common.PayloadType;
import Project.Common.RpsChoice;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

// lsl8 | 11/24/25
// GameRoom adds round logic, ready check, picks, elimination, points, away, spectators and sessions
public class GameRoom extends Room {

    // who is ready for the NEXT session
    private final Set<Long> readySet = new HashSet<>();

    // choices for the current round (null = not picked yet)
    private final Map<Long, RpsChoice> choices = new HashMap<>();

    // eliminated players are spectators until session resets
    private final Set<Long> eliminated = new HashSet<>();

    // players currently marked away (temporarily skipped but still in game)
    private final Set<Long> away = new HashSet<>();

    // players who joined explicitly as spectators (never part of turn flow)
    private final Set<Long> spectators = new HashSet<>();

    // points tracked across rounds within a session
    private final Map<Long, Integer> points = new HashMap<>();

    private GamePhase phase = GamePhase.ENDED;
    private Timer roundTimer;

    public GameRoom(String name) {
        super(name);
    }

    /**
     * /ready handler
     */
    public synchronized void handleReady(ServerThread sender) {
        long id = sender.getClientId();

        if (eliminated.contains(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot /ready.");
            return;
        }
        if (away.contains(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You are marked away and cannot /ready. Use /back to return first.");
            return;
        }
        if (spectators.contains(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Spectators cannot /ready.");
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
//lsl8 | 12/08/25
    public synchronized void handlePick(ServerThread sender, String arg) {
        if (phase != GamePhase.CHOOSING)
            return;

        long id = sender.getClientId();
        // spectators, eliminated and away cannot pick
        if (eliminated.contains(id) || away.contains(id) || spectators.contains(id)) {
            return;
        }

        RpsChoice pick = parse(arg);
        if (pick == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid pick. Use /pick r|p|s|l|k");
            return;
        }

        choices.put(id, pick);

        // tell everyone without revealing actual pick
        broadcastNotice(String.format("%s picked their choice", nameOf(id)));

        // if all active players picked, end round early
        if (allActivePicked()) {
            endRound();
        }
    }

    public synchronized void handleScoreRequest(ServerThread sender) {
        syncAllPoints();
    }
//lsl8 | 12/08/25
    /**
     * Toggle away status for a player.
     * Away players are skipped in the round flow but remain in the game.
     */
    public synchronized void handleAway(ServerThread sender, boolean isAwayFlag) {
        long id = sender.getClientId();

        if (eliminated.contains(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Eliminated players can't go away; they're already spectators.");
            return;
        }
        if (spectators.contains(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You are a spectator and are already out of the turn flow.");
            return;
        }

        String name = nameOf(id);
//lsl8 | 12/08/25 
        if (isAwayFlag) {
            if (away.add(id)) {
                // if they were ready, they no longer count toward ready check
                readySet.remove(id);
                broadcastNotice(String.format("%s is away", name));
            }
        } else {
            if (away.remove(id)) {
                broadcastNotice(String.format("%s is no longer away", name));
            }
        }
    }

    /**
     * Mark this client as a spectator in this game room.
     */
    public synchronized void handleSpectatorJoin(ServerThread sender) {
        long id = sender.getClientId();
        spectators.add(id);
        String name = nameOf(id);
        broadcastNotice(String.format("%s joined as a spectator", name));
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You joined as a spectator. You can see chat and game events but cannot ready or pick.");
    }

    /**
     * Stop spectating and join as an active player again.
     */
    public synchronized void handleStopSpectate(ServerThread sender) {
        long id = sender.getClientId();
        if (!spectators.remove(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You are not currently spectating.");
            return;
        }

        String name = nameOf(id);

        eliminated.remove(id);
        away.remove(id);

        broadcastNotice(String.format("%s is no longer a spectator and joined the game", name));
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You are no longer a spectator and have joined the game.");
    }

    public boolean isSpectator(long id) {
        return spectators.contains(id);
    }
//lsl8 | 12/08/25 Game Starts
    private void startRound() {
        choices.clear();
        phase = GamePhase.CHOOSING;

        if (roundTimer != null)
            roundTimer.cancel();
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
//lsl8 |12/08/25
    private void endRound() {
        if (phase != GamePhase.CHOOSING)
            return;
        phase = GamePhase.RESOLVING;

        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }

        // eliminate players who didn't pick (but only active players)
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)
                    && !away.contains(id)
                    && !spectators.contains(id)
                    && !choices.containsKey(id)) {
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
            startRound();
        }
    }

    private void processBattles() {
        List<Long> alive = aliveIds();
        int n = alive.size();
        if (n < 2)
            return;

        for (int i = 0; i < n; i++) {
            long attacker = alive.get(i);
            long defender = alive.get((i + 1) % n);
            resolveAttack(attacker, defender);
        }
    }
//lsl8 | 12/08/25 
    private void resolveAttack(long attacker, long defender) {
        RpsChoice aPick = choices.get(attacker);
        RpsChoice dPick = choices.get(defender);
        if (aPick == null || dPick == null)
            return; // one didn't pick

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
            eliminated.add(attacker); // attacker eliminated on attack-loss
        }
        // tie: no points, no elimination
    }

    private void broadcastNotice(String msg) {
        clientsInRoom.values().forEach(st -> st.sendPickedNotice(msg));
    }

    private void broadcastBattle(String msg) {
        clientsInRoom.values().forEach(st -> st.sendBattleResult(msg));
    }
//Lsl8 | 12/08/25
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
//lsl8 | 12/08/25 
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
        away.clear(); // clear away at end of session
        points.clear();
        readySet.clear();
        phase = GamePhase.ENDED;
        syncAllPoints();
        // spectators remain spectators across sessions
    }

    private boolean allActivePicked() {
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)
                    && !away.contains(id)
                    && !spectators.contains(id)
                    && !choices.containsKey(id)) {
                return false;
            }
        }
        return countActivePlayers() > 0;
    }
//lsl8 | 12/08/25
    private int countActivePlayers() {
        int count = 0;
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)
                    && !away.contains(id)
                    && !spectators.contains(id)) {
                count++;
            }
        }
        return count;
    }

    private List<Long> aliveIds() {
        List<Long> ids = new ArrayList<>();
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)
                    && !away.contains(id)
                    && !spectators.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private long remainingPlayerId() {
        for (ServerThread st : clientsInRoom.values()) {
            long id = st.getClientId();
            if (!eliminated.contains(id)
                    && !away.contains(id)
                    && !spectators.contains(id)) {
                return id;
            }
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
        if (a == b)
            return 0;
//lsl8 | 12/08/25
        switch (a) {
            case ROCK:
                // Rock crushes Scissors & Lizard
                return (b == RpsChoice.SCISSORS || b == RpsChoice.LIZARD) ? 1 : -1;
            case PAPER:
                // Paper covers Rock & disproves Spock
                return (b == RpsChoice.ROCK || b == RpsChoice.SPOCK) ? 1 : -1;
            case SCISSORS:
                // Scissors cut Paper & decapitate Lizard
                return (b == RpsChoice.PAPER || b == RpsChoice.LIZARD) ? 1 : -1;
            case LIZARD:
                // Lizard poisons Spock & eats Paper
                return (b == RpsChoice.SPOCK || b == RpsChoice.PAPER) ? 1 : -1;
            case SPOCK:
                // Spock smashes Scissors & vaporizes Rock
                return (b == RpsChoice.SCISSORS || b == RpsChoice.ROCK) ? 1 : -1;
            default:
                return 0;
        }
    }
//lsl8 | 12/08/25
    private RpsChoice parse(String arg) {
        if (arg == null)
            return null;
        switch (arg.trim().toLowerCase()) {
            case "r":
                return RpsChoice.ROCK;
            case "p":
                return RpsChoice.PAPER;
            case "s":
                return RpsChoice.SCISSORS;
            case "l":
                return RpsChoice.LIZARD;
            case "k":
                return RpsChoice.SPOCK;
            default:
                return null;
        }
    }
}
