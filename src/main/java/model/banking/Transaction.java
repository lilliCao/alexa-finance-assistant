package model.banking;

import amosalexa.handlers.utils.DateUtil;
import amosalexa.handlers.utils.DialogUtil;
import amosalexa.handlers.utils.NumberUtil;
import api.aws.DynamoDbMapper;
import api.banking.TransactionAPI;
import model.db.TransactionDB;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_IBAN;

public class Transaction extends ResourceSupport {

    private static final Logger log = LoggerFactory.getLogger(Transaction.class);
    private static final DateTimeFormatter apiTransactionFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

    private static List<Transaction> transactionCache;

    private Number transactionId;
    private Number amount;
    private Number value;
    private String destinationAccount;
    private String sourceAccount;
    private String valueDate;
    private String description;
    private String payee;
    private String remitter;

    private static String getTransactionIdText(Transaction transaction) {
        return "<break time=\"1s\"/>Nummer " + transaction.getTransactionId() + " ";
    }

    public static String getAskMoreTransactionText() {
        return "<break time=\"1s\"/> Möchtest du weitere Transaktionen hören";
    }

    private static String getTransactionFromAccountText(Transaction transaction) {
        return Transaction.getTransactionIdText(transaction) + "Von deinem Konto auf das Konto von " + transaction.getPayee() +
                " in Höhe von <say-as interpret-as=\"unit\">€"
                + Math.abs(transaction.getAmount().doubleValue()) + "</say-as>";
    }

    private static String getTransactionToAccountText(Transaction transaction) {
        return Transaction.getTransactionIdText(transaction) + "Von " + transaction.getRemitter() + " auf dein Konto in Höhe von <say-as interpret-as=\"unit\">€"
                + Math.abs(transaction.getAmount().doubleValue()) + "</say-as>";
    }

    public static String getTransactionSizeText(int size) {
        return "Du hast " + size + " Transaktionen. ";
    }

    /**
     * checks if the transaction is from your account
     *
     * @param transaction transaction
     * @return text for transaction
     */
    public static String getTransactionText(Transaction transaction) {

        String transactionText = "";
        if (transaction.getSourceAccount() != null) {
            transactionText = Transaction.getTransactionToAccountText(transaction);
        }
        if (transaction.getDestinationAccount() != null) {
            transactionText = Transaction.getTransactionFromAccountText(transaction);
        }
        return transactionText;
    }

    public Number getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Number transactionId) {
        this.transactionId = transactionId;
    }

    public Number getAmount() {
        return amount;
    }

    public void setAmount(Number amount) {
        this.amount = amount;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getValueDate() {
        return valueDate;
    }

    public DateTime getValueDateAsDateTime() {
        return apiTransactionFmt.parseDateTime(valueDate);
    }

    public void setValueDate(String valueDate) {
        this.valueDate = valueDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPayee() {
        if (payee == null) {
            return DialogUtil.getIbanSsmlOutput(getDestinationAccount());
        }
        return payee;
    }

    public Transaction setPayee(String payee) {
        this.payee = payee;
        return this;
    }

    public String getRemitter() {
        if (remitter == null) {
            return DialogUtil.getIbanSsmlOutput(getSourceAccount());
        }
        return remitter;
    }

    public Transaction setRemitter(String remitter) {
        this.remitter = remitter;
        return this;
    }

    /**
     * Checks if this is an outgoing transaction, i.e. that this is a transaction where we have a payee
     * but not a remitter.
     *
     * @return true if this transaction is outgoing
     */
    public boolean isOutgoing() {
        return getPayee() != null && getRemitter() == null;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", amount=" + amount +
                ", value=" + value +
                ", destinationAccount='" + destinationAccount + '\'' +
                ", sourceAccount='" + sourceAccount + '\'' +
                ", valueDate='" + valueDate + '\'' +
                ", description='" + description + '\'' +
                ", payee='" + payee + '\'' +
                ", remitter='" + remitter + '\'' +
                '}';
    }


    /**
     * gets all periodic transaction from DB/API
     *
     * @param accountNumber account number
     * @return List of all periodic transactions
     */
    public static List<Transaction> getPeriodicTransactions(String accountNumber) {
        ArrayList<Transaction> periodicTransactions = new ArrayList<>();
        DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();
        List<TransactionDB> transactionsDB = dynamoDbMapper.loadAll(TransactionDB.class);
        for (TransactionDB transactionDB : transactionsDB) {
            if (transactionDB.isPeriodic() && transactionDB.getAccountNumber().equals(accountNumber)) {
                Transaction transaction = getCachedTransactionForAccount(accountNumber, transactionDB.getTransactionId());
                if (transaction != null) {
                    log.info("add to periodic trans=" + transaction.toString());
                    periodicTransactions.add(transaction);
                }
            }
        }
        return periodicTransactions;
    }

    /**
     * saves result of get Transaction in cache and return Transaction by id
     *
     * @param accountNumber account number
     * @param transactionId transaction
     * @return transaction
     */
    private static Transaction getCachedTransactionForAccount(String accountNumber, String transactionId) {
        if (transactionCache == null) {
            transactionCache = TransactionAPI.getTransactionsForAccount(accountNumber);
        }
        for (Transaction transaction : transactionCache) {
            log.info("Compare local and remote " + transaction.getTransactionId() + " vs " + transactionId);
            if (transaction.getTransactionId().toString().equals(transactionId)) {
                return transaction;
            }
        }
        return null;
    }

    /**
     * returns all periodic transactions til a certain day of month
     *
     * @param periodicTransactions periodic transactions
     * @param futureDate           future date
     * @return list of periodic transactions
     */
    public static List<Transaction> getTargetDatePeriodicTransactions(List<Transaction> periodicTransactions, String futureDate) {
        log.info("in listing periodic transactions =" + periodicTransactions.size());
        List<Transaction> futurePeriodicTransactions = new ArrayList<>();
        for (Transaction periodicTransaction : periodicTransactions) {
            if (periodicTransaction != null) {
                log.info("Date=" + periodicTransaction.getValueDate() + "-future=" + futureDate);
                if (DateUtil.getDatesBetween(periodicTransaction.getValueDate(), futureDate) > 0) {
                    futurePeriodicTransactions.add(periodicTransaction);
                }
            }
        }
        log.info("periodic transactions = " + futurePeriodicTransactions.size());
        return futurePeriodicTransactions;
    }

    /**
     * returns the added transaction values
     *
     * @param futurePeriodicTransactions list of periodic transactions
     * @return balance of transactions
     */
    public static double getFutureTransactionBalance(List<Transaction> futurePeriodicTransactions, String futureDate) {
        double transactionBalance = 0;
        for (Transaction futurePeriodicTransaction : futurePeriodicTransactions) {
            int executionDates = DateUtil.getDatesBetween(futurePeriodicTransaction.getValueDate(), futureDate);
            log.info("periodic with amount=" + futurePeriodicTransaction.getAmount().doubleValue() + " x" + executionDates);
            if (futurePeriodicTransaction.getDestinationAccount().equalsIgnoreCase(ACCOUNT_IBAN)) {
                // income
                transactionBalance = transactionBalance + (executionDates * futurePeriodicTransaction.getAmount().doubleValue());
            } else if (futurePeriodicTransaction.getSourceAccount().equalsIgnoreCase(ACCOUNT_IBAN)) {
                // giving
                transactionBalance = transactionBalance - (executionDates * futurePeriodicTransaction.getAmount().doubleValue());
            }
        }
        return NumberUtil.round(transactionBalance, 2);
    }
}
