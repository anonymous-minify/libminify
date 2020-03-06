import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.reflect.TypeToken;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;


public class Weather {
    public static void main(String[] args) throws IOException {
        AnsiConsole.systemInstall();

        if(args.length == 0) {
            System.out.println(ansi().fgRed().render("No location provided."));
            System.exit(1);
        }

        String locationQuery = "https://www.metaweather.com/api/location/search/?query=";
        HttpTransport transport = new NetHttpTransport();
        JsonObjectParser jsonObjectParser = JacksonFactory.getDefaultInstance().createJsonObjectParser();

        HttpRequest request = transport
                .createRequestFactory()
                .buildGetRequest(new GenericUrl(locationQuery + args[0]))
                .setParser(jsonObjectParser);
        HttpResponse response = request.execute();

        Type listType = new TypeToken<ArrayList<Location>>() {}.getType();
        ArrayList<Location> locations = (ArrayList<Location>) response.parseAs(listType);

        if (locations == null || locations.size() == 0) {
            System.out.println(ansi().fgRed().render("Location not found"));
            System.exit(1);
        }

        System.out.println(ansi().fgGreen().render("Retrieving weather for: " + locations.get(0).getTitle()));

        String weatherQuery = "https://www.metaweather.com/api/location/"+locations.get(0).getWoeid() + "/";
        HttpRequest weatherRequest = transport
                .createRequestFactory()
                .buildGetRequest(new GenericUrl(weatherQuery))
                .setParser(jsonObjectParser);
        HttpResponse weatherResponse = weatherRequest.execute();

        System.out.println(ansi().fgBrightGreen().render("Today: " + weatherResponse.parseAs(ConsolidatedWeather.class)));

        AnsiConsole.systemUninstall();
    }
}
