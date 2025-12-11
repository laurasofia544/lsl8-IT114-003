// lsl8 | 12/08/25
package Project.Client;

import java.io.IOException;

import Project.Client.Interfaces.IClientEvents;
import Project.Common.RoomAction;

public enum ClientConsole {
    INSTANCE;

    private final Client client = Client.INSTANCE;


    public boolean connect(String host, int port, String username) {
        return client.connect(host, port, username);
    }

    public void registerCallback(IClientEvents e) {
        client.registerCallback(e);
    }


    public void sendMessage(String message) throws IOException {
        client.sendMessage(message);
    }

    public void sendRoomAction(String roomName, RoomAction action) throws IOException {
        client.sendRoomAction(roomName, action);
    }

    public void sendReady() throws IOException {
        client.sendReady();
    }

    public void sendDoTurn(String text) throws IOException {
        client.sendDoTurn(text);
    }

    public void sendPick(String choiceCode) throws IOException {
        client.sendPick(choiceCode);
    }

    public String getDisplayNameFromId(long id) {
        return client.getDisplayNameFromId(id);
    }

    public boolean isMyClientId(long id) {
        return client.isMyClientId(id);
    }
}
