package Project.Server;

import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.RoomAction;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    protected void info(String message) {
        System.out.println(TextFX.colorize(String.format("Thread[%s]: %s", this.getClientId(), message), Color.CYAN));
    }

    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        this.client = myClient;
        this.onInitializationComplete = onInitializationComplete;
    }

    // Start Send*() Methods
//lsl8 | 12/08/25 Round Start Message
    protected boolean sendRoundStart(String msg) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROUND_START);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage(msg);
        return sendToClient(p);
    }

    protected boolean sendDisconnect(long clientId) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(PayloadType.DISCONNECT);
        return sendToClient(payload);
    }

    protected boolean sendResetUserList() {
        return sendClientInfo(Constants.DEFAULT_CLIENT_ID, null, RoomAction.JOIN);
    }

    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action) {
        return sendClientInfo(clientId, clientName, action, false);
    }

    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action, boolean isSync) {
        ConnectionPayload payload = new ConnectionPayload();
        switch (action) {
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            default:
                break;
        }
        if (isSync) {
            payload.setPayloadType(PayloadType.SYNC_CLIENT);
        }
        payload.setClientId(clientId);
        payload.setClientName(clientName);
        return sendToClient(payload);
    }

    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());
        return sendToClient(payload);
    }

    protected boolean sendMessage(long clientId, String message) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setMessage(message);
        payload.setClientId(clientId);
        return sendToClient(payload);
    }

    // lsl8 | 12/08/25 | Round Start Message
    protected boolean sendRoundStart() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROUND_START);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage("Round started! Make your pick with /pick r|p|s|l|k");
        return sendToClient(p);
    }

    protected boolean sendPickedNotice(String msg) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PICKED_NOTICE);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage(msg);
        return sendToClient(p);
    }

    protected boolean sendBattleResult(String msg) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.BATTLE_RESULT);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage(msg);
        return sendToClient(p);
    }

    protected boolean sendGameOver(String msg) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.GAME_OVER);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage(msg);
        return sendToClient(p);
    }

    // End Send*() Methods

    @Override
    protected void processPayload(Payload incoming) {

        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                break;

            case DISCONNECT:
                currentRoom.handleDisconnect(this);
                break;

            case MESSAGE: {
                String msg = incoming.getMessage();

                if (msg != null && msg.startsWith("/")) {
                    String lower = msg.toLowerCase();

                    // /away
                    if (lower.equals("/away")) {
                        if (currentRoom instanceof GameRoom) {
                            ((GameRoom) currentRoom).handleAway(this, true);
                        } else {
                            sendMessage(Constants.DEFAULT_CLIENT_ID,
                                    "You must be in a game room to use /away");
                        }
                        break;
                    }

                    // /back
                    if (lower.equals("/back")) {
                        if (currentRoom instanceof GameRoom) {
                            ((GameRoom) currentRoom).handleAway(this, false);
                        } else {
                            sendMessage(Constants.DEFAULT_CLIENT_ID,
                                    "You must be in a game room to use /back");
                        }
                        break;
                    }

                    // /spectate <roomName>
                    if (lower.startsWith("/spectate")) {
                        String[] parts = msg.trim().split("\\s+", 2);
                        if (parts.length < 2 || parts[1].trim().isEmpty()) {
                            sendMessage(Constants.DEFAULT_CLIENT_ID,
                                    "Usage: /spectate <roomName>");
                        } else {
                            String roomName = parts[1].trim();
                            currentRoom.handleJoinRoom(this, roomName);
                            if (currentRoom instanceof GameRoom) {
                                ((GameRoom) currentRoom).handleSpectatorJoin(this);
                            } else {
                                sendMessage(Constants.DEFAULT_CLIENT_ID,
                                        "Room " + roomName + " is not a game room.");
                            }
                        }
                        break;
                    }

                    // /play -> stop spectating
                    if (lower.equals("/play")) {
                        if (currentRoom instanceof GameRoom) {
                            ((GameRoom) currentRoom).handleStopSpectate(this);
                        } else {
                            sendMessage(Constants.DEFAULT_CLIENT_ID,
                                    "You must be in a game room to use /play");
                        }
                        break;
                    }
                }

                // Block chat messages from spectators
                if (currentRoom instanceof GameRoom
                        && ((GameRoom) currentRoom).isSpectator(getClientId())) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID,
                            "Spectators can see chat but cannot send messages.");
                    break;
                }

                currentRoom.handleMessage(this, msg);
                break;
            }

            case REVERSE:
                currentRoom.handleReverseText(this, incoming.getMessage());
                break;

            case ROOM_CREATE:
                currentRoom.handleCreateRoom(this, incoming.getMessage());
                break;

            case ROOM_JOIN:
                currentRoom.handleJoinRoom(this, incoming.getMessage());
                break;

            case ROOM_LEAVE:
                currentRoom.handleJoinRoom(this, Room.LOBBY);
                break;

            case READY:
                if (currentRoom instanceof GameRoom) {
                    ((GameRoom) currentRoom).handleReady(this);
                } else {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a game room to /ready");
                }
                break;

            case CHOICE_PICKED:
                if (currentRoom instanceof GameRoom) {
                    ((GameRoom) currentRoom).handlePick(this, incoming.getMessage());
                } else {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a game room to /pick");
                }
                break;

            case POINTS_SYNC:
                if (currentRoom instanceof GameRoom) {
                    ((GameRoom) currentRoom).handleScoreRequest(this);
                }
                break;

            default:
                System.out.println(TextFX.colorize("Unknown payload type received", Color.RED));
                break;
        }
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }
}
