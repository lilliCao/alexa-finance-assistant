package amosalexa.services.transfertemplates;

import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import api.aws.DynamoDbClient;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.TransferTemplate;

import java.util.*;

public class TransferTemplateService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                LIST_TRANSFER_TEMPLATES_INTENT,
                DELETE_TRANSFER_TEMPLATES_INTENT,
                EDIT_TRANSFER_TEMPLATE_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                LIST_TRANSFER_TEMPLATES_INTENT,
                DELETE_TRANSFER_TEMPLATES_INTENT,
                EDIT_TRANSFER_TEMPLATE_INTENT,
                YES_INTENT,
                NO_INTENT
        );
    }

    /**
     * Default value for cards
     */
    private static final String TRANSFER_TEMPLATES = "Transfer templates";

    /**
     * ties the Speechlet Subject (Amos Alexa Speechlet) with an Speechlet Observer
     *
     * @param speechletSubject service
     */
    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for(String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    private static final String LIST_TRANSFER_TEMPLATES_INTENT = "ListTransferTemplatesIntent";
    private static final String DELETE_TRANSFER_TEMPLATES_INTENT = "DeleteTransferTemplatesIntent";
    private static final String EDIT_TRANSFER_TEMPLATE_INTENT = "EditTransferTemplateIntent";

    public TransferTemplateService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();
        String intentName = request.getIntent().getName();

        if (YES_INTENT.equals(intentName)) {
            Integer offset = (Integer) session.getAttribute("TransferTemplateService.offset");
            Integer templateId = (Integer) session.getAttribute("TransferTemplateService.delete");
            Integer editTemplateId = (Integer) session.getAttribute("TransferTemplateService.editTemplateId");

            if (offset != null) {
                return tellTemplates(session, offset, 3);
            }

            if (templateId != null) {
                TransferTemplate transferTemplate = new TransferTemplate(templateId);
                DynamoDbClient.instance.deleteItem(TransferTemplate.TABLE_NAME, transferTemplate);

                return getResponse(TRANSFER_TEMPLATES, "Vorlage wurde gelöscht.");
            }

            if (editTemplateId != null) {
                Double newAmount = Double.parseDouble(session.getAttribute("TransferTemplateService.newAmount").toString());

                TransferTemplate transferTemplate = (TransferTemplate) DynamoDbClient.instance.getItem(TransferTemplate.TABLE_NAME, editTemplateId, TransferTemplate::new);
                transferTemplate.setAmount(newAmount);
                DynamoDbClient.instance.putItem(TransferTemplate.TABLE_NAME, transferTemplate);

                return getResponse(TRANSFER_TEMPLATES, "Vorlage wurde erfolgreich gespeichert.");
            }
        } else if (NO_INTENT.equals(intentName)) {
            if (session.getAttribute("TransferTemplateService.offset") != null ||
                    session.getAttribute("TransferTemplateService.delete") != null ||
                    session.getAttribute("TransferTemplateService.editTemplateId") != null ||
                    session.getAttribute("TransferTemplateService.newAmount") != null) {
                return getResponse(TRANSFER_TEMPLATES, "Okay, dann halt nicht. Tschüss!");
            }
        } else if (LIST_TRANSFER_TEMPLATES_INTENT.equals(intentName)) {
            return tellTemplates(session, 0, 3);
        } else if (DELETE_TRANSFER_TEMPLATES_INTENT.equals(intentName)) {
            String templateIdStr = request.getIntent().getSlot("TemplateID").getValue();

            if (templateIdStr == null || templateIdStr.equals("")) {
                return null;
            } else {
                int templateId = Integer.parseInt(templateIdStr);
                session.setAttribute("TransferTemplateService.delete", templateId);

                return getAskResponse(TRANSFER_TEMPLATES, "Möchtest du Vorlage Nummer " + templateId + " wirklich löschen?");
            }
        } else if (EDIT_TRANSFER_TEMPLATE_INTENT.equals(intentName)) {
            String templateIdStr = request.getIntent().getSlot("TemplateID").getValue();
            String newAmountStr = request.getIntent().getSlot("NewAmount").getValue();

            if (templateIdStr == null || templateIdStr.equals("") || newAmountStr == null || newAmountStr.equals("")) {
                return null;
            } else {
                int templateId = Integer.parseInt(templateIdStr);

                TransferTemplate template = (TransferTemplate) DynamoDbClient.instance.getItem(TransferTemplate.TABLE_NAME, templateId, TransferTemplate::new);

                if (template == null) {
                    return getResponse(TRANSFER_TEMPLATES, "Ich kann Vorlage " + templateId + " nicht finden.");
                }

                double newAmount = 0;
                try {
                    newAmount = Double.parseDouble(newAmountStr);
                } catch (NumberFormatException ignored) {
                    // TODO: Maybe do some error handling here
                }

                session.setAttribute("TransferTemplateService.editTemplateId", templateId);
                session.setAttribute("TransferTemplateService.newAmount", newAmount);

                return getAskResponse(TRANSFER_TEMPLATES, "Möchtest du den Betrag von Vorlage " + templateId + " von " + template.getAmount() + " auf " + newAmount + " ändern?");
            }
        }

        return null;
    }

    private SpeechletResponse tellTemplates(Session session, int offset, int limit) {
        List<TransferTemplate> templateList = DynamoDbClient.instance.getItems(TransferTemplate.TABLE_NAME, TransferTemplate::new);
        List<TransferTemplate> templates = new ArrayList<>(templateList);

        if (offset >= templates.size()) {
            session.setAttribute("TransferTemplateService.offset", null);
            return getResponse(TRANSFER_TEMPLATES, "Keine weiteren Vorlagen.");
        }

        if (offset + limit >= templates.size()) {
            limit = templates.size() - offset;
        }

        Collections.sort(templates);

        StringBuilder response = new StringBuilder();

        for (int i = offset; i < offset + limit; i++) {
            TransferTemplate template = templates.get(i);

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(template.getCreatedAt());
            String dateFormatted = String.format("01.%02d.%d", calendar.get(Calendar.MONTH) + 2, calendar.get(Calendar.YEAR));

            response.append("Vorlage ")
                    .append(template.getId())
                    .append(" vom ")
                    .append(dateFormatted)
                    .append(": ");
            response.append("Überweise ")
                    .append(template.getAmount())
                    .append(" Euro an ")
                    .append(template.getTarget())
                    .append(". ");
        }

        boolean isAskResponse = templates.size() > offset + limit;

        if (isAskResponse) {
            response.append("Weitere Vorlagen vorlesen?");
            session.setAttribute("TransferTemplateService.offset", offset + limit);
            return getAskResponse(TRANSFER_TEMPLATES, response.toString());
        } else {
            response.append("Keine weiteren Vorlagen.");
            session.setAttribute("TransferTemplateService.offset", null);
            return getResponse(TRANSFER_TEMPLATES, response.toString());
        }
    }

}
