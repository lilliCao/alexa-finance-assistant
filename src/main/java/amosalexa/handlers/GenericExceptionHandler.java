package amosalexa.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GenericExceptionHandler implements ExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericExceptionHandler.class);

    @Override
    public boolean canHandle(HandlerInput handlerInput, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, Throwable throwable) {
        LOGGER.info("Input=" + handlerInput.getRequestEnvelopeJson().toString()
                + "Error=" + throwable.toString() + ": " + throwable.getStackTrace() + ": " + throwable.getMessage());
        return handlerInput.getResponseBuilder()
                .withSpeech("Ein Fehler ist aufgetreten. Bitte wiederhole deine Frage.")
                .build();
    }
}
