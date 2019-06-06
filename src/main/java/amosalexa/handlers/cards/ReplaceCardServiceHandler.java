package amosalexa.handlers.cards;

import amosalexa.handlers.Service;
import api.banking.AccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.banking.Card;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;
import static amosalexa.handlers.PasswordResponseHelper.*;
import static amosalexa.handlers.ResponseHelper.response;

@Service(
        functionGroup = Service.FunctionGroup.BANK_CONTACT,
        functionName = "Ersatzkarte anfordern",
        example = "Ich brauche eine Ersatzkarte",
        description = "Mit dieser Funktion kannst du eine neue Bankkarte anfordern."
)
public class ReplaceCardServiceHandler implements IntentRequestHandler {
    private static final String REPLACEMENT_CARD_INTENT = "ReplacementCardIntent";
    private static final String SLOT_CARD_ENDZIFFER = "CardEndZiffer";
    private static final String CARD_TITLE = "Ersatzkarte anfordern";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(REPLACEMENT_CARD_INTENT))
                || isNumberIntentForPass(input, REPLACEMENT_CARD_INTENT);
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Optional<Response> response = checkPin(input, intentRequest, false);
        if (response.isPresent()) return response;
        Intent intent = getRealIntent(input, intentRequest);

        if (intent.getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
            Map<String, Slot> slots = intentRequest.getIntent().getSlots();
            String cardEndZiffer = slots.get(SLOT_CARD_ENDZIFFER).getValue();

            Collection<Card> cards = AccountAPI.getCardsForAccount(ACCOUNT_NUMBER);
            if (cards == null || cards.size() == 0) {
                return response(input, CARD_TITLE, "Es wurden keine Kredit- oder EC-Karten gefunden.");
            }

            StringBuilder builder = new StringBuilder("Es existiert keine Karte mit deiner angegebene Nummer. Die folgende Karte wurde gefunden: ");
            for (Card card : cards) {
                if (card.getStatus() == Card.Status.ACTIVE) {
                    builder.append(card.getCardType() == Card.CardType.CREDIT ? "Kredit" : "EC-");
                    builder.append("karte mit den Endziffern <say-as interpret-as=\"digits\">" +
                            card.getCardNumber().substring(card.getCardNumber().length() - 4) + "</say-as>.");
                }
                if (card.getCardNumber().substring(card.getCardNumber().length() - 4).equals(cardEndZiffer)) {
                    //found
                    AccountAPI.orderReplacementCard(ACCOUNT_NUMBER, card.getCardId());
                    return response(input, CARD_TITLE, "Okay, eine Ersatzkarte wurde bestellt.");

                }
            }
            builder.append("Bitte versuch nochmal");
            return response(input, CARD_TITLE, builder.toString());


        }
        return response(input, CARD_TITLE);
    }
}
