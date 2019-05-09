package amosalexa.handlers.financing;

import amosalexa.handlers.Service;
import amosalexa.services.DateUtil;
import amosalexa.services.DialogUtil;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SlotConfirmationStatus;
import com.amazon.ask.request.Predicates;
import model.banking.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;


@Service(
        functionGroup = Service.FunctionGroup.SMART_FINANCING,
        functionName = "Transaktionenvorhersage",
        example = "Welche Transaktionen erwarten mich?",
        description = "Mit dieser Funktion kannst du deine zukünftigen Transaktionen vorhersagen. "
                + "Du kannst dabei den Parameter Datum festlegen."
)
public class TransactionForecastServiceHandler implements IntentRequestHandler {
    private final static String TRANSACTION_FORECAST_INTENT = "TransactionForecast";

    private final static String SLOT_TARGET_DATE = "TargetDate";
    private final static String SLOT_NEXT_INDEX = "NextIndex";

    private final static String CARD_TITLE = "Vorhersage der Transaktionen";
    private static final int TRANSACTION_LIMIT = 3;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(TRANSACTION_FORECAST_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Map<String, Slot> slots = intentRequest.getIntent().getSlots();
        String futureDate = slots.get(SLOT_TARGET_DATE).getValue();
        Slot nextIndex = slots.get(SLOT_NEXT_INDEX);
        if (DateUtil.isPastDate(futureDate))
            return response(input, CARD_TITLE, "Bitte nenne mir ein Datum in der Zukunft");

        List<Transaction> periodicTransactions = Transaction.getPeriodicTransactions(ACCOUNT_NUMBER);
        List<Transaction> futureDatePeriodicTransactions = Transaction.getTargetDatePeriodicTransactions(periodicTransactions, futureDate);

        int counter = futureDatePeriodicTransactions.size();
        if (counter == 0)
            return response(input, CARD_TITLE, "Ich konnte keine Transaktionen finden, die noch ausgeführt werden");

        double futureDateTransactionBalance = Transaction.getFutureTransactionBalance(futureDatePeriodicTransactions, futureDate);

        if (nextIndex.getValue() == null) {
            // first time
            StringBuilder builder = new StringBuilder();
            builder.append("Ich habe ").append(counter).append(" ").append(DialogUtil.getTransactionNumerus(counter))
                    .append(" gefunden, die noch bis zum ").append(futureDate).append(" ausgeführt werden. ")
                    .append("Insgesamt werden noch ").append(Math.abs(futureDateTransactionBalance)).append(" ")
                    .append(DialogUtil.getBalanceVerb(futureDateTransactionBalance)).append(".");

            for (int i = 0; i < TRANSACTION_LIMIT; i++) {
                if (i < futureDatePeriodicTransactions.size()) {
                    builder.append(Transaction.getTransactionText(futureDatePeriodicTransactions.get(i))).append(" .");
                }
            }
            if (futureDatePeriodicTransactions.size() > TRANSACTION_LIMIT) {
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
            StringBuilder builder = new StringBuilder(Transaction.getTransactionText(futureDatePeriodicTransactions.get(position)));
            position++;
            if (position < futureDatePeriodicTransactions.size()) {
                builder.append("Moechtest du einen weiteren Eintrag hoeren?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(position))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                builder.append("Das war");
                return response(input, CARD_TITLE, builder.toString());
            }
        } else {
            return response(input, CARD_TITLE);
        }

    }
}
