import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class AirQualityAlert {

    
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson gson = new Gson();

    record Location(String status, String message, String city, double lat, double lon) {}

    record CurrentReading(@SerializedName("us_aqi") Integer usAqi) {}

    record AirQuality(CurrentReading current) {}


    // main method
    public static void main(String[] args) {
        String locationJson = getUserLocation();
        Location location = parseCoordinates(locationJson);
        IO.println("You're near " + location.city() + " (" + location.lat() + ", " + location.lon() + ")");

        String airQualityJson = fetchAirQuality(location.lat(), location.lon());
        int aqi = parseAqi(airQualityJson);
        IO.println("Current US AQI: " + aqi);

        int warningLevel = askWarningLevel();

        boolean aboveWarningLevel = isAboveWarningLevel(aqi, warningLevel);

        if (aboveWarningLevel) {
            showWarning(aqi, warningLevel);
        } else {
            showSafe(aqi, warningLevel);
        }
    }

    // gets the user's location from the ip-api.com API
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


    // parses the JSON from the location API and returns a locatiion object
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

    // gets the air quality JSON from the Open-Meteo API
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

    // parses the JSON from the air quality API and returns the aqi
    public static int parseAqi(String json) {
        AirQuality airQuality = gson.fromJson(json, AirQuality.class);

        if (airQuality == null || airQuality.current() == null || airQuality.current().usAqi() == null) {
            IO.println("Error: No air quality reading is available for your location");
            System.exit(1);
        }

        return airQuality.current().usAqi();
    }

    // Turns a US AQI number into the EPA's category name
    public static String describeAqi(int aqi) {
        if (aqi <= 50) {
            return "Good";
        } else if (aqi <= 100) {
            return "Moderate";
        } else if (aqi <= 150) {
            return "Unhealthy for Sensitive Groups";
        } else if (aqi <= 200) {
            return "Unhealthy";
        } else if (aqi <= 300) {
            return "Very Unhealthy";
        } else {
            return "Hazardous";
        }
    }

    // Asks for an AQI warning level and keeps asking until it gets a whole number from 0 to 500
    public static int askWarningLevel() {
        while (true) {
            String input = IO.readln("Warn me when the AQI is above (0 - 500): ");

            if (input == null) {
                IO.println("Error: No warning level was entered");
                System.exit(1);
            }

            try {
                int level = Integer.parseInt(input.strip());

                if (level >= 0 && level <= 500) {
                    return level;
                }

                IO.println("Please enter a number from 0 to 500.");
            } catch (NumberFormatException e) {
                IO.println("Please enter a whole number, like 100.");
            }
        }
    }

    // Returns true when the current AQI is above the user's warning level
    public static boolean isAboveWarningLevel(int aqi, int warningLevel) {
        return aqi > warningLevel;
    }


    // shows the air quality is not safe according to the user's warning level
    public static void showWarning(int aqi, int warningLevel) {
        IO.println("Warning: The air is " + describeAqi(aqi) + " (AQI " + aqi
                + "), which is above your warning level of " + warningLevel + ".");
        IO.println("Tip: " + healthTip(aqi));
    }

    // shows the air quality is safe according to the user's warning level
    public static void showSafe(int aqi, int warningLevel) {
        IO.println("Safe: The air is " + describeAqi(aqi) + " (AQI " + aqi
                + "), which is at or below your warning level of " + warningLevel + ".");
    }

    // Based on aqi, displays the appropriate health tip
    public static String healthTip(int aqi) {
        if (aqi <= 50) {
            return "The air is still good, so it's fine to be active outside.";
        } else if (aqi <= 100) {
            return "If you are unusually sensitive to air pollution, take it easier during long or hard activity outside.";
        } else if (aqi <= 150) {
            return "Children, older adults, and people with asthma or heart or lung disease should cut back on long or hard activity outside.";
        } else if (aqi <= 200) {
            return "Cut back on long or hard activity outside, and take more breaks.";
        } else if (aqi <= 300) {
            return "Avoid long or hard activity outside, and move what you can indoors.";
        } else {
            return "Stay indoors and avoid all activity outside.";
        }
    }


    }


// JUnit tests for the methods that don't need the internet
class AirQualityAlertTest {

    // 50 is the highest AQI that still counts as Good
    @Test
    void describeAqiWorks() {
        assertEquals("Good", AirQualityAlert.describeAqi(50));
    }

    // 301 is the lowest AQI that counts as Hazardous
    @Test
    void healthTipWorks() {
        assertEquals("Stay indoors and avoid all activity outside.", AirQualityAlert.healthTip(301));
    }

    // An AQI equal to the warning level counts as safe
    @Test
    void isAboveWarningLevelWorks() {
        assertFalse(AirQualityAlert.isAboveWarningLevel(100, 100));
    }
}
