package observer.weather;

import java.util.ArrayList;
import java.util.List;

public class StatisticsDisplay implements Observer, DisplayElements {
    private final List<Float> temperatures = new ArrayList<>();
    private final Subject weatherData;

    public StatisticsDisplay(Subject weatherData) {
        this.weatherData = weatherData;
        weatherData.registerObserver(this);
    }

    @Override
    public void display() {
        float avgTemp = getAverageTemp(this.temperatures);
        float maxTemp = getMaxTemp(this.temperatures);
        float minTemp = getMinTemp(this.temperatures);
        System.out.println("Avg/Max/Min temperature: " + avgTemp + "/" + maxTemp + "/" + minTemp);
    }

    @Override
    public void update() {
        if (weatherData instanceof WeatherData) {
            this.temperatures.add(((WeatherData) weatherData).getTemperature());
        }

        display();
    }

    private float getAverageTemp(List<Float> temperatures) {
        float sum = 0;
        for (Float temp : temperatures) {
            sum += temp;
        }
        return sum / temperatures.size();
    }

    private float getMaxTemp(List<Float> temperatures) {
        float maxTemp = temperatures.get(0);
        for (int i = 1; i < temperatures.size(); i++) {
            if (temperatures.get(i) > maxTemp) {
                maxTemp = temperatures.get(i);
            }
        }
        return maxTemp;
    }

    private float getMinTemp(List<Float> temperatures) {
        float minTemp = temperatures.get(0);
        for (int i = 1; i < temperatures.size(); i++) {
            if (temperatures.get(i) < minTemp) {
                minTemp = temperatures.get(i);
            }
        }
        return minTemp;
    }
}
