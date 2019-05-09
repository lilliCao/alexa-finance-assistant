package amosalexa.services.contacts;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.DialogUtil;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbMapper;
import api.banking.TransactionAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;
import model.banking.Transaction;
import model.db.Contact;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for contact and contact list related intents.
 */
@Service(
        functionGroup = HelpService.FunctionGroup.ACCOUNT_INFORMATION,
        functionName = "Karte sperren",
        example = "Was sind meine Kontakte?",
        description = "Mit dieser Funktion kannst du Kontakte verwalten, um schnell Überweisungen vorzunehmen."
)
public class ContactService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    private static final String CONTACT_LIST_INFO_INTENT = "ContactListInfoIntent";
    private static final String CONTACT_ADD_INTENT = "ContactAddIntent";
    private static final String CONTACT_DELETE_INTENT = "ContactDeleteIntent";

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                CONTACT_LIST_INFO_INTENT,
                CONTACT_ADD_INTENT,
                CONTACT_DELETE_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                CONTACT_LIST_INFO_INTENT,
                CONTACT_ADD_INTENT,
                CONTACT_DELETE_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    public ContactService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    /**
     * Default value for cards
     */
    private static final String CONTACTS = "Kontakte";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactService.class);

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        Intent intent = requestEnvelope.getRequest().getIntent();
        String intentName = intent.getName();
        Session session = requestEnvelope.getSession();
        String context = (String) session.getAttribute(DIALOG_CONTEXT);

        if (CONTACT_LIST_INFO_INTENT.equals(intentName)) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, CONTACT_LIST_INFO_INTENT);
            return readContacts(session, 0, 3);
        } else if (CONTACT_ADD_INTENT.equals(intentName)) {
            session.getAttributes().clear();
            session.setAttribute(DIALOG_CONTEXT, CONTACT_ADD_INTENT);
            return askForNewContactConfirmation(intent, session);
        } else if (CONTACT_DELETE_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, CONTACT_DELETE_INTENT);
            return deleteContact(intent, session, false);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals(CONTACT_ADD_INTENT)) {
            return createNewContact(session);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals(CONTACT_DELETE_INTENT)) {
            return deleteContact(intent, session, true);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals(CONTACT_LIST_INFO_INTENT)) {
            return continueReadingContacts(intent, session);
        } else if (NO_INTENT.equals(intentName)) {
            return getResponse(CONTACTS, "OK, dann nicht. Auf wiedersehen!");
        } else {
            throw new SpeechletException("Unhandled intent: " + intentName);
        }
    }

    private SpeechletResponse askForNewContactConfirmation(Intent intent, Session session) {
        Map<String, Slot> slots = intent.getSlots();


        Slot transactionNumberSlot = slots.get("TransactionNumber");

        if (transactionNumberSlot == null
                || transactionNumberSlot.getValue() == null
                || StringUtils.isBlank(transactionNumberSlot.getValue())) {
            String speechText = "Das habe ich nicht ganz verstanden. Bitte wiederhole deine Eingabe.";
            session.getAttributes().clear();
            return getAskResponse(CONTACTS, speechText);
        }

        LOGGER.info("TransactionNumber: " + transactionNumberSlot.getValue());

        String speechText = "";
        List<Transaction> allTransactions = TransactionAPI.getTransactionsForAccount(AmosAlexaSpeechlet.ACCOUNT_ID);
        Number transactionNumber = Integer.valueOf(transactionNumberSlot.getValue());

        Transaction transaction = null;
        for (Transaction t : allTransactions) {
            if (transactionNumber.equals(t.getTransactionId())) {
                transaction = t;
            }
        }

        LOGGER.info("Found transaction: " + transaction);

        if (transaction == null) {
            speechText = "Ich habe keine Transaktion mit dieser Nummer gefunden. Bitte wiederhole deine Eingabe.";
            session.getAttributes().clear();
            return getAskResponse(CONTACTS, speechText);
        }

        LOGGER.info("Payee: " + transaction.getPayee());
        LOGGER.info("Remitter: " + transaction.getRemitter());
        String payee = transaction.getPayee();
        String remitter = transaction.getRemitter();
        String ibanRegex = ".*\\d+.*";
        if ((payee == null && remitter == null) || (transaction.isOutgoing() && payee.matches(ibanRegex)) ||
                (!transaction.isOutgoing() && remitter.matches(ibanRegex))) {
            speechText = "Ich kann fuer diese Transaktion keine Kontaktdaten speichern, weil der Name des";
            speechText = speechText.concat(transaction.isOutgoing() ? " Zahlungsempfaengers" : " Auftraggebers");
            speechText = speechText.concat(" nicht bekannt ist. Bitte wiederhole deine Eingabe oder breche ab, indem du \"Alexa, Stop!\" sagst.");
            session.getAttributes().clear();
            return getAskResponse(CONTACTS, speechText);
        } else {
            //TODO improve
            //Actually asking for confirmation
            session.setAttribute("ContactName", transaction.isOutgoing() ? payee : remitter);
            speechText = "Moechtest du ";
            speechText = speechText.concat(transaction.isOutgoing() ? payee : remitter);
            speechText = speechText.concat(" als Kontakt speichern?");
        }
        return getAskResponse(CONTACTS, speechText);
    }

    private SpeechletResponse createNewContact(Session session) {
        //Acutally create and save contact
        String contactName = (String) session.getAttribute("ContactName");
        Contact contact = new Contact(contactName, "DE50100000000000000001");
        DynamoDbMapper.getInstance().save(contact);
        return getResponse(CONTACTS, "Okay! Der Kontakt " + contactName + " wurde angelegt.");
    }

    private SpeechletResponse deleteContact(Intent intent, Session session, boolean confirmed) {
        if (!confirmed) {
            String contactIdStr = intent.getSlot("ContactID").getValue();

            if (contactIdStr == null || contactIdStr.equals("")) {
                return getResponse(CONTACTS, "Das habe ich nicht verstanden. Nuschel nicht so!");
            } else {
                int contactId = Integer.parseInt(contactIdStr);
                session.setAttribute(CONTACTS + ".delete", contactId);

                return getAskResponse(CONTACTS, "Moechtest du Kontakt Nummer " + contactId + " wirklich loeschen?");
            }
        } else {
            Integer contactId = (Integer) session.getAttribute(CONTACTS + ".delete");

            if (contactId != null) {
                Contact contact = new Contact(contactId);
                DynamoDbMapper.getInstance().delete(contact);

                return getResponse(CONTACTS, "Kontakt wurde geloescht.");
            }
        }

        return null;
    }

    private SpeechletResponse continueReadingContacts(Intent intent, Session session) {
        Integer offset = (Integer) session.getAttribute(CONTACTS + ".offset");

        if (offset == null) {
            offset = 0;
        }

        return readContacts(session, offset, 3);
    }

    private SpeechletResponse readContacts(Session session, int offset, int limit) {
        List<Contact> contactList = DynamoDbMapper.getInstance().loadAll(Contact.class);
        List<Contact> contacts = new ArrayList<>(contactList);
        LOGGER.info("Contacts: " + contacts);

        if (contacts.size() == 0) {
            return getResponse(CONTACTS, "Du hast keine Kontakte in deiner Kontaktliste.");
        }

        if (offset >= contacts.size()) {
            session.setAttribute(CONTACTS + ".offset", null);
            return getResponse(CONTACTS, "Keine weiteren Kontakte.");
        }

        if (offset + limit >= contacts.size()) {
            limit = contacts.size() - offset;
        }

        Collections.sort(contacts);

        StringBuilder response = new StringBuilder();
        response.append("<speak>");

        for (int i = offset; i < offset + limit; i++) {
            Contact contact = contacts.get(i);

            response.append("Kontakt ")
                    .append(contact.getId())
                    .append(": ");
            response.append("Name: ")
                    .append(contact.getName())
                    .append(", IBAN: ")
                    .append(DialogUtil.getIbanSsmlOutput(contact.getIban()))
                    .append(". ");
        }

        boolean isAskResponse = contacts.size() > offset + limit;

        if (isAskResponse) {
            response.append("Weitere Kontakte vorlesen?");
            response.append("</speak>");
            session.setAttribute(CONTACTS + ".offset", offset + limit);

            SsmlOutputSpeech speech = new SsmlOutputSpeech();
            speech.setSsml(response.toString());
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(speech);
            return SpeechletResponse.newAskResponse(speech, reprompt);
        } else {
            response.append("Keine weiteren Kontakte.");
            response.append("</speak>");
            session.setAttribute(CONTACTS + ".offset", null);

            SsmlOutputSpeech speech = new SsmlOutputSpeech();
            speech.setSsml(response.toString());
            return SpeechletResponse.newTellResponse(speech);
        }
    }
}
