package command.furniture;

public class Light {
    private boolean on = false;
    private final String location;

    public Light(String location) {
        this.location = location;
    }

    public void on() {
        this.on = true;
        System.out.println(this.location + " light is on");
    }

    public void off() {
        this.on = false;
        System.out.println(this.location + " light is off");
    }
}
