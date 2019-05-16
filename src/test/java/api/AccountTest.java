package api;

import api.banking.AccountAPI;
import model.banking.Account;
import model.banking.Card;
import model.banking.StandingOrder;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class AccountTest {

    public static final String ACCOUNT_NUMBER = "0000000000";
    public static final String ACCOUNT_NUMBER2 = "0000000001";
    public static final String CARD_NUMBER = "0000004711";
    private static final Logger log = LoggerFactory.getLogger(AccountTest.class);

    private static String getCurrentOpeningDate() {
        Calendar cal = Calendar.getInstance();
        Date time = cal.getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(time);
    }

    private static String getLastWeekOpeningDate() {
        DateTime today = DateTime.now();
        DateTime sameDayLastWeek = today.minusWeeks(1);
        return sameDayLastWeek.toString("yyyy-MM-dd");
    }

    @BeforeClass
    public static void setUpAccount() {
        AccountAPI.createAccount(ACCOUNT_NUMBER, 1250000, getCurrentOpeningDate());
        //AccountAPI.createAccount("0000000020", 1250000, getLastWeekOpeningDate());
    }

    @Test
    public void createRandNumber() throws InterruptedException {
        //DynamoDbMapper.getInstance().createTable(CategoryDB.class);
        //DynamoDbMapper.getInstance().save(new CategoryDB("1234", "mytest", 100));
    }


    /**
     * Account Information
     * <p>
     * object path /api/v1_0/accounts/{accountnumber}
     * <p>
     * https://s3.eu-central-1.amazonaws.com/amos-bank/api-guide.html#_konto_informationen
     */

    @Test
    public void testGetAccount() {
        Account account = AccountAPI.getAccount(ACCOUNT_NUMBER);

        // We can't check for balance here because maybe another transaction / standing order already modified the balance
        assertEquals(account.getNumber(), ACCOUNT_NUMBER);
        assertEquals(account.getOpeningDate(), getCurrentOpeningDate());
    }

    @Test
    public void testCreateAndGetCard() {
        Card card = new Card();
        card.setCardType(Card.CardType.DEBIT);
        card.setCardNumber(CARD_NUMBER);
        card.setStatus(Card.Status.ACTIVE);
        card.setExpirationDate(new DateTime(2018, 5, 1, 12, 0).toLocalDate().toString());
        card.setAccountNumber(ACCOUNT_NUMBER);

        Card newCard = AccountAPI.createCardForAccount(ACCOUNT_NUMBER, card);

        assertEquals(card.getCardType(), newCard.getCardType());
        assertEquals(card.getCardNumber(), newCard.getCardNumber());
        assertEquals(card.getStatus(), newCard.getStatus());
        assertEquals(card.getExpirationDate(), newCard.getExpirationDate());

        // Get cards
        Collection<Card> cards = AccountAPI.getCardsForAccount(ACCOUNT_NUMBER);

        boolean foundCard = false;

        for (Card card1 : cards) {
            if (card1.getCardNumber().equals(newCard.getCardNumber())) {
                foundCard = true;
            }
            AccountAPI.deleteCard(ACCOUNT_NUMBER, card1.getCardId());
        }

        assertTrue(foundCard);
    }

    @Test
    public void testCreateAndGetStandingOrder() {
        String firstExecution = new DateTime(2018, 5, 1, 12, 0).toLocalDate().toString();

        StandingOrder standingOrder = AccountAPI.createStandingOrderForAccount(ACCOUNT_NUMBER, "Test", 123, ACCOUNT_NUMBER2,
                firstExecution, StandingOrder.ExecutionRate.MONTHLY, "Description");

        assertEquals(standingOrder.getPayee(), "Test");
        assertEquals(standingOrder.getAmount(), 123);
        assertEquals(standingOrder.getDestinationAccount(), ACCOUNT_NUMBER2);
        assertEquals(standingOrder.getFirstExecution(), firstExecution);
        assertEquals(standingOrder.getExecutionRate(), StandingOrder.ExecutionRate.MONTHLY);
        assertEquals(standingOrder.getDescription(), "Description");

        // Get standing orders
        Collection<StandingOrder> standingOrders = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);

        boolean foundStandingOrder = false;

        for (StandingOrder standingOrder1 : standingOrders) {
            if (standingOrder1.getStandingOrderId().equals(standingOrder.getStandingOrderId())) {
                foundStandingOrder = true;
                break;
            }
        }

        assertTrue(foundStandingOrder);
    }

}
