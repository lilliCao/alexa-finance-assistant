package amosalexa.services.financing;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.DateUtil;
import amosalexa.services.DialogUtil;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service(
        functionGroup = HelpService.FunctionGroup.SMART_FINANCING,
        functionName = "Transaktionenvorhersage",
        example = "Welche Transaktionen erwarten mich?",
        description = "Mit dieser Funktion kannst du deine zukünftigen Transaktionen vorhersagen. " +
                "Du kannst dabei den Parameter " +
                "Datum definieren."
)
public class TransactionForecastService extends AbstractSpeechService implements SpeechService {

    private static final Logger log = LoggerFactory.getLogger(TransactionForecastService.class);

    /**
     * intents
     */
    private final static String TRANSACTION_FORECAST_INTENT = "TransactionForecast";
    private final static String PLAIN_DATE_INTENT = "PlainDate";

    /**
     * card
     */
    private final static String CARD = "Vorhersage deiner Transaktionen";

    /**
     * speech texts
     */
    public final static String DATE_ASK = "Sag mir bitte, bis wann möchtest du deine zukünftigen Transaktionen hören?";
    public final static String DATE_ERROR = "Ich konnte den Zeitpunkt nicht verstehen. Sag ein Datum bis wann du zukünftige Transaktionen hören willst";
    public final static String NO_TRANSACTION_INFO = "Ich konnte keine Transaktionen finden, die noch ausgeführt werden";
    public final static String DATE_PAST = "Der Zeitpunkt befindet sich in der Vergangenheit. Sag ein Datum bis wann du zukünftige Transaktionen hören willst";

    public final static String BYE = "OK, Tschüss";

    /**
     * slots
     */
    private final static String TARGET_DATE = "TargetDate";


    /**
     * session / intent
     */
    private static Session session;
    private static Intent intent;

    private static List<Transaction> futureDatePeriodicTransactions;

    public TransactionForecastService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Collections.singletonList(
                TRANSACTION_FORECAST_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                TRANSACTION_FORECAST_INTENT,
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

        String intentName = intent.getName();
        String futureDate = intent.getSlot(TARGET_DATE) != null ? intent.getSlot(TARGET_DATE).getValue() : null;

        if (DialogUtil.getDialogState("date?", session) != null) {
            return getFutureTransactionsResponse(futureDate);
        }

        if (DialogUtil.getDialogState("transaction?", session) != null ||
                DialogUtil.getDialogState("more?", session) != null ) {
            if(intentName.equals(YES_INTENT)){
                return getFutureTransactionListResponse();
            }
            if(intentName.equals(NO_INTENT)){
                return getGoodByeResponse();
            }
        }
        return askForPeriodicTimeResponse();
    }

    private SpeechletResponse getGoodByeResponse() {
        return getResponse(CARD, BYE);
    }

    private SpeechletResponse getFutureTransactionListResponse() {
        StringBuilder futureTransactionText = new StringBuilder();
        int size = futureDatePeriodicTransactions.size();
        Object indexAttribute = session.getAttribute("transactionIndex");
        int i = indexAttribute == null ? 0 : (int) indexAttribute;
        int limit = i + 3; // respond every time 3 transactions

        while(i < limit && i < size){
            futureTransactionText.append(Transaction.getTransactionText(futureDatePeriodicTransactions.get(i))).append(" ");
            i++;
        }
        session.setAttribute("transactionIndex", i);

        if(i < size){ //
            DialogUtil.setDialogState("more?", session);
            return getSSMLAskResponse(CARD, futureTransactionText.toString() + ". Möchtest du weitere hören");
        }

        return getSSMLResponse(CARD, futureTransactionText.toString() + ". Das waren alle zukünftigen Transaktionen");
    }

    private SpeechletResponse askForPeriodicTimeResponse() {
        DialogUtil.setDialogState("date?", session);
        return getAskResponse(CARD, DATE_ASK);
    }

    private SpeechletResponse getFutureTransactionsResponse(String futureDate) {

        log.info("Date: " + futureDate);
        int futureDayOfMonth = DateUtil.getDayOfMonth(futureDate);

        if(futureDayOfMonth == 0 || futureDate == null) return getAskResponse(CARD, DATE_ERROR);

        if(DateUtil.isPastDate(futureDate)) return getAskResponse(CARD, DATE_PAST);

        List<Transaction> periodicTransactions = Transaction.getPeriodicTransactions(AmosAlexaSpeechlet.ACCOUNT_ID);
        log.info("Periodic transactions = "+periodicTransactions.size());
        futureDatePeriodicTransactions = Transaction.getTargetDatePeriodicTransactions(periodicTransactions, futureDate);

        int counter = futureDatePeriodicTransactions.size();
        if(counter == 0) return getResponse(CARD, NO_TRANSACTION_INFO);

        double futureDateTransactionBalance = Transaction.getFutureTransactionBalance(futureDatePeriodicTransactions, futureDate);

        String futureTransactionText = "Ich habe " + counter + " " +
                DialogUtil.getTransactionNumerus(counter) +
                " gefunden, die noch bis zum " +
                futureDate + " ausgeführt werden. " +
                "Insgesamt werden noch €" + Math.abs(futureDateTransactionBalance)+
                " " + DialogUtil.getBalanceVerb(futureDateTransactionBalance) + ". " +
                "Soll ich diese Transaktionen auflisten?";


        log.info(futureTransactionText);

        DialogUtil.setDialogState("transaction?", session);
        return getAskResponse(CARD, futureTransactionText);
    }
}
