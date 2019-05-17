package amosalexa.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import java.util.Optional;

public class FallbackIntentHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(Predicates.intentName("AMAZON.FallbackIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        String speech = "Ich bin mir nicht sicher. Um über meine Funktionen zu erfahren, frage zum Beispiel Was kann AMOS?";
        return handlerInput.getResponseBuilder()
                .withSpeech(speech)
                .withReprompt(speech)
                .build();
    }
}
