package amosalexa.services.financing;

import amosalexa.Service;
import amosalexa.SessionStorage;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.AccountData;
import amosalexa.services.DialogUtil;
import amosalexa.services.SpeechService;
import amosalexa.services.financing.aws.model.Item;
import amosalexa.services.financing.aws.request.AWSLookup;
import amosalexa.services.financing.aws.util.AWSUtil;
import amosalexa.services.help.HelpService;
import api.aws.EMailClient;
import api.banking.AccountAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service(
        functionGroup = HelpService.FunctionGroup.SMART_FINANCING,
        functionName = "Produktbezahlbarkeit",
        example = "Was kostet ein Samsung Galaxy?",
        description = "Mit dieser Funktion kannst du ein Produkt auf Amazon suchen, den Preis überprüfen und per Email" +
                "vormerken lassen. Du kannst dabei den Parameter " +
                "Suchwort definieren."
)
public class AffordabilityService extends AbstractSpeechService implements SpeechService {


    private static final Logger log = LoggerFactory.getLogger(AffordabilityService.class);

    /**
     * intents
     */
    private final static String AFFORD_INTENT = "AffordProduct";

    /**
     * slot
     */
    private static final String KEYWORD_SLOT = "ProductKeyword";
    private static final String PRODUCT_SLOT = "ProductSelection";

    /**
     * cards
     */
    private static final String CARD = "BUY_CHECK";

    /**
     * speech texts
     */
    public static final String DESIRE_ASK = "Möchtest du ein Produkt kaufen";
    public static final String SELECTION_ASK = "Welches Produkt möchtest du kaufen Sag produkt a, b oder c";
    public static final String ERROR = "Ich konnte deine Eingabe nicht verstehen. Sprich Lauter oder versuche es später noch einmal";
    public static final String NO_RESULTS = "Die Suche ergab keine Ergebnisse";
    public static final String TOO_FEW_RESULTS = "Die Suche ergab zu wenig Ergebnisse";
    public static final String CANT_AFFORD = "Das Produkt kannst du dir nicht leisten!";
    public static final String OTHER_SELECTION = "Möchtest du nach etwas anderem suchen";
    public static final String NOTE_ASK = "Willst du das Produkt vormerken";
    public static final String EMAIL_ACK = "Eine E-mail mit einem Produkt-link zu Amazon wurde an dich geschickt";
    public static final String SEARCH_ASK = "Was suchst du. Frage mich zum Beispiel was ein Samsung Galaxy kostet";
    public static final String BYE = "Ok, Tschüss!";

    /**
     * slot values
     */
    private static String keywordSlot;
    private static String productSelectionSlot;

    /**
     * session / intent
     */
    private static Session session;
    private static Intent intent;

    /**
     * item selected after selection ask
     */
    private Item selectedItem;


    public AffordabilityService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                AFFORD_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                AFFORD_INTENT,
                YES_INTENT,
                NO_INTENT,
                STOP_INTENT
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

        getSlots();

        // error occurs when trying to request data from amazon product api
        Object errorRequest = DialogUtil.getDialogState("error!", session);
        if (errorRequest != null) {
            return getResponse(CARD, ERROR);
        }

        // user decides if he wants to buy a product
        if (DialogUtil.getDialogState("buy?", session) != null) {
            log.debug("Dialog State: buy?");
            if (intent.getName().equals(YES_INTENT)) {
                DialogUtil.setDialogState("which?", session);
                return getAskResponse(CARD, SELECTION_ASK);
            }
            return exitDialogRespond();
        }

        // user decides which product he wants to buy
        if (DialogUtil.getDialogState("which?", session) != null) {
            log.debug("Dialog State: which?");
            if (productSelectionSlot != null) {
                return getAffordabilityResponse();
            }
            DialogUtil.setDialogState("which?", session);
            return getAskResponse(CARD, SELECTION_ASK);
        }

        // user decides if he want to look for other products
        if (DialogUtil.getDialogState("other?", session) != null) {
            log.debug("Dialog State: other?");
            if (intent.getName().equals(YES_INTENT)) {
                return getAskResponse(CARD, SEARCH_ASK);
            }
            return exitDialogRespond();
        }

        // user decides if he wants a email with a product link
        if (DialogUtil.getDialogState("email?", session) != null) {
            log.debug("Dialog State: email?");
            if (intent.getName().equals(YES_INTENT)) {
                EMailClient.SendEMail("Amazon Produkt: " + selectedItem.getTitle(), selectedItem.getDetailPageURL());
                return getResponse(CARD, EMAIL_ACK);
            }
            return exitDialogRespond();
        }

        return getProductInformation();
    }

    /**
     * default exit response
     *
     * @return SpeechletResponse
     */
    private SpeechletResponse exitDialogRespond() {
        return getResponse(CARD, BYE);
    }

    /**
     * response if the selected items is affordable or not referring the account balance
     *
     * @return SpeechletResponse
     */
    private SpeechletResponse getAffordabilityResponse() {

        if (productSelectionSlot == null) {
            DialogUtil.setDialogState("which?", session);
            return getAskResponse(CARD, ERROR + " " + SELECTION_ASK);
        }

        if (productSelectionSlot.length() != 1) {
            DialogUtil.setDialogState("which?", session);
            return getAskResponse(CARD, ERROR + " " + SELECTION_ASK);
        }

        selectedItem = (Item) SessionStorage.getInstance().getObject(session.getSessionId(), "produkt " + productSelectionSlot.toLowerCase());

        if (selectedItem == null) {
            DialogUtil.setDialogState("which?", session);
            return getAskResponse(CARD, ERROR + " " + SELECTION_ASK);
        }

        // save selection in session
        SessionStorage.getInstance().putObject(session.getSessionId(), "selection", selectedItem);

        // get account data
        Account account = AccountAPI.getAccount(AccountData.ACCOUNT_FEW_MONEY);
        account.setSpeechTexts();
        Number balance = account.getBalance();

        if (selectedItem.getLowestNewPrice() / 100 > balance.doubleValue()) {
            DialogUtil.setDialogState("other?", session);
            return getSSMLAskResponse(CARD, getItemText(selectedItem)
                    + " " + CANT_AFFORD + " " + account.getBalanceText() + " " + OTHER_SELECTION);
        }

        DialogUtil.setDialogState("email?", session);
        return getAskResponse(CARD, "Produkt " + productSelectionSlot + " " + selectedItem.getTitleShort() + " " + NOTE_ASK);
    }

    /**
     * creates a speech response by the response of a amazon query and return 3 items
     *
     * @return SpeechletResponse
     */
    private SpeechletResponse getProductInformation() {
        if (keywordSlot != null) {

            List<Item> items;
            try {
                items = AWSLookup.itemSearch(keywordSlot, 1, null);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                log.error("Amazon ItemSearch Lookup Exception: " + e.getMessage());
                DialogUtil.setDialogState("error!", session);
                return getAskResponse(CARD, NO_RESULTS + " " + SEARCH_ASK);
            }

            if (items.isEmpty()) {
                log.error("no results by keywordSlot: " + keywordSlot);
                DialogUtil.setDialogState("error!", session);
                return getAskResponse(CARD, NO_RESULTS + " " + SEARCH_ASK);
            }

            if (items.size() < 3) {
                log.error("too few results by keywordSlot: " + keywordSlot);
                DialogUtil.setDialogState("error!", session);
                return getAskResponse(CARD, TOO_FEW_RESULTS + " " + SEARCH_ASK);
            }


            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 3; i++) {

                // save in session
                SessionStorage.getInstance().putObject(session.getSessionId(), "produkt " + (char) ('a' + i), items.get(i));

                // shorten title
                String productTitle = AWSUtil.shortTitle(items.get(i).getTitle());
                items.get(i).setTitleShort(productTitle);

                // Seems to happen if the product is not available? Just skip to prevent NullPointerException in getItemText()
                if(items.get(i).getLowestNewPrice() == null) {
                    items.remove(i);
                    i--;
                    continue;
                }

                // build respond
                text.append("Produkt ")
                        .append((char) ('a' + i))
                        .append("<break time=\"1s\"/> ")
                        .append(getItemText(items.get(i)));
            }

            text.append(DESIRE_ASK);
            DialogUtil.setDialogState("buy?", session);
            return getSSMLAskResponse(CARD, text.toString(), SEARCH_ASK);
        }

        return getAskResponse(CARD, "Ein Fehler ist aufgetreten. " + ERROR);
    }

    /**
     * get all slot values
     */
    private void getSlots() {
        keywordSlot = intent.getSlot(KEYWORD_SLOT) != null ? intent.getSlot(KEYWORD_SLOT).getValue() : null;
        productSelectionSlot = intent.getSlot(PRODUCT_SLOT) != null ? intent.getSlot(PRODUCT_SLOT).getValue() : null;

        log.info("Search Keyword: " + keywordSlot);
        log.info("Product Selection: " + productSelectionSlot);
    }

    /**
     * returns the text for a item of the amazon search
     *
     * @param item Item from Amazon
     * @return text
     */
    private String getItemText(Item item) {
        return item.getTitleShort() + "kostet <say-as interpret-as=\"unit\">€" + item.getLowestNewPrice() / 100 +
                "</say-as> <break time=\"1s\"/>";
    }


}
