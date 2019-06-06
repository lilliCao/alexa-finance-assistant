package amosalexa.handlers.budgetreport;

import amosalexa.handlers.Service;
import api.aws.EMailClient;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import model.db.Category;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.PasswordResponseHelper.checkPin;
import static amosalexa.handlers.PasswordResponseHelper.isNumberIntentForPass;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseDirective;

@Service(
        functionName = "Budget-Report anfordern",
        functionGroup = Service.FunctionGroup.ACCOUNT_INFORMATION,
        example = "Sende mir meinen Ausgabenreport.",
        description = "Diese Funktion schickt dir einen detaillierten Report über deine Ausgaben, wenn du willst."
)
public class BudgetReportServiceHandler implements IntentRequestHandler {
    public static final String BUDGET_REPORT_EMAIL_INTENT = "BudgetReportEMailIntent";
    private static final String CARD_TITLE = "Ausgabenreport";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(BUDGET_REPORT_EMAIL_INTENT))
                || isNumberIntentForPass(input, BUDGET_REPORT_EMAIL_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;

        // Load the mail template from resources
        JtwigTemplate template = JtwigTemplate.classpathTemplate("html-templates/budget-report.twig");
        List<BudgetReportCategory> categories = new ArrayList<>();
        List<Category> dbCategories = dynamoDbMapper.loadAll(Category.class);
        for (Category cat : dbCategories) {
            categories.add(new BudgetReportCategory(cat.getName(), cat.getSpending(), cat.getLimit()));
        }
        JtwigModel model = JtwigModel.newModel().with("categories", categories);

        // Render mail template
        String body = template.render(model);

        boolean isSuccess = EMailClient.SendHTMLEMail(CARD_TITLE, body);
        responseDirective(input, "Ich sende gerade deinen Ausgabenreport. Bitte warte kurz.");
        String answer = isSuccess ? "Okay, ich habe dir deinen Ausgabenreport per E-Mail gesendet."
                : "Ein Fehler ist aufgetreten. Leider konnte der Ausgabenreport nicht gesendet werden.";
        return response(input, CARD_TITLE, answer);
    }
}
