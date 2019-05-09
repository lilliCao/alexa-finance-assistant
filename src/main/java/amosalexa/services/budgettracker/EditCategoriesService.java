package amosalexa.services.budgettracker;

import amosalexa.Service;
import amosalexa.SessionStorage;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbMapper;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.db.Category;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Service(
        functionGroup = HelpService.FunctionGroup.BUDGET_TRACKING,
        functionName = "Alle Kategorien verwalten",
        example = "Zeig mir meine Kategorien!",
        description = "Mit dieser Funktion kannst du Kategorien ausgeben lassen, Kategorien erstellen und Kategorien löschen."
)
public class EditCategoriesService extends AbstractSpeechService implements SpeechService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditCategoriesService.class);
    private static final String ADD_CATEGORY_INTENT = "AddCategoryIntent";
    private static final String SHOW_CATEGORIES_INTENT = "ShowCategoriesIntent";
    private static final String DELETE_CATEGORY_INTENT = "DeleteCategoryIntent";
    private static final String SERVICE_CARD_TITLE = "Kategorien verwalten";

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                SHOW_CATEGORIES_INTENT,
                ADD_CATEGORY_INTENT,
                DELETE_CATEGORY_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                SHOW_CATEGORIES_INTENT,
                ADD_CATEGORY_INTENT,
                DELETE_CATEGORY_INTENT,
                YES_INTENT,
                NO_INTENT,
                STOP_INTENT
        );
    }

    public EditCategoriesService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

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
        LOGGER.info("Intent Name: " + intentName);

        Session session = requestEnvelope.getSession();
        String context = (String) session.getAttribute(DIALOG_CONTEXT);

        switch (intentName) {
            case DELETE_CATEGORY_INTENT:
                return deleteCategory(intent, session);
            case YES_INTENT:
                if (context.equals(DELETE_CATEGORY_INTENT)) {
                    return performDeletion(intent, session);
                }
                if (context.equals(ADD_CATEGORY_INTENT)) {
                    return addNewCategory(intent, session, true);
                }
                return null;
            case NO_INTENT:
                return getResponse(SERVICE_CARD_TITLE, "OK, verstanden. Dann bis bald.");
            case ADD_CATEGORY_INTENT:
                return addNewCategory(intent, session, false);
            case SHOW_CATEGORIES_INTENT:
                return showAllCategories(intent, session);
        }

        return null;
    }

    /**
     * @param intent
     * @param session
     * @return
     */
    private SpeechletResponse showAllCategories(Intent intent, Session session) {
        String intentName = intent.getName();
        LOGGER.info("Intent Name: " + intentName);

        List<Category> items = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        String namesOfCategories = "";

        if (items.size() == 0) {
            return getResponse(SERVICE_CARD_TITLE, "Aktuell hast du keine Kategorien.");
        }

        for (Category item : items) {
            namesOfCategories += item.getName() + ", ";
        }

        return getResponse(SERVICE_CARD_TITLE, "Aktuell hast du folgende Kategorien: " + namesOfCategories);
    }

    /**
     * @param intent
     * @param session
     * @return
     */
    private SpeechletResponse addNewCategory(Intent intent, Session session, boolean confirmed) {
        String intentName = intent.getName();
        LOGGER.info("Intent Name: " + intentName);

        if (!confirmed) {
            Slot categoryNameSlot = intent.getSlot("CategoryName");

            if (contains(categoryNameSlot.getValue()) == true) {
                return getResponse(SERVICE_CARD_TITLE, "Diese Kategorie existiert bereits.");
            }

            session.setAttribute(DIALOG_CONTEXT, ADD_CATEGORY_INTENT);
            SessionStorage.getInstance().putObject(session.getSessionId(), SERVICE_CARD_TITLE + ".categoryName", categoryNameSlot.getValue());

            return getAskResponse(SERVICE_CARD_TITLE, "Moechtest du die Kategorie " + categoryNameSlot.getValue() + " wirklich erstellen?");
        } else {
            String slotName = (String) SessionStorage.getInstance().getObject(session.getSessionId(), SERVICE_CARD_TITLE + ".categoryName");

            Category item = new Category(slotName);
            DynamoDbMapper.getInstance().save(item);
            //DynamoDbClient.instance.putItem(Category.TABLE_NAME, item);

            return getResponse(SERVICE_CARD_TITLE, "Verstanden. Die Kategorie " + slotName + " wurde erstellt.");
        }
    }

    /**
     * @param intent
     * @param session
     * @return
     */
    private SpeechletResponse deleteCategory(Intent intent, Session session) {
        String category = intent.getSlot("CategoryName").getValue();

        if (category == null || category.equals("")) {
            return getResponse(SERVICE_CARD_TITLE, "Ich konnte den Namen der Kategorie nicht verstehen. Bitte versuche es noch einmal.");
        }

        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);

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
            return getResponse(SERVICE_CARD_TITLE, "Ich konnte keine passende Kategorie finden.");
        }

        SessionStorage.getInstance().putObject(session.getSessionId(), SERVICE_CARD_TITLE + ".categoryId", closestCategory);
        session.setAttribute(DIALOG_CONTEXT, DELETE_CATEGORY_INTENT);

        return getAskResponse(SERVICE_CARD_TITLE, "Möchtest du die Kategorie mit dem Namen '"
                + closestCategory.getName()
                + "' und dem Limit von "
                + closestCategory.getLimit()
                + " Euro wirklich löschen?");
    }

    /**
     * @param intent
     * @param session
     * @return
     */
    private SpeechletResponse performDeletion(Intent intent, Session session) {
        Object closestCategoryObj = SessionStorage.getInstance().getObject(session.getSessionId(), SERVICE_CARD_TITLE + ".categoryId");

        if (closestCategoryObj == null || !(closestCategoryObj instanceof Category)) {
            return getResponse(SERVICE_CARD_TITLE,
                    "Oh, da ist etwas passiert was nicht hätte passieren dürfen. " +
                            "Vielleicht sind ein paar Elektronen in eine andere Speicherzelle getunnelt. " +
                            "Anders kann ich mir das nicht erklären.");
        }

        Category closestCategory = (Category) closestCategoryObj;

        //DynamoDbClient.instance.deleteItem(Category.TABLE_NAME, closestCategory);
        DynamoDbMapper.getInstance().delete(closestCategory);

        return getResponse(SERVICE_CARD_TITLE, "OK, wie du willst. Ich habe die Kategorie mit dem Namen '" + closestCategory.getName() + "' gelöscht.");
    }

    private boolean contains(String categoryName) {
        List<Category> items = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        categoryName = categoryName.toLowerCase();

        for (Category item : items) {
            if (StringUtils.getLevenshteinDistance(
                    item.getName().toLowerCase(),
                    categoryName) < 2) {
                return true;
            }
        }

        return false;
    }

}
