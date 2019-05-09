import amosalexa.AccountFactory;
import api.aws.DynamoDbMapper;
import com.amazonaws.services.s3.transfer.Transfer;
import model.banking.Account;
import model.db.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoAccountTest {

    private static Logger LOGGER = LoggerFactory.getLogger(DemoAccountTest.class);
    private static DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();


    @BeforeClass
    public static void createTables() throws InterruptedException {
        // Drop and recreate tables
        LOGGER.info("Drop all tables");
        LOGGER.info("Drop account db");
        dynamoDbMapper.dropTable(AccountDB.class);
        LOGGER.info("Drop category");
        dynamoDbMapper.dropTable(Category.class);
        LOGGER.info("Drop contact");
        dynamoDbMapper.dropTable(Contact.class);
        LOGGER.info("Drop test");
        dynamoDbMapper.dropTable(DynamoTestObject.class);
        LOGGER.info("Drop spending");
        dynamoDbMapper.dropTable(Spending.class);
        LOGGER.info("Drop standingorder");
        dynamoDbMapper.dropTable(StandingOrderDB.class);
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
        LOGGER.info("Create test");
        dynamoDbMapper.createTable(DynamoTestObject.class);
        LOGGER.info("Create spending");
        dynamoDbMapper.createTable(Spending.class);
        LOGGER.info("Create standingorder");
        dynamoDbMapper.createTable(StandingOrderDB.class);
        LOGGER.info("Create transaction");
        dynamoDbMapper.createTable(TransactionDB.class);
        LOGGER.info("Create user");
        dynamoDbMapper.createTable(model.db.User.class);
        LOGGER.info("Create last_ids");
        dynamoDbMapper.createTable(LastIds.class);
        LOGGER.info("Create template");
        dynamoDbMapper.createTable(TransferTemplateDB.class);
    }

    @Test
    public void createDemoAccount(){
       LOGGER.info("Attempt to create demo account");
       Account demoAccount = AccountFactory.getInstance().createDemo();
       LOGGER.info("Succeeded creating demo account");
       LOGGER.info(demoAccount.toString());
    }

}
