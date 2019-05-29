package amosalexa.handlers.financing;

import amosalexa.handlers.Service;
import amosalexa.handlers.financing.aws.model.Item;
import amosalexa.handlers.financing.aws.util.AWSUtil;
import api.aws.EMailClient;
import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;
import model.banking.Account;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.ResponseHelper.*;

@Service(
        functionGroup = Service.FunctionGroup.SMART_FINANCING,
        functionName = "Produktbezahlbarkeit",
        example = "Was kostet ein Samsung Galaxy?",
        description = "Mit dieser Funktion kannst du ein Produkt auf Amazon suchen, den Preis überprüfen und per Email" +
                "vormerken lassen. Du kannst dabei den Parameter " +
                "Suchwort definieren."
)
public class AffordabilityServiceHandler implements IntentRequestHandler {
    private final static String AFFORD_INTENT = "AffordProduct";

    private static final String SLOT_PRODUCT_KEYWORD = "ProductKeyword";
    private static final String SLOT_PRODUCT_SELECTION = "ProductSelection";

    private static final String CARD_TITLE = "Produkt Checken";
    private static final String SAVE_NAME = "save_name";
    private static final String SAVE_URL = "save_url";
    private static List<Item> items;


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(AFFORD_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Map<String, Slot> slots = intentRequest.getIntent().getSlots();
        String keyword = slots.get(SLOT_PRODUCT_KEYWORD).getValue();
        Slot productSelection = slots.get(SLOT_PRODUCT_SELECTION);

        if (productSelection.getValue() == null) {
            // show product selection
            //items = AWSLookup.itemSearch(keyword, 1, null);

            responseDirective(input, "Ich suche jetzt nach dem " + keyword + " .Bitte warte kurz.");
            // TODO demo item for test alexa because Amazon Advertising API not work yet
            Item item1 = new Item();
            Item item2 = new Item();
            Item item3 = new Item();
            items = Arrays.asList(item1, item2, item3);
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setTitle("Helloworld item " + i);
                items.get(i).setLowestNewPrice(100 + i);
                items.get(i).setDetailPageURL("https://senacor.com/");
            }


            if (items == null || items.isEmpty()) {
                return response(input, CARD_TITLE, "Die Suche ergab keine Ergebnisse");
            }
            items.removeIf(e -> e.getLowestNewPrice() == null);
            if (items.isEmpty()) {
                return response(input, CARD_TITLE, "Die Suche ergab keine Ergebnisse");
            }

            StringBuilder text = new StringBuilder("Ich habe folgende Produkten gefunden. Bitte wähle deine wünsche Produktnummer: ");
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                String productTitle = AWSUtil.shortTitle(item.getTitle());
                item.setTitleShort(productTitle);
                text.append("Produktnummer ")
                        .append(i + 1).append(": ")
                        .append("<break time=\"1s\"/> ")
                        .append(productTitle)
                        .append(" kostet <say-as interpret-as=\"unit\">€")
                        .append(item.getLowestNewPrice() / 100) //why /100 in old code?
                        .append("</say-as> <break time=\"1s\"/>.");
            }
            return responseElicitDelegate(input, CARD_TITLE, text.toString(), intentRequest.getIntent(), SLOT_PRODUCT_SELECTION);

        } else if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.NONE) {
            // get selection
            int selectIndex = Integer.valueOf(slots.get(SLOT_PRODUCT_SELECTION).getValue()) - 1;
            if (selectIndex < 0 || selectIndex >= items.size()) {
                return responseElicitDelegate(input, CARD_TITLE, "Bitte wähle Nummer zwischen 1 und " + items.size()
                        , intentRequest.getIntent(), SLOT_PRODUCT_SELECTION);

            }
            Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);
            account.setSpeechTexts();
            if (items.get(selectIndex).getLowestNewPrice() / 100 > account.getBalance().doubleValue()) {
                return response(input, CARD_TITLE, "Das Produkt " + items.get(selectIndex).getTitleShort() + " kannst du dir nicht leisten. " + account.getBalanceText());
            } else {
                Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
                sessionAttributes.put(SAVE_NAME, items.get(selectIndex).getTitleShort());
                sessionAttributes.put(SAVE_URL, items.get(selectIndex).getDetailPageURL());
                input.getAttributesManager().setSessionAttributes(sessionAttributes);
                return responseWithIntentConfirm(input, CARD_TITLE,
                        "Soll ich dir eine Produktlink per Email schicken?", intentRequest.getIntent());
            }
        } else if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
            //send email
            Map<String, Object> sessionAtt = input.getAttributesManager().getSessionAttributes();
            EMailClient.SendEMail("Amazon Produkt: " + sessionAtt.get(SAVE_NAME), (String) sessionAtt.get(SAVE_URL));
            return response(input, CARD_TITLE, "Ich habe eine Link per Email geschickt");
        } else {
            return response(input, CARD_TITLE);
        }
    }
}
