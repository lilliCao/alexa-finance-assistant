package amosalexa.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.slu.entityresolution.StatusCode;
import com.amazon.ask.request.Predicates;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseContinue;

public class PasswordResponseHelper {
    private static final String NUMBER_INTENT = "NumberIntent";
    private static final String INTENT_NAME = "IntentName";
    private static Logger LOGGER = LoggerFactory.getLogger(PasswordResponseHelper.class);
    private static final String SLOT_NUM_A = "NUM_A";
    private static final String SLOT_NUM_B = "NUM_B";
    private static final String SLOT_NUM_C = "NUM_C";
    private static final String SLOT_NUM_D = "NUM_D";
    private static final String SLOT_NUM_E = "NUM_E";
    private static final String SLOT_NUM_F = "NUM_F";
    private static final String SLOT_NUM_G = "NUM_G";
    private static final String SLOT_NUM_H = "NUM_H";
    private static final List<String> numberSlots = Arrays.asList(
            SLOT_NUM_A,
            SLOT_NUM_B,
            SLOT_NUM_C,
            SLOT_NUM_D,
            SLOT_NUM_E,
            SLOT_NUM_F,
            SLOT_NUM_G,
            SLOT_NUM_H
    );
    private static final String INTENT = "intent";
    public static final String STATE = "state";
    private static final String CHECKPIN_START = "CheckPin_Start";
    public static final String CHECKPIN_DONE = "CheckPin_Done";
    private static final String CHECKTAN_START = "CheckTan_Start";
    public static final String CHECKTAN_DONE = "CheckTan_Done";
    private static final ObjectMapper om = new ObjectMapper();

    public static Intent getRealIntent(HandlerInput input, IntentRequest intentRequest) {
        if (intentRequest.getIntent().getName().equals(NUMBER_INTENT)) {
            Object intentOb = input.getAttributesManager().getSessionAttributes().get(INTENT);
            input.getAttributesManager().getSessionAttributes().remove(INTENT);
            LOGGER.info("Intent ob from getRealIntent=" + intentOb.toString());
            return om.convertValue(intentOb, Intent.class);
        } else {
            return intentRequest.getIntent();
        }
    }

    public static boolean isNumberIntentForPass(HandlerInput input, String name) {
        String state = (String) input.getAttributesManager().getSessionAttributes().get(STATE);
        String intentName = (String) input.getAttributesManager().getSessionAttributes().get(INTENT_NAME);
        return input.matches(Predicates.intentName(NUMBER_INTENT))
                && intentName.equalsIgnoreCase(name)
                && (state.equals(CHECKPIN_START) ||
                state.equals(CHECKTAN_START));
    }


    public static Optional<Response> checkPin(HandlerInput input, IntentRequest intentRequest, boolean needTan) {
        Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
        if (!sessionAttributes.containsKey(STATE)) {
            LOGGER.info("Ask for pin");
            sessionAttributes.put(INTENT, intentRequest.getIntent());
            sessionAttributes.put(INTENT_NAME, intentRequest.getIntent().getName());
            sessionAttributes.put(STATE, CHECKPIN_START);
            sessionAttributes.put("trial", 1);
            return askPin(input, false);
        }

        String state = (String) sessionAttributes.get(STATE);

        if (state.equals(CHECKPIN_START)) {
            LOGGER.info("Get pin");
            String pin = getNumbers(intentRequest.getIntent());
            LOGGER.info("Check pin = " + pin);
            if (!pin.equalsIgnoreCase("1234")) {
                int trial = (Integer) sessionAttributes.get("trial");
                if (trial < 3) {
                    LOGGER.info("wrong pin. reask pin");
                    sessionAttributes.put("trial", ++trial);
                    return askPin(input, true);
                }
                LOGGER.info("over limit. cancel request");
                return response(input, "Anfrage abbrechen", "Deine angegebene PIN ist falsch. Ich breche jetzt den Prozess ab");

            }

            sessionAttributes.put(STATE, CHECKPIN_DONE);
        }

        if (needTan) {
            return checkTan(input, intentRequest);
        }

        return Optional.empty();
    }

    private static Optional<Response> checkTan(HandlerInput input, IntentRequest intentRequest) {
        Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
        String state = (String) sessionAttributes.get(STATE);
        if (state.equals(CHECKPIN_DONE)) {
            LOGGER.info("Ask for tan");
            sessionAttributes.put(STATE, CHECKTAN_START);
            sessionAttributes.put("trial", 1);
            return askTan(input, false);
        }

        if (state.equals(CHECKTAN_START)) {
            LOGGER.info("Get tan");
            String pin = getNumbers(intentRequest.getIntent());
            LOGGER.info("Check tan = " + pin);
            if (!pin.equalsIgnoreCase("123456")) {
                int trial = (Integer) sessionAttributes.get("trial");
                if (trial < 3) {
                    LOGGER.info("wrong tan. reask tan");
                    sessionAttributes.put("trial", ++trial);
                    return askTan(input, true);
                }
                LOGGER.info("over limit. cancel request");
                return response(input, "Anfrage abbrechen", "Deine angegebene TAN ist falsch. Ich breche jetzt den Prozess ab");
            }

            sessionAttributes.put(STATE, CHECKTAN_DONE);
        }

        return Optional.empty();
    }

    private static String getNumbers(Intent intent) {
        LOGGER.info("Get number slots");
        Map<String, Slot> slots = intent.getSlots();
        StringBuilder builder = new StringBuilder();
        for (String slotName : numberSlots) {
            if (slots.containsKey(slotName) && slots.get(slotName).getValue() != null
                    && slots.get(slotName).getResolutions().getResolutionsPerAuthority().get(0).getStatus().getCode() == StatusCode.ER_SUCCESS_MATCH
            ) {
                builder.append(slots.get(slotName).getResolutions()
                        .getResolutionsPerAuthority()
                        .get(0)
                        .getValues()
                        .get(0)
                        .getValue()
                        .getId()
                );
            }
        }
        return builder.toString();
    }

    private static Optional<Response> askPin(HandlerInput input, boolean reask) {
        List<String> possibleText = Arrays.asList(
                "Nenne mir bitte deine Voice PIN",
                "Sage mir bitte deine PIN",
                "deine Voice PIN bitte"
        );
        List<String> possibleReask = Arrays.asList(
                "Deine angegebene Pin ist falsch. Bitte korrigiere es.",
                "Ich kann deine PIN nicht verifizieren. Bitte wiederhole.",
                "Ich verstehe deine PIN nicht. Bitte wiederhole."
        );
        String text = reask ? possibleReask.get((new Random()).nextInt(possibleReask.size()))
                : possibleText.get((new Random()).nextInt(possibleText.size()));


        return responseContinue(input, "Überprüfe PIN", text);

    }

    private static Optional<Response> askTan(HandlerInput input, boolean reask) {
        List<String> possibleText = Arrays.asList(
                "Nenne mir bitte deine TAN",
                "Sage mir bitte deine TAN",
                "deine TAN bitte"
        );
        List<String> possibleReask = Arrays.asList(
                "Deine angegebene TAN ist falsch. Bitte korrigiere es.",
                "Ich kann deine TAN nicht verifizieren. Bitte wiederhole.",
                "Ich verstehe deine TAN nicht. Bitte wiederhole."
        );
        String text = reask ? possibleReask.get((new Random()).nextInt(possibleReask.size()))
                : possibleText.get((new Random()).nextInt(possibleText.size()));


        return responseContinue(input, "Überprüfe TAN", text);

    }
}
