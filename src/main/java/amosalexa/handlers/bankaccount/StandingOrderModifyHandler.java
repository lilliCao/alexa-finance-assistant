package amosalexa.handlers.bankaccount;

import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;

import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;

public class StandingOrderModifyHandler implements IntentRequestHandler {
    private static final String STANDING_ORDERS_MODIFY_INTENT = "StandingOrdersModifyIntent";
    private static final String SLOT_NUMBER = "Number";
    private static final String SLOT_AMOUNT = "Amount";
    private static final String SLOT_EXECUTIONRATE = "ExecutionRate";
    private static final String SLOT_FIRSTEXECUTION = "FirstExecution";

    private static final String CARD_TITLE = "Dauerauftrag ändern";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(STANDING_ORDERS_MODIFY_INTENT))
                || isNumberIntentForPass(input, STANDING_ORDERS_MODIFY_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, true);
        if (response.isPresent()) return response;

        Intent realIntent = getRealIntent(input, intentRequest);
        if (realIntent.getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
            return modifyStandingOrder(realIntent, input);
        }

        return Optional.empty();
    }

    private Optional<Response> modifyStandingOrder(Intent intent, HandlerInput inputReceived) {
        Map<String, Slot> slots = intent.getSlots();
        int numberSlot = Integer.parseInt(slots.get(SLOT_NUMBER).getValue());
        String amountSlot = slots.get(SLOT_AMOUNT).getValue();
        String executionRateSlot = slots.get(SLOT_EXECUTIONRATE).getValue();
        String firstExecutionSlot = slots.get(SLOT_FIRSTEXECUTION).getValue();

        // TODO check if this standingorder id really exist -> otherwise handle instead of exception
        StandingOrder standingOrder = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, numberSlot);
        String speech;

        if (standingOrder == null) {
            return response(inputReceived, CARD_TITLE, "Es gibt keinen Dauerauftrag mit dieser Nummer");
        }
        if (amountSlot != null) {
            standingOrder.setAmount(Integer.parseInt(amountSlot));
        }
        if (executionRateSlot != null) {
            standingOrder.setExecutionRateFromString(executionRateSlot);
        }
        if (firstExecutionSlot != null) {
            standingOrder.setFirstExecution(firstExecutionSlot);
        }

        if (AccountAPI.updateStandingOrder(ACCOUNT_NUMBER, standingOrder)) {
            speech = "Dauerauftrag Nummer " + numberSlot + " wurde geändert.";
        } else {
            speech = "Etwas ist schiefgelaufen. Bitte versuch noch mal";
        }

        return response(inputReceived, CARD_TITLE, speech);
    }
}
