package amosalexa.handlers.bankcontact;

import amosalexa.handlers.Service;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.deviceAddress.Address;
import com.amazon.ask.model.services.deviceAddress.DeviceAddressServiceClient;
import com.amazon.ask.request.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.walkercrou.places.Place;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.BANK_NAME;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.bankcontact.PlaceFinder.*;

@Service(
        functionGroup = Service.FunctionGroup.BANK_CONTACT,
        functionName = "Bankkontaktinformation",
        example = "Wo ist meine Bank? Wie lautet die Telefonummer meiner Bank?",
        description = "Mit dieser Funktion kannst du Informationen über deine Bank abfragen. Du kannst dabei Parameter wie " +
                "Öffnungszeiten und Banknamen angeben."
)
public class BankContactServiceHandler implements IntentRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankContactServiceHandler.class);
    private static final String BANK_OPENING_HOURS_INTENT = "BankOpeningHours";
    private static final String BANK_ADDRESS_INTENT = "BankAddress";
    private static final String BANK_TELEPHONE_INTENT = "BankTelephone";

    private static final String SLOT_BANK_NAME = "BankNameSlots";
    private static final String SLOT_NAME_OPENING_HOURS_DATE = "OpeningHoursDate";

    private static final String CARD_TITLE = "Bank Kontakt Informationen";
    private static final String FULL_ADDRESS_PERMISSION = "read::alexa:device:all:address";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(BANK_ADDRESS_INTENT))
                || input.matches(Predicates.intentName(BANK_OPENING_HOURS_INTENT))
                || input.matches(Predicates.intentName(BANK_TELEPHONE_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        if (input.getRequestEnvelope().getSession().getUser().getPermissions() == null) {
            LOGGER.info("Can not get address. Asking for permission");
            String speechText = "Dieser Skill hat keine Berechtigung auf deine Adresse. " +
                    "Gib bitte diesem Skill die Berechtigung auf deine Adresse zu zugreifen";
            return input.getResponseBuilder()
                    .withAskForPermissionsConsentCard(Arrays.asList(FULL_ADDRESS_PERMISSION))
                    .withSimpleCard(CARD_TITLE, speechText)
                    .withSpeech(speechText)
                    .build();
        }

        DeviceAddressServiceClient deviceAddressServiceClient = input.getServiceClientFactory().getDeviceAddressService();
        String deviceId = input.getRequestEnvelope().getContext().getSystem().getDevice().getDeviceId();
        Address address = deviceAddressServiceClient.getFullAddress(deviceId);


        if (address == null) {
            response(input, CARD_TITLE, "I kann keine Addresse finden");
        }

        String bankName = BANK_NAME;
        if (intentRequest.getIntent().getSlots().containsKey(SLOT_BANK_NAME)) {
            bankName = intentRequest.getIntent().getSlots().get(SLOT_BANK_NAME).getValue();
        }

        switch (intentRequest.getIntent().getName()) {
            case BANK_ADDRESS_INTENT:
                return getAdress(input, address, bankName);
            case BANK_OPENING_HOURS_INTENT:
                return getOpeningHours(input, intentRequest, address, bankName);
            default:
                //BANK_TELEPHONE_INTENT
                return getTelephone(input, address, bankName);
        }
    }

    private Optional<Response> getTelephone(HandlerInput input, Address address, String bankName) {
        // Find place with telephone
        List<Place> places = PlaceFinder.findNearbyPlace(address, bankName);
        Place place = findTelephoneNumberPlace(places, bankName);
        String speech = place == null ? "No place was found! Your address:" + address.getAddressLine1()
                : place.getName() + " hat die Telefonnummer " + place.getPhoneNumber();
        return response(input, CARD_TITLE, speech);
    }

    private Optional<Response> getOpeningHours(HandlerInput input, IntentRequest intentRequest, Address address, String bankName) {
        // finds nearby place according the slot value
        List<Place> places = findNearbyPlace(address, bankName);
        // check the list of places for one with opening hours
        Place place = findOpeningHoursPlace(places, bankName);
        if (place == null) {
            return response(input, CARD_TITLE, "No place was found! Your address: " + address.getAddressLine1());
        }
        if (intentRequest.getIntent().getSlots().containsKey(SLOT_NAME_OPENING_HOURS_DATE)) {
            // request a specific date
            String slotDate = intentRequest.getIntent().getSlots().get(SLOT_NAME_OPENING_HOURS_DATE).getValue();
            String opening = PlaceFinder.getHours(place, true, slotDate);
            String closing = PlaceFinder.getHours(place, false, slotDate);
            String weekday = PlaceFinder.getWeekday(slotDate, Locale.GERMAN);
            if (closing != null && opening != null) {
                return response(input, CARD_TITLE, place.getName() + " Geöffnet am " + weekday + " von " + opening + " bis " + closing);
            }
            LOGGER.info("No opening hours for " + place.getName());
            return response(input, CARD_TITLE, "No opening hours for " + place.getName());

        } else {
            // opening time
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(place.getName()).append(" hat am ");
            List<String> openingWeekdayHours = getCompleteWeekdayHours(place);

            for (String hours : openingWeekdayHours) {
                stringBuilder.append(hours);
            }
            return response(input, CARD_TITLE, stringBuilder.toString());
        }
    }

    private Optional<Response> getAdress(HandlerInput input, Address address, String bankName) {
        // finds nearby place according the slot value
        List<Place> places = findNearbyPlace(address, bankName);

        // check the list of places for one with opening hours
        Place place = findOpeningHoursPlace(places, bankName);

        String speech = place == null ? "No place was found! Your address: " + address.getAddressLine1()
                : place.getName() + " hat die Adresse: " + place.getAddress();
        return response(input, CARD_TITLE, speech);
    }
}
