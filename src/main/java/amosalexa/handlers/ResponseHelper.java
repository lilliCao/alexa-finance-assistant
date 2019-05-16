package amosalexa.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ResponseHelper {
    public static Optional<Response> response(HandlerInput input, String cardTitle, String text) {
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .withShouldEndSession(true)
                .build();
    }

    public static Optional<Response> response(HandlerInput input, String cardTitle) {
        List<String> possibleText = Arrays.asList(
                "Okay. Verstanden",
                "Alles klar",
                "Verstanden",
                "Okay. Tschüss",
                "Okay"
        );
        String text = possibleText.get((new Random()).nextInt(possibleText.size()));
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .withShouldEndSession(true)
                .build();
    }

    public static Optional<Response> responseWithSlotConfirm(HandlerInput input, String cardTitle, String text,
                                                             Intent intent, String slotName) {
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .addConfirmSlotDirective(slotName, intent)
                .withShouldEndSession(false)
                .build();
    }

    public static Optional<Response> responseWithIntentConfirm(HandlerInput input, String cardTitle, String text,
                                                               Intent intent) {
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .addConfirmIntentDirective(intent)
                .withReprompt(text)
                .withShouldEndSession(false)
                .build();
    }

    public static Optional<Response> responseDelegate(HandlerInput input, String cardTitle, String text,
                                                      Intent intent) {
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .addDelegateDirective(intent)
                .withShouldEndSession(false)
                .build();
    }

    public static Optional<Response> responseElicitDelegate(HandlerInput input, String cardTitle, String text,
                                                            Intent intent, String slotName) {
        return input.getResponseBuilder()
                .withSpeech(text)
                .withSimpleCard(cardTitle, text)
                .addElicitSlotDirective(slotName, intent)
                .withShouldEndSession(false)
                .build();
    }

}
