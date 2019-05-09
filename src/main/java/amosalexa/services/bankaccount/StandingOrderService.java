package amosalexa.services.bankaccount;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbMapper;
import api.aws.EMailClient;
import api.banking.AccountAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.banking.StandingOrder;
import model.db.Contact;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service(
        functionName = "Daueraufträge verwalten",
        functionGroup = HelpService.FunctionGroup.ONLINE_BANKING,
        example = "Wie lauten meine Daueraufträge?",
        description = "Diese Funktion ermöglicht dir das Verwalten von Daueraufträgen."
)
public class StandingOrderService extends AbstractSpeechService implements SpeechService {

    /**
     * Default value for cards
     */
    private static final String STANDING_ORDERS = "Daueraufträge";

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                STANDING_ORDERS_INFO_INTENT,
                STANDING_ORDERS_DELETE_INTENT,
                STANDING_ORDERS_MODIFY_INTENT,
                STANDING_ORDERS_KEYWORD_INTENT,
                STANDING_ORDERS_SMART_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                STANDING_ORDERS_INFO_INTENT,
                STANDING_ORDERS_DELETE_INTENT,
                STANDING_ORDERS_MODIFY_INTENT,
                STANDING_ORDERS_KEYWORD_INTENT,
                STANDING_ORDERS_SMART_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    private static final String STANDING_ORDERS_INFO_INTENT = "StandingOrdersInfoIntent";
    private static final String STANDING_ORDERS_DELETE_INTENT = "StandingOrdersDeleteIntent";
    private static final String STANDING_ORDERS_MODIFY_INTENT = "StandingOrdersModifyIntent";
    private static final String STANDING_ORDERS_KEYWORD_INTENT = "StandingOrdersKeywordIntent";
    private static final String STANDING_ORDERS_SMART_INTENT = "StandingOrderSmartIntent";

    // FIXME: Get the current account number from the session
    private static final String ACCOUNT_NUMBER = AmosAlexaSpeechlet.ACCOUNT_ID; //"9999999999";

    private static final Logger LOGGER = LoggerFactory.getLogger(StandingOrderService.class);

    private List<StandingOrder> standingOrders;

    public StandingOrderService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    /**
     * Ties the Speechlet Subject (Amos Alexa Speechlet) with an Speechlet Observer
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
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        Intent intent = requestEnvelope.getRequest().getIntent();
        String intentName = intent.getName();
        Session session = requestEnvelope.getSession();
        String context = (String) session.getAttribute(DIALOG_CONTEXT);

        LOGGER.info("Intent Name: " + intentName);
        LOGGER.info("Context: " + context);

        if (STANDING_ORDERS_INFO_INTENT.equals(intentName)) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, "StandingOrderInfo");
            return getStandingOrdersInfoResponse(intent, session);
        } else if (STANDING_ORDERS_DELETE_INTENT.equals(intentName)) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, "StandingOrderDeletion");
            return askForDDeletionConfirmation(intent, session);
        } else if (STANDING_ORDERS_MODIFY_INTENT.equals(intentName)) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, "StandingOrderModification");
            return askForModificationConfirmation(intent, session);
        } else if (STANDING_ORDERS_KEYWORD_INTENT.equals(intentName)) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, "StandingOrderKeyword");
            return getStandingOrdersInfoForKeyword(intent, session);
        } else if (STANDING_ORDERS_SMART_INTENT.equals(intentName) && session.getAttribute("StandingOrderToModify") == null) {
            LOGGER.info(getClass().toString() + " Intent started: " + intentName);
            session.setAttribute(DIALOG_CONTEXT, "StandingOrderSmartIntent");
            return smartUpdateStandingOrderConfirmation(intent, session);
        } else if (YES_INTENT.equals(intentName) && context != null && (context.equals("StandingOrderSmartIntent"))) {
            return smartUpdateStandingOrderResponse(session);
        } else if (context != null && (context.equals("StandingOrderSmartIntent")) && session.getAttribute("StandingOrderToModify") != null) {
            return  smartCreateStandingOrderResponse(session);
        } else if (YES_INTENT.equals(intentName) && context != null && (context.equals("StandingOrderInfo"))) {
            return getNextStandingOrderInfo(session);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals("StandingOrderDeletion")) {
            return getStandingOrdersDeleteResponse(intent, session);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals("StandingOrderKeyword")) {
            return getStandingOrderKeywordResultsInfo(session);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals("StandingOrderModification")) {
            return getStandingOrdersModifyResponse(intent, session);
        } else if (NO_INTENT.equals(intentName) && context != null && !(context.equals("StandingOrderDeletion") || context.equals("StandingOrderModification"))) {
            //General NoIntent for StandingOrderInfo (if asked 'Moechtest du einen weiteren Eintrag hoeren?')
            //Simply quit session with 'Tschuess'
            return getResponse(STANDING_ORDERS, "Okay, tschuess!");
        } else if (NO_INTENT.equals(intentName) && context != null && (context.equals("StandingOrderDeletion") || context.equals("StandingOrderModification"))) {
            //Specific NoIntent for Modification or Deletion
            //Ask for a correction afterwards
            return getCorrectionResponse(intent, session);
        } else {
            return null;
        }
    }

    /**
     * Creates a {@code SpeechletResponse} for the standing orders intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getStandingOrdersInfoResponse(Intent intent, Session session) {
        LOGGER.info("StandingOrdersResponse called.");

        Map<String, Slot> slots = intent.getSlots();

        Collection<StandingOrder> standingOrdersCollection = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Daueraufträge");

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

        if (standingOrdersCollection == null || standingOrdersCollection.isEmpty()) {
            card.setContent("Keine Daueraufträge vorhanden.");
            speech.setText("Keine Dauerauftraege vorhanden.");
            return SpeechletResponse.newTellResponse(speech, card);
        }

        standingOrders = new ArrayList<>(standingOrdersCollection);

        // Check if user requested to have their stranding orders sent to their email address
        Slot channelSlot = slots.get("Channel");
        boolean sendPerEmail = channelSlot != null &&
                channelSlot.getValue() != null &&
                channelSlot.getValue().toLowerCase().replace(" ", "").equals("email");

        StringBuilder builder = new StringBuilder();

        if (sendPerEmail) {
            StringBuilder standingOrderMailBody = new StringBuilder();
            for(StandingOrder so : standingOrders) {
                standingOrderMailBody.append(so.getSpeechOutput() + "\n");
            }
            EMailClient.SendEMail("Daueraufträge", standingOrderMailBody.toString());

            builder.append("Ich habe eine Übersicht deiner ")
                    .append(standingOrders.size())
                    .append(" Daueraufträge an deine E-Mail-Adresse gesendet.");
        } else {
            // We want to directly return standing orders here

            Slot payeeSlot = slots.get("Payee");
            String payee = (payeeSlot == null ? null : payeeSlot.getValue());

            if (payee != null) {
                // User specified a recipient
                standingOrders.clear();

                // Find closest standing orders that could match the request.
                for (int i = 0; i < standingOrders.size(); i++) {
                    if (StringUtils.getLevenshteinDistance(payee, standingOrders.get(i).getPayee()) <=
                            standingOrders.get(i).getPayee().length() / 3) {
                        standingOrders.add(standingOrders.get(i));
                    }
                }

                builder.append(standingOrders.size() == 1 ? "Es wurde ein Dauerauftrag gefunden. " :
                        "Es wurden " + standingOrders.size() +
                                " Dauerauftraege gefunden. ");

                for (int i = 0; i <= 1; i++) {
                    builder.append(standingOrders.get(i).getSpeechOutput());
                }

                if (standingOrders.size() > 2) {
                    return askForFurtherStandingOrderEntry(session, builder, 2);
                }
            } else {
                // Just return all standing orders

                builder.append("Du hast momentan ")
                        .append(standingOrders.size() == 1 ? "einen Dauerauftrag. " : standingOrders.size() + " Dauerauftraege. ");

                for (int i = 0; i <= 1; i++) {
                    builder.append(standingOrders.get(i).getSpeechOutput());
                }

                if (standingOrders.size() > 2) {
                    return askForFurtherStandingOrderEntry(session, builder, 2);
                }
            }
        }

        String text = builder.toString();
        card.setContent(text);
        speech.setText(text);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    private SpeechletResponse getStandingOrdersInfoForKeyword(Intent intent, Session session) {
        Map<String, Slot> slots = intent.getSlots();
        Slot standingOrderKeywordSlot = slots.get("StandingOrderKeyword");
        Slot transactionKeywordSlot = slots.get("TransactionKeyword");

        if ((standingOrderKeywordSlot == null || standingOrderKeywordSlot.getValue() == null) &&
                (transactionKeywordSlot == null || transactionKeywordSlot.getValue() == null)) {
            //TODO
            return null;
        }

        String keyword = standingOrderKeywordSlot.getValue() != null ? standingOrderKeywordSlot.getValue() : transactionKeywordSlot.getValue();
        LOGGER.info("Keyword: " + keyword);

        if (keyword.equals("ersparnisse")) {
            keyword = "sparplan regelm. einzahlung";
        }

        Collection<StandingOrder> standingOrdersCollection = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);
        standingOrders = new ArrayList<>();

        for (StandingOrder so : standingOrdersCollection) {
            String description = so.getDescription().toLowerCase();
            //LOGGER.info("Description: " + description);
            if (!description.equals(keyword)) {
                String[] singleWords = description.split("\\s+");
                for (String s : singleWords) {
                    if (keyword.equals(s)) {
                        standingOrders.add(so);
                        continue;
                    }
                    double similarity = getStringSimilarity(s, keyword);
                    //LOGGER.info("Similarity: " + s + ", " + similarity);

                    //0.9 seems to be a good value for similarity
                    if (similarity >= 0.9) {
                        standingOrders.add(so);
                    }
                }
            } else {
                standingOrders.add(so);
                continue;
            }
        }

        if (standingOrders.size() == 0) {
            String answer = "Ich konnte keine Dauerauftraege finden, die zu diesem Stichwort passen.";
            return getResponse(STANDING_ORDERS, answer);
        }

        double total = 0;
        for (StandingOrder so : standingOrders) {
            if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.MONTHLY)) {
                total += so.getAmount().doubleValue();
            } else if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.QUARTERLY)) {
                total += so.getAmount().doubleValue() / 3;
            } else if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.HALF_YEARLY)) {
                total += so.getAmount().doubleValue() / 6;
            } else if (so.getExecutionRate().equals(StandingOrder.ExecutionRate.YEARLY)) {
                total += so.getAmount().doubleValue() / 12;
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<speak>");
        builder.append("Aus ").append(standingOrders.size() == 1 ? "<phoneme alphabet=\"ipa\" ph=\"ˈaɪ̯nəm\">einem</phoneme>" +
                " gefundenen Dauerauftrag " :
                standingOrders.size() + " gefundenen Dauerauftraegen ")
                .append("konnte ich berechnen, dass du monatlich " + total + " Euro ")
                .append(keyword.equals("sparplan regelm. einzahlung") ? "zum Sparen zuruecklegst. " : "fuer " + keyword + " bezahlst. ");
        builder.append("Soll ich die gefundenen Dauerauftraege aufzaehlen? ");
        builder.append("</speak>");

        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml(builder.toString());

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    private SpeechletResponse getStandingOrderKeywordResultsInfo(Session session) {
        session.setAttribute(DIALOG_CONTEXT, "StandingOrderInfo");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= 1; i++) {
            builder.append(standingOrders.get(i).getSpeechOutput());
        }
        if (standingOrders.size() > 2) {
            return askForFurtherStandingOrderEntry(session, builder, 2);
        } else {
            builder.append("Das waren alle vorhandenen Dauerauftraege. ");
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(builder.toString());
            return SpeechletResponse.newTellResponse(speech);
        }
    }

    private static double getStringSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
    }

    private SpeechletResponse askForFurtherStandingOrderEntry(Session session, StringBuilder builder, int nextStandingOrder) {
        builder.append("Moechtest du einen weiteren Eintrag hoeren? ");
        String text = builder.toString();
        // Save current list offset in this session
        session.setAttribute("NextStandingOrder", nextStandingOrder);
        return getAskResponse(STANDING_ORDERS, text);
    }

    private SpeechletResponse getNextStandingOrderInfo(Session session) {
        int nextEntry = (int) session.getAttribute("NextStandingOrder");
        StringBuilder builder = new StringBuilder();

        if (nextEntry < standingOrders.size()) {
            builder.append(standingOrders.get(nextEntry).getSpeechOutput());

            if (nextEntry == (standingOrders.size() - 1)) {
                builder.append("Das waren alle vorhandenen Dauerauftraege. ");
                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
                speech.setText(builder.toString());
                return SpeechletResponse.newTellResponse(speech);
            } else {
                return askForFurtherStandingOrderEntry(session, builder, nextEntry + 1);
            }
        } else {
            // Create the plain text output
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText("Das waren alle vorhandenen Dauerauftraege. ");

            return SpeechletResponse.newTellResponse(speech);
        }
    }

    private SpeechletResponse askForDDeletionConfirmation(Intent intent, Session session) {
        Map<String, Slot> slots = intent.getSlots();
        Slot numberSlot = slots.get("Number");
        LOGGER.info("NumberSlot: " + numberSlot.getValue());

        session.setAttribute("StandingOrderToDelete", numberSlot.getValue());

        // Create the plain text output
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Moechtest du den Dauerauftrag mit der Nummer " + numberSlot.getValue()
                + " wirklich loeschen?");

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    private SpeechletResponse askForModificationConfirmation(Intent intent, Session session) {
        Map<String, Slot> slots = intent.getSlots();
        Slot numberSlot = slots.get("Number");
        Slot amountSlot = slots.get("Amount");
        Slot executionRateSlot = slots.get("ExecutionRate");
        Slot firstExecutionSlot = slots.get("FirstExecution");

        if (numberSlot.getValue() == null || (amountSlot.getValue() == null && executionRateSlot.getValue() == null && firstExecutionSlot.getValue() == null)) {
            String text = "Das habe ich nicht ganz verstanden. Bitte wiederhole deine Eingabe.";
            return getAskResponse(STANDING_ORDERS, text);
        }

        session.setAttribute("StandingOrderToModify", numberSlot.getValue());
        session.setAttribute("NewAmount", amountSlot.getValue());
        session.setAttribute("NewExecutionRate", executionRateSlot.getValue());
        session.setAttribute("NewFirstExecution", firstExecutionSlot.getValue());

        // Create the plain text output
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Soll ich den Betrag von Dauerauftrag Nummer " + numberSlot.getValue() + " wirklich auf " +
                amountSlot.getValue() + " Euro aendern?");
        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    private SpeechletResponse getStandingOrdersDeleteResponse(Intent intent, Session session) {
        LOGGER.info("StandingOrdersDeleteResponse called.");

        String standingOrderToDelete = (String) session.getAttribute("StandingOrderToDelete");

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Lösche Dauerauftrag");

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

        Number standingOrderNum = Integer.parseInt(standingOrderToDelete);

        if (!AccountAPI.deleteStandingOrder(ACCOUNT_NUMBER, standingOrderNum)) {
            card.setContent("Dauerauftrag Nummer " + standingOrderToDelete + " wurde nicht gefunden.");
            speech.setText("Dauerauftrag Nummer " + standingOrderToDelete + " wurde nicht gefunden.");
            return SpeechletResponse.newTellResponse(speech, card);
        }

        card.setContent("Dauerauftrag Nummer " + standingOrderToDelete + " wurde gelöscht.");
        speech.setText("Dauerauftrag Nummer " + standingOrderToDelete + " wurde geloescht.");
        return SpeechletResponse.newTellResponse(speech, card);
    }

    private SpeechletResponse getStandingOrdersModifyResponse(Intent intent, Session session) {
        String standingOrderToModify = (String) session.getAttribute("StandingOrderToModify");
        String newAmount = (String) session.getAttribute("NewAmount");
        //TODO never used
        String newExecutionRate = (String) session.getAttribute("NewExecutionRate");
        String newFirstExecution = (String) session.getAttribute("NewFirstExecution");

        ObjectMapper mapper = new ObjectMapper();

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Ändere Dauerauftrag");

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

        Number standingOrderNum = Integer.parseInt(standingOrderToModify);
        StandingOrder standingOrder = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, standingOrderNum);

        // TODO: Actually update the StandingOrder
        standingOrder.setAmount(Integer.parseInt(newAmount));

        AccountAPI.updateStandingOrder(ACCOUNT_NUMBER, standingOrder);

        card.setContent("Dauerauftrag Nummer " + standingOrderToModify + " wurde geändert.");
        speech.setText("Dauerauftrag Nummer " + standingOrderToModify + " wurde geaendert.");
        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} that asks the user if he wants to correct his previous input.
     * Should be called after an Amazon.NoIntent
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getCorrectionResponse(Intent intent, Session session) {
        String dialogContext = (String) session.getAttribute(DIALOG_CONTEXT);
        LOGGER.info("Context: " + dialogContext);
        Map<String, Slot> slots = intent.getSlots();
        LOGGER.info("Slots: " + slots);
        String standingOrderToModify = (String) session.getAttribute("StandingOrderToModify");

        //Default
        String answer = "Okay, bitte wiederhole die Anweisung oder breche den Vorgang ab, indem du \"Alexa, Stop!\" sagst.";

        if (dialogContext.equals("StandingOrderDeletion")) {
            answer = "Okay, sage die Nummer des Dauerauftrags, den du stattdessen loeschen willst oder " +
                    "breche den Vorgang ab, indem du \"Alexa, Stop!\" sagst.";
        } else if (dialogContext.equals("StandingOrderModification")) {
            answer = "Okay, den Betrag, auf den du Dauerauftrag Nummer " + standingOrderToModify + " stattdessen" +
                    " aendern willst, wiederhole die gesamte Aenderungsanweisung oder breche den Vorgang ab, indem du" +
                    " \"Alexa, Stop!\" sagst.";
        }
        answer = "<speak>\n" +
                "    <emphasis level=\"reduced\">" + answer + "</emphasis> \n" +
                "</speak>";
        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml(answer);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    /**
     * Creates a {@code SpeechletResponse} for the standing orders intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse smartUpdateStandingOrderConfirmation(Intent intent, Session session) {
        LOGGER.info("SmartStandingOrders called.");

        Map<String, Slot> slots = intent.getSlots();

        Collection<StandingOrder> standingOrdersCollection = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);

        standingOrders = new ArrayList<>(standingOrdersCollection);

        SimpleCard card = new SimpleCard();
        card.setTitle("Daueraufträge");

        Slot payeeSlot = slots.get("Payee");
        String payee = (payeeSlot == null ? null : payeeSlot.getValue());

        Slot payeeSecondNameSlot = slots.get("PayeeSecondName");
        String payeeSecondName = (payeeSecondNameSlot == null ? null : payeeSecondNameSlot.getValue());

        Slot amountSlot = slots.get("orderAmount");
        String amount = (amountSlot == null ? null : amountSlot.getValue());
        String payeeFullName;
        if (payee == null) {
            payeeFullName = payeeSecondName.toLowerCase();
        } else if (payeeSecondName == null) {
            payeeFullName = payee.toLowerCase();
        } else {
            payeeFullName = (payee + " " + payeeSecondName).toLowerCase();
        }
        LOGGER.info("full name: " + payeeFullName);

        session.setAttribute("NewAmount", amount);
        session.setAttribute("Payee", payee);
        session.setAttribute("PayeeSecondName", payeeSecondName);

        for (StandingOrder standingOrder : standingOrders) {

            String amountString = Integer.toString(standingOrder.getAmount().intValue());
            LOGGER.info(standingOrder.getPayee().toLowerCase() + ": " + payeeFullName);

            if (standingOrder.getPayee().toLowerCase().equals(payeeFullName)) {
                //getting data object
                DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
                Date executionDate;
                try {
                    executionDate = dateFormat.parse(standingOrder.getFirstExecution());
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }


                // Create the plain text output
                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
                speech.setText("Der Dauerauftrag für " + payeeFullName + " über " + standingOrder.getAmount() +
                        " Euro existiert schon. Möchtest du diesen aktualisieren");

                session.setAttribute("StandingOrderToModify", standingOrder.getStandingOrderId());

                // Create reprompt
                Reprompt reprompt = new Reprompt();
                reprompt.setOutputSpeech(speech);

                return SpeechletResponse.newAskResponse(speech, reprompt);
            }
        }

        //creating a new stating order if its in contact list
        List<Contact> contactList = DynamoDbMapper.getInstance().loadAll(Contact.class);
        List<Contact> contacts = new ArrayList<>(contactList);
        for (int i = 0; i < contacts.size(); i++) {
            LOGGER.info(contacts.get(i).getName().toString().toLowerCase());
            if (contacts.get(i).getName().toString().toLowerCase().equals(payeeFullName)) {
                StandingOrder standingOrder = new StandingOrder();
                standingOrder.setPayee(payeeFullName);
                standingOrder.setAmount(Integer.parseInt(amount));
                standingOrder.setDestinationAccount(contacts.get(i).getIban());
                standingOrder.setFirstExecution("09-09-2017");
                standingOrder.setDestinationAccount("DE39100000007777777777");
                AccountAPI.createStandingOrderForAccount(ACCOUNT_NUMBER, standingOrder);

                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
                speech.setText("Ich habe den neuen Dauerauftrag für" + payeeFullName +
                         " über " + amount + " Euro erfolgreich eingerichtet");

                //deleting attributes
                session.removeAttribute("NewAmount");
                session.removeAttribute("Payee");
                session.removeAttribute("PayeeSecondName");

                return SpeechletResponse.newTellResponse(speech, card);
            }
        }

        // Contact not found
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Ich habe " + payeeFullName + " in deiner Kontaktliste nicht gefunden. " +
                "Du musst ihm erst in der Kontaktliste hinzufügen");

        //deleting attributes
        session.removeAttribute("NewAmount");
        session.removeAttribute("Payee");
        session.removeAttribute("PayeeSecondName");

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the standing orders intent. (add new value to old value)
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse smartUpdateStandingOrderResponse(Session session) {
        LOGGER.info("SmartStandingOrders Confirmation called.");
        int standingOrderToModify = (int) session.getAttribute("StandingOrderToModify");
        String newAmount = (String) session.getAttribute("NewAmount");
        int orderAmount = Integer.parseInt(newAmount);

        //deleting old orders
        Collection<StandingOrder> standingOrdersCollection = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);
        standingOrders = new ArrayList<>(standingOrdersCollection);
        String payeeFullName = AccountAPI.getStandingOrder(ACCOUNT_NUMBER,standingOrderToModify).getPayee().toLowerCase();
        String firstExecution = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, standingOrderToModify).getFirstExecution().toString();

        for (int i = 0; i < standingOrders.size(); i++) {
            if (standingOrders.get(i).getStandingOrderId().intValue() != standingOrderToModify &&
                    standingOrders.get(i).getPayee().toLowerCase().equals(payeeFullName) &&
                    standingOrders.get(i).getFirstExecution().toString().equals(firstExecution)) {
                orderAmount +=  standingOrders.get(i).getAmount().intValue();
                AccountAPI.deleteStandingOrder(ACCOUNT_NUMBER, standingOrders.get(i).getStandingOrderId().intValue());
            }
        }

        StandingOrder standingOrder = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, standingOrderToModify);

        standingOrder.setAmount(orderAmount);

        AccountAPI.updateStandingOrder(ACCOUNT_NUMBER, standingOrder);

        String speechText = "Der Dauerauftrag Nummer " + standingOrderToModify +
                " für " + standingOrder.getPayee() + " über " + orderAmount + " euro wurde erfolgreich aktualisiert";

        //delete session attributes
        session.removeAttribute("SmartCreateStandingOrderIntent");
        session.removeAttribute("StandingOrderToModify");
        session.removeAttribute("NewAmount");

        return getAskResponse(STANDING_ORDERS, speechText);
    }

    /**
     * Creates a {@code SpeechletResponse} for the standing orders intent. (reset the amount with new value)
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse smartCreateStandingOrderResponse(Session session) {
        SimpleCard card = new SimpleCard();
        card.setTitle("Daueraufträge");

        LOGGER.info("SmartStandingOrders create called.");
        int standingOrderToModify = (int) session.getAttribute("StandingOrderToModify");
        String newAmount = (String) session.getAttribute("NewAmount");

        StandingOrder oldStandingOrder = AccountAPI.getStandingOrder(ACCOUNT_NUMBER, standingOrderToModify);

        StandingOrder standingOrder = new StandingOrder();
        standingOrder.setAmount(Integer.parseInt(newAmount));
        standingOrder.setDescription("description");
        standingOrder.setDestinationAccount(oldStandingOrder.getDestinationAccount());
        standingOrder.setExecutionRate(oldStandingOrder.getExecutionRate());
        standingOrder.setFirstExecution(oldStandingOrder.getFirstExecution());
        standingOrder.setPayee(oldStandingOrder.getPayee());
        standingOrder.setSourceAccount(oldStandingOrder.getSourceAccount());
        standingOrder.setStatus(oldStandingOrder.getStatus());

        AccountAPI.createStandingOrderForAccount(ACCOUNT_NUMBER, standingOrder);

        // Create the plain text output
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Der neue Dauerauftrag für " + oldStandingOrder.getPayee().toLowerCase() + " über " + newAmount +
                " Euro wurde erfolgreich eingerichtet");

        //delete session attributes
        session.removeAttribute("SmartCreateStandingOrderIntent");
        session.removeAttribute("StandingOrderToModify");
        session.removeAttribute("NewAmount");


        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newTellResponse(speech, card);
    }
}