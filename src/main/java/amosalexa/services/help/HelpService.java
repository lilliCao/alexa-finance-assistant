package amosalexa.services.help;

import amosalexa.Service;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.util.*;

/**
 * Help service which provides information on how to use different functionality.
 */
public class HelpService extends AbstractSpeechService implements SpeechService {

    /**
     * Represents a feature category.
     */
    public enum FunctionGroup {
        PRODUCT_QUERY("Produktanfrage"),
        ACCOUNT_INFORMATION("Kontoinformation"),
        SMART_FINANCING("Smart Financing"),
        ONLINE_BANKING("Online Banking"),
        BUDGET_TRACKING("Budget"),
        BANK_CONTACT("Bankkontakt");

        /**
         * Feature that belongs to a category.
         */
        public static class Feature {
            String name;
            String description;
            String example;

            Feature(String name, String description, String example) {
                this.name = name;
                this.description = description;
                this.example = example;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        String name;
        List<Feature> features = new LinkedList<>();

        FunctionGroup(String name) {
            this.name = name;
        }

        void addFeature(Feature feature) {
            features.add(feature);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static {
        // Scan for classes having the @Service annotation
        Reflections reflections = new Reflections("amosalexa.services");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Service.class);

        for (Class<?> controller : annotated) {
            Service request = controller.getAnnotation(Service.class);
            request.functionGroup().addFeature(
                    new FunctionGroup.Feature(
                            request.functionName(),
                            request.description(),
                            request.example()
                    )
            );
        }
    }

    /**
     * Default value for cards
     */
    private static final String INTRODUCTION = "AMOS Einführung";

    private static final String INTRODUCTION_INTENT = "IntroductionIntent";
    private static final String OVERVIEW_INTENT = "FunctionGroupOverviewIntent";
    private static final String FUNCTION_GROUP_INTENT = "FunctionGroupIntent";

    public HelpService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();
        Intent intent = request.getIntent();
        String intentName = intent.getName();

        switch (intentName) {
            case INTRODUCTION_INTENT:
                return getIntroduction(intent);
            case OVERVIEW_INTENT:
                return getFunctionGroups(intent);
            case FUNCTION_GROUP_INTENT:
                return getFunctionGroup(intent, session);
        }

        return null;
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    @Override
    public String getDialogName() {
        return HelpService.class.getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                INTRODUCTION_INTENT,
                OVERVIEW_INTENT,
                FUNCTION_GROUP_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                INTRODUCTION_INTENT,
                OVERVIEW_INTENT,
                FUNCTION_GROUP_INTENT
        );
    }

    private SpeechletResponse getIntroduction(Intent intent) {
        //TODO improve speech by ssml, continue
        String introductionSsml = "Willkommen bei AMOS, der sprechenden Banking-App! Mit mir kannst du deine Bank-Geschäfte" +
                " mit Sprachbefehlen erledigen. Ich möchte dir kurz vorstellen, was ich alles kann. Um eine Übersicht über die" +
                " verfügbaren Kategorien von Funktionen zu erhalten, sage \"Übersicht über Kategorien\". Um mehr über die Funktionen einer" +
                " Kategorie zu erfahren, sage \"Mehr über Kategorie\" und dann den Namen der Kategorie, zum Beispiel \"Mehr über Kategorie Smart Financing\".";
        return getSSMLResponse(INTRODUCTION, introductionSsml);
    }

    private SpeechletResponse getFunctionGroups(Intent intent) {
        StringBuilder sb = new StringBuilder();

        sb.append("Die Funktionen sind in folgende Kategorien gruppiert: ");
        boolean first = true;

        EnumSet<FunctionGroup> categories = EnumSet.allOf(FunctionGroup.class);
        for (Iterator<FunctionGroup> iter = categories.iterator(); iter.hasNext(); ) {
            FunctionGroup functionGroup = iter.next();

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

        return getSSMLResponse(INTRODUCTION, sb.toString());
    }

    private SpeechletResponse getFunctionGroup(Intent intent, Session session) {
        Slot functionGroupSlot = intent.getSlot("FunctionGroup");

        if (functionGroupSlot == null) {
            return getResponse(INTRODUCTION, "Das sollte nicht passieren: ein interner Fehler ist aufgetreten.");
        }

        String functionGroup = functionGroupSlot.getValue();

        if (functionGroup == null) {
            return getResponse(INTRODUCTION, "Das habe ich nicht verstanden. Sprich deutlicher.");
        }

        FunctionGroup closestFunctionGroup = null;
        int closestDist = Integer.MAX_VALUE;
        for (FunctionGroup category : EnumSet.allOf(FunctionGroup.class)) {
            String name = category.name.toLowerCase();

            int dist = StringUtils.getLevenshteinDistance(name, functionGroup);

            if (dist < closestDist) {
                closestFunctionGroup = category;
                closestDist = dist;
            }
        }

        return getSSMLResponse(INTRODUCTION, getFunctionGroupInfo(closestFunctionGroup));
    }

    private String getFunctionGroupInfo(FunctionGroup functionGroup) {
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
        for (FunctionGroup.Feature feature : functionGroup.features) {
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

}
