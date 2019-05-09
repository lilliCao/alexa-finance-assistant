package amosalexa.handlers.bankaccount;

import amosalexa.Service;
import amosalexa.services.help.HelpService;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import model.db.User;

import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.USER_ID;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.response;

@Service(
        functionName = "Kontolimit verwalten",
        functionGroup = HelpService.FunctionGroup.BUDGET_TRACKING,
        example = "Setze mein Kontolimit auf 800 Euro",
        description = "Diese Funktion erlaubt es dir ein Kontolimit zu setzen, sodass du nicht aus Versehen zu viel Geld ausgibst."
)

public class BalanceLimitServiceHandler implements IntentRequestHandler {
    public static final String SET_BALANCE_LIMIT_INTENT = "SetBalanceLimitIntent";
    public static final String GET_BALANCE_LIMIT_INTENT = "GetBalanceLimitIntent";
    public static final String SLOT_BALANCE_LIMIT_AMOUNT = "BalanceLimitAmount";
    private static final String CARD_TITLE = "Kontolimit";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(SET_BALANCE_LIMIT_INTENT))
                || input.matches(Predicates.intentName(GET_BALANCE_LIMIT_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Intent intent = intentRequest.getIntent();
        User user = (User) dynamoDbMapper.load(User.class, USER_ID);
        String speechText;
        if (intent.getName().equalsIgnoreCase(GET_BALANCE_LIMIT_INTENT)) {
            speechText = "Dein aktuelles Kontolimit beträgt " + user.getBalanceLimit() + " Euro.";
            return response(input, CARD_TITLE, speechText);
        } else if (intent.getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
            Long amount = Long.valueOf(intent.getSlots().get(SLOT_BALANCE_LIMIT_AMOUNT).getValue());
            user.setBalanceLimit(amount);
            dynamoDbMapper.save(user);
            speechText = "Okay, dein Kontolimit wurde auf " + amount + " Euro gesetzt.";
            return response(input, CARD_TITLE, speechText);
        } else {
            return response(input, CARD_TITLE);
        }
    }
}
