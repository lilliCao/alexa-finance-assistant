package amosalexa.handlers.cards;

import amosalexa.handlers.Service;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import java.util.Optional;

import static amosalexa.handlers.ResponseHelper.response;

@Service(
        functionGroup = Service.FunctionGroup.BANK_CONTACT,
        functionName = "Karte sperren",
        example = "Sperre Karte 001",
        description = "Mit dieser Funktion kannst du Karten sperren lassen, wenn sie verloren gegangen sind oder gestolen wurden."
)
public class BlockCardServiceHandler implements IntentRequestHandler {
    private static final String BLOCK_CARD_INTENT = "BlockCardIntent";
    private static final String SLOT_BANK_CARD_NUMMER = "BankCardNumber";
    private static final String CARD_TITLE = "Block card";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(BLOCK_CARD_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
            String bankCardNumber = intentRequest.getIntent().getSlots().get(SLOT_BANK_CARD_NUMMER).getValue();
            // TODO: Lock card with number cardNumber
            return response(input, CARD_TITLE, "Karte " + bankCardNumber + " wurde gesperrt.");
        } else {
            return response(input, CARD_TITLE);
        }
    }
}
