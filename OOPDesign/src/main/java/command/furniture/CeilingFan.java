package command.furniture;

public class CeilingFan {
    private SpeedLevel speedLevel;
    private final String location;

    public CeilingFan(String location) {
        this.location = location;
        this.speedLevel = SpeedLevel.OFF;
    }

    public void high() {
        this.speedLevel = SpeedLevel.HIGH;
    }

    public void medium() {
        this.speedLevel = SpeedLevel.MEDIUM;
    }

    public void low() {
        this.speedLevel = SpeedLevel.LOW;
    }

    public void off() {
        this.speedLevel = SpeedLevel.OFF;
    }

    public SpeedLevel getSpeedLevel() {
        return this.speedLevel;
    }
}
