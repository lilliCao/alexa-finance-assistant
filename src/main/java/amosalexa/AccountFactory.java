package amosalexa;

import api.aws.DynamoDbMapper;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import model.banking.Account;
import model.banking.StandingOrder;
import model.banking.Transaction;
import model.db.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static amosalexa.handlers.AmosStreamHandler.USER_ID;

public class AccountFactory {

    private static final Logger log = LoggerFactory.getLogger(AccountFactory.class);
    private static AccountFactory accountFactory = new AccountFactory();
    private static final long ACCOUNT_BALANCE_DEMO = 1000000;
    private static final String ACCOUNT_OPENING_DATE_DEMO = new DateTime().minusMonths(1).toString("yyyy-MM-dd");
    private static final String TODAY_DATE = new DateTime().toString("yyyy-MM-dd");
    private static final String TWO_MONTH_FROM_NOW = (new DateTime()).plusMonths(2).toString("yyyy-MM-dd");
    private static DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();

    public static AccountFactory getInstance() {
        synchronized (AccountFactory.class) {
            return accountFactory;
        }
    }

    public static Account account;
    public static Account savingAccount;

    /**
     * 7 transactions
     * 3 standing orders - with categories
     * 3 stocks
     * 5 categories with 3 keywords
     * 3 contacts with easy names
     */
    public Account createDemo() {
        log.info("-------------------------Creating demo accounts and other necessary demo data-------------------------");
        // Uncomment the following line to create and use NEW demo accounts
        //log.info("-------------------------Removing old accounts");
        //removeDemoAccounts();

        Account existingDemoAccount = getDemoAccount();
        if (existingDemoAccount != null) {
            log.info("Existing demo account " + existingDemoAccount.getNumber());
            savingAccount = createSavingsAccount();
            account = existingDemoAccount;
            return existingDemoAccount;
        }

        // user - needed for authenticating all following API calls
        createDemoUser();

        // create account + savings account
        account = createDemoAccount();
        savingAccount = createSavingsAccount();
        log.info("-------------------------Demo account = " + account.getNumber());
        log.info("-------------------------Saving account = " + savingAccount.getNumber());

        log.info("-------------------------Saving created accounts to db");
        saveAccount(account.getNumber(), savingAccount.getNumber(), true);

        // contact accounts
        log.info("-------------------------Creating contacts account and storing them in db");
        List<Account> contactAccounts = createContactsAccount();
        saveContactAccounts(contactAccounts);

        // categories
        log.info("-------------------------Creating categories");
        createCategories(account);

        // standing orders
        log.info("-------------------------Creating standing orders");
        createStandingOrders(account, contactAccounts);


        // periodic transactions
        log.info("-------------------------Creating transactions");
        createPeriodicTransactions(account);

        //create template
        log.info("-------------------------Creating template");
        createTemplate();

        log.info("-------------------------Finish creating account demo-------------------------");
        return account;
    }

    private void createTemplate() {
        String number = account.getNumber();
        dynamoDbMapper.save(new TransferTemplateDB("demo target 1", 10, number));
        dynamoDbMapper.save(new TransferTemplateDB("demo target 2", 20, number));
        dynamoDbMapper.save(new TransferTemplateDB("demo target 3", 30, number));
        dynamoDbMapper.save(new TransferTemplateDB("demo target 4", 40, number));
    }

    private void createDemoUser() {
        User user = new User();
        user.setId(USER_ID);
        DynamoDbMapper.getInstance().save(user);
    }

    private void saveContactAccounts(List<Account> contactAccounts) {
        String[] names = {"bob", "bobby", "lucas", "max mustermann"};
        int i = 0;
        for (Account contactAccount : contactAccounts) {
            Contact c = new Contact();
            c.setAccountNumber(contactAccount.getNumber());
            c.setName(names[i]);
            c.setIban(contactAccount.getIban());
            dynamoDbMapper.save(c);
            i++;
        }
    }

    private Account createDemoAccount() {
        String accountNumber = getRandomAccountNumber();
        Account newDemoAccount = null;
        if (!existAccount(accountNumber)) {
            newDemoAccount = AccountAPI.createAccount(accountNumber, ACCOUNT_BALANCE_DEMO, ACCOUNT_OPENING_DATE_DEMO);
        }
        return newDemoAccount;
    }

    private Account createSavingsAccount() {
        String accountNumber = getRandomAccountNumber();
        Account newDemoSavingsAccount = null;
        if (!existAccount(accountNumber)) {
            newDemoSavingsAccount = AccountAPI.createAccount(accountNumber, ACCOUNT_BALANCE_DEMO, ACCOUNT_OPENING_DATE_DEMO);
        }
        return newDemoSavingsAccount;
    }

    private List<Account> createContactsAccount() {
        List<Account> contactAccounts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String accountNumber = getRandomAccountNumber();
            Account contactAccount = AccountAPI.createAccount(accountNumber, ACCOUNT_BALANCE_DEMO, ACCOUNT_OPENING_DATE_DEMO);
            contactAccounts.add(contactAccount);
        }
        return contactAccounts;
    }

    private void createCategories(Account newDemoAccount) {
        DynamoDbMapper.getInstance().dropTable(Category.class);
        try {
            DynamoDbMapper.getInstance().createTable(Category.class);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        dynamoDbMapper.save(new Category(newDemoAccount.getNumber(), "auto", 150));
        dynamoDbMapper.save(new Category(newDemoAccount.getNumber(), "lebensmittel", 300));
        dynamoDbMapper.save(new Category(newDemoAccount.getNumber(), "freizeit", 200));
        dynamoDbMapper.save(new Category(newDemoAccount.getNumber(), "reisen", 200));
        dynamoDbMapper.save(new Category(newDemoAccount.getNumber(), "sonstiges", 150));

        List<Category> allCategories = dynamoDbMapper.loadAll(Category.class);
        for (Category cat : allCategories) {
            dynamoDbMapper.save(new Spending(newDemoAccount.getNumber(), cat.getId(), cat.getLimit()));
        }
    }

    private void createStandingOrders(Account demoAccount, List<Account> contactAccounts) {
        for (Account contactAccount : contactAccounts) {
            AccountAPI.createStandingOrderForAccount(demoAccount.getNumber(), getContactName(contactAccount.getNumber()), 50,
                    contactAccount.getIban(), TODAY_DATE, StandingOrder.ExecutionRate.MONTHLY, "Demo Dauerauftrag");
        }
    }

    private void createPeriodicTransactions(Account newDemoAccount) {
        int transactions[] = {31, 32, 33};
        for (int i : transactions) {
            TransactionDB transactionDb = (TransactionDB) dynamoDbMapper.load(TransactionDB.class, Integer.toString(i));

            if (transactionDb == null) {
                Transaction trans = new Transaction();
                trans.setAmount(100);
                trans.setSourceAccount("testIban");
                trans.setDestinationAccount(newDemoAccount.getIban());
                trans.setValueDate(TWO_MONTH_FROM_NOW);
                trans.setDescription("demo description periodic transfer");
                trans.setPayee("demo payee periodic");
                trans.setRemitter("demo remitter periodic");
                Transaction transaction = TransactionAPI.createTransaction(trans);

                transactionDb = new TransactionDB(transaction.getTransactionId().toString());
                transactionDb.setPeriodic(true);
                transactionDb.setAccountNumber(newDemoAccount.getNumber());
                transactionDb.setAccountNumber(newDemoAccount.getNumber());

            }

            dynamoDbMapper.save(transactionDb);
        }
    }

    private String getRandomCategoryId() {
        List<Category> categoryDBList = dynamoDbMapper.loadAll(Category.class);
        int randomNum = ThreadLocalRandom.current().nextInt(0, categoryDBList.size());
        return categoryDBList.get(randomNum).getId();
    }

    private String getContactName(String accountNumber) {
        List<Contact> contactDBList = dynamoDbMapper.loadAll(Contact.class);
        for (Contact contactDB : contactDBList) {
            if (contactDB.getAccountNumber() != null && contactDB.getAccountNumber().equals(accountNumber))
                return contactDB.getName();
        }
        return null;
    }

    private void removeDemoAccounts() {
        log.info("in Removing demo data");
        List<AccountDB> accountDBList = dynamoDbMapper.loadAll(AccountDB.class);
        for (AccountDB accountDB : accountDBList) {
            if (accountDB.isDemo()) {

                log.info("in Removing demo account");
                dynamoDbMapper.delete(accountDB);

                log.info("in Removing others data");
                removeDemoCategories(accountDB.getAccountNumber());
                removeDemoTransactions(accountDB.getAccountNumber());
                removeDemoSpending(accountDB.getAccountNumber());
                removeDemoContacts(accountDB.getAccountNumber());
                removeDemoTemplate(accountDB.getAccountNumber());
            }
        }
    }

    private void removeDemoTemplate(String accountNumber) {
        List<TransferTemplateDB> templateDBList = dynamoDbMapper.loadAll(TransferTemplateDB.class);
        for (TransferTemplateDB template : templateDBList) {
            if (template.getAccountNumber() != null && template.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(template);
            }
        }
    }

    private void removeDemoCategories(String accountNumber) {
        List<Category> categoryDBList = dynamoDbMapper.loadAll(Category.class);
        for (Category categoryDB : categoryDBList) {
            if (categoryDB.getAccountNumber() != null && categoryDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(categoryDB);
            }
        }
    }

    private void removeDemoTransactions(String accountNumber) {
        List<TransactionDB> categoryDBListList = dynamoDbMapper.loadAll(TransactionDB.class);
        for (TransactionDB transactionDB : categoryDBListList) {
            if (transactionDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(transactionDB);
            }
        }
    }

    private void removeDemoSpending(String accountNumber) {
        List<Spending> spendingList = dynamoDbMapper.loadAll(Spending.class);
        for (Spending spending : spendingList) {
            if (spending.getAccountNumber() != null && spending.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(spending);
            }
        }
    }

    private void removeDemoContacts(String accountNumber) {
        List<Contact> contactDBList = dynamoDbMapper.loadAll(Contact.class);
        for (Contact contactDB : contactDBList) {
            if (contactDB.getAccountNumber() != null && contactDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(contactDB);
            }
        }
    }


    private Account getDemoAccount() {
        List<AccountDB> accountDBList = dynamoDbMapper.loadAll(AccountDB.class);
        for (AccountDB accountDB : accountDBList) {
            if (accountDB.isDemo()) {
                return AccountAPI.getAccount(accountDB.getAccountNumber());
            }
        }
        return null;
    }


    public String getDemoAccountNumber() {
        if (getDemoAccount() != null) {
            return getDemoAccount().getNumber();
        }
        return null;
    }

    /**
     * saves account number to db
     *
     * @param accountNumber account number
     * @param isDemo        is valid account for demo
     */
    private void saveAccount(String accountNumber, String savingsAccountNumber, boolean isDemo) {
        dynamoDbMapper.save(new AccountDB(accountNumber, savingsAccountNumber, isDemo));
    }

    private boolean existAccount(String accountNumber) {
        try {
            AccountAPI.getAccount(accountNumber);
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        }
    }

    /**
     * generates random account number
     *
     * @return new account number
     */
    private String getRandomAccountNumber() {
        Random rnd = new Random();
        BigInteger min = new BigInteger("1000000000");
        BigInteger max = new BigInteger("9999999999");
        do {
            BigInteger i = new BigInteger(max.bitLength(), rnd);
            if (i.compareTo(min) > 0 && i.compareTo(max) <= 0 && !existAccount(i.toString()))
                return i.toString();
        } while (true);
    }
}
