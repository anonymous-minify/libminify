import com.google.api.client.util.Key;

import java.util.ArrayList;

public class ConsolidatedWeather {

    public ArrayList<DayWeather> getConsolidated_weather() {
        return consolidated_weather;
    }

    public void setConsolidated_weather(ArrayList<DayWeather> consolidated_weather) {
        this.consolidated_weather = consolidated_weather;
    }

    @Key
    private ArrayList<DayWeather> consolidated_weather = new ArrayList<DayWeather>();

    @Override
    public String toString() {
        if (consolidated_weather.size() == 0) return "";

        DayWeather today = consolidated_weather.get(0);

        return String.format("%s (%.2f - %.2f °C)", today.getWeather_state_name(), today.getMin_temp(), today.getMax_temp());
     }
}
