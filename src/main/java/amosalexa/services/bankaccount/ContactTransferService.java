package amosalexa.services.bankaccount;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.Service;
import amosalexa.SessionStorage;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.bankcontact.BankContactService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbClient;
import api.aws.DynamoDbMapper;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.Account;
import model.banking.Transaction;
import model.db.Category;
import model.db.Contact;
import model.db.TransactionDB;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This dialog allows users to perform a transfer to a {@link Contact contact} from the contact book.
 */
@Service(
        functionGroup = HelpService.FunctionGroup.ONLINE_BANKING,
        functionName = "Überweisung",
        example = "Überweise zehn Euro an Bob!",
        description = "Mit dieser Funktion kannst du Überweisungen an deine Kontakte tätigen. Du kannst dabei Parameter wie " +
                "Kontaktname und Geldbetrag festlegen."
)
public class ContactTransferService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Collections.singletonList(
                CONTACT_TRANSFER_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                CONTACT_TRANSFER_INTENT,
                CONTACT_CHOICE_INTENT,
                PLAIN_CATEGORY_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    /**
     * Specifies if this intent is being tested or not by assigning the normal or the testing table name.
     */
    public static String contactTable = Contact.TABLE_NAME;

    /**
     * Logger for debugging purposes.
     */
    private static final Logger log = LoggerFactory.getLogger(BankContactService.class);

    /**
     * This is the default title that this skill will be using for cards.
     */
    private static final String CONTACT_TRANSFER_CARD = "Überweisung an Kontakt";

    /**
     * String used as a prefix for session variables.
     */
    private static final String SESSION_PREFIX = "ContactTransfer";

    /**
     * Entry intent for this service.
     */
    private static final String CONTACT_TRANSFER_INTENT = "ContactTransferIntent";

    /**
     * Intent to specify which contact to transfer money to.
     */
    private static final String CONTACT_CHOICE_INTENT = "ContactChoiceIntent";

    /**
     *
     */
    private static final String PLAIN_CATEGORY_INTENT = "PlainCategoryIntent";

    /**
     * Account number.
     */
    private static final String ACCOUNT_NUMBER = AmosAlexaSpeechlet.ACCOUNT_ID;

    private static final SessionStorage SESSION_STORAGE = SessionStorage.getInstance();

    private static final String CATEGORY_SLOT = "Category";

    private DynamoDbMapper dynamoDbMapper = new DynamoDbMapper(DynamoDbClient.getAmazonDynamoDBClient());

    private static final String TRANSACTION_ID_ATTRIBUTE = "transaction";

    public ContactTransferService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        Intent intent = requestEnvelope.getRequest().getIntent();
        Session session = requestEnvelope.getSession();
        String intentName = intent.getName();
        String context = (String) session.getAttribute(DIALOG_CONTEXT);

        switch (intentName) {
            case CONTACT_TRANSFER_INTENT:
                return contactTransfer(intent, session);
            case CONTACT_CHOICE_INTENT:
                return contactChoice(intent, session);
            case YES_INTENT:
                if (context.equals(CONTACT_CHOICE_INTENT)) {
                    return performTransfer(intent, session);
                }
                return null;
            case NO_INTENT:
                session.setAttribute(DIALOG_CONTEXT, "");
                return getResponse(CONTACT_TRANSFER_CARD, "Okay, verstanden. Dann bis zum nächsten Mal.");
            // adds standing order to category
            case PLAIN_CATEGORY_INTENT:
               return transactionCategoryResponse(intent, session);
            default:
                return null;
        }
    }

    private SpeechletResponse transactionCategoryResponse(Intent intent, Session session){
        String categoryName = intent.getSlot(CATEGORY_SLOT) != null ? intent.getSlot(CATEGORY_SLOT).getValue().toLowerCase() : null;
        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        for (Category category : categories) {
            if (category.getName().equals(categoryName)){
                String transactionId = (String) session.getAttribute(TRANSACTION_ID_ATTRIBUTE);
                dynamoDbMapper.save(new TransactionDB(transactionId, "" + category.getId(), ACCOUNT_NUMBER));
                return getResponse(CONTACT_TRANSFER_CARD, "Verstanden. Die Transaktion wurde zur Kategorie " + categoryName + " hinzugefügt");
            }
        }
        return getResponse(CONTACT_TRANSFER_CARD, "Ich konnte die Kategorie nicht finden. Tschüss");
    }

    /**
     * Lists possible contact or directly asks for confirmation.
     */
    private SpeechletResponse contactTransfer(Intent intent, Session session) {
        // Try to get the contact name
        String contactName = intent.getSlot("Contact").getValue();
        if (contactName == null || contactName.equals("")) {
            return getResponse(CONTACT_TRANSFER_CARD, "Ich konnte den Namen des Kontakts nicht verstehen. Versuche es noch einmal.");
        }
        log.info("ContactName: " + contactName);

        contactName = contactName.toLowerCase();

        // Try to get the amount
        double amount;
        try {
            amount = Double.parseDouble(intent.getSlot("Amount").getValue());
        } catch (NumberFormatException ignored) {
            return getResponse(CONTACT_TRANSFER_CARD, "Ich konnte den Betrag nicht verstehen. Versuche es noch einmal.");
        }

        session.setAttribute(SESSION_PREFIX + ".amount", amount);

        // Query database
        List<Contact> contacts = DynamoDbMapper.getInstance().loadAll(Contact.class);

        List<Contact> contactsFound = new LinkedList<>();
        for (Contact contact : contacts) {
            String thisContactName = contact.getName().toLowerCase();

            log.info("Contacts in DB: " + thisContactName);

            int dist = StringUtils.getLevenshteinDistance(thisContactName, contactName);
            if (dist < contactName.length() / 2) {
                contactsFound.add(contact);
            } else {
                if (thisContactName.startsWith(contactName)) {
                    contactsFound.add(contact);
                } else if (thisContactName.contains(contactName)) {
                    contactsFound.add(contact);
                } else {
                    int middle = thisContactName.length() / 2;
                    String firstHalf = thisContactName.substring(0, middle);
                    String secondHalf = thisContactName.substring(middle);

                    dist = Math.min(
                            StringUtils.getLevenshteinDistance(firstHalf, contactName),
                            StringUtils.getLevenshteinDistance(secondHalf, contactName));

                    if (dist < middle / 1.5) {
                        contactsFound.add(contact);
                    }
                }
            }
        }

        if (contactsFound.size() == 0) {
            return getResponse(CONTACT_TRANSFER_CARD, "Ich konnte keinen Kontakt finden mit dem Namen: " + contactName);
        }

        SESSION_STORAGE.putObject(session.getSessionId(), SESSION_PREFIX + ".contactsFound", contactsFound);

        if (contactsFound.size() == 1) {
            session.setAttribute(SESSION_PREFIX + ".choice", 1);
            return confirm(session);
        }

        StringBuilder contactListString = new StringBuilder();

        contactListString
                .append("Ich habe ")
                .append(contactsFound.size())
                .append(" passende Kontakte gefunden. Bitte wähle einen aus: ");

        int i = 1;
        for (Contact contact : contactsFound) {
            contactListString
                    .append("Kontakt Nummer ")
                    .append(i).append(": ")
                    .append(contact.getName())
                    .append(". ");
            i++;
        }

        log.info("Several contacts found: " + contactListString.toString());
        session.setAttribute(DIALOG_CONTEXT, CONTACT_TRANSFER_INTENT);

        return getAskResponse(CONTACT_TRANSFER_CARD, contactListString.toString());
    }

    private SpeechletResponse confirm(Session session) {
        Object contactsFoundObj = SESSION_STORAGE.getObject(session.getSessionId(), SESSION_PREFIX + ".contactsFound");

        if (contactsFoundObj == null || !(contactsFoundObj instanceof List)) {
            return getResponse(CONTACT_TRANSFER_CARD, "Da ist etwas schiefgegangen. Tut mir Leid.");
        }

        Object choiceObj = session.getAttribute(SESSION_PREFIX + ".choice");

        if (choiceObj == null || !(choiceObj instanceof Integer)) {
            return getResponse(CONTACT_TRANSFER_CARD, "Da ist etwas schiefgegangen. Tut mir Leid.");
        }

        Object amountObj = session.getAttribute(SESSION_PREFIX + ".amount");

        if (amountObj == null || !(amountObj instanceof Double || amountObj instanceof Integer)) {
            return getResponse(CONTACT_TRANSFER_CARD, "Da ist etwas schiefgegangen. Tut mir Leid.");
        }

        Contact contact = ((List<Contact>) contactsFoundObj).get((int) choiceObj - 1);

        double amount;
        if (amountObj instanceof Double) {
            amount = (double) amountObj;
        } else {
            amount = (int) amountObj;
        }

        session.setAttribute(DIALOG_CONTEXT, CONTACT_CHOICE_INTENT);

        Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
        String balanceBeforeTransation = String.valueOf(account.getBalance());

        return getAskResponse(CONTACT_TRANSFER_CARD, "Dein aktueller Kontostand beträgt " +
                balanceBeforeTransation + " Euro. Möchtest du " + amount + " Euro an " + contact.getName() + " überweisen?");
    }

    /**
     * Asks for confirmation.
     */
    private SpeechletResponse contactChoice(Intent intent, Session session) {
        int choice;
        try {
            choice = Integer.parseInt(intent.getSlot("ContactIndex").getValue());
        } catch (NumberFormatException ignored) {
            return getAskResponse(CONTACT_TRANSFER_CARD, "Ich habe leider nicht verstanden welche Person du meinst. Bitte wiederhole.");
        }

        session.setAttribute(SESSION_PREFIX + ".choice", choice);

        return confirm(session);
    }

    /**
     * Performs the transfer.
     */
    private SpeechletResponse performTransfer(Intent intent, Session session) {
        Object contactsFoundObj = SESSION_STORAGE.getObject(session.getSessionId(), SESSION_PREFIX + ".contactsFound");

        if (contactsFoundObj == null || !(contactsFoundObj instanceof List)) {
            return getResponse(CONTACT_TRANSFER_CARD, "Da ist etwas schiefgegangen. Tut mir Leid.");
        }

        Object choiceObj = session.getAttribute(SESSION_PREFIX + ".choice");

        if (choiceObj == null || !(choiceObj instanceof Integer)) {
            return getResponse(CONTACT_TRANSFER_CARD, "Da ist etwas schiefgegangen. Tut mir Leid.");
        }

        Object amountObj = session.getAttribute(SESSION_PREFIX + ".amount");

        if (amountObj == null || !(amountObj instanceof Double || amountObj instanceof Integer)) {
            return getResponse(CONTACT_TRANSFER_CARD, "Da ist etwas schiefgegangen. Tut mir Leid.");
        }

        Contact contact = ((List<Contact>) contactsFoundObj).get((int) choiceObj - 1);

        double amount;
        if (amountObj instanceof Double) {
            amount = (double) amountObj;
        } else {
            amount = (int) amountObj;
        }

        DateTimeFormatter apiTransactionFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        String valueDate = DateTime.now().toString(apiTransactionFmt);
        Transaction transaction = TransactionAPI.createTransaction((int) amount, /*"DE50100000000000000001"*/ AmosAlexaSpeechlet.ACCOUNT_IBAN, contact.getIban(), valueDate,
                "Beschreibung", "Hans", null);


        Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
        String balanceAfterTransaction = String.valueOf(account.getBalance());


        //save transaction id to save in db
        session.setAttribute(TRANSACTION_ID_ATTRIBUTE, transaction.getTransactionId().toString());

        //add category ask to success response
        String categoryAsk = "Zu welcher Kategorie soll die Transaktion hinzugefügt werden. Sag zum Beispiel Kategorie " + Category.categoryListText();

        return getAskResponse(CONTACT_TRANSFER_CARD, "Erfolgreich. " + amount + " Euro wurden an " + contact.getName() + " überwiesen. Dein neuer Kontostand beträgt " + balanceAfterTransaction + " Euro. " + categoryAsk);
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }
}
