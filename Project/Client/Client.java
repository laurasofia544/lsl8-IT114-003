package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Project.Common.Command;
import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.PointsPayload;
import Project.Common.RoomAction;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Common.User;


/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public enum Client {
    INSTANCE;

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<Long, User>();
    private User myUser = new User();

    private void error(String message) {
        System.out.println(TextFX.colorize(String.format("%s", message), Color.RED));
    }

    // needs to be private now that the enum logic is handling this
    private Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     * @throws IOException
     */
//lsl8 11/03/25 Handling the user's name before connecting to the server
    // lsl8 | 11/24/25 - Controller for client commands
private boolean processClientCommand(String text) throws IOException {
    boolean wasCommand = false;

    if (text.startsWith(Constants.COMMAND_TRIGGER)) {
        text = text.substring(1);

        if (isConnection("/" + text)) {
            if (myUser.getClientName() == null || myUser.getClientName().isEmpty()) {
                System.out.println(
                        TextFX.colorize("Please set your name via /name <name> before connecting", Color.RED));
                return true;
            }

            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            sendClientName(myUser.getClientName());
            wasCommand = true;

        } else if (text.startsWith(Command.NAME.command)) {
            text = text.replace(Command.NAME.command, "").trim();
            if (text == null || text.length() == 0) {
                System.out.println(TextFX.colorize("This command requires a name as an argument", Color.RED));
                return true;
            }
            myUser.setClientName(text);
            System.out.println(TextFX.colorize(
                    String.format("Name set to %s", myUser.getClientName()), Color.YELLOW));
            wasCommand = true;

        } else if (text.equalsIgnoreCase(Command.LIST_USERS.command)) {
            System.out.println(TextFX.colorize("Known clients:", Color.CYAN));
            knownClients.forEach((key, value) -> {
                System.out.println(TextFX.colorize(
                        String.format("%s%s", value.getDisplayName(),
                                key == myUser.getClientId() ? " (you)" : ""), Color.CYAN));
            });
            wasCommand = true;

        } else if (Command.QUIT.command.equalsIgnoreCase(text)) {
            close();
            wasCommand = true;

        } else if (Command.DISCONNECT.command.equalsIgnoreCase(text)) {
            sendDisconnect();
            wasCommand = true;

        } else if (text.startsWith(Command.REVERSE.command)) {
            text = text.replace(Command.REVERSE.command, "").trim();
            sendReverse(text);
            wasCommand = true;

        } else if (text.startsWith(Command.CREATE_ROOM.command)) {
            text = text.replace(Command.CREATE_ROOM.command, "").trim();
            if (text == null || text.length() == 0) {
                System.out.println(TextFX.colorize(
                        "This command requires a room name as an argument", Color.RED));
                return true;
            }
            sendRoomAction(text, RoomAction.CREATE);
            wasCommand = true;

        } else if (text.startsWith(Command.JOIN_ROOM.command)) {
            text = text.replace(Command.JOIN_ROOM.command, "").trim();
            if (text == null || text.length() == 0) {
                System.out.println(TextFX.colorize(
                        "This command requires a room name as an argument", Color.RED));
                return true;
            }
            sendRoomAction(text, RoomAction.JOIN);
            wasCommand = true;

        } else if (text.startsWith(Command.LEAVE_ROOM.command) || text.startsWith("leave")) {
            sendRoomAction(text, RoomAction.LEAVE);
            wasCommand = true;

        } else if (text.equalsIgnoreCase("ready")) {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.READY);
            sendToServer(p);
            wasCommand = true;

        } else if (text.startsWith("pick ")) {
            String arg = text.substring(5).trim().toLowerCase();
            Payload p = new Payload();
            p.setPayloadType(PayloadType.CHOICE_PICKED);
            p.setMessage(arg);
            sendToServer(p);
            wasCommand = true;

        } else if (text.equalsIgnoreCase("score")) {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.POINTS_SYNC);
            p.setMessage("request");
            sendToServer(p);
            wasCommand = true;
        }
    } else {
    System.out.println(TextFX.colorize("Unknown command: /" + text, Color.YELLOW));
    wasCommand = true;
}
    return wasCommand;
}

    // Start Send*() methods

    /**
     * Sends a room action to the server
     * 
     * @param roomName
     * @param roomAction (join, leave, create)
     * @throws IOException
     */
    private void sendRoomAction(String roomName, RoomAction roomAction) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(roomName);
        switch (roomAction) {
            case CREATE:
                payload.setPayloadType(PayloadType.ROOM_CREATE);
                break;
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            default:
                System.out.println(TextFX.colorize("Invalid room action", Color.RED));
                break;
        }
        sendToServer(payload);
    }

    /**
     * Sends a reverse message action to the server
     * 
     * @param message
     * @throws IOException
     */
    private void sendReverse(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.REVERSE);
        sendToServer(payload);

    }
//lsl8 11/03/25 Sends the disconnect action to the server side 
    /**
     * Sends a disconnect action to the server
     * 
     * @throws IOException
     */
    private void sendDisconnect() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.DISCONNECT);
        sendToServer(payload);
    }
//lsl8 11/03/25 String Message
    /**
     * Sends a message to the server
     * 
     * @param message
     * @throws IOException
     */
    private void sendMessage(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.MESSAGE);
        sendToServer(payload);
    }

    /**
     * Sends the client's name to the server (what the user desires to be called)
     * 
     * @param name
     * @throws IOException
     */
    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (isConnected()) {
            out.writeObject(payload);
            out.flush(); // good practice to ensure data is written out immediately
        } else {
            System.out.println(
                    "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
        }
    }
    // End Send*() methods

    public void start() throws IOException {
        System.out.println("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject(); // blocking read
                if (fromServer != null) {
                    processPayload(fromServer);

                } else {
                    System.out.println("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            System.err.println("Error reading object as specified type: " + cce.getMessage());
            cce.printStackTrace();
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("Connection dropped");
                e.printStackTrace();
            }
        } finally {
            closeServerConnection();
        }
        System.out.println("listenToServer thread stopped");
    }
//lsl8 | 11/24/25
    private void processPayload(Payload payload) {
    switch (payload.getPayloadType()) {
        case CLIENT_CONNECT:
            break;
        case CLIENT_ID:
            processClientData(payload);
            break;
        case DISCONNECT:
            processDisconnect(payload);
            break;
        case MESSAGE:
            processMessage(payload);
            break;
        case REVERSE:
            processReverse(payload);
            break;
        case ROOM_JOIN:
        case ROOM_LEAVE:
        case SYNC_CLIENT:
            processRoomAction(payload);
            break;
        case CHOICE_PICKED:
        // Server says someone picked (without revealing their choice)
            System.out.println(
            TextFX.colorize(payload.getMessage(), Color.YELLOW));
            break;

        case ROUND_START:
            // server sends "Round started! Make your pick..."
            System.out.println(TextFX.colorize(payload.getMessage(), Color.GREEN));
            break;
        case PICKED_NOTICE:
            System.out.println(TextFX.colorize(payload.getMessage(), Color.YELLOW));
            break;
        case BATTLE_RESULT:
            System.out.println(TextFX.colorize(payload.getMessage(), Color.BLUE));
            break;
        case POINTS_SYNC:
            if (payload instanceof PointsPayload) {
                PointsPayload pp = (PointsPayload) payload;
                System.out.println(TextFX.colorize(
                    String.format("Points update -> %s: %d",
                        knownClients.containsKey(pp.getTargetClientId())
                            ? knownClients.get(pp.getTargetClientId()).getDisplayName()
                            : ("#" + pp.getTargetClientId()),
                        pp.getPoints()
                    ),
                    Color.CYAN
                ));
            } else {
                System.out.println(TextFX.colorize(payload.getMessage(), Color.CYAN));
            }
            break;
        case GAME_OVER:
            System.out.println(TextFX.colorize(payload.getMessage(), Color.YELLOW));
            break;
        default:
            System.out.println(TextFX.colorize("Unhandled payload type -> " + payload.getPayloadType(),Color.YELLOW));
        break;
    }
}
//lsl8 11/03/25 Client receives confirmation and logs "connected"
    // Start process*() methods
    private void processClientData(Payload payload) {
        if (myUser.getClientId() != Constants.DEFAULT_CLIENT_ID) {
            System.out.println(TextFX.colorize("Client ID already set, this shouldn't happen", Color.YELLOW));

        }
        myUser.setClientId(payload.getClientId());
        myUser.setClientName(((ConnectionPayload) payload).getClientName());// confirmation from Server
        knownClients.put(myUser.getClientId(), myUser);
        System.out.println(TextFX.colorize("Connected", Color.GREEN));
    }
//lsl8 11/03/25 how the client processes disconnect
    private void processDisconnect(Payload payload) {
        if (payload.getClientId() == myUser.getClientId()) {
            knownClients.clear();
            myUser.reset();
            System.out.println(TextFX.colorize("You disconnected", Color.RED));
        } else if (knownClients.containsKey(payload.getClientId())) {
            User disconnectedUser = knownClients.remove(payload.getClientId());
            if (disconnectedUser != null) {
                System.out.println(TextFX.colorize(String.format("%s disconnected", disconnectedUser.getDisplayName()),
                        Color.RED));
            }
        }

    }

    private void processRoomAction(Payload payload) {
        if (!(payload instanceof ConnectionPayload)) {
            error("Invalid payload subclass for processRoomAction");
            return;
        }
        ConnectionPayload connectionPayload = (ConnectionPayload) payload;
        // use DEFAULT_CLIENT_ID to clear knownClients (mostly for disconnect and room
        // transitions)
        if (connectionPayload.getClientId() == Constants.DEFAULT_CLIENT_ID) {
            knownClients.clear();
            return;
        }
        switch (connectionPayload.getPayloadType()) {

            case ROOM_LEAVE:
                if (knownClients.containsKey(connectionPayload.getClientId())) {
                    knownClients.remove(connectionPayload.getClientId());
                }
                if (connectionPayload.getMessage() != null) {
                    System.out.println(TextFX.colorize(connectionPayload.getMessage(), Color.YELLOW));
                }

                break;
            case ROOM_JOIN:
                if (connectionPayload.getMessage() != null) {
                    System.out.println(TextFX.colorize(connectionPayload.getMessage(), Color.GREEN));
                }
            case SYNC_CLIENT:
                if (!knownClients.containsKey(connectionPayload.getClientId())) {
                    User user = new User();
                    user.setClientId(connectionPayload.getClientId());
                    user.setClientName(connectionPayload.getClientName());
                    knownClients.put(connectionPayload.getClientId(), user);
                }
                break;
            default:
                error("Invalid payload type for processRoomAction");
                break;
        }
    }

    private void processMessage(Payload payload) {
        System.out.println(TextFX.colorize(payload.getMessage(), Color.BLUE));
    }

    private void processReverse(Payload payload) {
        System.out.println(TextFX.colorize(payload.getMessage(), Color.PURPLE));
    }
    // End process*() methods

    /**
     * Listens for keyboard input from the user
     */
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input");
            while (isRunning) { // Run until isRunning is false
                String userInput = si.nextLine();
                if (!processClientCommand(userInput)) {
                    sendMessage(userInput);
                }
            }
        } catch (IOException ioException) {
            System.out.println("Error in listentToInput()");
            ioException.printStackTrace();
        }
        System.out.println("listenToInput thread stopped");
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        System.out.println("Client terminated");
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        try {
            if (out != null) {
                System.out.println("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (in != null) {
                System.out.println("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (server != null) {
                System.out.println("Closing connection");
                server.close();
                System.out.println("Closed socket");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            System.out.println("Exception from main()");
            e.printStackTrace();
        }
    }
}