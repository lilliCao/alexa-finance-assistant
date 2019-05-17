package location;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import configuration.ConfigurationAMOS;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LocationTest {

    private static final Logger log = LoggerFactory.getLogger(LocationTest.class);

    @Test
    public void geoCodingTest() throws InterruptedException, ApiException, IOException {
        GeoApiContext context = new GeoApiContext.Builder().apiKey(ConfigurationAMOS.googleGeoCodingApiKey).build();
        GeocodingResult[] results = GeocodingApi.geocode(context,
                "Wölkernstraße 11 90459 Nürnberg").await();
        for (GeocodingResult result : results) {
            System.out.println(result.geometry.location.toString());
        }
    }
}
