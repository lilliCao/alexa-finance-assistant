package amosalexa.handlers.bankaccount;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import model.db.User;

import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.USER_ID;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.PasswordResponseHelper.checkPin;
import static amosalexa.handlers.PasswordResponseHelper.isNumberIntentForPass;
import static amosalexa.handlers.ResponseHelper.response;

public class BalanceLimitGetHandler implements IntentRequestHandler {
    private static final String GET_BALANCE_LIMIT_INTENT = "GetBalanceLimitIntent";
    private static final String CARD_TITLE = "Kontolimit abfragen";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(GET_BALANCE_LIMIT_INTENT))
                || isNumberIntentForPass(input, GET_BALANCE_LIMIT_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;

        User user = (User) dynamoDbMapper.load(User.class, USER_ID);
        String speechText = "Dein aktuelles Kontolimit beträgt " + user.getBalanceLimit() + " Euro.";
        return response(input, CARD_TITLE, speechText);
    }
}
