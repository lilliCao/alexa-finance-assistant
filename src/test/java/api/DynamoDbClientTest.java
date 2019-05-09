package api;

import api.aws.DynamoDbClient;
import api.aws.DynamoDbMapper;
import model.banking.TransferTemplate;
import model.db.DynamoTestObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class DynamoDbClientTest {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbClientTest.class);

    private DynamoDbClient client = DynamoDbClient.instance;

    private DynamoDbMapper dynamoDbMapper = new DynamoDbMapper(DynamoDbClient.getAmazonDynamoDBClient());

    @Test
    public void CRUDTest() throws InterruptedException {
        String val1 = "mytest-1";
        String val2 = "mytest-2";

        DynamoTestObject dynamoTestObject = new DynamoTestObject();
        dynamoTestObject.setValue(val1);

        // create
        dynamoDbMapper.createTable(DynamoTestObject.class);
        log.info("Create dummy table");

        // save
        dynamoDbMapper.save(dynamoTestObject);
        log.info("save dummy object");

        // load
        DynamoTestObject loadedDynamoTestObject = (DynamoTestObject) dynamoDbMapper.load(DynamoTestObject.class, dynamoTestObject.getId());
        log.info("load dummy object");
        assertEquals(val1, loadedDynamoTestObject.getValue());

        // update
        loadedDynamoTestObject.setValue(val2);
        dynamoDbMapper.save(loadedDynamoTestObject);
        log.info("update dummy object");

        // load
        DynamoTestObject updatedDynamoTestObject = (DynamoTestObject) dynamoDbMapper.load(DynamoTestObject.class, loadedDynamoTestObject.getId());
        assertEquals(val2, updatedDynamoTestObject.getValue());
        log.info("load dummy object");

        // delete
        dynamoDbMapper.delete(updatedDynamoTestObject);
        log.info("delete dummy object");

        // drop
        dynamoDbMapper.dropTable(DynamoTestObject.class);
        log.info("drop dummy table");
    }

    @Test
    public void getItemsTest() {
        client.getItems(TransferTemplate.TABLE_NAME, TransferTemplate::new);
    }

    @Test
    public void putAndDeleteItemTest() {
        TransferTemplate transferTemplate1 = TransferTemplate.make("max", 10.0);
        TransferTemplate transferTemplate2 = TransferTemplate.make("johannes", 10.0);

        // create TransferTemplate Object means creating and saving in database, where the id is get
        // from last_ids table and increment and update
        log.info(transferTemplate1.getId()+"-"
                +transferTemplate1.getDynamoDbKey().toString()
                +"-item="+transferTemplate1.getDynamoDbItem().toString()
                +"-atts="+transferTemplate1.getDynamoDbItem().toString());
        log.info(transferTemplate2.getId()+"-"
                +transferTemplate2.getDynamoDbKey().toString()
                +"-item="+transferTemplate2.getDynamoDbItem().toString()
                +"-atts="+transferTemplate2.getDynamoDbItem().toString());

        assertEquals(transferTemplate1.getId() + 1, transferTemplate2.getId());

        List<TransferTemplate> transferTemplateList = client.getItems(TransferTemplate.TABLE_NAME, TransferTemplate::new);

        assert (transferTemplateList.contains(transferTemplate1));
        assert (transferTemplateList.contains(transferTemplate2));

        client.deleteItem(TransferTemplate.TABLE_NAME, transferTemplate1);

        transferTemplateList = client.getItems("transfer_template", TransferTemplate::new);

        assertFalse(transferTemplateList.contains(transferTemplate1));
        assert (transferTemplateList.contains(transferTemplate2));

        client.deleteItem(TransferTemplate.TABLE_NAME, transferTemplate2);

        transferTemplateList = client.getItems("transfer_template", TransferTemplate::new);

        assertFalse(transferTemplateList.contains(transferTemplate1));
        assertFalse(transferTemplateList.contains(transferTemplate2));
    }

    @Test
    public void createAndDeleteItemTest() {
        TransferTemplate mockTemplate = new TransferTemplate(0) {
            TransferTemplate init() {
                this.createdAt = new Date();
                this.target = "alex";
                this.amount = 5.0;
                return this;
            }
        }.init();

        assertEquals(0, mockTemplate.getId());

        client.putItem(TransferTemplate.TABLE_NAME, mockTemplate);

        log.info("id after put in db which is automatically get from last_ids table ="+mockTemplate.getId());

        assertNotEquals(0, mockTemplate.getId());

        client.deleteItem(TransferTemplate.TABLE_NAME, mockTemplate);

        List<TransferTemplate> transferTemplateList = client.getItems("transfer_template", TransferTemplate::new);

        assertFalse(transferTemplateList.contains(mockTemplate));
    }
}