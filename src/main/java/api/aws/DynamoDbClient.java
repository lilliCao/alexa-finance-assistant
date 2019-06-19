package api.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import configuration.ConfigurationAMOS;

import java.util.Map;
import java.util.TreeMap;

public class DynamoDbClient {

    private static AmazonDynamoDB dynamoDB;
    public final static DynamoDbClient instance = new DynamoDbClient();

    /**
     * Creates a new DynamoDB client.
     */
    private DynamoDbClient() {
        dynamoDB = getAmazonDynamoDBClient();
    }

    public static AmazonDynamoDB getAmazonDynamoDBClient() {
        AWSCredentials awsCredentails = new BasicAWSCredentials(ConfigurationAMOS.awsAccessKey, ConfigurationAMOS.awsSecretKey);
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentails))
                .withRegion(Regions.US_EAST_1)
                .build();

        // Local DynamoDB
        //dynamoDB = new AmazonDynamoDBClient();
        //dynamoDB.setEndpoint("http://localhost:8000");

        return dynamoDB;
    }


    public static int getNewId(String tableName) {
        // Create a new item by fetching the id first.
        Map<String, AttributeValue> request = new TreeMap<>();
        request.put("table_name", new AttributeValue(tableName));

        // Try to get the last id and leave it at 0 if cannot find last id
        int id = 0;
        try {
            GetItemResult result = dynamoDB.getItem("last_ids", request);
            if (result.getItem() != null) {
                AttributeValue aid = result.getItem().get("id");
                id = Integer.parseInt(aid.getN());
            }
        } catch (ResourceNotFoundException ignored) {
        }

        // Increment the id
        id++;

        // Store the id
        request.put("id", new AttributeValue().withN(Integer.toString(id)));
        dynamoDB.putItem("last_ids", request);

        return id;
    }


}
