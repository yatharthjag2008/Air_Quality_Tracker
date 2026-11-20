import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class AirQualityAlert {

    
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson gson = new Gson();

    record Location(String status, String message, String city, double lat, double lon) {}

    record CurrentReading(@SerializedName("us_aqi") Integer usAqi) {}

    record AirQuality(CurrentReading current) {}



    public static void main(String[] args) {
        String locationJson = getUserLocation();
        Location location = parseCoordinates(locationJson);
        IO.println("You're near " + location.city() + " (" + location.lat() + ", " + location.lon() + ")");

        String airQualityJson = fetchAirQuality(location.lat(), location.lon());
        int aqi = parseAqi(airQualityJson);
        IO.println("Current US AQI: " + aqi);
    }

    
    public static String getUserLocation() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/?fields=status,message,city,lat,lon"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                IO.println("Error: Location request failed (HTTP " + response.statusCode() + ")");
                System.exit(1);
            }

            return response.body();

        } catch (Exception e) {
            IO.println("Error: Could not get your location - " + e.getMessage());
            System.exit(1);
            return null; 
        }
    }

    public static Location parseCoordinates(String json) {
       
        Location location = gson.fromJson(json, Location.class);

        if (location == null) {
            IO.println("Error: Could not find your location - empty reply");
            System.exit(1);
        }

        if (!location.status().equals("success")) {
            IO.println("Error: Could not find your location - " + location.message());
            System.exit(1);
        }


        return location;


    }

    public static String fetchAirQuality(double lat, double lon) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://air-quality-api.open-meteo.com/v1/air-quality"
                            + "?latitude=" + lat + "&longitude=" + lon + "&current=us_aqi"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                IO.println("Error: Air quality request failed (HTTP " + response.statusCode() + ")");
                System.exit(1);
            }

            return response.body();

        } catch (Exception e) {
            IO.println("Error: Could not get the air quality - " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    public static int parseAqi(String json) {
        AirQuality airQuality = gson.fromJson(json, AirQuality.class);

        if (airQuality == null || airQuality.current() == null || airQuality.current().usAqi() == null) {
            IO.println("Error: No air quality reading is available for your location");
            System.exit(1);
        }

        return airQuality.current().usAqi();
    }

        
    }