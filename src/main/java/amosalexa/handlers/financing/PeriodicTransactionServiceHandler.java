package amosalexa.handlers.financing;

import amosalexa.handlers.Service;
import api.banking.TransactionAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.Transaction;
import model.db.TransactionDB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;

@Service(
        functionGroup = Service.FunctionGroup.SMART_FINANCING,
        functionName = "Periodische Transaktionen verwalten",
        example = "Wie sind meine periodischen Transaktionen?",
        description = "Mit dieser Funktion kannst du deine periodische Transaktionen verwalten. " +
                "Somit hast du eine bessere Überblick von deinem Einkommen"
)
public class PeriodicTransactionServiceHandler implements IntentRequestHandler {
    private static final String PERIODIC_TRANSACTION_LIST_INTENT = "PeriodicTransactionListIntent";
    private static final String PERIODIC_TRANSACTION_ADD_INTENT = "PeriodicTransactionAddIntent";
    private static final String PERIODIC_TRANSACTION_DELETE_INTENT = "PeriodicTransactionDeleteIntent";

    private static final String SLOT_TRANSACTION_NUMBER = "TransactionNumber";
    private static final String SLOT_NEXT_INDEX = "NextIndex";

    private static final String CARD_TITLE = "Periodische Transaktionen";
    private static final int TRANSACTION_LIMIT = 3;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(PERIODIC_TRANSACTION_ADD_INTENT))
                || input.matches(Predicates.intentName(PERIODIC_TRANSACTION_DELETE_INTENT))
                || input.matches(Predicates.intentName(PERIODIC_TRANSACTION_LIST_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        switch (intentRequest.getIntent().getName()) {
            case PERIODIC_TRANSACTION_LIST_INTENT:
                return getPeriodicTransaction(input, intentRequest);
            case PERIODIC_TRANSACTION_ADD_INTENT:
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return markPeriodicTransaction(input, intentRequest);
                }
                return response(input, CARD_TITLE);
            default:
                //DELETE
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return removePeriodicTransaction(input, intentRequest);
                }
                return response(input, CARD_TITLE);
        }

    }

    private Optional<Response> removePeriodicTransaction(HandlerInput input, IntentRequest intentRequest) {
        String transactionId = intentRequest.getIntent().getSlots().get(SLOT_TRANSACTION_NUMBER).getValue();
        TransactionDB transactionDb = (TransactionDB) dynamoDbMapper.load(TransactionDB.class, transactionId);
        if (transactionDb == null) {
            return response(input, CARD_TITLE, "Ich kann Transaktion Nummer " + transactionId + " nicht finden." +
                    " Bitte aendere deine Eingabe.");
        }

        dynamoDbMapper.delete(transactionDb);
        return response(input, CARD_TITLE, "Transaktion Nummer " + transactionId + " ist nun nicht mehr als periodisch markiert.");
    }

    private Optional<Response> markPeriodicTransaction(HandlerInput input, IntentRequest intentRequest) {
        String transactionId = intentRequest.getIntent().getSlots().get(SLOT_TRANSACTION_NUMBER).getValue();
        Transaction transaction = TransactionAPI.getTransactionForAccount(ACCOUNT_NUMBER, transactionId);
        if (transaction == null) {
            return response(input, CARD_TITLE, "Ich kann Transaktion Nummer " + transactionId + " nicht finden." +
                    " Bitte aendere deine Eingabe.");
        }
        TransactionDB transactionDb = (TransactionDB) dynamoDbMapper.load(TransactionDB.class, transactionId);
        if (transactionDb == null) {
            transactionDb = new TransactionDB(transactionId);
        }
        transactionDb.setPeriodic(true);
        transactionDb.setAccountNumber(ACCOUNT_NUMBER);
        dynamoDbMapper.save(transactionDb);
        return response(input, CARD_TITLE, "Transaktion Nummer " + transactionId + " wurde als periodisch markiert.");
    }


    private Optional<Response> getPeriodicTransaction(HandlerInput input, IntentRequest intentRequest) {
        Slot nextIndex = intentRequest.getIntent().getSlots().get(SLOT_NEXT_INDEX);
        List<TransactionDB> allTransactionsDb = dynamoDbMapper.loadAll(TransactionDB.class);
        List<TransactionDB> transactionsDb = new ArrayList<>();
        for (TransactionDB transactionDb : allTransactionsDb) {
            if (transactionDb.getAccountNumber().equals(ACCOUNT_NUMBER) && transactionDb.isPeriodic())
                transactionsDb.add(transactionDb);
        }

        if (transactionsDb.isEmpty()) {
            return response(input, CARD_TITLE, "Keine periodische Transaktionen vorhanden");
        }

        if (nextIndex.getValue() == null) {
            // first
            StringBuilder builder = new StringBuilder();
            String sizeText = transactionsDb.size() == 1 ? " 1 periodische Transaktion" : transactionsDb.size() + " Transaktionen";
            builder.append("Ich habe ").append(sizeText)
                    .append(" gefunden.");

            for (int i = 0; i < TRANSACTION_LIMIT; i++) {
                if (i < transactionsDb.size()) {
                    Transaction transaction = TransactionAPI.getTransactionForAccount(ACCOUNT_NUMBER, transactionsDb.get(i).getTransactionId());
                    builder.append(Transaction.getTransactionText(transaction));
                }
            }
            if (transactionsDb.size() > TRANSACTION_LIMIT) {
                builder.append("Moechtest du einen weiteren Eintrag hoeren?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(TRANSACTION_LIMIT))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            // read more if confirmed
            int position = Integer.parseInt(nextIndex.getValue());
            Transaction transaction = TransactionAPI.getTransactionForAccount(ACCOUNT_NUMBER, transactionsDb.get(position).getTransactionId());
            StringBuilder builder = new StringBuilder(Transaction.getTransactionText(transaction));
            position++;
            if (position < transactionsDb.size()) {
                builder.append("Moechtest du einen weiteren Eintrag hoeren?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(position))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                builder.append("Das war.");
                return response(input, CARD_TITLE, builder.toString());
            }
        } else {
            return response(input, CARD_TITLE);
        }
    }
}
