package amosalexa.handlers.financing;

import amosalexa.handlers.Service;
import amosalexa.handlers.utils.DateUtil;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;
import model.banking.StandingOrder;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static amosalexa.handlers.AmosStreamHandler.*;
import static amosalexa.handlers.ResponseHelper.*;


@Service(
        functionGroup = Service.FunctionGroup.SMART_FINANCING,
        functionName = "Sparplan erstellen",
        example = "Erstelle einen Sparplan!",
        description = "Mit dieser Funktion kannst du einen individuellen Sparplan erstellen. Du kannst dabei Parameter wie " +
                "Laufzeit, monatlicher Sparbetrag und initialer Sparbetrag festlegen."
)
public class SavingPlanServiceHandler implements IntentRequestHandler {
    private static final String SAVINGS_PLAN_INTRO_INTENT = "SavingsPlanIntroIntent";

    private static final String SLOT_BASIC_AMOUNT = "BasicAmount";
    private static final String SLOT_DURATION = "Duration";
    private static final String SLOT_MONTHLY_PAYMENT = "MonthlyPayment";

    private static final String SAVE_BASIC_AMOUNT = "SaveBasicAmount";
    private static final String SAVE_DURATION = "SaveDuration";
    private static final String SAVE_MONTHLY_PAYMENT = "SaveMonthlyPayment";

    private static final String CARD_TITLE = "Sparplan";

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName(SAVINGS_PLAN_INTRO_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        switch (intentRequest.getIntent().getConfirmationStatus()) {
            case NONE:
                Map<String, Slot> slots = intentRequest.getIntent().getSlots();
                int amount = Integer.valueOf(slots.get(SLOT_BASIC_AMOUNT).getValue());
                int duration = Integer.valueOf(slots.get(SLOT_DURATION).getValue());
                int monthlyPayment = Integer.valueOf(slots.get(SLOT_MONTHLY_PAYMENT).getValue());

                String calculationString = calculateSavings(amount, monthlyPayment, duration);

                StringBuilder builder = new StringBuilder();
                builder.append("<speak>").append("Mit ").append(amount).append(" Euro als Grundbetrag, ")
                        .append(monthlyPayment).append(" Euro als monatliche Einzahlung")
                        .append(" und bei einem Zinssatz von zwei Prozent waere der Gesamtsparbetrag am Ende des Zeitraums ")
                        .append(duration).append(duration == 1 ? "Jahr" : " Jahre")
                        .append(" insgesamt <say-as interpret-as=\"number\">").append(calculationString)
                        .append("</say-as> Euro. Soll ich diesen Sparplan fuer dich anlegen?</speak>");

                Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();
                sessionAttributes.put(SAVE_BASIC_AMOUNT, amount);
                sessionAttributes.put(SAVE_MONTHLY_PAYMENT, monthlyPayment);
                input.getAttributesManager().setSessionAttributes(sessionAttributes);

                return responseWithIntentConfirm(input, CARD_TITLE, builder.toString(), intentRequest.getIntent());

            case CONFIRMED:
                Map<String, Object> sessionAtt = input.getAttributesManager().getSessionAttributes();
                Number saveAmount = (Integer) sessionAtt.get(SAVE_BASIC_AMOUNT);
                Number saveMonthlyPayment = (Integer) sessionAtt.get(SAVE_MONTHLY_PAYMENT);

                responseDirective(input, "Ich lege jetzt deinen Sparplan an. Bitte warte kurz.");
                // transfer basic amount
                String valueDate = DateUtil.formatterStandard.format(new Date());
                TransactionAPI.createTransaction(saveAmount, ACCOUNT_IBAN, ACCOUNT_SAVING_IBAN, valueDate,
                        "Sparplan Einmaleinzahlung", "demo payee sparplan", "demo remitter sparplan");

                // monthly
                StandingOrder standingOrder = AccountAPI.createStandingOrderForAccount(ACCOUNT_NUMBER, "demo payee monthly savings", saveMonthlyPayment,
                        ACCOUNT_SAVING_IBAN, DateUtil.getFirstDayNextMonth(), StandingOrder.ExecutionRate.MONTHLY,
                        "Sparplan regelm. Einzahlung");


                String speechtext = "Okay! Ich habe den Sparplan angelegt. Der Grundbetrag von " + saveAmount + " Euro wird deinem Sparkonto " +
                        "gutgeschrieben. Die erste regelmaeßige Einzahlung von " + saveMonthlyPayment + " Euro erfolgt am "
                        + standingOrder.getFirstExecutionSpeechString() + ".";

                return response(input, CARD_TITLE, speechtext);

            default:
                return response(input, CARD_TITLE);

        }
    }


    private String calculateSavings(int amount, int monthlyPayment, int duration) {
        DecimalFormat df = new DecimalFormat("#.##");

        // TODO depends on bank and amount -> get from API
        double r = 0.02;
        //The number of times that interest is compounded per year (monthly = 12)
        double n = 12;

        double amount1 = amount * Math.pow(1 + (r / n), (n * duration));
        double amount2 = monthlyPayment * (((Math.pow((1 + r / n), n * duration)) - 1) / (r / n)); //* (1+ (r/n));
        //Note that last multiplication is optional! (Two ways of calculating)

        double totalAmount = amount1 + amount2;
        return df.format(totalAmount);
    }

}