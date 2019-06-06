package amosalexa.handlers.bankaccount;

import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;

public class StandingOrderDeleteHandler implements IntentRequestHandler {
    private static final String STANDING_ORDERS_DELETE_INTENT = "StandingOrdersDeleteIntent";
    private static final String SLOT_NUMBER = "Number";

    private static final String CARD_TITLE = "Dauerauftrag löschen";


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(STANDING_ORDERS_DELETE_INTENT))
                || isNumberIntentForPass(input, STANDING_ORDERS_DELETE_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, true);
        if (response.isPresent()) return response;

        Intent realIntent = getRealIntent(input, intentRequest);
        if (realIntent.getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
            return deleteStandingOrder(realIntent, input);
        }

        return Optional.empty();

    }

    private Optional<Response> deleteStandingOrder(Intent intent, HandlerInput inputReceived) {
        String number = intent.getSlots().get(SLOT_NUMBER).getValue();
        if (!AccountAPI.deleteStandingOrder(ACCOUNT_NUMBER, Integer.parseInt(number))) {
            return response(inputReceived, CARD_TITLE,
                    "Dauerauftrag Nummer " + number + " wurde nicht gefunden.");
        } else {
            return response(inputReceived, CARD_TITLE,
                    "Dauerauftrag Nummer " + number + " wurde gelöscht.");
        }
    }
}
