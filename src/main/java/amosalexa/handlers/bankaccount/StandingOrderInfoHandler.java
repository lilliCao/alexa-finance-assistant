package amosalexa.handlers.bankaccount;

import amosalexa.handlers.Service;
import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;


@Service(
        functionName = "Daueraufträge verwalten",
        functionGroup = Service.FunctionGroup.ONLINE_BANKING,
        example = "Wie lauten meine Daueraufträge?",
        description = "Diese Funktion ermöglicht dir das Verwalten von Daueraufträgen."
)
public class StandingOrderInfoHandler implements IntentRequestHandler {
    private static final String STANDING_ORDERS_INFO_INTENT = "StandingOrdersInfoIntent";

    private static final String SLOT_NEXT_INDEX = "NextIndex";
    private static final String SLOT_CHANNEL = "Channel";
    private static final String SLOT_VALUE_EMAIL = "Email";
    private static final String SLOT_PAYEE = "Payee";

    private static final String CARD_TITLE = "Dauerauftrag abfragen";
    private static final String TEXT_NEXT_STANDINGORDER = "Moechtest du einen weiteren Eintrag hoeren?";

    private static ArrayList<StandingOrder> standingOrderCollection;


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(STANDING_ORDERS_INFO_INTENT))
                || isNumberIntentForPass(input, STANDING_ORDERS_INFO_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;

        Intent intent = getRealIntent(input, intentRequest);
        Slot nextIndex = intent.getSlots().get(SLOT_NEXT_INDEX);
        if (nextIndex.getValue() == null) {
            return getStandingOrdersInfo(intent, input);
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            return nextStandingOrder(Integer.parseInt(nextIndex.getValue()), intent, input);
        } else {
            return response(input, CARD_TITLE);
        }
    }

    private Optional<Response> nextStandingOrder(int nextIndex, Intent intent, HandlerInput inputReceived) {
        StringBuilder builder = new StringBuilder();
        builder.append(standingOrderCollection.get(nextIndex).getSpeechOutput());
        nextIndex++;
        if (nextIndex < standingOrderCollection.size()) {
            intent.getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(nextIndex))
                    .withName(SLOT_NEXT_INDEX).build());
            builder.append(TEXT_NEXT_STANDINGORDER);
            return responseWithSlotConfirm(inputReceived, CARD_TITLE, builder.toString(), intent, SLOT_NEXT_INDEX);
        } else {
            return response(inputReceived, CARD_TITLE, builder.toString());
        }
    }

    private Optional<Response> getStandingOrdersInfo(Intent intent, HandlerInput inputReceived) {
        Map<String, Slot> slots = intent.getSlots();
        StringBuilder builder = new StringBuilder();

        // get list of standing orders
        if (slots.get(SLOT_PAYEE).getValue() != null) {
            Collection<StandingOrder> standingOrdersForAccount = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);
            standingOrderCollection = new ArrayList<>();
            for (StandingOrder so : standingOrdersForAccount) {
                if (StringUtils.getLevenshteinDistance(
                        slots.get(SLOT_PAYEE).getValue(), so.getPayee())
                        <= so.getPayee().length() / 3) {
                    standingOrderCollection.add(so);
                }
            }
        } else {
            standingOrderCollection = new ArrayList<>(AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER));
        }

        // handle send email or read
        if (standingOrderCollection.isEmpty()) {
            return response(inputReceived, CARD_TITLE, "Keine Daueraufträge vorhanden.");
        }

        // email
        if (slots.get(SLOT_CHANNEL).getValue() != null
                && slots.get(SLOT_CHANNEL).getValue().equalsIgnoreCase(SLOT_VALUE_EMAIL)) {
            // send per email
            //TODO call email client
            builder.append("Ich habe eine Übersicht deiner ")
                    .append(standingOrderCollection.size())
                    .append(" Daueraufträge an deine E-Mail-Adresse gesendet.");
            return response(inputReceived, CARD_TITLE, builder.toString());
        }

        // read
        if (standingOrderCollection.size() == 1) {
            builder.append("Es wurde ein Dauerauftrag gefunden. ")
                    .append(standingOrderCollection.get(0).getSpeechOutput());
            return response(inputReceived, CARD_TITLE, builder.toString());
        } else {
            builder.append("Es wurden " + standingOrderCollection.size() + " Dauerauftraege gefunden. ")
                    .append(standingOrderCollection.get(0).getSpeechOutput())
                    .append(TEXT_NEXT_STANDINGORDER);
            intent.getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue("1")
                    .withName(SLOT_NEXT_INDEX).build());
            return responseWithSlotConfirm(inputReceived, CARD_TITLE, builder.toString(), intent, SLOT_NEXT_INDEX);
        }
    }
}
