package amosalexa.handlers.financing;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;

public class AffordabilityServiceHandler implements IntentRequestHandler {
    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return false;
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        return Optional.empty();
    }
}
