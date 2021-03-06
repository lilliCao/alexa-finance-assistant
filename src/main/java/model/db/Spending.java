package model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@DynamoDBTable(tableName = "spending")
public class Spending {

    private static final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String creationDateTime;
    private String categoryId;
    private double amount;
    private String accountNumber;

    public Spending() {
    }

    public Spending(String accountNumber, String categoryId, double amount) {
        this.accountNumber = accountNumber;
        this.creationDateTime = DateTime.now().toString(fmt);
        this.categoryId = categoryId;
        this.amount = amount;
    }

    public Spending(String categoryId, double amount, DateTime creationDateTime) {
        this.creationDateTime = creationDateTime.toString(fmt);
        this.categoryId = categoryId;
        this.amount = amount;
    }

    @DynamoDBHashKey
    @DynamoDBAutoGeneratedKey
    public String getId() {
        return id;
    }

    public Spending setId(String id) {
        this.id = id;
        return this;
    }

    @DynamoDBAttribute
    public String getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(String dateTime) {
        this.creationDateTime = dateTime;
    }

    @DynamoDBIgnore
    public DateTime getCreationDateTimeAsDateTime() {
        return fmt.parseDateTime(creationDateTime);
    }

    @DynamoDBAttribute
    public String getCategoryId() {
        return categoryId;
    }

    public Spending setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    @DynamoDBAttribute
    public double getAmount() {
        return amount;
    }

    public Spending setAmount(double amount) {
        this.amount = amount;
        return this;
    }

    @DynamoDBAttribute
    public String getAccountNumber() {
        return accountNumber;
    }

    public Spending setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }
}
