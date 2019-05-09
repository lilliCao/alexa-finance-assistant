package amosalexa.services.budgetreport;

import amosalexa.Service;
import amosalexa.SessionStorage;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbMapper;
import api.aws.EMailClient;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.db.Category;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service(
        functionName = "Budget-Report anfordern",
        functionGroup = HelpService.FunctionGroup.ACCOUNT_INFORMATION,
        example = "Sende mir meinen Ausgabenreport.",
        description = "Diese Funktion schickt dir einen detaillierten Report über deine Ausgaben, wenn du willst."
)
public class BudgetReportService extends AbstractSpeechService implements SpeechService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetReportService.class);

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                BUDGET_REPORT_EMAIL_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                BUDGET_REPORT_EMAIL_INTENT
        );
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    public BudgetReportService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    private static final String BUDGET_REPORT_EMAIL_INTENT = "BudgetReportEMailIntent";

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        IntentRequest request = requestEnvelope.getRequest();

        if (request.getIntent().getName().equals(BUDGET_REPORT_EMAIL_INTENT)) {
            // Load the mail template from resources
            JtwigTemplate template = JtwigTemplate.classpathTemplate("html-templates/budget-report.twig");

            List<BudgetReportCategory> categories = new ArrayList<>();
            List<Category> dbCategories = DynamoDbMapper.getInstance().loadAll(Category.class); //DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
            for (Category cat : dbCategories) {
                categories.add(new BudgetReportCategory(cat.getName(), cat.getSpending(), cat.getLimit()));
            }
            
            JtwigModel model = JtwigModel.newModel().with("categories", categories);

            // Render mail template
            String body = template.render(model);

            String answer = "Okay, ich habe dir deinen Ausgabenreport per E-Mail gesendet.";
            boolean isDebug = SessionStorage.getInstance().getStorage(requestEnvelope.getSession().getSessionId()).containsKey("DEBUG");
            boolean isSuccess = EMailClient.SendHTMLEMail("Ausgabenreport", body);
            LOGGER.info("Sending email="+isSuccess);
            if (!isDebug && !isSuccess) {
                answer = "Leider konnte der Ausgabenreport nicht gesendet werden.";
            }
            return getResponse("Ausgabenreport", answer);
        }

        return null;
    }

}
