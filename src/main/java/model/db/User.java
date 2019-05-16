package model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "user")
public class User {

    private String id;
    private long balanceLimit;
    private String accessToken;
    private String accessTokenExpiryTime;

    public User() {
    }

    @DynamoDBHashKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBAttribute
    public long getBalanceLimit() {
        return balanceLimit;
    }

    public void setBalanceLimit(long balanceLimit) {
        this.balanceLimit = balanceLimit;
    }

    @DynamoDBAttribute
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @DynamoDBAttribute
    public String getAccessTokenExpiryTime() {
        return accessTokenExpiryTime;
    }

    public void setAccessTokenExpiryTime(String accessTokenExpiryTime) {
        this.accessTokenExpiryTime = accessTokenExpiryTime;
    }

}
