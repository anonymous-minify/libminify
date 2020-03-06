import com.google.api.client.util.Key;

public class DayWeather {
    @Key
    private long id;

    @Key
    private String weather_state_name;

    @Key
    private double min_temp;

    @Key
    private double max_temp;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWeather_state_name() {
        return weather_state_name;
    }

    public void setWeather_state_name(String weather_state_name) {
        this.weather_state_name = weather_state_name;
    }

    public double getMin_temp() {
        return min_temp;
    }

    public void setMin_temp(double min_temp) {
        this.min_temp = min_temp;
    }

    public double getMax_temp() {
        return max_temp;
    }

    public void setMax_temp(double max_temp) {
        this.max_temp = max_temp;
    }

    /*
    "id": 5510186766696448,
        "weather_state_name": "Heavy Rain",
        "weather_state_abbr": "hr",
        "wind_direction_compass": "SW",
        "created": "2020-02-24T12:16:02.201216Z",
        "applicable_date": "2020-02-24",
        "min_temp": 6.59,
        "max_temp": 11.54,
        "the_temp": 10.91,
        "wind_speed": 11.968932260815505,
        "wind_direction": 231.00244214605812,
        "air_pressure": 1007.5,
        "humidity": 85,
        "visibility": 6.5591943052572965,
        "predictability": 77
        s
 */
}
