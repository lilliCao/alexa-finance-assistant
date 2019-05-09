package amosalexa.services.securitiesAccount;

import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import api.banking.SecuritiesAccountAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import model.banking.SecuritiesAccount;
import model.banking.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SecuritiesAccountInformationService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                SECURITIES_ACCOUNT_INFORMATION_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                SECURITIES_ACCOUNT_INFORMATION_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    /**
     * Default value for cards
     */
    private static final String SECURITIES_ACCOUNT_INFORMATION = "Depotstatus";

    // FIXME: Hardcoded SecuritiesAccount Id
    private static final Number SEC_ACCOUNT_ID = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritiesAccountInformationService.class);

    private static final String SECURITIES_ACCOUNT_INFORMATION_INTENT = "SecuritiesAccountInformationIntent";

    private List<Security> securities;

    public SecuritiesAccountInformationService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    /**
     * ties the Speechlet Subject (Amos Alexa Speechlet) with an Speechlet Observer
     *
     * @param speechletSubject service
     */
    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();
        String intentName = request.getIntent().getName();
        LOGGER.info("Intent Name: " + intentName);
        String context = (String) session.getAttribute(DIALOG_CONTEXT);

        if (SECURITIES_ACCOUNT_INFORMATION_INTENT.equals(intentName)) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, intentName);
            return getSecuritiesAccountInformation(session);
        }
        if (YES_INTENT.equals(intentName) && context != null && context.equals(SECURITIES_ACCOUNT_INFORMATION_INTENT)) {
            return getNextSecuritiesAccountInformation(request.getIntent(), session);
        } else if (NO_INTENT.equals(intentName) && context != null && context.equals(SECURITIES_ACCOUNT_INFORMATION_INTENT)) {
            return getResponse(SECURITIES_ACCOUNT_INFORMATION, "Okay, tschuess!");
        } else {
            throw new SpeechletException("Unhandled intent: " + intentName);
        }
    }

    private SpeechletResponse getSecuritiesAccountInformation(Session session) {
        LOGGER.info("SecuritiesAccountInformation called.");
        SecuritiesAccount securitiesAccount = SecuritiesAccountAPI.getSecuritiesAccount(SEC_ACCOUNT_ID);

        Collection<Security> securitiesCollection = SecuritiesAccountAPI.getSecuritiesForAccount(SEC_ACCOUNT_ID);

        if (securitiesAccount == null || securitiesCollection == null || securitiesCollection.size() == 0) {
            return getResponse(SECURITIES_ACCOUNT_INFORMATION, "Keine Depotinformationen vorhanden.");
        }

        securities = new LinkedList<>(securitiesCollection);

        StringBuilder textBuilder = new StringBuilder();

        textBuilder.append("Du hast momentan ")
                .append(securities.size() == 1 ? "ein Wertpapier in deinem Depot. " : securities.size()
                        + " Wertpapiere in deinem Depot. ");

        for (int i = 0; i <= 1; i++) {
            String stockPrice = FinanceApi.getStockPrice(securities.get(i));
            LOGGER.info("Stock Price: " + stockPrice);
            textBuilder.append("Wertpapier Nummer ")
                    .append(securities.get(i).getSecurityId())
                    .append(": ").append(securities.get(i).getDescription()).
                    append(", " + securities.get(i).getQuantity() + " Stueck, ").
                    append("mit einem momentanen Wert von ").append(stockPrice).append(" Euro. ");
        }

        if (securities.size() > 2) {
            textBuilder.append("Moechtest du einen weiteren Eintrag hoeren?");
            String text = textBuilder.toString();
            // Save current list offset in this session
            session.setAttribute("NextSecurity", 2);
            return getAskResponse(SECURITIES_ACCOUNT_INFORMATION, text);
        }

        String text = textBuilder.toString();
        return getResponse(SECURITIES_ACCOUNT_INFORMATION, text);
    }

    private SpeechletResponse getNextSecuritiesAccountInformation(Intent intent, Session session) {
        int nextEntry = (int) session.getAttribute("NextSecurity");
        Security nextSecurity;
        StringBuilder textBuilder = new StringBuilder();

        if (nextEntry < securities.size()) {
            String stockPrice = FinanceApi.getStockPrice(securities.get(nextEntry));
            nextSecurity = securities.get(nextEntry);
            textBuilder.append("Wertpapier Nummer ")
                    .append(nextSecurity.getSecurityId())
                    .append(": ").append(nextSecurity.getDescription()).
                    append(", " + nextSecurity.getQuantity() + " Stueck, ").
                    append("mit einem momentanen Wert von ").append(stockPrice).append(" Euro. ");

            if (nextEntry == (securities.size() - 1)) {
                textBuilder.append("Das waren alle Wertpapiere in deinem Depot.");
                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
                speech.setText(textBuilder.toString());
                return SpeechletResponse.newTellResponse(speech);
            } else {
                textBuilder.append("Moechtest du einen weiteren Eintrag hoeren?");
            }

            // Save current list offset in this session
            session.setAttribute("NextSecurity", nextEntry + 1);

            String text = textBuilder.toString();

            // Create the plain text output
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(text);

            // Create reprompt
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(speech);

            return SpeechletResponse.newAskResponse(speech, reprompt);
        } else {
            // Create the plain text output
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText("Das waren alle Wertpapiere in deinem Depot.");
            return SpeechletResponse.newTellResponse(speech);
        }
    }
}