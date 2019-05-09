package amosalexa.services.budgettracker;

import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.AccountData;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbClient;
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
import java.util.Map;

/**
 * Budget tracking service
 */
@Service(
        functionGroup = HelpService.FunctionGroup.BUDGET_TRACKING,
        functionName = "Einzelne Kategorie verwalten",
        example = "Wie ist das Limit für Lebensmittel?",
        description = "Mit dieser Funktion kannst du das Limit für eine Kategorie ändern oder den Status einer Kategorie abfragen."
)
public class BudgetTrackerService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    private static final String CATEGORY_LIMIT_INFO_INTENT = "CategoryLimitInfoIntent";
    private static final String CATEGORY_STATUS_INFO_INTENT = "CategoryStatusInfoIntent";
    private static final String PLAIN_CATEGORY_INTENT = "PlainCategoryIntent";
    private static final String CATEGORY_LIMIT_SET_INTENT = "CategoryLimitSetIntent";
    private static final String CATEGORY_SPENDING_INTENT = "CategorySpendingIntent";

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                CATEGORY_LIMIT_INFO_INTENT,
                CATEGORY_LIMIT_SET_INTENT,
                CATEGORY_SPENDING_INTENT,
                CATEGORY_STATUS_INFO_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                CATEGORY_LIMIT_INFO_INTENT,
                CATEGORY_SPENDING_INTENT,
                CATEGORY_STATUS_INFO_INTENT,
                PLAIN_CATEGORY_INTENT,
                CATEGORY_LIMIT_SET_INTENT,
                PLAIN_NUMBER_INTENT,
                PLAIN_EURO_INTENT,
                YES_INTENT,
                NO_INTENT,
                STOP_INTENT
        );
    }

    public BudgetTrackerService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    /**
     * Default value for cards
     */
    private static final String BUDGET_TRACKER = "Budget-Tracker";
    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetTrackerService.class);

    private static final String CATEGORY = "Category";
    private static final String AMOUNT = "Amount";
    private static final String CATEGORY_LIMIT = "CategoryLimit";

    private DynamoDbMapper dynamoDbMapper = new DynamoDbMapper(DynamoDbClient.getAmazonDynamoDBClient());

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
        LOGGER.info("Intent name: " + intentName);
        Session session = requestEnvelope.getSession();
        String context = (String) session.getAttribute(DIALOG_CONTEXT);
        String withinDialogContext = (String) session.getAttribute(WITHIN_DIALOG_CONTEXT);
        if(withinDialogContext == null) {
            withinDialogContext = "";
        }

        if (CATEGORY_LIMIT_INFO_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, CATEGORY_LIMIT_INFO_INTENT);
            return getCategoryLimitInfo(intent);
        } else if (PLAIN_CATEGORY_INTENT.equals(intentName)) {
            return getCategoryLimitInfo(intent);
        } else if (CATEGORY_LIMIT_SET_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, CATEGORY_LIMIT_SET_INTENT);
            return saveSlotValuesAndAskForConfirmation(intent, session);
        } else if ((PLAIN_NUMBER_INTENT.equals(intentName) || PLAIN_EURO_INTENT.equals(intentName)) && context != null
                && context.equals(CATEGORY_LIMIT_SET_INTENT)) {
            Map<String, Slot> slots = intent.getSlots();
            String newLimit = slots.get(NUMBER_SLOT_KEY) != null ? slots.get(NUMBER_SLOT_KEY).getValue() : null;
            if (newLimit == null) {
                return getAskResponse(BUDGET_TRACKER, "Das habe ich nicht ganz verstanden. Bitte wiederhole deine Eingabe.");
            }
            session.setAttribute(CATEGORY_LIMIT, newLimit);
            String categoryName = (String) session.getAttribute(CATEGORY);
            return askForConfirmation(categoryName, newLimit);
        } else if (CATEGORY_SPENDING_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, intentName);
            return saveSpendingAmountForCategory(intent, session, false);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals(CATEGORY_SPENDING_INTENT) &&
                !withinDialogContext.equals("createCategory")) {
            return saveSpendingAmountForCategory(intent, session, true);
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals(CATEGORY_SPENDING_INTENT) &&
                withinDialogContext.equals("createCategory")) {
            String categoryName = (String) session.getAttribute("categoryName");
            String amount = (String) session.getAttribute("categoryAmount");
            Category category = new Category(categoryName);
            DynamoDbMapper.getInstance().save(category);
            return getResponse(BUDGET_TRACKER, "Ich habe die Kategorie " + categoryName + " erstellt und " + amount + " Euro hinzugefuegt.");
        } else if (YES_INTENT.equals(intentName) && context != null && context.equals(CATEGORY_LIMIT_SET_INTENT)) {
            return setCategoryLimit(session);
        } else if (NO_INTENT.equals(intentName) && context != null && context.equals(CATEGORY_LIMIT_SET_INTENT)) {
            return askForCorrection(session);
        } else if (STOP_INTENT.equals(intentName) && context != null && context.equals(CATEGORY_LIMIT_SET_INTENT)) {
            return getResponse("Stop", null);
        } else if (CATEGORY_STATUS_INFO_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, intentName);
            return getCategoryStatusInfo(intent);
        } else {
            throw new SpeechletException("Unhandled intent: " + intentName);
        }
    }

    private SpeechletResponse saveSpendingAmountForCategory(Intent intent, Session session, boolean isConfirmation) {
        Map<String, Slot> slots = intent.getSlots();
        Slot spendingSlot = slots.get(AMOUNT);
        Slot categorySlot = slots.get(CATEGORY);

        if (!isConfirmation) {
            if (spendingSlot == null) {
                return getAskResponse(BUDGET_TRACKER, "Das habe ich nicht ganz verstanden. Bitte wiederhole deine Eingabe");
            }

            if (categorySlot == null) {
                return getAskResponse(BUDGET_TRACKER, "Fuer welche Kategorie soll ich diesen Betrag notieren?");
            }

            session.setAttribute(AMOUNT, spendingSlot.getValue());
            session.setAttribute(CATEGORY, categorySlot.getValue());
        }

        String spendingAmount = isConfirmation ? (String) session.getAttribute(AMOUNT) : spendingSlot.getValue();
        String categoryName = isConfirmation ? (String) session.getAttribute(CATEGORY) : categorySlot.getValue();

        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        Category category = null;
        for (Category cat : categories) {
            //TODO duplicate. write find and update category method
            if (cat.getName().equals(categoryName)) {
                category = cat;
            }
        }
        if (category != null) {
            //LOGGER.info("Category spending before: " + category.getSpending());
            //LOGGER.info("Add spending for category " + spendingAmount);
            double newSpending = category.getSpending() + Double.valueOf(spendingAmount);
            if (!isConfirmation && newSpending > category.getLimit()) {
                //Fallback if the user is about to exceed the limit
                return getAskResponse(BUDGET_TRACKER, "Warnung! Durch diese Aktion wirst du das Limit von " +
                        Math.round(category.getLimit()) + " fuer die Kategorie " + categoryName + " ueberschreiten. Soll ich den" +
                        " Betrag trotzdem notieren?");
            }

            // Create new spending in DB
            BudgetManager.instance.createSpending(AccountData.ACCOUNT_DEFAULT, category.getId(), Double.valueOf(spendingAmount));

            //DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);
            //LOGGER.info("Category spending afterwards: " + category.getSpending());
        } else {
            // Ask for category creation
            session.setAttribute("categoryAmount", spendingAmount);
            session.setAttribute("categoryName", categoryName);
            session.setAttribute(WITHIN_DIALOG_CONTEXT, "createCategory");
            return getAskResponse(BUDGET_TRACKER, "Ich konnte diese Kategorie nicht finden. Soll ich eine Kategorie" +
                    " mit dem Namen " + categoryName + " anlegen und den Betrag hinzufuegen?");
        }

        String speechResponse = "";
        Long spendingPercentage = category.getSpendingPercentage();
        LOGGER.info("SpendingPercentage: " + spendingPercentage);

        if (spendingPercentage >= 90) {
            //TODO SSML
            speechResponse = speechResponse + " Warnung! Du hast in diesem Monat bereits "
                    + Math.round(category.getSpending()) + " Euro fuer " + category.getName() + " ausgegeben. Das sind "
                    + spendingPercentage + "% des Limits fuer diese Kategorie.";
        }

        speechResponse = "Okay. Ich habe " + spendingAmount + " Euro fuer "
                + categoryName + " notiert." + speechResponse;
        return getResponse(BUDGET_TRACKER, speechResponse);
    }

    private SpeechletResponse getCategoryLimitInfo(Intent intent) {
        Map<String, Slot> slots = intent.getSlots();
        Slot categorySlot = slots.get(CATEGORY);
        LOGGER.info("Category: " + categorySlot.getValue());

        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        LOGGER.info("All categories: " + categories);
        Category category = null;

        for (Category cat : categories) {
            if (cat.getName().equals(categorySlot.getValue())) {
                category = cat;
            }
        }
        if (category != null) {
            return getResponse(BUDGET_TRACKER, "Das Limit fuer die Kategorie " + categorySlot.getValue() + " liegt bei " +
                    Double.valueOf(category.getLimit()) + " Euro.");
        } else {
            return getAskResponse(BUDGET_TRACKER, "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder" +
                    " erhalte eine Info zu den verfuegbaren Kategorien.");
        }
    }

    private SpeechletResponse saveSlotValuesAndAskForConfirmation(Intent intent, Session session) {
        Map<String, Slot> slots = intent.getSlots();
        Slot categorySlot = slots.get(CATEGORY);
        Slot categoryLimitSlot = slots.get(CATEGORY_LIMIT);
        //LOGGER.info("Category: " + categorySlot.getValue());
        //LOGGER.info("CategoryLimit: " + categoryLimitSlot.getValue());

        if (categorySlot == null || categoryLimitSlot == null ||
                StringUtils.isBlank(categorySlot.getValue()) ||
                StringUtils.isBlank(categoryLimitSlot.getValue())) {
            return getAskResponse(BUDGET_TRACKER, "Das habe ich nicht ganz verstanden, bitte wiederhole deine Eingabe.");
        } else {
            session.setAttribute(CATEGORY, categorySlot.getValue());
            session.setAttribute(CATEGORY_LIMIT, categoryLimitSlot.getValue());
            return askForConfirmation(categorySlot.getValue(), categoryLimitSlot.getValue());
        }
    }

    private SpeechletResponse askForConfirmation(String category, String categoryLimit) {
        return getAskResponse(BUDGET_TRACKER, "Moechtest du das Ausgabelimit fuer die Kategorie " + category
                + " wirklich auf " + categoryLimit + " Euro setzen?");
    }

    private SpeechletResponse askForCorrection(Session session) {
        String categoryName = (String) session.getAttribute(CATEGORY);
        String response = "<emphasis level=\"reduced\">Nenne den Betrag, auf den du das Limit fuer Kategorie " + categoryName +
                " stattdessen setzen willst oder beginne mit einer neuen Eingabe.</emphasis>";
        return getSSMLAskResponse(BUDGET_TRACKER, response);
    }

    private SpeechletResponse setCategoryLimit(Session session) {
        String categoryName = (String) session.getAttribute(CATEGORY);
        String categoryLimit = (String) session.getAttribute(CATEGORY_LIMIT);

        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        LOGGER.info("Categories: " + categories);
        Category category = null;

        for (Category cat : categories) {
            if (cat.getName().equals(categoryName)) {
                category = cat;
            }
        }
        if (category != null) {
            //LOGGER.info("Category limit before: " + category.getLimit());
            //LOGGER.info("Set limit for category " + categoryName);
            category.setLimit(Double.valueOf(categoryLimit));
            //DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);
            DynamoDbMapper.getInstance().save(category);
            //LOGGER.info("Category limit afterwards: " + category.getLimit());
        } else {
            //LOGGER.info("Category does not exist yet. Create category...");
            //LOGGER.info("Set limit for category " + categoryName + " to value: " + categoryLimit);
            category = new Category(categoryName);
            category.setLimit(Double.valueOf(categoryLimit));
            DynamoDbMapper.getInstance().save(category);
            //DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);
        }
        return getResponse(BUDGET_TRACKER, "Limit fuer " + categoryName + " wurde gesetzt.");
    }

    private SpeechletResponse getCategoryStatusInfo(Intent intent) {
        Map<String, Slot> slots = intent.getSlots();
        Slot categorySlot = slots.get(CATEGORY);
        LOGGER.info("Category: " + categorySlot.getValue());

        List<Category> categories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        LOGGER.info("All categories: " + categories);
        Category category = null;

        for (Category cat : categories) {
            if (cat.getName().equals(categorySlot.getValue())) {
                category = cat;
            }
        }

        //LOGGER.info("Category limit: " + category.getLimit());
        //LOGGER.info("Category spending: " + category.getSpending());

        if (category != null) {
            double spending = category.getSpending();
            long percentage = Math.round(spending / category.getLimit() * 100);
            double remaining = category.getLimit() - spending > 0 ? category.getLimit() - spending : 0;
            String response = "Du hast bereits " + percentage + "% des Limits von " + category.getLimit() + " Euro fuer" +
                    " die Kategorie " + category.getName() + " ausgegeben. " +
                    "Du kannst noch " + remaining + " Euro für diese Kategorie ausgeben.";
            return getResponse(BUDGET_TRACKER, response);
        } else
            return getAskResponse(BUDGET_TRACKER, "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder" +
                    " erhalte eine Info zu den verfuegbaren Kategorien.");
    }
}