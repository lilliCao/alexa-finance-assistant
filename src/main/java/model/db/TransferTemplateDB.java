package model.db;

import api.aws.DynamoDbClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.Date;

@DynamoDBTable(tableName = "transfer_template")
public class TransferTemplateDB {
    private int id;
    private String target;
    private double amount;
    private Date createdAt;
    private String accountNumber;

    public TransferTemplateDB() {
        this.id = DynamoDbClient.getNewId("transfer_template");
    }

    public TransferTemplateDB(int id) {
        this.id = id;
    }

    public TransferTemplateDB(String target, double amount, String accountNumber) {
        this.id = DynamoDbClient.getNewId("transfer_template");
        this.target = target;
        this.amount = amount;
        this.createdAt = new Date();
        this.accountNumber = accountNumber;
    }

    @DynamoDBHashKey
    public int getId() {
        return id;
    }

    @DynamoDBAttribute
    public String getTarget() {
        return target;
    }

    @DynamoDBAttribute
    public double getAmount() {
        return amount;
    }

    @DynamoDBAttribute
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
