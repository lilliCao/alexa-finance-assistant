package amosalexa.handlers.bankaccount;

import amosalexa.handlers.Service;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SlotConfirmationStatus;
import com.amazon.ask.request.Predicates;
import model.banking.Account;
import model.banking.Transaction;

import java.util.List;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;

@Service(
        functionName = "Kontoinformation",
        functionGroup = Service.FunctionGroup.ACCOUNT_INFORMATION,
        example = "Wie ist mein Kontostand? ",
        description = "Mit dieser Funktion kannst du Informationen über dein Konto abfragen. Du kannst dabei Parameter wie " +
                "Kontostand, Kreditlimit, Überweisungen, IBAN und Abhebegebühr angeben."
)
public class AccountInformationIntentHandler implements IntentRequestHandler {
    public static final String ACCOUNT_INFORMATION_INTENT = "AccountInformationIntent";
    public static final String SLOT_ACCOUNT_KEYWORD = "AccountKeyword";
    public static final String SLOT_NEXT_INDEX = "NextIndex";
    public static final String SLOT_VALUE_TRANSAKTIONEN = "Transaktionen";
    private static final String CARD_TITLE = "Kontoinformation";
    public static final int TRANSAKTIONEN_LIMIT = 3;


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(ACCOUNT_INFORMATION_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        String slotValue = intentRequest.getIntent().getSlots().get(SLOT_ACCOUNT_KEYWORD)
                .getResolutions().getResolutionsPerAuthority().get(0)
                .getValues().get(0)
                .getValue().getName();

        //transactions
        if (slotValue.equalsIgnoreCase(SLOT_VALUE_TRANSAKTIONEN)) {
            return handleTransaction(input, intentRequest);

            // other account information
        } else {
            Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
            account.setSpeechTexts();
            return response(input, CARD_TITLE, account.getSpeechTexts().get(slotValue.toLowerCase()));
        }
    }

    private Optional<Response> handleTransaction(HandlerInput input, IntentRequest intentRequest) {
        Slot nextIndex = intentRequest.getIntent().getSlots().get(SLOT_NEXT_INDEX);
        String speechText;
        List<Transaction> transactions = TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER);

        if (transactions == null || transactions.isEmpty()) {
            speechText = "Du hast keine Transaktionen in deinem Konto";
            return response(input, CARD_TITLE, speechText);
        } else if (nextIndex.getValue() == null) {
            //read transactions by limit for the first time
            StringBuilder builder = new StringBuilder(Transaction.getTransactionSizeText(transactions.size()));
            for (int i = 0; i < TRANSAKTIONEN_LIMIT; i++) {
                if (i < transactions.size()) {
                    builder.append(Transaction.getTransactionText(transactions.get(i)));
                }
            }
            if (transactions.size() > TRANSAKTIONEN_LIMIT) {
                builder.append(Transaction.getAskMoreTransactionText());
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(TRANSAKTIONEN_LIMIT))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            //read more if confirmed
            int position = Integer.parseInt(nextIndex.getValue());
            StringBuilder builder = new StringBuilder(Transaction.getTransactionText(transactions.get(position)));
            position++;
            if (position < transactions.size()) {
                builder.append(Transaction.getAskMoreTransactionText());
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(position))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else {
            return response(input, CARD_TITLE);
        }
    }

}
