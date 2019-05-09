package amosalexa.services.financing;

import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.*;
import amosalexa.services.help.HelpService;
import api.banking.AccountAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.StandingOrder;
import model.banking.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service(
        functionGroup = HelpService.FunctionGroup.SMART_FINANCING,
        functionName = "Kontostandvorhersage",
        example = "Sag mir mein Kontostand vorraus",
        description = "Mit dieser Funktion kannst du dein Kontostand zu einem bestimmten Datum vorhersagen lassen. " +
                "Du kannst dabei den Parameter " +
                "Datum definieren."
)
public class AccountBalanceForecastService extends AbstractSpeechService implements SpeechService {

    private static final Logger log = LoggerFactory.getLogger(AccountBalanceForecastService.class);

    /**
     * intents
     */
    private final static String ACCOUNT_BALANCE_FORECAST_INTENT = "AccountBalanceForecast";
    private final static String PLAIN_DATE_INTENT = "PlainDate";


    /**
     * card
     */
    private final static String CARD = "Vorhersage deines Kontostandes";

    /**
     * speech texts
     */
    public final static String DATE_ASK = "Sag mir bitte, zu welchem Zeitpunkt möchtest du dein Kontostand erfahren";
    public final static String DATE_ERROR = "Ich konnte den Zeitpunkt nicht verstehen. Sag ein Datum an dem du dein Kontostand erfahren willst";
    public final static String DATE_PAST = "Der Zeitpunkt befindet sich in der Vergangenheit. Sag ein zuküngtiges Datum an dem du dein Kontostand erfahren willst";

    /**
     * slots
     */
    private final static String TARGET_DATE = "TargetDate";


    /**
     * session / intent
     */
    private static Session session;
    private static Intent intent;


    public AccountBalanceForecastService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Collections.singletonList(
                ACCOUNT_BALANCE_FORECAST_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                ACCOUNT_BALANCE_FORECAST_INTENT,
                PLAIN_DATE_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {

        intent = requestEnvelope.getRequest().getIntent();
        session = requestEnvelope.getSession();

        String futureDate = intent.getSlot(TARGET_DATE) != null ? intent.getSlot(TARGET_DATE).getValue() : null;

        if(DialogUtil.getDialogState("date?", session) != null){
            return getFutureAccountBalance(futureDate);
        }

        if(intent.getName().equals(PLAIN_DATE_INTENT) || futureDate != null){
            return getFutureAccountBalance(futureDate);
        }

        return getAskForDateResponse();
    }

    private SpeechletResponse getFutureAccountBalance(String futureDate) {

        log.info("Future Date: " + futureDate);

        int futureDayOfMonth = DateUtil.getDayOfMonth(futureDate);
        if(futureDayOfMonth == 0 || futureDate == null) return getAskResponse(CARD, DATE_ERROR);

        if(DateUtil.isPastDate(futureDate)) return getAskResponse(CARD, DATE_PAST);

        double balance = AccountAPI.getAccount(AccountData.ACCOUNT_DEFAULT).getBalance().doubleValue();
        double futureTransactionBalance = getFutureTransactionBalance(futureDate);
        double futureStandingOrderBalance = StandingOrder.getFutureStandingOrderBalance(AccountData.ACCOUNT_DEFAULT, futureDate);
        double futureBalance = balance + futureTransactionBalance + futureStandingOrderBalance;

        log.info("Future Balance: " + futureBalance);

        String balanceText = "Dein Kontostand beträgt vorraussichtlich am " + futureDate + " €" + NumberUtil.round(futureBalance, 2);

        return getResponse(CARD, balanceText);
    }

    private double getFutureTransactionBalance(String futureDate){
        List<Transaction> periodicTransactions = Transaction.getPeriodicTransactions(AccountData.ACCOUNT_DEFAULT);
        List<Transaction> futureDatePeriodicTransactions = Transaction.getTargetDatePeriodicTransactions(periodicTransactions, futureDate);
        return Transaction.getFutureTransactionBalance(futureDatePeriodicTransactions, futureDate);
    }

    private SpeechletResponse getAskForDateResponse() {
        DialogUtil.setDialogState("date?", session);
        return getAskResponse(CARD, DATE_ASK);
    }

}
