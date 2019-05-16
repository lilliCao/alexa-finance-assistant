package model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "transaction")
public class TransactionDB {

    private String transactionId;
    private String categoryId;
    private String accountNumber;

    /**
     * Marks if this transaction has been marked as periodic transaction.
     */
    private boolean periodic;

    public TransactionDB() {
    }

    public TransactionDB(String transactionId) {
        this.transactionId = transactionId;
        this.categoryId = null;
    }

    public TransactionDB(String transactionId, String categoryId, String accountNumber) {
        this.accountNumber = accountNumber;
        this.transactionId = transactionId;
        this.categoryId = categoryId;
    }

    @DynamoDBHashKey
    public String getTransactionId() {
        return transactionId;
    }

    public TransactionDB setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    @DynamoDBAttribute
    public String getCategoryId() {
        return categoryId;
    }

    public TransactionDB setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    @DynamoDBAttribute
    public boolean isPeriodic() {
        return periodic;
    }

    public void setPeriodic(boolean periodic) {
        this.periodic = periodic;
    }

    @Override
    public String toString() {
        return "TransactionDB{" +
                "transactionId='" + transactionId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", periodic=" + periodic +
                '}';
    }

    @DynamoDBAttribute
    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionDB setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }
}
