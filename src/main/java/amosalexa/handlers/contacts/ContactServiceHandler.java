package amosalexa.handlers.contacts;

import amosalexa.handlers.Service;
import amosalexa.handlers.utils.DialogUtil;
import api.banking.TransactionAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.Transaction;
import model.db.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.*;

@Service(
        functionGroup = Service.FunctionGroup.ACCOUNT_INFORMATION,
        functionName = "Kontakte verwalten",
        example = "Was sind meine Kontakte?",
        description = "Mit dieser Funktion kannst du Kontakte verwalten, um schnell Überweisungen vorzunehmen."
)
public class ContactServiceHandler implements IntentRequestHandler {
    private static final String CONTACT_LIST_INFO_INTENT = "ContactListInfoIntent";
    private static final String CONTACT_ADD_INTENT = "ContactAddIntent";
    private static final String CONTACT_DELETE_INTENT = "ContactDeleteIntent";

    private static final String CARD_TITLE = "Kontakte verwalten";

    private static final String SLOT_NEXT_INDEX = "NextIndex";
    private static final String SLOT_CONTACT_ID = "ContactID";
    private static final String SLOT_TRANSACTION_NUMBER = "TransactionNumber";

    private static final int CONTACT_LIMIT = 3;
    private static final String SAVE_CONTACT_NAME = "save_contact_name";
    private static final String SAVE_IBAN = "save_iban";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(CONTACT_LIST_INFO_INTENT))
                || input.matches(Predicates.intentName(CONTACT_ADD_INTENT))
                || input.matches(Predicates.intentName(CONTACT_DELETE_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        switch (intentRequest.getIntent().getName()) {
            case CONTACT_LIST_INFO_INTENT:
                return getContactList(input, intentRequest);
            case CONTACT_ADD_INTENT:
                switch (intentRequest.getIntent().getConfirmationStatus()) {
                    case NONE:
                        return checkTransaction(input, intentRequest);
                    case CONFIRMED:
                        return addNewContact(input);
                    default:
                        return response(input, CARD_TITLE);
                }
            default:
                //CONTACT_DELETE
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return deleteContact(input, intentRequest);
                } else {
                    return response(input, CARD_TITLE);
                }
        }
    }

    private Optional<Response> addNewContact(HandlerInput input) {
        Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
        String contactName = (String) sessionAttributes.get(SAVE_CONTACT_NAME);
        String iban = (String) sessionAttributes.get(SAVE_IBAN);
        Contact contact = new Contact(contactName, iban);
        dynamoDbMapper.save(contact);
        return response(input, CARD_TITLE, "Okay! Der Kontakt " + contactName + " wurde angelegt.");
    }

    private Optional<Response> checkTransaction(HandlerInput input, IntentRequest intentRequest) {
        Number id = Integer.valueOf(intentRequest.getIntent().getSlots().get(SLOT_TRANSACTION_NUMBER).getValue());
        List<Transaction> allTransactions = TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER);
        for (Transaction transaction : allTransactions) {
            if (id.equals(transaction.getTransactionId())) {
                // found
                String payee = transaction.getPayee();
                String remitter = transaction.getRemitter();
                String ibanRegex = ".*\\d+.*";
                if ((payee == null && remitter == null) || (transaction.isOutgoing() && payee.matches(ibanRegex)) ||
                        (!transaction.isOutgoing() && remitter.matches(ibanRegex))) {
                    String speechText = "Ich kann fuer diese Transaktion keine Kontaktdaten speichern, weil der Name des"
                            + (transaction.isOutgoing() ? " Zahlungsempfaengers" : " Auftraggebers")
                            + " nicht bekannt ist. Bitte wiederhole deine Eingabe oder breche ab, indem du \"Alexa, Stop!\" sagst.";
                    return response(input, CARD_TITLE, speechText);
                } else {
                    String name = (transaction.isOutgoing() ? payee : remitter);
                    String iban = transaction.isOutgoing() ? transaction.getSourceAccount() : transaction.getDestinationAccount();
                    String speechText = "Moechtest du "
                            + name
                            + " als Kontakt speichern?";
                    Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
                    sessionAttributes.put(SAVE_CONTACT_NAME, name);
                    sessionAttributes.put(SAVE_IBAN, iban);
                    input.getAttributesManager().setSessionAttributes(sessionAttributes);
                    return responseWithIntentConfirm(input, CARD_TITLE, speechText, intentRequest.getIntent());
                }
            }
        }

        return response(input, CARD_TITLE, "Ich habe keine Transaktion mit dieser Nummer gefunden. Bitte wiederhole deine Eingabe.");
    }

    private Optional<Response> deleteContact(HandlerInput input, IntentRequest intentRequest) {
        int id = Integer.valueOf(intentRequest.getIntent().getSlots().get(SLOT_CONTACT_ID).getValue());
        Contact contact = new Contact(id);
        dynamoDbMapper.delete(contact);
        return response(input, CARD_TITLE, "Kontakt wurde geloescht.");
    }

    private Optional<Response> getContactList(HandlerInput input, IntentRequest intentRequest) {
        Slot nextIndex = intentRequest.getIntent().getSlots().get(SLOT_NEXT_INDEX);
        List<Contact> contacts = new ArrayList<>(dynamoDbMapper.loadAll(Contact.class));
        if (contacts == null || contacts.size() == 0) {
            return response(input, CARD_TITLE, "Du hast keine Kontakte in deiner Kontaktliste.");
        }

        if (nextIndex.getValue() == null) {
            //read contact by limit for the first time
            StringBuilder builder = new StringBuilder("Du hast ingesamt " + contacts.size() + " Kontakte. ");
            for (int i = 0; i < CONTACT_LIMIT; i++) {
                if (i < contacts.size()) {
                    Contact contact = contacts.get(i);
                    builder.append(" Kontakt ").append(contact.getId())
                            .append(" Name ").append(contact.getName())
                            .append(" IBAN ").append(DialogUtil.getIbanSsmlOutput(contact.getIban())).append(".");
                }
            }
            if (contacts.size() > CONTACT_LIMIT) {
                builder.append("Willst du noch weitere Kontakte hören?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(CONTACT_LIMIT))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            //read more if confirmed
            int position = Integer.parseInt(nextIndex.getValue());
            StringBuilder builder = new StringBuilder();
            Contact contact = contacts.get(position);
            builder.append(" Kontakt ").append(contact.getId())
                    .append(" Name ").append(contact.getName())
                    .append(" IBAN ").append(DialogUtil.getIbanSsmlOutput(contact.getIban()));
            position++;
            if (position < contacts.size()) {
                builder.append("Willst du noch weitere Kontakte hören?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(position))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else {
            return response(input, CARD_TITLE);
        }
    }
}