package amosalexa.handlers.help;

import amosalexa.handlers.Service;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static amosalexa.handlers.ResponseHelper.response;

public class HelpServiceHandler implements IntentRequestHandler {
    private static final String CARD_TITLE = "AMOS Einführung";

    private static final String INTRODUCTION_INTENT = "IntroductionIntent";
    private static final String OVERVIEW_INTENT = "FunctionGroupOverviewIntent";
    private static final String FUNCTION_GROUP_INTENT = "FunctionGroupIntent";

    private static final String SLOT_FUNCTION_GROUP = "FunctionGroup";

    static {
        // Scan for classes having the @Service annotation
        Reflections reflections = new Reflections("amosalexa.handlers");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Service.class);

        for (Class<?> controller : annotated) {
            Service request = controller.getAnnotation(Service.class);
            request.functionGroup().addFeature(
                    new Service.FunctionGroup.Feature(
                            request.functionName(),
                            request.description(),
                            request.example()
                    )
            );
        }
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(INTRODUCTION_INTENT))
                || input.matches(Predicates.intentName(OVERVIEW_INTENT))
                || input.matches(Predicates.intentName(FUNCTION_GROUP_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        switch (intentRequest.getIntent().getName()) {
            case INTRODUCTION_INTENT:
                return getIntroduction(input);
            case OVERVIEW_INTENT:
                return getOverview(input);
            default:
                //FUNCTION_GROUP_INTENT
                return getFunction(input, intentRequest);
        }
    }

    private Optional<Response> getFunction(HandlerInput input, IntentRequest intentRequest) {
        String functionGroup = intentRequest.getIntent().getSlots().get(SLOT_FUNCTION_GROUP).getValue();

        Service.FunctionGroup closestFunctionGroup = null;
        int closestDist = Integer.MAX_VALUE;
        for (Service.FunctionGroup category : EnumSet.allOf(Service.FunctionGroup.class)) {
            String name = category.name.toLowerCase();
            int dist = StringUtils.getLevenshteinDistance(name, functionGroup);
            if (dist < closestDist) {
                closestFunctionGroup = category;
                closestDist = dist;
            }
        }
        return response(input, CARD_TITLE, getFunctionGroupInfo(closestFunctionGroup));

    }

    private String getFunctionGroupInfo(Service.FunctionGroup functionGroup) {
        StringBuilder sb = new StringBuilder();

        sb.append("Gerne erkläre ich dir die Funktionen in der Kategorie ")
                .append(functionGroup.name)
                .append(" mit insgesamt ")
                .append(functionGroup.features.size())
                .append(" Funktionen: ");

        String[] beginnings = new String[]{
                "Es gibt die Funktion",
                "Dann gibt es die Funktion",
                "Außerdem gibt es die Funktion",
                "Auch hilfreich ist die Funktion",
                "Sehr zu empfehlen ist die Funktion",
                "Gerne erkläre ich dir auch die Funktion"
        };

        int i = 0;
        for (Service.FunctionGroup.Feature feature : functionGroup.features) {
            String beginning = beginnings[i];

            sb
                    .append(beginning)
                    .append(" ")
                    .append(feature.name)
                    .append(". ");

            sb
                    .append(feature.description)
                    .append(" ");

            sb
                    .append("Beispielsweise kannst du sagen: \"")
                    .append(feature.example)
                    .append("\"");

            i = (i + 1) % beginnings.length;
        }

        return sb.toString();
    }

    private Optional<Response> getOverview(HandlerInput input) {
        StringBuilder sb = new StringBuilder();

        sb.append("Die Funktionen sind in folgende Kategorien gruppiert: ");
        boolean first = true;

        EnumSet<Service.FunctionGroup> categories = EnumSet.allOf(Service.FunctionGroup.class);
        for (Iterator<Service.FunctionGroup> iter = categories.iterator(); iter.hasNext(); ) {
            Service.FunctionGroup functionGroup = iter.next();

            if (functionGroup.features.size() == 0) continue;

            if (first) {
                first = false;
            } else {
                if (iter.hasNext()) {
                    sb.append(", ");
                } else {
                    sb.append(" und ");
                }
            }

            sb.append(functionGroup.name);
        }

        sb.append(". Um mehr über eine Kategorie zu erfahren, sage \"Mehr über Kategorie\" und dann den Namen der Kategorie.");

        return response(input, CARD_TITLE, sb.toString());
    }

    private Optional<Response> getIntroduction(HandlerInput input) {
        String introduction = "Willkommen bei AMOS, der sprechenden Banking-App! Mit mir kannst du deine Bank-Geschäfte" +
                " mit Sprachbefehlen erledigen. Ich möchte dir kurz vorstellen, was ich alles kann. Um eine Übersicht über die" +
                " verfügbaren Kategorien von Funktionen zu erhalten, sage \"Übersicht über Kategorien\". Um mehr über die Funktionen einer" +
                " Kategorie zu erfahren, sage \"Mehr über Kategorie\" und dann den Namen der Kategorie, zum Beispiel \"Mehr über Kategorie Smart Financing\".";
        return response(input, CARD_TITLE, introduction);
    }
}
