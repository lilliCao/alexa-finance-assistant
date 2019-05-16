package api;

import api.aws.DynamoDbMapper;
import api.banking.AccountAPI;
import api.banking.TransactionAPI;
import model.banking.Transaction;
import model.db.TransactionDB;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransactionTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    private static final String ACCOUNT_NUMBER1 = "0000000000";
    private static final String ACCOUNT_NUMBER2 = "1234567890";

    private static final String ACCOUNT_IBAN1 = "DE77100000000000000000";
    private static final String ACCOUNT_IBAN2 = "DE50100000000000000001";
    private static final String VALUE_DATE = new DateTime(2017, 5, 1, 12, 0).toLocalDate().toString();


    @BeforeClass
    public static void setUpAccount() {
        Calendar cal = Calendar.getInstance();
        Date time = cal.getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String openingDate = formatter.format(time);

        AccountAPI.createAccount(ACCOUNT_NUMBER1, 1250000, openingDate);
        AccountAPI.createAccount(ACCOUNT_NUMBER2, 1250000, openingDate);
    }

    @Test
    public void testAndGetTransaction() {
        Transaction newTransaction = TransactionAPI.createTransaction(1, ACCOUNT_IBAN1, ACCOUNT_IBAN2, VALUE_DATE, "TestDescription", "Hans", "Helga");

        assertEquals(1, newTransaction.getValue());
        assertEquals(ACCOUNT_IBAN1, newTransaction.getSourceAccount());
        assertEquals(ACCOUNT_IBAN2, newTransaction.getDestinationAccount());
        assertEquals(VALUE_DATE, newTransaction.getValueDate());
        assertEquals("TestDescription", newTransaction.getDescription());

        Collection<Transaction> transactions = TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER1);

        boolean foundTransaction = false;

        log.info("Found transactions: " + transactions.size());
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionId().equals(newTransaction.getTransactionId())) {
                foundTransaction = true;
                break;
            }
        }

        assertTrue(foundTransaction);
    }

    @Test
    public void getTransactions() {
        Collection<Transaction> transactions = TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER1);
        System.out.println(AccountAPI.getAccount(ACCOUNT_NUMBER1).getIban());

        List<Transaction> txs = new ArrayList<>(transactions);
        Collections.reverse(txs);

        for (Transaction transaction : txs) {
            System.out.println("ID: " + transaction.getTransactionId());
            System.out.println("Source: " + transaction.getSourceAccount());
            System.out.println("Destination: " + transaction.getDestinationAccount());
            System.out.println("Date: " + transaction.getValueDate());
        }
    }

    @Test
    public void createPeriodicTransactionTest() {

        DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();

        String source = AccountAPI.getAccount(ACCOUNT_NUMBER1).getIban();
        String destination = AccountAPI.getAccount(ACCOUNT_NUMBER2).getIban();

        // create sample transactions
        String date = new DateTime(2017, 8, 14, 12, 0).toLocalDate().toString();
        Transaction transaction = TransactionAPI.createTransaction(10, source, destination, date,
                "Netflix", "Netflix", "Peter Müller");

        TransactionDB tDB1 = new TransactionDB(transaction.getTransactionId().toString(), "", ACCOUNT_NUMBER1);
        tDB1.setPeriodic(true);
        dynamoDbMapper.save(tDB1);

    }

}
