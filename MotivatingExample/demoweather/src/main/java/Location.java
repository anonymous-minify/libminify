import com.google.api.client.util.Key;

public class Location {
    @Key
    private String title;
    @Key
    private String location_type;
    @Key
    private int woeid;
    @Key
    private String latt_long;

    public String getTitle() {
        return title;
    }

    public String getLocation_type() {
        return location_type;
    }

    public int getWoeid() {
        return woeid;
    }

    public String getLatt_long() {
        return latt_long;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLocation_type(String location_type) {
        this.location_type = location_type;
    }

    public void setWoeid(int woeid) {
        this.woeid = woeid;
    }

    public void setLatt_long(String latt_long) {
        this.latt_long = latt_long;
    }

    @Override
    public String toString() {
        return title + "(" + woeid + ")";
    }
}
