package amosalexa.services.contacts;

import api.aws.DynamoDbClient;
import api.aws.DynamoDbMapper;
import model.db.Contact;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ContactTest {

    @Test
    public void createAndDeleteContact() {
        Contact contact = new Contact("Lucas", "DE12345678901234");

        DynamoDbMapper.getInstance().save(contact);

        List<Contact> contacts = DynamoDbMapper.getInstance().loadAll(Contact.class);

        assertTrue(contacts.contains(contact));

        DynamoDbMapper.getInstance().delete(contact);

        contacts = DynamoDbMapper.getInstance().loadAll(Contact.class);

        assertFalse(contacts.contains(contact));
    }

    /*@Test
    public void createSomeContacts() {
        Contact contact1 = new Contact("Bob Marley", "DE12345678901234");
        Contact contact2 = new Contact("Bob Ray Simmons", "DE12345678901234");

        DynamoDbClient.instance.putItem(Contact.TABLE_NAME, contact1);
        DynamoDbClient.instance.putItem(Contact.TABLE_NAME, contact2);
    }*/

}