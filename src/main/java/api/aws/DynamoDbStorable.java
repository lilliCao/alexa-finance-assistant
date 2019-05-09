package api.aws;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

/**
 * Implemented by classes that can be stored in a DynamoDB database.
 */
public interface DynamoDbStorable {

    interface Factory<T extends DynamoDbStorable> {
        /**
         * Called by {@link DynamoDbClient#getItems(String, Class)} to build a list of items.
         *
         * @return blank instance that is used to set values afterwards
         */
        T newInstance();
    }

    /**
     * Thrown if an attribute name is not known.
     */
    class UnknownAttributeException extends Exception {
    }

    /**
     * @return this objects as DynamoDB representation in the form of an attribute map
     */
    Map<String, AttributeValue> getDynamoDbItem();

    /**
     * @return this object's key as DynamoDB representation in the form of an attribute map
     */
    Map<String, AttributeValue> getDynamoDbKey();

    /**
     * Sets the value corresponding to this attribute.
     *
     * @param attributeName  name of the attribute
     * @param attributeValue value of the attribute
     * @throws UnknownAttributeException if the attribute name is not known
     */
    void setDynamoDbAttribute(String attributeName, AttributeValue attributeValue) throws UnknownAttributeException;

    /**
     * Sets the id of this object.
     */
    void setId(int id);

    /**
     * @return id of this object
     */
    int getId();

}
