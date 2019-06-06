package amosalexa.handlers;
/*
import amosalexa.handlers.bankaccount.AccountInformationIntentHandler;
import amosalexa.handlers.bankaccount.BalanceLimitServiceHandler;
import amosalexa.handlers.bankaccount.StandingOrderServiceHandler;
import amosalexa.handlers.budgetreport.BudgetReportServiceHandler;
import amosalexa.handlers.budgettracker.BudgetTrackerServiceHandler;
import amosalexa.handlers.financing.AccountBalanceForecastServiceHandler;
import amosalexa.handlers.financing.AffordabilityServiceHandler;
import amosalexa.handlers.financing.SavingPlanServiceHandler;
import amosalexa.handlers.financing.TransactionForecastServiceHandler;
import amosalexa.handlers.securitiesAccount.SecuritiesAccountInformationServiceHandler;
import com.amazon.ask.model.IntentConfirmationStatus;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SlotConfirmationStatus;
import com.amazon.ask.model.slu.entityresolution.Resolution;
import com.amazon.ask.model.slu.entityresolution.Resolutions;
import com.amazon.ask.model.slu.entityresolution.Value;
import com.amazon.ask.model.slu.entityresolution.ValueWrapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static amosalexa.handlers.Utils.createIntentRequest;
import static amosalexa.handlers.Utils.test;
import static amosalexa.handlers.bankaccount.AccountInformationIntentHandler.*;
import static amosalexa.handlers.bankaccount.BalanceLimitServiceHandler.*;
import static amosalexa.handlers.budgetreport.BudgetReportServiceHandler.BUDGET_REPORT_EMAIL_INTENT;
import static amosalexa.handlers.budgettracker.BudgetTrackerServiceHandler.*;

public class AmosHandlerTest {

    @Test
    public void testGetBalanceimitIntentHandler() {
        createIntentRequest(GET_BALANCE_LIMIT_INTENT);
        test(new BalanceLimitServiceHandler(), "(.*)");
        Map<String, Slot> slots=new HashMap<>();
        slots.put("VoicePin", Slot.builder().withName("VoicePin").withValue("1111").build());
        createIntentRequest(GET_BALANCE_LIMIT_INTENT, slots);
        test(new BalanceLimitServiceHandler(), "Dein aktuelles Kontolimit beträgt (.*) Euro.");

    }

    @Test
    public void testSetBalanceLimitIntentHandler() {
        Map<String, Slot> slots = new HashMap<>();
        slots.put(SLOT_BALANCE_LIMIT_AMOUNT, Slot.builder().withValue("100").build());
        createIntentRequest(SET_BALANCE_LIMIT_INTENT, slots, IntentConfirmationStatus.CONFIRMED);
        test(new BalanceLimitServiceHandler(), "Okay, dein Kontolimit wurde auf (.*) Euro gesetzt.");
    }

    @Test
    public void testAccountInformationIntentHandler() {
        AccountInformationIntentHandler handler = new AccountInformationIntentHandler();
        Map<String, Slot> slots = new HashMap<>();
        slots.put(SLOT_ACCOUNT_KEYWORD, Slot.builder()
                .withResolutions(Resolutions.builder().addResolutionsPerAuthorityItem(
                        Resolution.builder()
                                .addValuesItem(ValueWrapper.builder().withValue(Value.builder().withName("Kontostand").build()).build())
                                .build())
                        .build())
                .build());
        createIntentRequest(ACCOUNT_INFORMATION_INTENT, slots);
        test(handler, "Dein Kontostand beträgt (.*)");

        //transaction
        slots.clear();
        slots.put(SLOT_ACCOUNT_KEYWORD, Slot.builder()
                .withResolutions(Resolutions.builder().addResolutionsPerAuthorityItem(
                        Resolution.builder()
                                .addValuesItem(ValueWrapper.builder().withValue(Value.builder().withName("Transaktionen").build()).build())
                                .build())
                        .build())
                .build());
        slots.put(SLOT_NEXT_INDEX, Slot.builder().withValue(null).build());
        createIntentRequest(ACCOUNT_INFORMATION_INTENT, slots);
        test(handler, "Du hast keine Transaktionen in deinem Konto" +
                "|Du hast \\d+ Transaktionen. (.*)");

        // be sure to have more than 1 transaction to test this
        slots.remove(SLOT_NEXT_INDEX);
        slots.put(SLOT_NEXT_INDEX, Slot.builder().withValue(String.valueOf(TRANSAKTIONEN_LIMIT))
                .withConfirmationStatus(SlotConfirmationStatus.CONFIRMED).build());
        createIntentRequest(ACCOUNT_INFORMATION_INTENT, slots);
        test(handler, "Nummer \\d+ (.*)");
    }

    @Test
    public void testStandingOrderSmart() {
        StandingOrderServiceHandler handler = new StandingOrderServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put("StandingOrderKeyword", Slot.builder().withValue("Miete").build());
        slots.put("NextIndex", Slot.builder().build());
        slots.put("TransactionKeyword", Slot.builder().build());

        createIntentRequest("StandingOrdersKeywordIntent", slots);
        test(handler, "(.*)");
    }

    @Test
    public void testBudgetReportIntentHandler() {
        createIntentRequest(BUDGET_REPORT_EMAIL_INTENT);
        test(new BudgetReportServiceHandler(), "Okay, ich habe dir deinen Ausgabenreport per E-Mail gesendet.");
    }

    @Test
    public void testCategoryLimitInfoIntentHandler() {
        BudgetTrackerServiceHandler handler = new BudgetTrackerServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put(SLOT_CATEGORY, Slot.builder().withValue("freizeit").build());
        createIntentRequest(CATEGORY_LIMIT_INFO_INTENT, slots);
        test(handler, "Das Limit fuer die Kategorie freizeit"
                + " liegt bei (.*) Euro.");

        // not contains
        slots.clear();
        slots.put(SLOT_CATEGORY, Slot.builder().withValue("whatever").build());
        createIntentRequest(CATEGORY_LIMIT_INFO_INTENT, slots);
        test(handler, "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder"
                + " erhalte Information zu den verfuegbaren Kategorien.");
    }

    @Test
    public void testCategoryLimitSetIntentHandler() {
        BudgetTrackerServiceHandler handler = new BudgetTrackerServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        String categoryName = "freizeit";
        String amount = "100";
        slots.put(SLOT_CATEGORY, Slot.builder().withValue(categoryName).build());
        slots.put(SLOT_CATEGORY_LIMIT, Slot.builder().withValue(amount).build());
        createIntentRequest(CATEGORY_LIMIT_SET_INTENT, slots, IntentConfirmationStatus.CONFIRMED);
        test(handler, "Limit für " + categoryName + " wurde auf " + amount + "(.*) gesetzt");
    }

    @Test
    public void testCategoryStatusInfoIntentHandler() {
        BudgetTrackerServiceHandler handler = new BudgetTrackerServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put(SLOT_CATEGORY, Slot.builder().withValue("freizeit").build());
        createIntentRequest(CATEGORY_STATUS_INFO_INTENT, slots);
        test(handler, "Du hast bereits (.*)% des Limits von (.*) Euro fuer" +
                " die Kategorie (.*) ausgegeben. " +
                "Du kannst noch (.*) Euro für diese Kategorie ausgeben.");

        // not contains
        slots.clear();
        slots.put(SLOT_CATEGORY, Slot.builder().withValue("whatever").build());
        createIntentRequest(CATEGORY_STATUS_INFO_INTENT, slots);
        test(handler, "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder"
                + " erhalte Information zu den verfuegbaren Kategorien.");
    }

    @Test
    public void testCategorySpendingIntentHandler() {
        BudgetTrackerServiceHandler handler = new BudgetTrackerServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put(SLOT_CATEGORY, Slot.builder().withValue("freizeit").build());
        slots.put(SLOT_AMOUNT, Slot.builder().withValue("10").build());
        createIntentRequest(CATEGORY_SPENDING_INTENT, slots, IntentConfirmationStatus.CONFIRMED);
        test(handler, "Okay. Ich habe (.*) Euro fuer freizeit notiert.(.*)");

        // not contains
        slots.clear();
        slots.put(SLOT_CATEGORY, Slot.builder().withValue("robot").build());
        slots.put(SLOT_AMOUNT, Slot.builder().withValue("10").build());
        createIntentRequest(CATEGORY_SPENDING_INTENT, slots, IntentConfirmationStatus.CONFIRMED);
        test(handler, "Ich konnte diese Kategorie nicht finden. Soll ich eine Kategorie mit dem Namen (.*) anlegen und den Betrag (.*) Euro hinzufuegen\\?");
        createIntentRequest(CATEGORY_SPENDING_INTENT, slots, IntentConfirmationStatus.CONFIRMED);
        test(handler, "Ich habe die Kategorie (.*) erstellt und (.*) Euro ");
    }

    @Test
    public void testAccountBalanceForecastServiceHandler() {
        AccountBalanceForecastServiceHandler handler = new AccountBalanceForecastServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put("TargetDate", Slot.builder().withValue("2020-05-20").build());
        createIntentRequest("AccountBalanceForecast", slots);
        test(handler, "(.*)");
    }

    @Test
    public void testTransactionForecastServiceHandler() {
        TransactionForecastServiceHandler handler = new TransactionForecastServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put("TargetDate", Slot.builder().withValue("2020-05-20").build());
        slots.put("NextIndex", Slot.builder().build());

        createIntentRequest("TransactionForecast", slots);
        test(handler, "(.*)");
    }

    @Test
    public void testSavingPlanServiceHandler() {
        SavingPlanServiceHandler handler = new SavingPlanServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put("BasicAmount", Slot.builder().withValue("200").build());
        slots.put("Duration", Slot.builder().withValue("3").build());
        slots.put("MonthlyPayment", Slot.builder().withValue("3").build());

        createIntentRequest("SavingsPlanIntroIntent", slots, IntentConfirmationStatus.NONE);
        test(handler, "(.*)");
    }

    @Test
    public void testAffordabilityServiceHandler() {
        AffordabilityServiceHandler handler = new AffordabilityServiceHandler();
        Map<String, Slot> slots = new HashMap<>();

        // contains
        slots.put("ProductKeyword", Slot.builder().withValue("egal").build());
        slots.put("ProductSelection", Slot.builder().build());

        createIntentRequest("AffordProduct", slots, IntentConfirmationStatus.NONE);
        test(handler, "(.*)");

        slots.put("ProductSelection", Slot.builder().withValue("5").build());
        createIntentRequest("AffordProduct", slots, IntentConfirmationStatus.NONE);
        test(handler, "(.*)");

        slots.put("ProductSelection", Slot.builder().withValue("2").build());
        createIntentRequest("AffordProduct", slots, IntentConfirmationStatus.NONE);
        test(handler, "(.*)");
    }

    @Test
    public void testSecuritiesAccountInformation() {
        SecuritiesAccountInformationServiceHandler handler = new SecuritiesAccountInformationServiceHandler();
        Map<String, Slot> slots  = new HashMap<>();
        slots.put("NextIndex", Slot.builder().build());
        createIntentRequest("SecuritiesAccountInformationIntent", slots);
        test(handler, ".*");
    }
}
 */
