package observer.weather;

public class CurrentConditionsDisplay implements Observer, DisplayElements {
    private float temperature;
    private float humidity;
    private final Subject weatherData;

    public CurrentConditionsDisplay(Subject weatherData) {
        this.weatherData = weatherData;
        weatherData.registerObserver(this);
    }

    @Override
    public void display() {
        System.out.println("Current conditions: " + this.temperature
                + "F degrees and " + this.humidity + "% humidity");
    }

    @Override
    public void update() {
        if (weatherData instanceof WeatherData) {
            this.temperature = ((WeatherData) weatherData).getTemperature();
            this.humidity = ((WeatherData) weatherData).getHumidity();
        }

        display();
    }
}
