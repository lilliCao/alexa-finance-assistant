package amosalexa.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.model.ui.OutputSpeech;
import com.amazon.ask.model.ui.PlainTextOutputSpeech;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Utils {
    private static String sessionId = String.valueOf(UUID.randomUUID());
    private static IntentRequest intentRequest;
    private static Logger LOGGER = LoggerFactory.getLogger(Utils.class.getName());

    public static void test(IntentRequestHandler handler, String output) {
        HandlerInput input = createHandlerInput();
        if (handler.canHandle(input, intentRequest)) {
            LOGGER.info("Handler can handle");
            Optional<Response> result = handler.handle(input, intentRequest);
            boolean isSuccess = false;
            if (result.isPresent()) {
                LOGGER.info("Handler responses");
                if (getOutputSpeechText(result.get().getOutputSpeech()) != null) {
                    LOGGER.info("Can read handler response");
                    isSuccess = getOutputSpeechText(result.get().getOutputSpeech()).toLowerCase().matches(output.toLowerCase());
                    LOGGER.info("Expected   :" + output);
                    LOGGER.info("Got        :" + getOutputSpeechText(result.get().getOutputSpeech()));
                }
            }
            assertTrue(isSuccess);
        } else {
            assertFalse(true);
            LOGGER.info("Handler can not handle this intent = " + intentRequest.getIntent().getName());
        }
    }

    private static HandlerInput createHandlerInput() {
        return HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                        .withRequest(intentRequest)
                        .withSession(Session.builder().withSessionId(sessionId).build())
                        .build())
                .build();
    }

    public static void createIntentRequest(String intentName) {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName(intentName).build())
                .build();
    }

    public static void createIntentRequest(String intentName, Map<String, Slot> slots) {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName(intentName).withSlots(slots).build())
                .build();
    }

    public static void setIntentRequest(IntentRequest intentRequestR) {
        intentRequest = intentRequestR;
    }

    public static void createIntentRequest(String intentName, Map<String, Slot> slots, IntentConfirmationStatus confirmationStatus) {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName(intentName).withConfirmationStatus(confirmationStatus).withSlots(slots).build())
                .build();
    }

    public static void newSession() {
        sessionId = String.valueOf(UUID.randomUUID());
    }

    private static String getOutputSpeechText(OutputSpeech outputSpeech) {
        if (outputSpeech instanceof SsmlOutputSpeech) {
            SsmlOutputSpeech ssmlOutputSpeech = (SsmlOutputSpeech) outputSpeech;
            return ssmlOutputSpeech.getSsml().replaceAll("\\<.*?>", "");
        }
        if (outputSpeech instanceof PlainTextOutputSpeech) {
            PlainTextOutputSpeech plainTextOutputSpeech = (PlainTextOutputSpeech) outputSpeech;
            return plainTextOutputSpeech.getText().replaceAll("\\<.*?>", "");
        }

        return null;
    }
}
