package Project.Common;

// lsl8 | 11/24/25 | PointsPayload for syncing points
public class PointsPayload extends Payload {
    private long targetClientId;
    private int points;

    public long getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(long targetClientId) {
        this.targetClientId = targetClientId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return String.format("PointsPayload{type=%s, targetId=%d, points=%d}",
                getPayloadType(), targetClientId, points);
    }
}
