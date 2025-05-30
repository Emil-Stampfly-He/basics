package command.furniture;

public class GarageDoor {
    private boolean up = false;
    private boolean moving = false;
    private boolean lightOn = false;

    public GarageDoor() {}

    public void up() {
        try {
            System.out.println("Garage Door Up");
            this.moving = true;
            this.up = true;
            Thread.sleep(2000);
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void down() {
        try {
            System.out.println("Garage Door Down");
            this.moving = false;
            this.up = false;
            Thread.sleep(2000);
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.moving = false;
        System.out.println("Garage Door Stop");
    }

    public void lightOn() {
        this.lightOn = true;
        System.out.println("Garage Door Light On");
    }

    public void lightOff() {
        this.lightOn = false;
        System.out.println("Garage Door Light Off");
    }
}
