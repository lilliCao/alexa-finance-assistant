package amosalexa;

import api.aws.DynamoDbMapper;
import api.banking.AccountAPI;
import model.banking.Account;
import model.banking.StandingOrder;
import model.db.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DemoAccount {

    private static Logger LOGGER = LoggerFactory.getLogger(DemoAccount.class);
    private static DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();

    public static void main(String[] args) throws InterruptedException {
        // create demo account from scratch
        createDemoAccount();
        // get current account information
        getAllDatabaseInfo();
    }

    public static void createDemoAccount() throws InterruptedException {
        createTables();
        LOGGER.info("Attempt to create demo account");
        Account demoAccount = AccountFactory.getInstance().createDemo();
        LOGGER.info("Succeeded creating demo account");
        LOGGER.info(demoAccount.toString());
    }

    private static void createTables() throws InterruptedException {
        // Drop and recreate tables
        LOGGER.info("Drop all tables");
        LOGGER.info("Drop account db");
        dynamoDbMapper.dropTable(AccountDB.class);
        LOGGER.info("Drop category");
        dynamoDbMapper.dropTable(Category.class);
        LOGGER.info("Drop contact");
        dynamoDbMapper.dropTable(Contact.class);
        LOGGER.info("Drop spending");
        dynamoDbMapper.dropTable(Spending.class);
        LOGGER.info("Drop transaction");
        dynamoDbMapper.dropTable(TransactionDB.class);
        LOGGER.info("Drop user");
        dynamoDbMapper.dropTable(model.db.User.class);
        LOGGER.info("Drop last_ids");
        dynamoDbMapper.dropTable(LastIds.class);
        LOGGER.info("Drop template");
        dynamoDbMapper.dropTable(TransferTemplateDB.class);

        LOGGER.info("Create all tables");
        LOGGER.info("Create account db");
        dynamoDbMapper.createTable(AccountDB.class);
        LOGGER.info("Create category");
        dynamoDbMapper.createTable(Category.class);
        LOGGER.info("Create contact");
        dynamoDbMapper.createTable(Contact.class);
        LOGGER.info("Create spending");
        dynamoDbMapper.createTable(Spending.class);
        LOGGER.info("Create transaction");
        dynamoDbMapper.createTable(TransactionDB.class);
        LOGGER.info("Create user");
        dynamoDbMapper.createTable(model.db.User.class);
        LOGGER.info("Create last_ids");
        dynamoDbMapper.createTable(LastIds.class);
        LOGGER.info("Create template");
        dynamoDbMapper.createTable(TransferTemplateDB.class);
    }

    private static void getAllDatabaseInfo() {
        LOGGER.info("Current data in database");
        currentAcc();
        currentCate();
        currentContact();
        currentSpending();
        currentStandingorder();
        currentTransaction();
        currentTemplate();
    }

    private static void currentAcc() {
        LOGGER.info("Current accounts");
        List<AccountDB> accounts = dynamoDbMapper.loadAll(AccountDB.class);
        accounts.stream().forEach(e -> {
            System.out.format("acc nr=%s saving nr=%s\n",
                    e.getAccountNumber(),
                    e.getSavingsAccountNumber());
        });
    }

    private static void currentCate() {
        LOGGER.info("Current category");
        List<Category> cates = dynamoDbMapper.loadAll(Category.class);
        cates.stream().forEach(e -> {
            System.out.format("Cate=%s acc nr=%s limit=%.2f spending=%.2f\n",
                    e.getName(),
                    e.getAccountNumber(),
                    e.getLimit(),
                    e.getSpending());
        });
    }

    private static void currentContact() {
        LOGGER.info("Current contact");
        List<Contact> obs = dynamoDbMapper.loadAll(Contact.class);
        obs.stream().forEach(e -> {
            System.out.format("Contact name=%s acc nr=%s iban=%s\n",
                    e.getName(),
                    e.getAccountNumber(),
                    e.getIban());
        });
    }

    private static void currentSpending() {
        LOGGER.info("Current spending");
        List<Spending> obs = dynamoDbMapper.loadAll(Spending.class);
        obs.stream().forEach(e -> {
            System.out.format("Spend on cate = %s acc nr=%s amount=%.2f\n",
                    e.getCategoryId(),
                    e.getAccountNumber(),
                    e.getAmount());
        });
    }

    private static void currentStandingorder() {
        LOGGER.info("Current standingorder");
        List<StandingOrder> obs = new ArrayList
                (AccountAPI.getStandingOrdersForAccount(AccountFactory.account.getNumber()));
        obs.stream().forEach(e -> {
            System.out.format("Standingorder id=%s amount=%f %s to %s \n",
                    e.getStandingOrderId(),
                    e.getAmount().doubleValue(),
                    e.getExecutionRateString(),
                    e.getPayee());
        });
    }

    private static void currentTransaction() {
        LOGGER.info("Current periodic transaction");
        List<TransactionDB> obs = dynamoDbMapper.loadAll(TransactionDB.class);
        obs.stream().forEach(e -> {
            System.out.format("Transaction id=%s acc nr=%s cate id=%s peridic=%b\n",
                    e.getTransactionId(),
                    e.getAccountNumber(),
                    e.getCategoryId(),
                    e.isPeriodic());
        });
    }

    private static void currentTemplate() {
        LOGGER.info("Current template");
        List<TransferTemplateDB> obs = dynamoDbMapper.loadAll(TransferTemplateDB.class);
        obs.stream().forEach(e -> {
            System.out.format("Template id=%d amount=%f date=%s target=%s\n",
                    e.getId(),
                    e.getAmount(),
                    e.getCreatedAt().toString(),
                    e.getTarget());
        });
    }

}
