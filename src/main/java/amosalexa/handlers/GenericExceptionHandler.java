package amosalexa.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import java.util.Optional;

public class GenericExceptionHandler implements ExceptionHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, Throwable throwable) {

        return handlerInput.getResponseBuilder()
                .withSpeech("Ein Fehler ist aufgetreten. Bitte wiederhole deine Frage.")
                .build();
    }
}
