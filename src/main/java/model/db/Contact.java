package model.db;

import api.aws.DynamoDbClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a contact to which we can transfer money.
 */
@DynamoDBTable(tableName = "contact")
public class Contact implements Comparable<Contact>, Serializable {
    private int id;
    private String accountNumber;
    private String name;
    private String iban;
    private Date createdAt;

    public static final String TABLE_NAME = "contact";

    public Contact() {
        this.id = DynamoDbClient.getNewId("contact");
    }

    public Contact(String name, String iban) {
        this.id = DynamoDbClient.getNewId("contact");
        this.name = name;
        this.iban = iban;
        this.createdAt = new Date();
    }

    public Contact(int id) {
        this.id = id;
    }

    @DynamoDBHashKey
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @DynamoDBAttribute
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @DynamoDBAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBAttribute
    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    @DynamoDBAttribute
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Contact)) {
            return false;
        }

        Contact oc = (Contact) o;
        return oc.id == id && oc.name.equals(name) && oc.iban.equals(iban) && oc.createdAt.equals(createdAt);
    }

    @Override
    public int compareTo(Contact o) {
        return Integer.compare(id, o.id);
    }
}
