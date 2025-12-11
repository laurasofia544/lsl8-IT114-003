package Project.Common;

public class TimerPayload extends Payload {
    private TimerType timerType;
    private int time;

    public TimerType getTimerType() {
        return timerType;
    }

    public void setTimerType(TimerType timerType) {
        this.timerType = timerType;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "TimerPayload{" +
                "type=" + getPayloadType() +
                ", clientId=" + getClientId() +
                ", timerType=" + timerType +
                ", time=" + time +
                '}';
    }
}
