package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

import Project.Client.Client;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IMessageEvents;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.ITimeEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.Constants;
import Project.Common.TimerType;

/**
 * Game area UI for RPS:
 * - Player list with points, pending & eliminated & away & spectator markers
 * - Game Event panel (separate from chat)
 * - R/P/S (+ Lizard/Spock) pick buttons
 * - Round countdown label
 * - Optional cooldown: same option can't be picked twice in a row
 */
public class GameView extends JPanel implements IMessageEvents, IPointsEvent,
        ITurnEvent, IReadyEvent, IRoomEvents, ITimeEvents {

    // ----- internal player model -----
    private static class PlayerInfo {
        long id;
        String name; 
        int points = 0;
        boolean ready = false;
        boolean tookTurn = false;
        boolean eliminated = false;
        boolean away = false;
        boolean spectator = false;
    }

    private final HashMap<Long, PlayerInfo> players = new HashMap<>();

    // UI elements
    private final DefaultListModel<String> playerListModel = new DefaultListModel<>();
    private final JList<String> playerList = new JList<>(playerListModel);

    private final JTextArea gameEventArea = new JTextArea();
    private final JLabel timerLabel = new JLabel("Timer: --");

    private final JButton rockButton = new JButton("Rock");
    private final JButton paperButton = new JButton("Paper");
    private final JButton scissorsButton = new JButton("Scissors");
    private final JButton lizardButton = new JButton("Lizard");
    private final JButton spockButton = new JButton("Spock");

    // state
    private boolean inPickingPhase = false;
    private boolean extendedOptionsEnabled = false;
    private boolean cooldownEnabled = false;
    // remember my own last pick (r/p/s/l/k) for cooldown
    private String myLastPickCode = null;

    // simple local countdown timer (in seconds)
    private Timer countdownTimer;
    private int remainingSeconds = 0;
    private static final int ROUND_SECONDS = 15;

    public GameView(ICardControls controls) {
        super();
        setLayout(new BorderLayout());

        // --- LEFT/CENTER: Game events + buttons ---
        gameEventArea.setEditable(false);
        JScrollPane gameEventScroll = new JScrollPane(gameEventArea);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(timerLabel, BorderLayout.NORTH);
        centerPanel.add(gameEventScroll, BorderLayout.CENTER);
        centerPanel.add(buildPickButtonsPanel(), BorderLayout.SOUTH);

        // --- RIGHT: Player list ---
        JScrollPane playerScroll = new JScrollPane(playerList);
        playerList.setVisibleRowCount(8);

        add(centerPanel, BorderLayout.CENTER);
        add(playerScroll, BorderLayout.EAST);

        // start with extra options disabled
        setExtendedOptionsEnabled(false);

        // register for updates from Client
        Client.INSTANCE.registerCallback(this);
    }

    // Panel with all choice buttons
    private JPanel buildPickButtonsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));

        rockButton.addActionListener(e -> sendPickAndDisable("r"));
        paperButton.addActionListener(e -> sendPickAndDisable("p"));
        scissorsButton.addActionListener(e -> sendPickAndDisable("s"));
        // Extra options
        lizardButton.addActionListener(e -> sendPickAndDisable("l"));
        spockButton.addActionListener(e -> sendPickAndDisable("k")); 

        p.add(new JLabel("Your choice: "));
        p.add(rockButton);
        p.add(paperButton);
        p.add(scissorsButton);
        p.add(lizardButton);
        p.add(spockButton);

        return p;
    }

    private void sendPickAndDisable(String code) {
        try {
            Client.INSTANCE.sendPick(code);
            myLastPickCode = code;
            // after picking once, disable buttons for this round
            rockButton.setEnabled(false);
            paperButton.setEnabled(false);
            scissorsButton.setEnabled(false);
            lizardButton.setEnabled(false);
            spockButton.setEnabled(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Called from ChatGameView when a new round starts.
     */
    public void resetPickButtonsForNewRound() {
        // base enabled state
        rockButton.setEnabled(true);
        paperButton.setEnabled(true);
        scissorsButton.setEnabled(true);
        lizardButton.setEnabled(extendedOptionsEnabled);
        spockButton.setEnabled(extendedOptionsEnabled);
        // apply cooldown rule, if enabled
        applyCooldownToButtons();
    }

    /**
     * Called from ChatGameView when the session creator toggles
     * the "extended options" checkbox during Ready Check.
     */
    public void setExtendedOptionsEnabled(boolean enabled) {
        extendedOptionsEnabled = enabled;
        lizardButton.setVisible(enabled);
        spockButton.setVisible(enabled);
        if (!enabled) {
            lizardButton.setEnabled(false);
            spockButton.setEnabled(false);
        }
        // respect cooldown if needed
        applyCooldownToButtons();
        revalidate();
        repaint();
    }

    /**
     * Called from ChatGameView when the session creator toggles
     * the "cooldown" checkbox during Ready Check.
     */
    public void setCooldownEnabled(boolean enabled) {
        cooldownEnabled = enabled;
        applyCooldownToButtons();
    }

    /**
     * Disable the button that matches myLastPickCode if cooldown is enabled.
     */
//lsl8 | 12/08/25
    private void applyCooldownToButtons() {
        if (!cooldownEnabled || myLastPickCode == null) {
            return;
        }
        String code = myLastPickCode.toLowerCase();
        switch (code) {
            case "r":
                rockButton.setEnabled(false);
                break;
            case "p":
                paperButton.setEnabled(false);
                break;
            case "s":
                scissorsButton.setEnabled(false);
                break;
            case "l":
                if (extendedOptionsEnabled) {
                    lizardButton.setEnabled(false);
                }
                break;
            case "k":
                if (extendedOptionsEnabled) {
                    spockButton.setEnabled(false);
                }
                break;
            default:
                break;
        }
    }

    // ----- helper: ensure player exists -----
    private PlayerInfo getOrCreatePlayer(long id) {
        PlayerInfo pi = players.get(id);
        if (pi == null) {
            pi = new PlayerInfo();
            pi.id = id;
            pi.name = Client.INSTANCE.getDisplayNameFromId(id); 
            players.put(id, pi);
        }
        return pi;
    }

    // ----- helper: rebuild player list UI -----
    private void refreshPlayerList() {
        List<PlayerInfo> list = new ArrayList<>(players.values());

        // sort by points DESC, then name ASC
        Collections.sort(list, new Comparator<PlayerInfo>() {
            @Override
            public int compare(PlayerInfo a, PlayerInfo b) {
                if (a.points != b.points) {
                    return Integer.compare(b.points, a.points); 
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });

        playerListModel.clear();
        for (PlayerInfo p : list) {
            String label = formatPlayerLine(p);
            playerListModel.addElement(label);
        }
    }

    private String formatPlayerLine(PlayerInfo p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.name).append(" | pts: ").append(p.points);
        if (p.spectator) {
            sb.append("  [SPECTATOR]");
        } else if (p.eliminated) {
            sb.append("  [ELIMINATED]");
        } else if (p.away) {
            sb.append("  [AWAY]");
        } else if (inPickingPhase && !p.tookTurn) {
            sb.append("  [PENDING]");
        }
        return sb.toString();
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase();
    }

    // ----------------- countdown helpers -----------------
    private void startLocalCountdown() {
        stopLocalCountdown();
        remainingSeconds = ROUND_SECONDS;
        timerLabel.setText("Timer: " + remainingSeconds + "s");

        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                timerLabel.setText("Timer: 0s");
                stopLocalCountdown();
            } else {
                timerLabel.setText("Timer: " + remainingSeconds + "s");
            }
        });
        countdownTimer.start();
    }

    private void stopLocalCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    // ----------------- IMessageEvents -----------------
    @Override
    public void onMessageReceive(long clientId, String message) {
        // Game events come through the special GAME_EVENT_CHANNEL
        if (clientId != Constants.GAME_EVENT_CHANNEL) {
            return;
        }
        if (message == null) return;

        // append to game event log
        gameEventArea.append(message + System.lineSeparator());
        gameEventArea.setCaretPosition(gameEventArea.getDocument().getLength());

        // Detect spectator join messages
        if (message.endsWith(" joined as a spectator")) {
            String name = message.substring(0, message.length() - " joined as a spectator".length()).trim();
            setSpectatorByName(name, true);
            return;
        }

        // Detect away status messages
        if (message.endsWith(" is away")) {
            String name = message.substring(0, message.length() - " is away".length()).trim();
            setAwayByName(name, true);
            return;
        }
        if (message.endsWith(" is no longer away")) {
            String name = message.substring(0, message.length() - " is no longer away".length()).trim();
            setAwayByName(name, false);
            return;
        }

        // Round start – everyone pending, not eliminated
        if (message.startsWith("Round started")) {
            inPickingPhase = true;
            for (PlayerInfo p : players.values()) {
                p.tookTurn = false;
                p.eliminated = false;
            }
            resetPickButtonsForNewRound();
            startLocalCountdown();
            refreshPlayerList();
            return;
        }

        // Battle lines -> award points
        if (message.contains("attacks") && message.contains("->")) {
            awardPointFromBattleMessage(message);
            return;
        }

        // Winner line -> mark eliminated / end picking phase
        if (message.startsWith("Winner:")) {
            inPickingPhase = false;
            stopLocalCountdown();
            String after = message.substring("Winner:".length()).trim();
            String[] parts = after.split("\\s+");
            String winnerRaw = parts.length > 0 ? parts[0] : "";
            String winnerNorm = normalizeName(winnerRaw);

            for (PlayerInfo p : players.values()) {
                p.eliminated = !normalizeName(p.name).equals(winnerNorm);
            }
            refreshPlayerList();
            return;
        }

        // Tie message: "No players remain. It's a tie."
        if (message.startsWith("No players remain")) {
            inPickingPhase = false;
            stopLocalCountdown();
            for (PlayerInfo p : players.values()) {
                p.eliminated = false; // nobody eliminated on tie
            }
            refreshPlayerList();
        }
    }

    private void setAwayByName(String displayName, boolean isAway) {
        String target = displayName.trim();
        for (PlayerInfo p : players.values()) {
            if (p.name.equals(target)) {
                p.away = isAway;
            }
        }
        refreshPlayerList();
    }

    private void setSpectatorByName(String displayName, boolean isSpec) {
        String target = displayName.trim();
        for (PlayerInfo p : players.values()) {
            if (p.name.equals(target)) {
                p.spectator = isSpec;
            }
        }
        refreshPlayerList();
    }

    private void awardPointFromBattleMessage(String msg) {
        String lower = msg.toLowerCase();
        if (lower.contains("-> tie")) {
            // tie, no points
            return;
        }

        int arrowIdx = msg.indexOf("->");
        if (arrowIdx < 0) return;
        String afterArrow = msg.substring(arrowIdx + 2).trim();
        String[] parts = afterArrow.split("\\s+");
        if (parts.length == 0) return;

        String winnerRaw = parts[0];
        String winnerNorm = normalizeName(winnerRaw);

        for (PlayerInfo p : players.values()) {
            if (normalizeName(p.name).equals(winnerNorm)) {
                p.points = p.points + 1;
                break;
            }
        }
        refreshPlayerList();
    }

    // ----------------- IPointsEvent -----------------
    @Override
    public void onPointsUpdate(long clientId, int points) {
        // Ignore server POINTS payloads; we keep score locally
        // based on the battle log messages.
    }

    // ----------------- ITurnEvent -----------------
    @Override
    public void onTookTurn(long clientId, boolean didTakeTurn) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            for (PlayerInfo p : players.values()) {
                p.tookTurn = false;
            }
        } else {
            PlayerInfo p = getOrCreatePlayer(clientId);
            p.tookTurn = didTakeTurn;
        }
        refreshPlayerList();
    }

    // ----------------- IReadyEvent -----------------
    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            // RESET_READY: clear ready/turn, but KEEP eliminated + points
            for (PlayerInfo p : players.values()) {
                p.ready = false;
                p.tookTurn = false;
            }
            inPickingPhase = false;
            refreshPlayerList();
            return;
        }
        PlayerInfo p = getOrCreatePlayer(clientId);
        p.ready = isReady;
        refreshPlayerList();
    }

    // ----------------- IRoomEvents -----------------
    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            // left room / reset
            players.clear();
            refreshPlayerList();
            stopLocalCountdown();
            timerLabel.setText("Timer: --");
            inPickingPhase = false;
            return;
        }
        if (isJoin) {
            PlayerInfo p = getOrCreatePlayer(clientId);
            p.name = Client.INSTANCE.getDisplayNameFromId(clientId);
        } else {
            // leaving
            players.remove(clientId);
        }
        refreshPlayerList();
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
    }

    // ----------------- ITimeEvents -----------------
    @Override
    public void onTimerUpdate(TimerType timerType, int time) {
        // If the server ever sends TIME payloads, this will keep in sync.
        timerLabel.setText("Timer: " + time + "s");
    }
}
