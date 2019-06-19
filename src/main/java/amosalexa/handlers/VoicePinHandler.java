package amosalexa.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static amosalexa.handlers.ResponseHelper.response;

/**
 * This class checks voice pin as a number for any intent with a type of VoicePin.
 * Since Alexa can not recognize german number as digits e.g. 4321=ViertausendDreihundertEinsundZwanzig,
 * the work around class PasswordResponseHelper is used instead.
 */
public class VoicePinHandler implements IntentRequestHandler {
    private static final String CARD_TITLE = "Voice Pin";
    private static final String SLOT_VOICE_PIN = "VoicePin";
    private static final int PIN_LIMIT = 3;
    private static int trial;
    // demo voice pin
    private static final String USER_VOICE_PIN = "14";


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return intentRequest.getIntent().getSlots().containsKey(SLOT_VOICE_PIN)
                && (intentRequest.getIntent().getSlots().get(SLOT_VOICE_PIN).getValue() == null
                || !intentRequest.getIntent().getSlots().get(SLOT_VOICE_PIN).getValue().equalsIgnoreCase(USER_VOICE_PIN));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Intent intent = intentRequest.getIntent();

        if (intent.getConfirmationStatus() == IntentConfirmationStatus.DENIED) {
            return response(input, "Anfrage abbrechen");
        }

        // Check voice pin
        if (intent.getSlots().get(SLOT_VOICE_PIN).getValue() == null) {
            trial = 1;
            return responseElicitDelegatePin(input, CARD_TITLE, intent, false);
        }
        if (!intent.getSlots().get(SLOT_VOICE_PIN).getValue().equalsIgnoreCase(USER_VOICE_PIN)) {
            if (trial < PIN_LIMIT) {
                trial++;
                return responseElicitDelegatePin(input, CARD_TITLE, intent, true);
            }
            trial = 0;
            return response(input, CARD_TITLE, "Deine angegebene Pin ist falsch. Ich breche jetzt den Prozess ab");
        }

        // Got correct pin, pass intent to the handler
        return input.getResponseBuilder()
                .addDelegateDirective(intent)
                .build();
    }

    private static Optional<Response> responseElicitDelegatePin(HandlerInput input, String cardTitle,
                                                                Intent intent, boolean reask) {
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
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .addElicitSlotDirective(SLOT_VOICE_PIN, intent)
                .withShouldEndSession(false)
                .build();
    }
}
