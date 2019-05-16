package api.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DynamoDbMapper {

    private AmazonDynamoDB dynamoDbClient;
    private DynamoDBMapper mapper;
    private DynamoDB dynamoDB;

    private static DynamoDbMapper dynamoDbMapper = new DynamoDbMapper(DynamoDbClient.getAmazonDynamoDBClient());

    public DynamoDbMapper(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapper = new DynamoDBMapper(dynamoDbClient);
        this.dynamoDB = new DynamoDB(dynamoDbClient);
    }

    public static DynamoDbMapper getInstance() {
        synchronized (DynamoDbMapper.class) {
            return dynamoDbMapper;
        }
    }

    /**
     * creates a new table by a pojo class
     *
     * @param cl class which will be mapped to the new table
     */
    public void createTable(Class cl) throws InterruptedException {
        CreateTableRequest tableRequest = mapper.generateCreateTableRequest(cl);
        tableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        Table table = dynamoDB.createTable(tableRequest);
        table.waitForActive();
    }

    /**
     * drops table by a pojo class
     *
     * @param cl class which will be mapped to drop a table
     */
    public void dropTable(Class cl) {
        DeleteTableRequest tableRequest = mapper.generateDeleteTableRequest(cl);
        Table table = dynamoDB.getTable(tableRequest.getTableName());
        try {
            dynamoDbClient.deleteTable(tableRequest);
            table.waitForDelete();
        } catch (ResourceNotFoundException e) {
            // Table does not exist
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * saves entity in db
     *
     * @param object entity
     */
    public void save(Object object) {
        mapper.save(object);
    }

    /**
     * delete entity from db
     *
     * @param object entity
     */
    public void delete(Object object) {
        mapper.delete(object);
    }


    /**
     * load entity from db
     *
     * @param cl        mapping class
     * @param objectKey key
     * @return entity
     */
    public Object load(Class cl, Object objectKey) {
        return mapper.load(cl, objectKey);
    }

    /**
     * load all entities for one class from db
     *
     * @param cl mapping class
     * @return entity list
     */
    public <T> List<T> loadAll(Class cl) {
        PaginatedScanList<T> paginatedScanList = mapper.scan(cl, new DynamoDBScanExpression());
        paginatedScanList.loadAllResults();

        List<T> list = new ArrayList<T>(paginatedScanList.size());

        Iterator<T> iterator = paginatedScanList.iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            list.add(element);
        }
        return list;
    }
}
