package amosalexa.handlers.securitiesAccount;

import amosalexa.handlers.Service;
import api.banking.SecuritiesAccountAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SlotConfirmationStatus;
import com.amazon.ask.request.Predicates;
import model.banking.Security;

import java.util.ArrayList;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.SECURITIES_ACCOUNT_ID;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;

@Service(
        functionGroup = Service.FunctionGroup.ACCOUNT_INFORMATION,
        functionName = "Depotstatus",
        example = "Wie sind meine Aktien",
        description = "Mit dieser Funktion kannst du deine Depotstatus abfragen."
)
public class SecuritiesAccountInformationServiceHandler implements IntentRequestHandler {
    private static final String SECURITIES_ACCOUNT_INFORMATION_INTENT = "SecuritiesAccountInformationIntent";

    private static final String SLOT_NEXT_INDEX = "NextIndex";
    private static final String CARD_TITLE = "Depotstatus";

    private static final int SECURITIES_LIMIT = 3;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(SECURITIES_ACCOUNT_INFORMATION_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Slot nextIndex = intentRequest.getIntent().getSlots().get(SLOT_NEXT_INDEX);
        ArrayList<Security> securities = new ArrayList<>(SecuritiesAccountAPI.getSecuritiesForAccount(SECURITIES_ACCOUNT_ID));
        if (securities == null || securities.size() == 0) {
            return response(input, CARD_TITLE, "Keine Depotinformationen vorhanden.");
        }

        if (nextIndex.getValue() == null) {
            // first
            StringBuilder builder = new StringBuilder();
            builder.append("Du hast momentan ")
                    .append(securities.size() == 1 ? "ein Wertpapier in deinem Depot. " : securities.size())
                    .append(" Wertpapiere in deinem Depot. ");

            for (int i = 0; i < SECURITIES_LIMIT; i++) {
                if (i < securities.size()) {
                    Security security = securities.get(i);
                    String stockPrice = FinanceApi.getStockPrice(security);
                    String stockPriceText = stockPrice==null ? " leider keine aktuelle Preise vorhanden. "
                            : "mit einem momentanen Wert von "+stockPrice+" Euro.";
                    builder.append("Wertpapier Nummer ")
                            .append(security.getSecurityId())
                            .append(": ").append(security.getDescription())
                            .append(", " + security.getQuantity() + " Stueck, ")
                            .append(stockPriceText);

                }
            }
            if (securities.size() > SECURITIES_LIMIT) {
                builder.append("Moechtest du einen weiteren Eintrag hoeren?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(SECURITIES_LIMIT))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            // read more if confirmed
            int position = Integer.parseInt(nextIndex.getValue());
            StringBuilder builder = new StringBuilder();
            Security security = securities.get(position);
            String stockPrice = FinanceApi.getStockPrice(security);
            String stockPriceText = stockPrice==null ? " leider keine aktuelle Preise vorhanden. "
                    : "mit einem momentanen Wert von "+stockPrice+" Euro.";
            builder.append("Wertpapier Nummer ")
                    .append(security.getSecurityId())
                    .append(": ").append(security.getDescription())
                    .append(", " + security.getQuantity() + " Stueck, ")
                    .append(stockPriceText);

            position++;
            if (position < securities.size()) {
                builder.append("Moechtest du einen weiteren Eintrag hoeren?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(position))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else {
            return response(input, CARD_TITLE);
        }
    }
}
