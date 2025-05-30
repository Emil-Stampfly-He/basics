package command.furniture;

public enum Media {
    CD("cd"), DVD("dvd"), RADIO("radio");

    private final String mediaName;
    Media(String mediaName) {this.mediaName = mediaName;}
    public String getMediaName() {return this.mediaName;}
}
