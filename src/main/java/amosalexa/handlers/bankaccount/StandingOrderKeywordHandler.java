package amosalexa.handlers.bankaccount;

import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;

public class StandingOrderKeywordHandler implements IntentRequestHandler {
    private static final String STANDING_ORDERS_KEYWORD_INTENT = "StandingOrdersKeywordIntent";
    private static final String SLOT_NEXT_INDEX = "NextIndex";
    private static final String SLOT_STANDINGORDER_KEYWORD = "StandingOrderKeyword";
    private static final String SLOT_TRANSACTION_KEYWORD = "TransactionKeyword";

    private static final String CARD_TITLE = "Dauerauftrag nach Stickwort";
    private static final String TEXT_NEXT_STANDINGORDER = "Moechtest du einen weiteren Eintrag hoeren?";

    private ArrayList<StandingOrder> standingOrderCollection;


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(STANDING_ORDERS_KEYWORD_INTENT))
                || isNumberIntentForPass(input, STANDING_ORDERS_KEYWORD_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;

        Intent intent = getRealIntent(input, intentRequest);
        Slot nextIndex = intent.getSlots().get(SLOT_NEXT_INDEX);
        if (nextIndex.getValue() == null) {
            return getStandingOrdersKeyword(intent, input);
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            return nextStandingOrder(Integer.parseInt(nextIndex.getValue()), intent, input);
        } else {
            return response(input, CARD_TITLE);
        }
    }

    private Optional<Response> getStandingOrdersKeyword(Intent intent, HandlerInput inputReceived) {
        String standingOrderKeywordSlot = intent.getSlots().get(SLOT_STANDINGORDER_KEYWORD).getValue();
        String transactionKeywordSlot = intent.getSlots().get(SLOT_TRANSACTION_KEYWORD).getValue();
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
        intent.getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue("0")
                .withName(SLOT_NEXT_INDEX).build());
        return responseWithSlotConfirm(inputReceived, CARD_TITLE, builder.toString(), intent, SLOT_NEXT_INDEX);
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
}
