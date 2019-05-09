package amosalexa.services.bankcontact;


import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import configuration.ConfigurationAMOS;
import model.location.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GeoCoder {

    public static final String GOOGLE_MAP_API_KEY = ConfigurationAMOS.googleGeoCodingApiKey;
    private static final Logger log = LoggerFactory.getLogger(GeoCoder.class);

    /**
     *  converts address to geo code to perform place search
     * @param address device address
     * @return LatLng of device
     */
    public static LatLng getLatLng(Address address){

        if(address == null){
            address = new Address();
        }

        log.info("Your address: " + address.getCity() + " " + address.getAddressLine1());

        GeoApiContext context = new GeoApiContext().setApiKey(GOOGLE_MAP_API_KEY);
        GeocodingResult[] results = null;
        try {
            results =  GeocodingApi.geocode(context, address.getAddressLine1() + " " + address.getCity() + " " + address.getPostalCode()).await();
        } catch (ApiException | InterruptedException | IOException e) {
            e.printStackTrace();
        }

        if(results.length == 0)
            return null;

        return results[0].geometry.location;
    }


}
