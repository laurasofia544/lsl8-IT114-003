package Project.Common;

public class User {
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;

    private boolean ready = false;
    private boolean tookTurn = false;
    private int points = -1;

    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDisplayName() {
        if (clientName == null || clientName.isEmpty()) {
            return String.format("#%d", clientId);
        }
        return String.format("%s#%d", clientName, clientId);
    }

    public void reset() {
        clientId = Constants.DEFAULT_CLIENT_ID;
        clientName = null;
        ready = false;
        tookTurn = false;
        points = -1;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean didTakeTurn() {
        return tookTurn;
    }

    public void setTookTurn(boolean tookTurn) {
        this.tookTurn = tookTurn;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
