package Project.Common;

public class ReadyPayload extends Payload {
    private boolean ready;
    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        return "ReadyPayload{" +
                "type=" + getPayloadType() +
                ", clientId=" + getClientId() +
                ", ready=" + ready +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
