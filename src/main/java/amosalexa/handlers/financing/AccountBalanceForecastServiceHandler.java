package amosalexa.handlers.financing;

import amosalexa.handlers.Service;
import amosalexa.services.DateUtil;
import amosalexa.services.NumberUtil;
import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;
import model.banking.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.ResponseHelper.response;

@Service(
        functionGroup = Service.FunctionGroup.SMART_FINANCING,
        functionName = "Kontostandvorhersage",
        example = "Sag mir mein Kontostand vorraus",
        description = "Mit dieser Funktion kannst du dein Kontostand zu einem bestimmten Datum vorhersagen lassen. "
                + "Du kannst dabei den Parameter Datum festlegen."
)
public class AccountBalanceForecastServiceHandler implements IntentRequestHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceForecastServiceHandler.class);

    private final static String ACCOUNT_BALANCE_FORECAST_INTENT = "AccountBalanceForecast";

    private final static String SLOT_TARGET_DATE = "TargetDate";

    private final static String CARD_TITLE = "Vorhersage deines Kontostandes";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(ACCOUNT_BALANCE_FORECAST_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        String futureDate = intentRequest.getIntent().getSlots().get(SLOT_TARGET_DATE).getValue();
        if (DateUtil.isPastDate(futureDate))
            return response(input, CARD_TITLE, "Bitte nenne mir ein Datum in der Zukunft");

        double balance = AccountAPI.getAccount(ACCOUNT_NUMBER).getBalance().doubleValue();
        List<Transaction> periodicTransactions = Transaction.getPeriodicTransactions(ACCOUNT_NUMBER);
        List<Transaction> futureDatePeriodicTransactions = Transaction.getTargetDatePeriodicTransactions(periodicTransactions, futureDate);
        double futureTransactionBalance = Transaction.getFutureTransactionBalance(futureDatePeriodicTransactions, futureDate);
        double futureStandingOrderBalance = StandingOrder.getFutureStandingOrderBalance(ACCOUNT_NUMBER, futureDate);
        LOGGER.info(String.format("balance=%f, standingorder=%f, futureTrans=%s", balance, futureStandingOrderBalance, futureTransactionBalance));

        double futureBalance = balance + futureTransactionBalance + futureStandingOrderBalance;

        String balanceText = "Dein Kontostand beträgt vorraussichtlich am " + futureDate + " €" + NumberUtil.round(futureBalance, 2);
        return response(input, CARD_TITLE, balanceText);
    }
}
