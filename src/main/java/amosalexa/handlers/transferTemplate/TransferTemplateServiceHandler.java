package amosalexa.handlers.transferTemplate;

import amosalexa.handlers.Service;
import amosalexa.services.DateUtil;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import model.db.TransferTemplateDB;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;
import static amosalexa.handlers.ResponseHelper.response;
import static amosalexa.handlers.ResponseHelper.responseWithSlotConfirm;

@Service(
        functionGroup = Service.FunctionGroup.ONLINE_BANKING,
        functionName = "Überweisungsvorlage verwalten",
        example = "Überweisungsvorlagen",
        description = "Mit dieser Funktion kannst du deine Überweisungsvorlagen abfragen, löschen oder modifizieren."
)
public class TransferTemplateServiceHandler implements IntentRequestHandler {
    private static final String LIST_TRANSFER_TEMPLATES_INTENT = "ListTransferTemplatesIntent";
    private static final String DELETE_TRANSFER_TEMPLATES_INTENT = "DeleteTransferTemplatesIntent";
    private static final String EDIT_TRANSFER_TEMPLATE_INTENT = "EditTransferTemplateIntent";

    private static final String SLOT_NEXT_INDEX = "NextIndex";
    private static final String SLOT_TEMPLATE_ID = "TemplateID";
    private static final String SLOT_NEW_AMOUNT = "NewAmount";
    private static final String CARD_TITLE = "Transfer templates";

    private static final int TEMPLATE_LIMIT = 3;

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(LIST_TRANSFER_TEMPLATES_INTENT))
                || input.matches(Predicates.intentName(DELETE_TRANSFER_TEMPLATES_INTENT))
                || input.matches(Predicates.intentName(EDIT_TRANSFER_TEMPLATE_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        switch (intentRequest.getIntent().getName()) {
            case LIST_TRANSFER_TEMPLATES_INTENT:
                return getTemplates(input, intentRequest);
            case DELETE_TRANSFER_TEMPLATES_INTENT:
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return deleteTemplate(input, intentRequest);
                } else {
                    return response(input, CARD_TITLE);
                }
            default:
                //EDIT_TRANSFER_TEMPLATE_INTENT
                if (intentRequest.getIntent().getConfirmationStatus() == IntentConfirmationStatus.CONFIRMED) {
                    return editTemplate(input, intentRequest);
                } else {
                    return response(input, CARD_TITLE);
                }
        }
    }

    private Optional<Response> editTemplate(HandlerInput input, IntentRequest intentRequest) {
        Map<String, Slot> slots = intentRequest.getIntent().getSlots();
        int templatId = Integer.valueOf(slots.get(SLOT_TEMPLATE_ID).getValue());
        double amount = Double.valueOf(slots.get(SLOT_NEW_AMOUNT).getValue());

        ArrayList<TransferTemplateDB> templates = new ArrayList<>(dynamoDbMapper.loadAll(TransferTemplateDB.class));
        for (TransferTemplateDB template : templates) {
            if (template.getId() == templatId) {
                template.setAmount(amount);
                dynamoDbMapper.save(template);
                return response(input, CARD_TITLE, "Vorlage wurde erfolgreich gespeichert.");
            }
        }
        return response(input, CARD_TITLE, "Ich habe deine angegebene Vorlage id nicht gefunden.");
    }

    private Optional<Response> deleteTemplate(HandlerInput input, IntentRequest intentRequest) {
        int templatId = Integer.valueOf(intentRequest.getIntent().getSlots().get(SLOT_TEMPLATE_ID).getValue());
        ArrayList<TransferTemplateDB> templates = new ArrayList<>(dynamoDbMapper.loadAll(TransferTemplateDB.class));
        for (TransferTemplateDB template : templates) {
            if (template.getId() == templatId) {
                dynamoDbMapper.delete(new TransferTemplateDB(templatId));
                return response(input, CARD_TITLE, "Vorlage wurde gelöscht.");
            }
        }
        return response(input, CARD_TITLE, "Ich habe deine angegebene Vorlage id nicht gefunden.");
    }

    private Optional<Response> getTemplates(HandlerInput input, IntentRequest intentRequest) {
        Slot nextIndex = intentRequest.getIntent().getSlots().get(SLOT_NEXT_INDEX);
        ArrayList<TransferTemplateDB> templates = new ArrayList<>(dynamoDbMapper.loadAll(TransferTemplateDB.class));
        if (templates == null || templates.size() == 0) {
            return response(input, CARD_TITLE, "Keine Vorlagen vorhanden.");
        }

        if (nextIndex.getValue() == null) {
            // first
            StringBuilder builder = new StringBuilder("Du hast " + templates.size() + " Vorlage. ");
            for (int i = 0; i < TEMPLATE_LIMIT; i++) {
                if (i < templates.size()) {
                    TransferTemplateDB template = templates.get(i);
                    builder.append("Vorlage ")
                            .append(template.getId())
                            .append(" vom ").append(DateUtil.formatter.format(template.getCreatedAt()))
                            .append(": ").append("Überweise ").append(template.getAmount()).append(" Euro an ")
                            .append(template.getTarget()).append(". ");
                }
            }
            if (templates.size() > TEMPLATE_LIMIT) {
                builder.append("Moechtest du einen weiteren Vorlage hoeren?");
                intentRequest.getIntent().getSlots().put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(TEMPLATE_LIMIT))
                        .withName(SLOT_NEXT_INDEX).build());
                return responseWithSlotConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent(), SLOT_NEXT_INDEX);
            } else {
                return response(input, CARD_TITLE, builder.toString());
            }
        } else if (nextIndex.getConfirmationStatus() == SlotConfirmationStatus.CONFIRMED) {
            // read more if confirmed
            int position = Integer.parseInt(nextIndex.getValue());
            StringBuilder builder = new StringBuilder();
            TransferTemplateDB template = templates.get(position);
            builder.append("Vorlage ")
                    .append(template.getId())
                    .append(" vom ").append(DateUtil.formatter.format(template.getCreatedAt()))
                    .append(": ").append("Überweise ").append(template.getAmount()).append(" Euro an ")
                    .append(template.getTarget()).append(". ");
            position++;
            if (position < templates.size()) {
                builder.append("Moechtest du einen weiteren Vorlage hoeren?");
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
