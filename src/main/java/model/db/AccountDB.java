package model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "account")
public class AccountDB {

    private String accountNumber;
    private String savingsAccountNumber;
    private boolean isDemo;

    public AccountDB() {}

    public AccountDB(String accountNumber, String savingsAccountNumber, boolean isDemo) {
        this.accountNumber = accountNumber;
        this.savingsAccountNumber = savingsAccountNumber;
        this.isDemo = isDemo;
    }

    @DynamoDBHashKey
    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountDB setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    @DynamoDBAttribute
    public boolean isDemo() {
        return isDemo;
    }

    public AccountDB setDemo(boolean demo) {
        isDemo = demo;
        return this;
    }

    @DynamoDBAttribute
    public String getSavingsAccountNumber() {
        return savingsAccountNumber;
    }

    public AccountDB setSavingsAccountNumber(String savingsAccountNumber) {
        this.savingsAccountNumber = savingsAccountNumber;
        return this;
    }
}
