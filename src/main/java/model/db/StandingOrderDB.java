package model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "standing_order_category")
public class StandingOrderDB {

    private String id;
    private String standingOrderId;
    private String categoryId;
    private String accountNumber;

    public StandingOrderDB() {
    }

    public StandingOrderDB(String accountNumber, String standingOrderId, String categoryId) {
        this.accountNumber = accountNumber;
        this.standingOrderId = standingOrderId;
        this.categoryId = categoryId;
    }

    @DynamoDBHashKey
    @DynamoDBAutoGeneratedKey
    public String getId() {
        return id;
    }

    public StandingOrderDB setId(String id) {
        this.id = id;
        return this;
    }

    @DynamoDBAttribute
    public String getStandingOrderId() {
        return standingOrderId;
    }

    public StandingOrderDB setStandingOrderId(String standingOrderId) {
        this.standingOrderId = standingOrderId;
        return this;
    }

    @DynamoDBAttribute
    public String getCategoryId() {
        return categoryId;
    }

    public StandingOrderDB setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    @DynamoDBAttribute
    public String getAccountNumber() {
        return accountNumber;
    }

    public StandingOrderDB setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }
}
