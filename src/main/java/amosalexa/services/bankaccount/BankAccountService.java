package amosalexa.services.bankaccount;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.Service;
import amosalexa.SessionStorage;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.Account;
import model.banking.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * speech service for bank account information
 * <p>
 * registered Intent @BANK_ACCOUNT_INTENT, @YES_INTENT, @NO_INTENT
 */
@Service(
        functionGroup = HelpService.FunctionGroup.ACCOUNT_INFORMATION,
        functionName = "Kontoinformation",
        example = "Wie ist mein Kontostand? ",
        description = "Mit dieser Funktion kannst du Informationen über dein Konto abfragen. Du kannst dabei Parameter wie " +
                "Kontostand, Kreditlimit, Überweisungen, IBAN und Abhebegebühr angeben."
)
public class BankAccountService extends AbstractSpeechService implements SpeechService {

    /**
     * Account number of the bank account to be used.
     */
    public static final String ACCOUNT_NUMBER = AmosAlexaSpeechlet.ACCOUNT_ID;

    private static final Logger LOGGER = LoggerFactory.getLogger(BankAccountService.class);
    /**
     * amount of transaction responded at once
     */
    private static final int TRANSACTION_LIMIT = 3;
    /**
     * intents
     */
    private static final String BANK_ACCOUNT_INTENT = "AccountInformation";
    /**
     * cards
     */
    private static final String CARD_NAME = "Kontoinformation";
    /**
     * Name for custom slot types
     */
    private static final String SLOT_NAME = "AccountInformationSlots";
    /**
     * default speech texts
     */
    private static final String REPROMPT_TEXT = "Was möchtest du über dein Konto erfahren? Frage mich etwas!";
    private static final String EMPTY_TRANSACTIONS_TEXT = "Du hast keine Transaktionen in deinem Konto";
    private static final String LIST_END_TRANSACTIONS_TEXT = "Du hast keine weiteren Transaktionen";
    private static final String ACCEPTANCE_TEXT = "Verstanden!";
    /**
     * account
     */
    private static Account account;
    /**
     * slots for transactions
     */
    private final List<String> transactionSlots = new ArrayList<String>() {{
        add("transaktionen");
        add("überweisungen");
        add("umsätze");
    }};
    /**
     * session id
     */
    private String sessionID;
    /**
     * session attribute for transaction list indexe
     */
    private String CONTEXT_FURTHER_TRANSACTION_INDEX = "transaction_dialog_index";

    public BankAccountService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                BANK_ACCOUNT_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                BANK_ACCOUNT_INTENT,
                YES_INTENT,
                NO_INTENT
        );
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
        Intent intent = requestEnvelope.getRequest().getIntent();
        sessionID = requestEnvelope.getSession().getSessionId();

        // get dialog context index
        SessionStorage sessionStorage = SessionStorage.getInstance();
        Integer furtherTransactionDialogIndex = (Integer) sessionStorage.getObject(sessionID, CONTEXT_FURTHER_TRANSACTION_INDEX);

        if (furtherTransactionDialogIndex != null) {
            if (intent.getName().equals(YES_INTENT)) {
                return getNextTransaction(furtherTransactionDialogIndex);
            }

            if (intent.getName().equals(NO_INTENT)) {
                return getResponse(CARD_NAME, ACCEPTANCE_TEXT);
            }
        }

        String slotValue = intent.getSlot(SLOT_NAME) != null ? intent.getSlot(SLOT_NAME).getValue().toLowerCase() : null;
        if (slotValue != null) {
            setAccount();
            if (transactionSlots.contains(slotValue)) {
                return handleTransactionSpeech();
            }
            String speech = account.getSpeechTexts().get(slotValue);
            return getSSMLResponse(CARD_NAME, speech);
        }

        return null;
    }

    /**
     * set up speech texts in account
     */
    private void setAccount() {
        account = AccountAPI.getAccount(ACCOUNT_NUMBER);
        account.setSpeechTexts();
    }

    /**
     * responses each transaction from a account
     *
     * @return SpeechletResponse to alexa
     */
    private SpeechletResponse handleTransactionSpeech() {
        List<Transaction> transactions = TransactionAPI.getTransactionsForAccount(account.getNumber());

        if (transactions == null || transactions.isEmpty()) {
            LOGGER.warn("Account: " + account.getNumber() + " has no transactions");
            return getResponse(CARD_NAME, EMPTY_TRANSACTIONS_TEXT);
        }

        StringBuilder stringBuilder = new StringBuilder(Transaction.getTransactionSizeText(transactions.size()));
        int i;
        for (i = 0; i < TRANSACTION_LIMIT; i++) {
            stringBuilder.append(Transaction.getTransactionText(transactions.get(i)));
        }

        if (i - 1 < transactions.size()) {
            stringBuilder.append(Transaction.getAskMoreTransactionText());
            SessionStorage sessionStorage = SessionStorage.getInstance();
            sessionStorage.putObject(sessionID, CONTEXT_FURTHER_TRANSACTION_INDEX, i);
        } else {
            return getResponse(CARD_NAME, LIST_END_TRANSACTIONS_TEXT);
        }

        return getSSMLAskResponse(CARD_NAME, stringBuilder.toString(), REPROMPT_TEXT);
    }

    /**
     * returns a response with the next transaction in the list
     *
     * @param i index at the current postion in the transaction list
     * @return speechletResponse
     */
    private SpeechletResponse getNextTransaction(int i) {
        List<Transaction> transactions = TransactionAPI.getTransactionsForAccount(account.getNumber());
        String transactionText = Transaction.getTransactionText(transactions.get(i));
        if (i - 1 < transactions.size()) {
            transactionText = transactionText + Transaction.getAskMoreTransactionText();
            SessionStorage sessionStorage = SessionStorage.getInstance();
            sessionStorage.putObject(sessionID, CONTEXT_FURTHER_TRANSACTION_INDEX, i + 1);
        }
        return getSSMLAskResponse(CARD_NAME, transactionText, REPROMPT_TEXT);
    }
}
