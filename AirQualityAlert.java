import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AirQualityAlert {

    
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        String locationJson = getUserLocation();
       IO.println(locationJson);
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
}
