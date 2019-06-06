package amosalexa.handlers.budgettracker;

import amosalexa.handlers.Service;
import api.aws.DynamoDbMapper;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import model.db.Category;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithIntentConfirm;

@Service(
        functionGroup = Service.FunctionGroup.BUDGET_TRACKING,
        functionName = "Alle Kategorien verwalten",
        example = "Zeig mir meine Kategorien!",
        description = "Mit dieser Funktion kannst du Kategorien ausgeben lassen, Kategorien erstellen und Kategorien löschen."
)
public class EditCategoriesServiceHandler implements IntentRequestHandler {
    public static final String ADD_CATEGORY_INTENT = "AddCategoryIntent";
    public static final String SHOW_CATEGORIES_INTENT = "ShowCategoriesIntent";
    public static final String DELETE_CATEGORY_INTENT = "DeleteCategoryIntent";

    private static final String CARD_TITLE = "Kategorien verwalten";
    private static final String SLOT_CATEGORY_NAME = "CategoryName";
    private static final String SAVE_CATEGORY_ID = "save_category_id";
    private static final String SAVE_CATEGORY_NAME = "save_category_name";


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(ADD_CATEGORY_INTENT))
                || input.matches(Predicates.intentName(SHOW_CATEGORIES_INTENT))
                || input.matches(Predicates.intentName(DELETE_CATEGORY_INTENT))
                || isNumberIntentForPass(input, ADD_CATEGORY_INTENT, SHOW_CATEGORIES_INTENT, DELETE_CATEGORY_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;
        Intent intent = getRealIntent(input, intentRequest);

        switch (intent.getName()) {
            case SHOW_CATEGORIES_INTENT:
                return showCategories(input);
            case ADD_CATEGORY_INTENT:
                if (intent.getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return addCategory(intent, input);
                }
            default:
                //DELETE_CATEGORY_INTENT
                switch (intent.getConfirmationStatus()) {
                    case NONE:
                        return askDelete(intent, input);
                    default:
                        //CONFIRMED
                        return deleteCategory(input);
                }
        }
    }

    private Optional<Response> askDelete(Intent intent, HandlerInput inputR) {
        String category = intent.getSlots().get(SLOT_CATEGORY_NAME).getValue();
        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class);

        int closestDist = Integer.MAX_VALUE;
        Category closestCategory = null;

        for (Category categoryIter : categories) {
            int dist = StringUtils.getLevenshteinDistance(category, categoryIter.getName());
            if (dist < closestDist) {
                closestDist = dist;
                closestCategory = categoryIter;
            }
        }

        if (closestDist > 5) {
            return response(inputR, CARD_TITLE, "Ich konnte keine passende Kategorie finden.");
        }

        Map<String, Object> sessionAttributes = inputR.getAttributesManager().getSessionAttributes();
        sessionAttributes.put(SAVE_CATEGORY_ID, closestCategory.getId());
        sessionAttributes.put(SAVE_CATEGORY_NAME, closestCategory.getName());
        inputR.getAttributesManager().setSessionAttributes(sessionAttributes);

        return responseWithIntentConfirm(inputR, CARD_TITLE, "Möchtest du die Kategorie mit dem Namen '"
                + closestCategory.getName()
                + "' und dem Limit von "
                + closestCategory.getLimit()
                + " Euro wirklich löschen?", intent);
    }

    private Optional<Response> deleteCategory(HandlerInput inputR) {
        Map<String, Object> sessionAttributes = inputR.getAttributesManager().getSessionAttributes();
        String id = (String) sessionAttributes.get(SAVE_CATEGORY_ID);
        String name = (String) sessionAttributes.get(SAVE_CATEGORY_NAME);
        DynamoDbMapper.getInstance().delete(new Category().setId(id));
        return response(inputR, CARD_TITLE, "OK, wie du willst. Ich habe die Kategorie mit dem Namen '" + name + "' gelöscht.");
    }


    private Optional<Response> addCategory(Intent intent, HandlerInput inputR) {
        String categoryName = intent.getSlots().get("CategoryName").getValue();

        List<Category> items = DynamoDbMapper.getInstance().loadAll(Category.class);
        categoryName = categoryName.toLowerCase();

        for (Category item : items) {
            if (item.getName().equalsIgnoreCase(categoryName)) {
                return response(inputR, CARD_TITLE, "Diese Kategorie existiert bereits.");
            }
        }
        Category item = new Category(categoryName);
        DynamoDbMapper.getInstance().save(item);
        return response(inputR, CARD_TITLE, "Verstanden. Die Kategorie " + categoryName + " wurde erstellt.");
    }

    private Optional<Response> showCategories(HandlerInput inputR) {

        List<Category> items = dynamoDbMapper.loadAll(Category.class);
        String namesOfCategories = "";

        if (items.size() == 0) {
            return response(inputR, CARD_TITLE, "Aktuell hast du keine Kategorien.");
        }

        for (Category item : items) {
            namesOfCategories += item.getName() + ", ";
        }

        return response(inputR, CARD_TITLE, "Aktuell hast du folgende Kategorien: " + namesOfCategories);
    }
}
