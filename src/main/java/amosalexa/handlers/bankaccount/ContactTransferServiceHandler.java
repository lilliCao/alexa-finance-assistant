package amosalexa.handlers.bankaccount;

import amosalexa.Service;
import amosalexa.services.help.HelpService;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;
import model.banking.Account;
import model.banking.Transaction;
import model.db.Category;
import model.db.Contact;
import model.db.TransactionDB;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static amosalexa.AmosAlexaSpeechlet.ACCOUNT_IBAN;
import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.*;

@Service(
        functionGroup = HelpService.FunctionGroup.ONLINE_BANKING,
        functionName = "Überweisung",
        example = "Überweise zehn Euro an Bob!",
        description = "Mit dieser Funktion kannst du Überweisungen an deine Kontakte tätigen. Du kannst dabei Parameter wie " +
                "Kontaktname und Geldbetrag festlegen."
)
public class ContactTransferServiceHandler implements IntentRequestHandler {
    private static final String CONTACT_TRANSFER_INTENT = "ContactTransferIntent";
    private static final String CONTACT_CHOICE_INTENT = "ContactChoiceIntent";
    private static final String PLAIN_CATEGORY_INTENT = "PlainCategoryIntent";

    private static final String SLOT_CATEGORY = "Category";
    private static final String SLOT_AMOUNT_EURO = "AmountEuro";
    private static final String SLOT_CONTACT = "Contact";
    private static final String SLOT_CONTACT_INDEX = "ContactIndex";

    private static final String CARD_TITLE = "Überweisung an Kontakt";
    private static final String SAVE_AMOUNT = "save_amount";
    private static final String SAVE_NAME = "save_name";
    private static final String SAVE_IBAN = "save_iban";
    private static final String SAVE_TRANSACTION_ID = "save_transaction_id";

    private static List<Contact> contacts;
    private static HandlerInput inputReceived;
    private static IntentRequest intentRequestReceived;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(CONTACT_TRANSFER_INTENT))
                || input.matches(Predicates.intentName(CONTACT_CHOICE_INTENT))
                || (input.matches(Predicates.intentName(PLAIN_CATEGORY_INTENT))
                && input.getAttributesManager().getSessionAttributes().containsKey(SAVE_TRANSACTION_ID));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        inputReceived = input;
        intentRequestReceived = intentRequest;
        switch (intentRequestReceived.getIntent().getName()) {
            case CONTACT_TRANSFER_INTENT:
                switch (intentRequestReceived.getIntent().getConfirmationStatus()) {
                    case NONE:
                        return contactTransferAction();
                    case CONFIRMED:
                        return transfer();
                    default:
                        return response(inputReceived, CARD_TITLE);
                }
            case CONTACT_CHOICE_INTENT:
                return contactChoiceAction();
            default:
                //PLAIN_CATEGORY_INTENT:
                return addCategory();
        }
    }

    private Optional<Response> addCategory() {
        if (intentRequestReceived.getIntent().getSlots().get(SLOT_CATEGORY) != null) {
            String categoryName = intentRequestReceived.getIntent().getSlots().get(SLOT_CATEGORY).getValue();
            List<Category> categories = dynamoDbMapper.loadAll(Category.class);
            for (Category category : categories) {
                if (category.getName().equalsIgnoreCase(categoryName)) {
                    String transactionId = String.valueOf(inputReceived.getAttributesManager().getSessionAttributes()
                            .get(SAVE_TRANSACTION_ID));
                    dynamoDbMapper.save(new TransactionDB(transactionId, "" + category.getId(), ACCOUNT_NUMBER));
                    return response(inputReceived, CARD_TITLE, "Verstanden. Die Transaktion wurde zur Kategorie " + categoryName + " hinzugefügt");
                }
            }
        }
        return response(inputReceived, CARD_TITLE, "Ich konnte die Kategorie nicht finden. Tschüss");
    }

    private Optional<Response> contactChoiceAction() {
        int choice;
        try {
            choice = Integer.parseInt(intentRequestReceived.getIntent().getSlots().get(SLOT_CONTACT_INDEX).getValue());
        } catch (NumberFormatException ignored) {
            return responseDelegate(inputReceived, CARD_TITLE,
                    "Ich habe leider nicht verstanden welche Person du meinst. Bitte wiederhole.",
                    Intent.builder().withName(CONTACT_CHOICE_INTENT).build());
        }
        if (choice <= contacts.size()) {
            Contact contact = contacts.get(choice - 1);
            return confirmTransfer(inputReceived, Intent.builder().withName(CONTACT_TRANSFER_INTENT).build(), contact);
        } else {
            return responseDelegate(inputReceived, CARD_TITLE,
                    "Bitte wähle eine andere Index.",
                    Intent.builder().withName(CONTACT_CHOICE_INTENT).build());
        }
    }

    private Optional<Response> contactTransferAction() {
        Map<String, Slot> slots = intentRequestReceived.getIntent().getSlots();
        String name = slots.get(SLOT_CONTACT).getValue().toLowerCase();

        double amount = Double.parseDouble(slots.get(SLOT_AMOUNT_EURO).getValue());
        Map<String, Object> sessionAttributes = inputReceived.getAttributesManager().getSessionAttributes();
        sessionAttributes.put(SAVE_AMOUNT, amount);
        inputReceived.getAttributesManager().setSessionAttributes(sessionAttributes);

        List<Contact> contactsFound = queryContact(name);
        if (contactsFound.isEmpty()) {
            String speechText = "Ich konnte keinen Kontakt mit dem Namen: " + name + " finden";
            return response(inputReceived, CARD_TITLE, speechText);
        } else if (contactsFound.size() == 1) {
            return confirmTransfer(inputReceived, intentRequestReceived.getIntent(), contactsFound.get(0));
        } else {
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
            contacts = contactsFound;
            return responseDelegate(inputReceived, CARD_TITLE, contactListString.toString(),
                    Intent.builder().withName(CONTACT_CHOICE_INTENT).build());
        }
    }

    private Optional<Response> confirmTransfer(HandlerInput input, Intent intent, Contact contact) {
        Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
        sessionAttributes.put(SAVE_NAME, contact.getName());
        sessionAttributes.put(SAVE_IBAN, contact.getIban());
        input.getAttributesManager().setSessionAttributes(sessionAttributes);

        String amount = String.valueOf(sessionAttributes.get(SAVE_AMOUNT));
        Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
        String speechText = "Dein aktueller Kontostand beträgt " +
                account.getBalance() + " Euro. Möchtest du " + amount + " Euro an "
                + contact.getName() + " überweisen?";
        return responseWithIntentConfirm(input, CARD_TITLE, speechText, intent);
    }

    private Optional<Response> transfer() {
        Map<String, Object> sessionAttributes = inputReceived.getAttributesManager().getSessionAttributes();
        String name = (String) sessionAttributes.get(SAVE_NAME);
        String iban = (String) sessionAttributes.get(SAVE_IBAN);
        double amount = (Double) sessionAttributes.get(SAVE_AMOUNT);
        DateTimeFormatter apiTransactionFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        String valueDate = DateTime.now().toString(apiTransactionFmt);

        Transaction transaction = TransactionAPI.createTransaction(amount, ACCOUNT_IBAN, iban, valueDate,
                "Beschreibung", "Hans", null);
        sessionAttributes.put(SAVE_TRANSACTION_ID, transaction.getTransactionId());
        inputReceived.getAttributesManager().setSessionAttributes(sessionAttributes);

        Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
        String speechText = "Erfolgreich. " + amount + " Euro wurden an " + name + " überwiesen."
                + " Dein neuer Kontostand beträgt " + account.getBalance() + " Euro."
                + " Zu welcher Kategorie soll die Transaktion hinzugefügt werden. Sag zum Beispiel Kategorie "
                + Category.categoryListText();
        return responseDelegate(inputReceived, CARD_TITLE, speechText, Intent.builder().withName(PLAIN_CATEGORY_INTENT).build());
    }


    private List<Contact> queryContact(String name) {
        List<Contact> contacts = dynamoDbMapper.loadAll(Contact.class);
        List<Contact> contactsFound = new LinkedList<>();
        for (Contact contact : contacts) {
            String thisContactName = contact.getName().toLowerCase();

            int dist = StringUtils.getLevenshteinDistance(thisContactName, name);
            if (dist < name.length() / 2) {
                contactsFound.add(contact);
            } else {
                if (thisContactName.contains(name)) {
                    contactsFound.add(contact);
                } else {
                    int middle = thisContactName.length() / 2;
                    String firstHalf = thisContactName.substring(0, middle);
                    String secondHalf = thisContactName.substring(middle);

                    dist = Math.min(
                            StringUtils.getLevenshteinDistance(firstHalf, name),
                            StringUtils.getLevenshteinDistance(secondHalf, name));

                    if (dist < middle / 1.5) {
                        contactsFound.add(contact);
                    }
                }
            }
        }
        return contactsFound;
    }

}
