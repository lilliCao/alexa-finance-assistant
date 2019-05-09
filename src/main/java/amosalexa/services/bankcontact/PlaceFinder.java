package amosalexa.services.bankcontact;


import com.google.maps.model.LatLng;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.walkercrou.places.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceFinder {

    /**
     *
     */
    private static final Logger log = LoggerFactory.getLogger(PlaceFinder.class);

    /**
     * maximum place requests results
     */
    private static final int LIMIT = 10;

    /**
     * search for places near latLng
     * @param latLng latitude - longitude
     * @param keywordParams slot value for place search
     * @return List of places
     */
    public static List<Place> findNearbyPlace(LatLng latLng, String... keywordParams){
        GooglePlaces client = new GooglePlaces(GeoCoder.GOOGLE_MAP_API_KEY);

        Param extraParams = new Param("keyword");
        StringBuilder stringBuilder = new StringBuilder();
        // create keyword Params
        for (String keywordParam : keywordParams) {
            stringBuilder.append(keywordParam);
        }
        extraParams.value(stringBuilder.toString());

        return client.getNearbyPlacesRankedByDistance(latLng.lat, latLng.lng, LIMIT, extraParams);
    }

    /**
     * checks list of places for place with opening hours and containing the slot value name
     * @param places list of places
     * @param slotName slot value
     * @return place with opening hours
     */
    public static Place findOpeningHoursPlace(List<Place> places, String slotName){
        for(Place place : places){
            // get additional place details
            place = place.getDetails();
                for(Hours.Period period : place.getHours().getPeriods()){
                    // place has opening hours
                    if(!period.getOpeningDay().name().isEmpty()) {
                        // place name contains slot value
                        if(place.getName().toLowerCase().contains(slotName.toLowerCase()))
                            return place;
                    }
                }
            }
        return null;
    }

    /**
     * looking for a place nearby your address with a telephone number
     * @param places list of places
     * @param slotName slot value
     * @return place with telephone number
     */
    public static Place findTelephoneNumberPlace(List<Place> places, String slotName){

        for(Place place : places){
            place = place.getDetails();
            if(place.getName().toLowerCase().contains(slotName.toLowerCase())){
                if(place.getPhoneNumber() != null){
                    return place;
                }
            }
        }
        return null;
    }


    /**
     * get the day of the week
     * @param date string date
     * @param locale locale
     * @return weekday
     */
    public static String getWeekday(String date, Locale locale){
        DateTime dateTime = new DateTime(date);
        return dateTime.dayOfWeek().getAsText(locale);
    }

    /**
     * get the opening or closing hours of the place depending of the weekday
     * @param place place
     * @param isOpening opening/closing
     * @param wd weekday
     * @return time
     */
    public static String getHours(Place place, boolean isOpening, String wd){

        String weekday = getWeekday(wd, Locale.ENGLISH);

        for(Hours.Period period : place.getHours().getPeriods()){
            String time = period.getClosingTime();
            if(isOpening){
                time = period.getOpeningTime();
            }
            if(period.getOpeningDay().name().toLowerCase().equals(weekday.toLowerCase())){
                return convertTime(time);
            }
        }
        return null;
    }

    /**
     * returns a list of strings with opening hours for each weekday
     * @param place place with opening hours
     * @return list of opening hours
     */
    public static List<String> getCompleteWeekdayHours(Place place){

        List<String> openingWeekdayHours = new ArrayList<>();

        LocalDate date = new LocalDate();

        for(Hours.Period period : place.getHours().getPeriods()){
            String dayOfWeek = period.getOpeningDay().name();
            date = date.withDayOfWeek(DayOfWeek.valueOf(dayOfWeek).getValue());
            String weekdayHours =
                    date.dayOfWeek().getAsText(Locale.GERMAN) +
                    " geöffnet von " +
                    "<say-as interpret-as=\"time\">" + convertTime(period.getOpeningTime()) + "</say-as> " +
                    " bis <say-as interpret-as=\"time\">" + convertTime(period.getClosingTime()) + "</say-as>. ";
            openingWeekdayHours.add(weekdayHours);
         }
        return openingWeekdayHours;
    }


    /**
     * converts time e.g. 1200 to 12:00
     * @param time input time
     * @return formatted output time
     */
    public static String convertTime(String time){
        DateTimeFormatter inputFormatter = DateTimeFormat.forPattern("HHmm");
        DateTime dateTime = inputFormatter.parseDateTime(time);
        DateTimeFormatter outputFormatter = DateTimeFormat.forPattern("HH:mm");
        return outputFormatter.print(dateTime);
    }
}
