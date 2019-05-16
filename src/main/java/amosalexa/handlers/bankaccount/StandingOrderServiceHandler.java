package amosalexa.handlers.bankaccount;

import amosalexa.handlers.Service;
import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;
import model.db.Contact;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.*;

@Service(
        functionName = "Daueraufträge verwalten",
        functionGroup = Service.FunctionGroup.ONLINE_BANKING,
        example = "Wie lauten meine Daueraufträge?",
        description = "Diese Funktion ermöglicht dir das Verwalten von Daueraufträgen."
)
public class StandingOrderServiceHandler implements IntentRequestHandler {
    private static final String STANDING_ORDERS_INFO_INTENT = "StandingOrdersInfoIntent";
    private static final String STANDING_ORDERS_DELETE_INTENT = "StandingOrdersDeleteIntent";
    private static final String STANDING_ORDERS_MODIFY_INTENT = "StandingOrdersModifyIntent";
    private static final String STANDING_ORDERS_KEYWORD_INTENT = "StandingOrdersKeywordIntent";
    private static final String STANDING_ORDERS_SMART_INTENT = "StandingOrderSmartIntent";

    private static final String SLOT_CHANNEL = "Channel";
    private static final String SLOT_VALUE_EMAIL = "Email";
    private static final String SLOT_PAYEE = "Payee";
    private static final String SLOT_NEXT_INDEX = "NextIndex";
    private static final String SLOT_NUMBER = "Number";
    private static final String SLOT_AMOUNT = "Amount";
    private static final String SLOT_EXECUTIONRATE = "ExecutionRate";
    private static final String SLOT_FIRSTEXECUTION = "FirstExecution";
    private static final String SLOT_STANDINGORDER_KEYWORD = "StandingOrderKeyword";
    private static final String SLOT_TRANSACTION_KEYWORD = "TransactionKeyword";
    private static final String SLOT_ORDER_AMOUNT = "orderAmount";

    private static final String CARD_TITLE = "Daueraufträge";
    private static final String TEXT_NEXT_STANDINGORDER = "Moechtest du einen weiteren Eintrag hoeren?";
    private static final String SAVE_STANDINGORDER_ID = "save_standingorder_id";
    private static final String SAVE_AMOUNT = "save_amount";
    private static ArrayList<StandingOrder> standingOrderCollection;
    private static HandlerInput inputReceived;
    private static IntentRequest intentRequestReceived;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(STANDING_ORDERS_INFO_INTENT))
                || input.matches(Predicates.intentName(STANDING_ORDERS_DELETE_INTENT))
                || input.matches(Predicates.intentName(STANDING_ORDERS_MODIFY_INTENT))
                || input.matches(Predicates.intentName(STANDING_ORDERS_KEYWORD_INTENT))
                || input.matches(Predicates.intentName(STANDING_ORDERS_SMART_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        inputReceived = input;
        intentRequestReceived = intentRequest;
        switch (intentRequest.getIntent().getName()) {
            case STANDING_ORDERS_INFO_INTENT:
                Slot nextIndex = intentRequestReceived.getIntent().getSlots().get(SLOT_NEXT_INDEX);
                if (nextIndex.getValue() == null) {
                    return getStandingOrdersInfo();
                } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
                    return nextStandingOrder(Integer.parseInt(nextIndex.getValue()));
                } else {
                    return response(input, CARD_TITLE);
                }
            case STANDING_ORDERS_DELETE_INTENT:
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return deleteStandingOrder();
                } else {
                    return response(input, CARD_TITLE);
                }
            case STANDING_ORDERS_MODIFY_INTENT:
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return modifyStandingOrder();
                } else {
                    return response(input, CARD_TITLE);
                }
            case STANDING_ORDERS_KEYWORD_INTENT:
                Slot nextEntry = intentRequestReceived.getIntent().getSlots().get(SLOT_NEXT_INDEX);
                if (nextEntry.getValue() == null) {
                    return getStandingOrdersKeyword();
                } else if (nextEntry.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
                    return nextStandingOrder(Integer.parseInt(nextEntry.getValue()));
                } else {
                    return response(input, CARD_TITLE);
                }
            default:
                //STANDING_ORDERS_SMART_INTENT:
                switch (intentRequestReceived.getIntent().getConfirmationStatus()) {
                    case NONE:
                        return getStandingOrdersSmart();
                    case CONFIRMED:
                        return updateStandingOrdersSmart();
                    default:
                        return response(input, CARD_TITLE);
                }

        }
    }

    private Optional<Response> updateStandingOrdersSmart() {
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

    private Optional<Response> getStandingOrdersSmart() {
        Map<String, Slot> slots = intentRequestReceived.getIntent().getSlots();
        standingOrderCollection = new ArrayList<>(AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER));

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
                        speech, intentRequestReceived.getIntent());
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
                String speech = "Ich habe den neuen Dauerauftrag für" + payee
                        + " über " + amount + " Euro erfolgreich eingerichtet";
                return response(inputReceived, CARD_TITLE, speech);
            }
        }

        // Contact not found
        return response(inputReceived, CARD_TITLE,
                "Ich habe " + payee + " in deiner Kontaktliste nicht gefunden. " +
                        "Du musst ihm erst in der Kontaktliste hinzufügen");
    }


    private Optional<Response> getStandingOrdersKeyword() {
        String standingOrderKeywordSlot = intentRequestReceived.getIntent().getSlots().get(SLOT_STANDINGORDER_KEYWORD).getValue();
        String transactionKeywordSlot = intentRequestReceived.getIntent().getSlots().get(SLOT_TRANSACTION_KEYWORD).getValue();
        String keyword = transactionKeywordSlot;
        boolean isStandingOrderKeywordSlot = false;
        if (standingOrderKeywordSlot != null) {
            keyword = standingOrderKeywordSlot;
            isStandingOrderKeywordSlot = true;
        }
        //get standingorders with keyword
        Collection<StandingOrder> listStandingOrder = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);
        standingOrderCollection = new ArrayList<>();
        for (StandingOrder so : listStandingOrder) {
            String description = so.getDescription().toLowerCase();
            if (!description.equalsIgnoreCase(keyword) && description.contains(keyword)) {
                String[] singleWords = description.split("\\s+");
                for (String s : singleWords) {
                    double similarity = getStringSimilarity(s, keyword);
                    if (similarity >= 0.9) {
                        standingOrderCollection.add(so);
                        continue;
                    }
                }
            } else {
                standingOrderCollection.add(so);
                continue;
            }
        }
        if (standingOrderCollection.size() == 0) {
            String answer = "Ich konnte keine Dauerauftraege finden, die zu diesem Stichwort passen.";
            return response(inputReceived, CARD_TITLE, answer);
        }

        double total = 0;
        for (StandingOrder so : standingOrderCollection) {
            if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.MONTHLY)) {
                total += so.getAmount().doubleValue();
            } else if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.QUARTERLY)) {
                total += so.getAmount().doubleValue() / 3;
            } else if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.HALF_YEARLY)) {
                total += so.getAmount().doubleValue() / 6;
            } else if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.YEARLY)) {
                total += so.getAmount().doubleValue() / 12;
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Aus ")
                .append(standingOrderCollection.size() == 1 ?
                        "<phoneme alphabet=\"ipa\" ph=\"ˈaɪ̯nəm\">einem</phoneme> gefundenen Dauerauftrag "
                        : standingOrderCollection.size() + " gefundenen Dauerauftraegen ")
                .append("konnte ich berechnen, dass du monatlich " + String.format("%.2f", total) + " Euro ")
                .append(isStandingOrderKeywordSlot ? "fuer " + keyword + " bezahlst. " : "für " + keyword + " hast");
        builder.append("Soll ich die gefundenen Dauerauftraege aufzaehlen? ");
        intentRequestReceived.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue("0")
                .withName(SLOT_NEXT_INDEX).build());
        return responseWithSlotConfirm(inputReceived, CARD_TITLE, builder.toString(), intentRequestReceived.getIntent(), SLOT_NEXT_INDEX);
    }

    private static double getStringSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
    }

    private Optional<Response> modifyStandingOrder() {
        Map<String, Slot> slots = intentRequestReceived.getIntent().getSlots();
        int numberSlot = Integer.parseInt(slots.get(SLOT_NUMBER).getValue());
        String amountSlot = slots.get(SLOT_AMOUNT).getValue();
        String executionRateSlot = slots.get(SLOT_EXECUTIONRATE).getValue();
        String firstExecutionSlot = slots.get(SLOT_FIRSTEXECUTION).getValue();

        StandingOrder standingOrder = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, numberSlot);
        if (amountSlot != null) {
            standingOrder.setAmount(Integer.parseInt(amountSlot));
        }
        if (executionRateSlot != null) {
            standingOrder.setExecutionRateFromString(executionRateSlot);
        }
        if (firstExecutionSlot != null) {
            standingOrder.setFirstExecution(firstExecutionSlot);
        }

        String speech;
        if (AccountAPI.updateStandingOrder(ACCOUNT_NUMBER, standingOrder)) {
            speech = "Dauerauftrag Nummer " + numberSlot + " wurde geändert.";
        } else {
            speech = "Etwas ist schiefgelaufen. Bitte versuch noch mal";
        }

        return response(inputReceived, CARD_TITLE, speech);
    }

    private Optional<Response> deleteStandingOrder() {
        String number = intentRequestReceived.getIntent().getSlots().get(SLOT_NUMBER).getValue();
        if (!AccountAPI.deleteStandingOrder(ACCOUNT_NUMBER, Integer.parseInt(number))) {
            return response(inputReceived, CARD_TITLE,
                    "Dauerauftrag Nummer " + number + " wurde nicht gefunden.");
        } else {
            return response(inputReceived, CARD_TITLE,
                    "Dauerauftrag Nummer " + number + " wurde gelöscht.");
        }
    }

    private Optional<Response> nextStandingOrder(int nextIndex) {
        StringBuilder builder = new StringBuilder();
        builder.append(standingOrderCollection.get(nextIndex).getSpeechOutput());
        nextIndex++;
        if (nextIndex < standingOrderCollection.size()) {
            intentRequestReceived.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(nextIndex))
                    .withName(SLOT_NEXT_INDEX).build());
            builder.append(TEXT_NEXT_STANDINGORDER);
            return responseWithSlotConfirm(inputReceived, CARD_TITLE, builder.toString(), intentRequestReceived.getIntent(), SLOT_NEXT_INDEX);
        } else {
            return response(inputReceived, CARD_TITLE, builder.toString());
        }
    }

    private Optional<Response> getStandingOrdersInfo() {
        Map<String, Slot> slots = intentRequestReceived.getIntent().getSlots();
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
            intentRequestReceived.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue("1")
                    .withName(SLOT_NEXT_INDEX).build());
            return responseWithSlotConfirm(inputReceived, CARD_TITLE, builder.toString(), intentRequestReceived.getIntent(), SLOT_NEXT_INDEX);
        }
    }

}
