package command.furniture;

public enum SpeedLevel {
    OFF(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int speedLevel;
    SpeedLevel(int i) {this.speedLevel = i;}
    public int getSpeedLevel() {return this.speedLevel;}
}
