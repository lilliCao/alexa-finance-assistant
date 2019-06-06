package amosalexa.handlers.bankaccount;

import amosalexa.handlers.Service;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.Account;
import model.banking.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.PasswordResponseHelper.*;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountInformationIntentHandler.class);


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(ACCOUNT_INFORMATION_INTENT))
                || isNumberIntentForPass(input, ACCOUNT_INFORMATION_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        LOGGER.info("in AccountInformationIntent");
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;

        Intent realIntent = getRealIntent(input, intentRequest);
        // TODO report bug: weird unsolved resolution from alexa caused NullPointerException
        String slotValue = realIntent.getSlots().get(SLOT_NEXT_INDEX).getValue() == null
                ? realIntent.getSlots().get(SLOT_ACCOUNT_KEYWORD)
                .getResolutions().getResolutionsPerAuthority().get(0)
                .getValues().get(0)
                .getValue().getName() : SLOT_VALUE_TRANSAKTIONEN;

        LOGGER.info("start handling request");
        if (slotValue.equalsIgnoreCase(SLOT_VALUE_TRANSAKTIONEN)) {
            //transactions
            return handleTransaction(input, realIntent);
        } else {
            // other account information
            Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
            account.setSpeechTexts();
            return response(input, CARD_TITLE, account.getSpeechTexts().get(slotValue.toLowerCase()));
        }

    }

    private Optional<Response> handleTransaction(HandlerInput input, Intent intent) {
        LOGGER.info("Handle transaction = " + intent.toString());
        Slot nextIndex = intent.getSlots().get(SLOT_NEXT_INDEX);
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
                intent.getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(TRANSAKTIONEN_LIMIT))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intent, SLOT_NEXT_INDEX);
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
                intent.getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(position))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intent, SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else {
            return response(input, CARD_TITLE);
        }
    }

}
