package amosalexa.handlers.bankaccount;

import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;
import model.db.Contact;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithIntentConfirm;

public class StandingOrderSmartHandler implements IntentRequestHandler {
    private static final String STANDING_ORDERS_SMART_INTENT = "StandingOrderSmartIntent";

    private static final String SLOT_PAYEE = "Payee";
    private static final String SLOT_ORDER_AMOUNT = "orderAmount";
    private static final String SAVE_STANDINGORDER_ID = "save_standingorder_id";
    private static final String SAVE_AMOUNT = "save_amount";

    private static final String CARD_TITLE = "Dauerauftrag ändern";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(STANDING_ORDERS_SMART_INTENT))
                || isNumberIntentForPass(input, STANDING_ORDERS_SMART_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, true);
        if (response.isPresent()) return response;

        Intent intent = getRealIntent(input, intentRequest);

        switch (intent.getConfirmationStatus()) {
            case NONE:
                return getStandingOrdersSmart(intent, input);
            case CONFIRMED:
                return updateStandingOrdersSmart(input);
            default:
                return response(input, CARD_TITLE);
        }
    }

    private Optional<Response> getStandingOrdersSmart(Intent intent, HandlerInput inputReceived) {
        Map<String, Slot> slots = intent.getSlots();
        ArrayList<StandingOrder> standingOrderCollection = new ArrayList<>(AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER));

        // get name and amount
        String payee = slots.get(SLOT_PAYEE).getValue();
        String amount = slots.get(SLOT_ORDER_AMOUNT).getValue();

        for (StandingOrder standingOrder : standingOrderCollection) {
            if (standingOrder.getPayee().equalsIgnoreCase(payee)) {
                String speech = "Der Dauerauftrag für " + payee + " über " + standingOrder.getAmount()
                        + " Euro existiert schon. Möchtest du diesen aktualisieren";
                Map<String, Object> sessionAttributes = inputReceived.getAttributesManager().getSessionAttributes();
                sessionAttributes.put(SAVE_STANDINGORDER_ID, standingOrder.getStandingOrderId());
                sessionAttributes.put(SAVE_AMOUNT, amount);
                inputReceived.getAttributesManager().setSessionAttributes(sessionAttributes);
                return responseWithIntentConfirm(inputReceived, CARD_TITLE,
                        speech, intent);
            }
        }

        //create new
        List<Contact> contacts = dynamoDbMapper.loadAll(Contact.class);
        Date time = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String today = formatter.format(time);
        for (Contact contact : contacts) {
            if (contact.getName().equalsIgnoreCase(payee)) {
                StandingOrder standingOrder = new StandingOrder();
                standingOrder.setPayee(payee);
                standingOrder.setAmount(Integer.parseInt(amount));
                standingOrder.setDestinationAccount(contact.getIban());
                standingOrder.setFirstExecution(today);
                standingOrder.setExecutionRate(StandingOrder.ExecutionRate.YEARLY); //TODO include in slot
                AccountAPI.createStandingOrderForAccount(ACCOUNT_NUMBER, standingOrder);
                String speech = "Ich habe den neuen Dauerauftrag für " + payee
                        + " über " + amount + " Euro erfolgreich eingerichtet";
                return response(inputReceived, CARD_TITLE, speech);
            }
        }

        // Contact not found
        return response(inputReceived, CARD_TITLE,
                "Ich habe " + payee + " in deiner Kontaktliste nicht gefunden. " +
                        "Du musst ihm erst in der Kontaktliste hinzufügen");
    }

    private Optional<Response> updateStandingOrdersSmart(HandlerInput inputReceived) {
        // get id and amount
        Map<String, Object> sessionAttributes = inputReceived.getAttributesManager().getSessionAttributes();
        Number id = (Number) sessionAttributes.get(SAVE_STANDINGORDER_ID);
        String amount = (String) sessionAttributes.get(SAVE_AMOUNT);


        StandingOrder standingOrder = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, id);
        standingOrder.setAmount(Integer.parseInt(amount));
        AccountAPI.updateStandingOrder(ACCOUNT_NUMBER, standingOrder);

        String speechText = "Der Dauerauftrag Nummer " + id.toString() +
                " für " + standingOrder.getPayee() + " über " + amount + " euro wurde erfolgreich aktualisiert";
        return response(inputReceived, CARD_TITLE, speechText);
    }

}
