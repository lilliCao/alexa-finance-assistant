package amosalexa.handlers;

import amosalexa.AccountFactory;
import amosalexa.handlers.bankaccount.*;
import amosalexa.handlers.bankcontact_not_work.BankContactServiceHandler;
import amosalexa.handlers.budgetreport.BudgetReportServiceHandler;
import amosalexa.handlers.budgettracker.BudgetTrackerServiceHandler;
import amosalexa.handlers.budgettracker.EditCategoriesServiceHandler;
import amosalexa.handlers.cards.BlockCardServiceHandler;
import amosalexa.handlers.cards.ReplaceCardServiceHandler;
import amosalexa.handlers.contacts.ContactServiceHandler;
import amosalexa.handlers.financing.*;
import amosalexa.handlers.help.HelpServiceHandler;
import amosalexa.handlers.securitiesAccount.SecuritiesAccountInformationServiceHandler;
import amosalexa.handlers.transferTemplate.TransferTemplateServiceHandler;
import api.aws.DynamoDbMapper;
import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import model.banking.Account;

public class AmosStreamHandler extends SkillStreamHandler {

    public static final String USER_ID = "4711";
    private static final Account demoAccount = AccountFactory.getInstance().createDemo();
    public static final String ACCOUNT_NUMBER = demoAccount.getNumber();
    public static final String ACCOUNT_IBAN = demoAccount.getIban();
    public static final String ACCOUNT_SAVING_NUMBER = AccountFactory.savingAccount.getNumber();
    public static final String ACCOUNT_SAVING_IBAN = AccountFactory.savingAccount.getIban();

    public static DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();
    public static final Number SECURITIES_ACCOUNT_ID = 1;
    public static final String BANK_NAME = "Sparkasse";

    private static Skill getSkill() {
        return Skills.standard()
                .addRequestHandlers(
                        new LaunchRequestHandler(),
                        new CancelandStopIntentHandler(),
                        new SessionEndedRequestHandler(),
                        new FallbackIntentHandler(),

                        //new VoicePinHandler(),
                        //new TanHandler(),

                        //balance
                        new BalanceLimitGetHandler(),
                        new BalanceLimitSetHandler(),
                        new AccountInformationIntentHandler(),
                        new ContactTransferServiceHandler(),
                        //standingorder
                        new StandingOrderInfoHandler(),
                        new StandingOrderDeleteHandler(),
                        new StandingOrderModifyHandler(),
                        new StandingOrderKeywordHandler(),
                        new StandingOrderSmartHandler(),

                        new BudgetReportServiceHandler(),

                        new BudgetTrackerServiceHandler(),
                        new EditCategoriesServiceHandler(),

                        new BlockCardServiceHandler(),
                        new ReplaceCardServiceHandler(),

                        new ContactServiceHandler(),

                        new HelpServiceHandler(),

                        new SecuritiesAccountInformationServiceHandler(),

                        new TransferTemplateServiceHandler(),

                        new BankContactServiceHandler(),

                        new AccountBalanceForecastServiceHandler(),
                        new TransactionForecastServiceHandler(),
                        new PeriodicTransactionServiceHandler(),
                        new AffordabilityServiceHandler(),
                        new SavingPlanServiceHandler()
                )
                //.withSkillId("amzn1.ask.skill.41bcb0a0-9336-4dc6-b8dd-e145531c8ec5") comment because of lambda verification problem
                .addExceptionHandler(new GenericExceptionHandler())
                .build();
    }

    public AmosStreamHandler() {
        super(getSkill());
    }

}
