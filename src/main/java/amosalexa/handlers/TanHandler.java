package amosalexa.handlers;

import api.banking.OneTimeAuthenticationAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * This class checks tan as a number for any intent with a type of TanCode.
 * Since Alexa can not recognize german number as digits e.g. 4321=ViertausendDreihundertEinsundZwanzig,
 * the work around class PasswordResponseHelper is used instead.
 */
public class TanHandler implements IntentRequestHandler {
    private static final String CARD_TITLE = "TAN Code";
    private static final String SLOT_TAN = "TanCode";
    private static final int TAN_LIMIT = 3;
    private static int trial;


    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return intentRequest.getIntent().getSlots().containsKey(SLOT_TAN)
                && (intentRequest.getIntent().getSlots().get(SLOT_TAN).getValue() == null
                || !OneTimeAuthenticationAPI.validateOTP(Integer.parseInt(intentRequest.getIntent().getSlots().get(SLOT_TAN).getValue())));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Intent intent = intentRequest.getIntent();

        // Check voice pin
        if (intent.getSlots().get(SLOT_TAN).getValue() == null) {
            trial = 1;
            return responseElicitDelegatePin(input, CARD_TITLE, intent, false);
        }
        if (OneTimeAuthenticationAPI.validateOTP(Integer.parseInt(intent.getSlots().get(SLOT_TAN).getValue()))) {
            if (trial < TAN_LIMIT) {
                trial++;
                return responseElicitDelegatePin(input, CARD_TITLE, intent, true);
            }
            trial = 0;
            return ResponseHelper.response(input, CARD_TITLE, "Deine angegebene TAN ist falsch. Ich breche jetzt den Prozess ab");
        }

        // Got correct pin, pass intent to the handler
        return input.getResponseBuilder()
                .addDelegateDirective(intent)
                .build();
    }

    private static Optional<Response> responseElicitDelegatePin(HandlerInput input, String cardTitle,
                                                                Intent intent, boolean reask) {
        List<String> possibleText = Arrays.asList(
                "Nenne mir die TAN Nummer bitte",
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
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .addElicitSlotDirective(SLOT_TAN, intent)
                .withShouldEndSession(false)
                .build();
    }
}
