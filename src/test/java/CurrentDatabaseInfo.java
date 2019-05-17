import amosalexa.AccountFactory;
import api.aws.DynamoDbMapper;
import api.banking.AccountAPI;
import configuration.ConfigurationAMOS;
import model.banking.StandingOrder;
import model.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CurrentDatabaseInfo {
    private static Logger LOGGER = LoggerFactory.getLogger(ConfigurationAMOS.class);
    private static DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();

    public static void main(String[] args) {
        //recreateContactIfTestFail();
        //getAllDatabaseInfo();
        //currentAcc();
        //currentTransaction();
        //currentTemplate();
        currentTransaction();
    }

    public static void recreateContactIfTestFail() {
        // Rebuild contact db
        currentContact();
        List<Contact> contacts = Arrays.asList(
                new Contact("Taylor Marley", "UK1"),
                new Contact("Taylor Ray Simmons", "UK2"),
                new Contact("Tim", "DE1"),
                new Contact("hanna", "DE2"));
        LOGGER.info("Delete all added contacts");
        List<Contact> contactList = dynamoDbMapper.loadAll(Contact.class);
        for (Contact contact : contacts) {
            for (Contact realContact : contactList) {
                if (contact.getIban().equalsIgnoreCase(realContact.getIban())) {
                    LOGGER.info("Remove " + contact.getName() + "from db");
                    dynamoDbMapper.delete(new Contact(realContact.getId()));
                    break;
                }
            }
        }
        currentContact();
    }

    public static void getAllDatabaseInfo() {
        LOGGER.info("Current data in database");
        currentAcc();
        currentCate();
        currentContact();
        currentSpending();
        currentStandingorder();
        currentTransaction();
        currentTemplate();
    }

    public static void currentAcc() {
        LOGGER.info("Current accounts");
        List<AccountDB> accounts = dynamoDbMapper.loadAll(AccountDB.class);
        accounts.stream().forEach(e -> {
            System.out.format("acc nr=%s saving nr=%s\n",
                    e.getAccountNumber(),
                    e.getSavingsAccountNumber());
        });
    }

    public static void currentCate() {
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

    public static void currentContact() {
        LOGGER.info("Current contact");
        List<Contact> obs = dynamoDbMapper.loadAll(Contact.class);
        obs.stream().forEach(e -> {
            System.out.format("Contact name=%s acc nr=%s iban=%s\n",
                    e.getName(),
                    e.getAccountNumber(),
                    e.getIban());
        });
    }

    public static void currentSpending() {
        LOGGER.info("Current spending");
        List<Spending> obs = dynamoDbMapper.loadAll(Spending.class);
        obs.stream().forEach(e -> {
            System.out.format("Spend on cate = %s acc nr=%s amount=%.2f\n",
                    e.getCategoryId(),
                    e.getAccountNumber(),
                    e.getAmount());
        });
    }

    public static void currentStandingorder() {
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

    public static void currentTransaction() {
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
