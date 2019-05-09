package amosalexa.handlers.budgettracker;


import amosalexa.handlers.Service;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;
import model.db.Category;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithIntentConfirm;

@Service(
        functionGroup = Service.FunctionGroup.BUDGET_TRACKING,
        functionName = "Einzelne Kategorie verwalten",
        example = "Wie ist das Limit für Lebensmittel?",
        description = "Mit dieser Funktion kannst du das Limit für eine Kategorie ändern oder den Status einer Kategorie abfragen."
)
public class BudgetTrackerServiceHandler implements IntentRequestHandler {
    public static final String CATEGORY_LIMIT_INFO_INTENT = "CategoryLimitInfoIntent";
    public static final String CATEGORY_STATUS_INFO_INTENT = "CategoryStatusInfoIntent";
    public static final String CATEGORY_LIMIT_SET_INTENT = "CategoryLimitSetIntent";
    public static final String CATEGORY_SPENDING_INTENT = "CategorySpendingIntent";

    public static final String SLOT_CATEGORY = "Category";
    public static final String SLOT_AMOUNT = "Amount";
    public static final String SLOT_CATEGORY_LIMIT = "CategoryLimit";

    private static final String CARD_TITLE = "Budget-Tracker";

    private static final String SAVE_AMOUNT = "save_amount";
    private static final String SAVE_CATEGORY = "save_category";

    private static HandlerInput inputR;
    private static IntentRequest intentRequestR;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(CATEGORY_LIMIT_INFO_INTENT))
                || (input.matches(Predicates.intentName(CATEGORY_LIMIT_SET_INTENT)))
                || input.matches(Predicates.intentName(CATEGORY_STATUS_INFO_INTENT))
                || input.matches(Predicates.intentName(CATEGORY_SPENDING_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        inputR = input;
        intentRequestR = intentRequest;
        switch (intentRequestR.getIntent().getName()) {
            case CATEGORY_LIMIT_INFO_INTENT:
                return getCategoryLimitInfo();
            case CATEGORY_LIMIT_SET_INTENT:
                if (intentRequestR.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return setCategoryLimit();
                } else {
                    return response(inputR, CARD_TITLE);
                }
            case CATEGORY_STATUS_INFO_INTENT:
                return getCategoryStatusInfo();
            default:
                //CATEGORY_SPENDING_INTENT:
                if (intentRequestR.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    if (inputR.getAttributesManager().getSessionAttributes().containsKey(SAVE_CATEGORY)) {
                        return createNewSpending();
                    } else {
                        return setSpending();
                    }
                } else {
                    return response(inputR, CARD_TITLE);
                }
        }
    }

    private Optional<Response> createNewSpending() {
        Map<String, Object> sessionAttributes = inputR.getAttributesManager().getSessionAttributes();
        String categoryName = (String) sessionAttributes.get(SAVE_CATEGORY);
        double amount = (Double) sessionAttributes.get(SAVE_AMOUNT);
        Category category = new Category(categoryName);
        dynamoDbMapper.save(category);
        return response(inputR, CARD_TITLE, "Ich habe die Kategorie " + categoryName + " erstellt und " + amount + " Euro ");
    }

    private Optional<Response> setSpending() {
        Map<String, Slot> slots = intentRequestR.getIntent().getSlots();
        String categoryName = slots.get(SLOT_CATEGORY).getValue();
        double amount = Double.valueOf(slots.get(SLOT_AMOUNT).getValue());
        List<Category> categories = dynamoDbMapper.loadAll(Category.class);
        for (Category cate : categories) {
            if (cate.getName().equalsIgnoreCase(categoryName)) {
                StringBuilder text = new StringBuilder();

                if (cate.getSpendingPercentage() > 100) {
                    text.append("Warnung! Ich kann diesen Betrag leider nicht speichern. Deine Limit ist ")
                            .append(cate.getLimit())
                            .append("und somit kannst du maximal nur noch ")
                            .append(cate.getLimit() - cate.getSpending())
                            .append(" Euro für diese Kategorie ausgeben");
                    return response(inputR, CARD_TITLE, text.toString());
                }

                BudgetManager.instance.createSpending(ACCOUNT_NUMBER, cate.getId(), amount);
                text.append("Okay. Ich habe " + amount + " Euro fuer "
                        + categoryName + " notiert.");
                if (cate.getSpendingPercentage() >= 90) {
                    text.append(" Warnung! Du hast in diesem Monat bereits "
                            + Math.round(cate.getSpending()) + " Euro fuer " + cate.getName() + " ausgegeben. Das sind "
                            + cate.getSpendingPercentage() + "% des Limits fuer diese Kategorie.");
                }
                return response(inputR, CARD_TITLE, text.toString());
            }
        }

        String speech = "Ich konnte diese Kategorie nicht finden. Soll ich eine Kategorie" +
                " mit dem Namen " + categoryName + " anlegen und den Betrag " + amount + " Euro hinzufuegen?";
        Map<String, Object> sessionAttributes = inputR.getAttributesManager().getSessionAttributes();
        sessionAttributes.put(SAVE_AMOUNT, amount);
        sessionAttributes.put(SAVE_CATEGORY, categoryName);
        inputR.getAttributesManager().setSessionAttributes(sessionAttributes);
        return responseWithIntentConfirm(inputR, CARD_TITLE, speech, intentRequestR.getIntent());
    }

    private Optional<Response> getCategoryStatusInfo() {
        String categoryName = intentRequestR.getIntent().getSlots()
                .get(SLOT_CATEGORY).getValue();
        List<Category> categories = dynamoDbMapper.loadAll(Category.class);
        for (Category cate : categories) {
            if (cate.getName().equalsIgnoreCase(categoryName)) {
                String speechText = "Du hast bereits " + cate.getSpendingPercentage() + "% des Limits von " + cate.getLimit() + " Euro fuer" +
                        " die Kategorie " + cate.getName() + " ausgegeben. " +
                        "Du kannst noch " + (cate.getLimit() - cate.getSpending()) + " Euro für diese Kategorie ausgeben.";
                return response(inputR, CARD_TITLE, speechText);
            }
        }
        return response(inputR, CARD_TITLE, "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder" +
                " erhalte Information zu den verfuegbaren Kategorien.");
    }

    private Optional<Response> setCategoryLimit() {
        Map<String, Slot> slots = intentRequestR.getIntent().getSlots();
        String categoryName = slots.get(SLOT_CATEGORY).getValue();
        double amount = Double.valueOf(slots.get(SLOT_CATEGORY_LIMIT).getValue());

        List<Category> categories = dynamoDbMapper.loadAll(Category.class);
        for (Category cate : categories) {
            if (cate.getName().equalsIgnoreCase(categoryName)) {
                cate.setLimit(amount);
                break;
            }
        }

        Category newCate = new Category(categoryName);
        newCate.setLimit(amount);
        dynamoDbMapper.save(newCate);
        return response(inputR, CARD_TITLE, "Limit für " + categoryName + " wurde auf " + amount + " gesetzt");
    }

    private Optional<Response> getCategoryLimitInfo() {
        String category = intentRequestR.getIntent().getSlots().get(SLOT_CATEGORY).getValue();
        List<Category> categories = dynamoDbMapper.loadAll(Category.class);
        for (Category cate : categories) {
            if (cate.getName().equalsIgnoreCase(category)) {
                String speech = "Das Limit fuer die Kategorie " + cate.getName()
                        + " liegt bei " + cate.getLimit() + " Euro.";
                return response(inputR, CARD_TITLE, speech);
            }
        }

        return response(inputR, CARD_TITLE,
                "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder"
                        + " erhalte Information zu den verfuegbaren Kategorien.");
    }

}
