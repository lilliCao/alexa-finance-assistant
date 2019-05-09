package amosalexa.services.cards;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.banking.AccountAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Service(
        functionGroup = HelpService.FunctionGroup.BANK_CONTACT,
        functionName = "Karte sperren",
        example = "Sperre Karte 001",
        description = "Mit dieser Funktion kannst du Karten sperren lassen, wenn sie verloren gegangen sind oder gestolen wurden."
)
public class BlockCardService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                BLOCK_CARD_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                BLOCK_CARD_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    /**
     * Default value for cards
     */
    private static final String BLOCK_CARD = "Block card";

    private static final Logger log = LoggerFactory.getLogger(BlockCardService.class);

    /**
     *
     */
    private static final String ACCOUNT_ID = AmosAlexaSpeechlet.ACCOUNT_ID;

    private static final String BLOCK_CARD_INTENT = "BlockCardIntent";

    public BlockCardService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    /**
     * ties the Speechlet Subject (Amos Alexa Speechlet) with an Speechlet Observer
     *
     * @param speechletSubject service
     */
    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for(String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();

        // TODO: Use account later to actually block a card
        Account account = AccountAPI.getAccount(ACCOUNT_ID);

        if (request.getIntent().getName().equals(YES_INTENT)) {
            String cardNumberObj = (String) session.getAttribute("BlockCardService.CardNumber");

            if (cardNumberObj != null) {
                long cardNumber = Long.parseLong(cardNumberObj);

                // TODO: Lock card with number cardNumber

                return getResponse(BLOCK_CARD, "Karte " + cardNumberObj + " wurde gesperrt.");
            }

            return null;
        } else if (request.getIntent().getName().equals(NO_INTENT)) {
            session.setAttribute("BlockCardService.CardNumber", null);
            return getResponse(BLOCK_CARD, "Okay, tschüss.");
        } else if (request.getIntent().getName().equals(BLOCK_CARD_INTENT)) {
            String bankCardNumber = request.getIntent().getSlot("BankCardNumber").getValue();

            if (bankCardNumber == null) {
                return getAskResponse(BLOCK_CARD, "Wie lautet die Nummber der Karte?");
            } else {
                session.setAttribute("BlockCardService.CardNumber", bankCardNumber);
                return getAskResponse(BLOCK_CARD, "Möchten Sie die Karte " + bankCardNumber + " wirklich sperren?");
            }
        }

        return null;
    }
}
