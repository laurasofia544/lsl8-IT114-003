package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import Project.Client.CardViewName;
import Project.Client.Client;
import Project.Client.ClientConsole;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.IReadyEvent;
import Project.Common.Constants;
import Project.Common.Phase;

public class ChatGameView extends JPanel implements IRoomEvents, IPhaseEvent, IReadyEvent {
    private final ChatView chatView;
    private final GameView gameView;
    private final JSplitPane splitPane;

    // Ready / Away / Spectate UI
    private JPanel readyPanel;
    private JButton readyButton;
    private JLabel readyStatusLabel;
    private JCheckBox extendedOptionsCheckbox;
    private JCheckBox cooldownCheckbox;
    private JButton awayButton;
    private JButton backButton;
    private JButton spectateButton;
    private JButton joinGameButton;

    private boolean iAmReady = false;
    private boolean iAmAway = false;
    private boolean iAmSpectator = false;

    public ChatGameView(ICardControls controls) {
        super();
        setLayout(new BorderLayout());

        setName(CardViewName.CHAT_GAME_SCREEN.name());
        controls.registerView(CardViewName.CHAT_GAME_SCREEN.name(), this);

        chatView = new ChatView(controls);
        gameView = new GameView(controls);
        gameView.setVisible(false);
        gameView.setBackground(Color.BLUE);
        chatView.setBackground(Color.GRAY);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gameView, chatView);
        splitPane.setResizeWeight(0.6);
        splitPane.setOneTouchExpandable(false);
        splitPane.setEnabled(false);

        add(splitPane, BorderLayout.CENTER);

        gameView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.6);
            }
        });

        initReadyPanel();
        add(readyPanel, BorderLayout.SOUTH);

        showChatOnlyView();

        ClientConsole.INSTANCE.registerCallback(this);
        Client.INSTANCE.registerCallback(this);
    }

    private void initReadyPanel() {
        readyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        readyButton = new JButton("I'm Ready!");
        readyStatusLabel = new JLabel("Not ready");
        extendedOptionsCheckbox = new JCheckBox("Enable Lizard/Spock");
        cooldownCheckbox = new JCheckBox("Enable cooldown (no same pick)");
        awayButton = new JButton("Away");
        backButton = new JButton("Back");
        spectateButton = new JButton("Spectate");
        joinGameButton = new JButton("Join Game");

        extendedOptionsCheckbox.addActionListener(e -> {
            boolean enabled = extendedOptionsCheckbox.isSelected();
            gameView.setExtendedOptionsEnabled(enabled);
        });

        cooldownCheckbox.addActionListener(e -> {
            boolean enabled = cooldownCheckbox.isSelected();
            gameView.setCooldownEnabled(enabled);
        });

        awayButton.addActionListener(e -> {
            if (iAmSpectator) return;
            try {
                Client.INSTANCE.sendMessage("/away");
                setLocalAwayState(true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        backButton.addActionListener(e -> {
            if (iAmSpectator) return;
            try {
                Client.INSTANCE.sendMessage("/back");
                setLocalAwayState(false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        readyButton.addActionListener(e -> {
            if (!iAmReady && !iAmAway && !iAmSpectator) {
                try {
                    Client.INSTANCE.sendReady();
                    iAmReady = true;
                    readyButton.setEnabled(false);
                    readyStatusLabel.setText("Waiting for others...");
                    extendedOptionsCheckbox.setEnabled(false);
                    cooldownCheckbox.setEnabled(false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        spectateButton.addActionListener(e -> {
            if (iAmSpectator) return;
            try {
                Client.INSTANCE.sendMessage("/spectate rps");
                iAmSpectator = true;

                readyButton.setEnabled(false);
                readyButton.setText("Spectating");
                readyStatusLabel.setText("You are spectating");

                awayButton.setEnabled(false);
                backButton.setEnabled(false);
                extendedOptionsCheckbox.setEnabled(false);
                cooldownCheckbox.setEnabled(false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        joinGameButton.addActionListener(e -> {
            if (!iAmSpectator) return;
            try {
                Client.INSTANCE.sendMessage("/play");
                iAmSpectator = false;

                if (!iAmAway && !iAmReady) {
                    readyButton.setEnabled(true);
                    readyButton.setText("I'm Ready!");
                    readyStatusLabel.setText("Click Ready to start!");
                } else if (iAmAway) {
                    readyButton.setEnabled(false);
                    readyButton.setText("You are Away");
                    readyStatusLabel.setText("You are marked away");
                } else if (iAmReady) {
                    readyButton.setEnabled(false);
                    readyButton.setText("I'm Ready!");
                    readyStatusLabel.setText("Waiting for others...");
                }

                awayButton.setEnabled(true);
                backButton.setEnabled(true);
                extendedOptionsCheckbox.setEnabled(!iAmAway);
                cooldownCheckbox.setEnabled(!iAmAway);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        readyPanel.add(readyButton);
        readyPanel.add(readyStatusLabel);
        readyPanel.add(extendedOptionsCheckbox);
        readyPanel.add(cooldownCheckbox);
        readyPanel.add(awayButton);
        readyPanel.add(backButton);
        readyPanel.add(spectateButton);
        readyPanel.add(joinGameButton);
    }

    public void setLocalAwayState(boolean away) {
        iAmAway = away;

        if (iAmSpectator) {
            readyButton.setEnabled(false);
            readyButton.setText("Spectating");
            readyStatusLabel.setText("You are spectating");
            return;
        }

        if (away) {
            readyButton.setEnabled(false);
            readyButton.setText("You are Away");
            readyStatusLabel.setText("You are marked away");
        } else {
            readyButton.setText("I'm Ready!");
            if (!iAmReady) {
                readyButton.setEnabled(true);
                readyStatusLabel.setText("Click Ready to start!");
            } else {
                readyStatusLabel.setText("Waiting for others...");
            }
        }
    }

    public void showGameView() {
        gameView.setVisible(true);
        splitPane.setDividerLocation(0.6);
        gameView.resetPickButtonsForNewRound();
        revalidate();
        repaint();
    }

    public void showChatOnlyView() {
        gameView.setVisible(false);
        chatView.setVisible(true);
        splitPane.setDividerLocation(1.0);
        revalidate();
        repaint();
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // unused
    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (isJoin && Constants.LOBBY.equals(roomName)) {
            showChatOnlyView();
            return;
        }

        if (isJoin && !Constants.LOBBY.equals(roomName)) {
            showGameView();
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {
        showGameView();

        if (readyPanel != null) {
            if (phase == Phase.READY) {
                readyPanel.setVisible(true);

                if (!iAmReady && !iAmAway && !iAmSpectator) {
                    readyButton.setEnabled(true);
                    readyButton.setText("I'm Ready!");
                    readyStatusLabel.setText("Click Ready to start!");
                }

                if (!iAmReady && !iAmAway && !iAmSpectator) {
                    extendedOptionsCheckbox.setEnabled(true);
                    cooldownCheckbox.setEnabled(true);
                }
            } else {
                readyPanel.setVisible(false);
            }
        }
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID && isQuiet) {
            iAmReady = false;

            if (iAmSpectator) {
                readyButton.setEnabled(false);
                readyButton.setText("Spectating");
                readyStatusLabel.setText("You are spectating");
            } else if (iAmAway) {
                readyButton.setEnabled(false);
                readyButton.setText("You are Away");
                readyStatusLabel.setText("You are marked away");
            } else {
                readyButton.setEnabled(true);
                readyButton.setText("I'm Ready!");
                readyStatusLabel.setText("Click Ready to start the next round");
            }

            extendedOptionsCheckbox.setEnabled(!iAmSpectator && !iAmAway);
            cooldownCheckbox.setEnabled(!iAmSpectator && !iAmAway);
            return;
        }

        if (!isQuiet && readyStatusLabel != null && !iAmSpectator) {
            if (isReady) {
                readyStatusLabel.setText("Someone marked ready...");
            } else {
                readyStatusLabel.setText("Someone is no longer ready...");
            }
        }
    }
}
