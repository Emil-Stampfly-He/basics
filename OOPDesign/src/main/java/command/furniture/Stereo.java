package command.furniture;

public class Stereo {

    private boolean on = false;
    private Media media;
    private int volume;

    public Stereo() {}

    public void on() {
        this.on = true;
    }

    public void off() {
        this.on = false;
        this.media = null;
        this.volume = 0;
    }

    public void setCd() {
        this.media = Media.CD;
    }

    public void setDvd() {
        this.media = Media.DVD;
    }

    public void setRadio() {
        this.media = Media.RADIO;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
