package amosalexa.handlers.budgettracker;

import api.aws.DynamoDbClient;
import api.aws.DynamoDbMapper;
import api.banking.TransactionAPI;
import model.banking.Transaction;
import model.db.Spending;
import model.db.TransactionDB;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;

import static amosalexa.handlers.AmosStreamHandler.ACCOUNT_NUMBER;

/**
 * BudgetManager class. Offers and interface to create spendings and retrieve the total spendings amount (including transactions) for a given category.
 */
public class BudgetManager {

    public final static BudgetManager instance = new BudgetManager();

    private DynamoDbMapper dynamoDbMapper = new DynamoDbMapper(DynamoDbClient.getAmazonDynamoDBClient());

    /**
     * Create spending with the current DateTime as creation DateTime.
     *
     * @param categoryId the category id
     * @param amount     the amount
     */
    public void createSpending(String accountNumber, String categoryId, double amount) {
        dynamoDbMapper.save(new Spending(accountNumber, categoryId, amount));
    }

    /**
     * Gets total spending for category (including transactions) and a given month.
     *
     * @param categoryId the category id
     * @param forMonth   an arbitrary DateTime value.
     * @return the total spending for category
     */
    public double getTotalSpendingForCategory(String categoryId, DateTime forMonth) {
        // Calculate start and end of the month (as limits)
        DateTime start = forMonth.withTimeAtStartOfDay().withDayOfMonth(1);
        DateTime end = start.plusMonths(1);

        return getTransactionAmount(categoryId, start, end) + getSpendingAmount(categoryId, start, end);
    }

    /**
     * Gets total spending for category in the current month.
     *
     * @param categoryId the category id
     * @return the total spending for category
     */
    public double getTotalSpendingForCategory(String categoryId) {
        DateTime forMonth = DateTime.now();
        return getTotalSpendingForCategory(categoryId, forMonth);
    }

    /**
     * Gets total spending for category in the last month.
     *
     * @param categoryId the category id
     * @return the total spending for category
     */
    public double getTotalSpendingForCategoryLastMonth(String categoryId) {
        DateTime forMonth = DateTime.now().minusMonths(1);
        return getTotalSpendingForCategory(categoryId, forMonth);
    }

    private double getTransactionAmount(String categoryId, DateTime start, DateTime end) {
        double sum = 0;

        Collection<Transaction> apiTransactions = TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER);

        List<TransactionDB> dbTransactions = dynamoDbMapper.loadAll(TransactionDB.class);

        for (TransactionDB transactionDB : dbTransactions) {
            if (transactionDB.getCategoryId() == null || !transactionDB.getCategoryId().equals(categoryId)) {
                continue;
            }
            for (Transaction transaction : apiTransactions) {
                if (transactionDB.getTransactionId().equals(transaction.getTransactionId().toString())) {
                    if (transaction.getValueDateAsDateTime().isAfter(start) && transaction.getValueDateAsDateTime().isBefore(end)) {
                        sum += Math.abs(transaction.getAmount().doubleValue());
                    }
                }
            }
        }

        return sum;
    }

    private double getSpendingAmount(String categoryId, DateTime start, DateTime end) {
        double sum = 0;

        List<Spending> dbSpendings = dynamoDbMapper.loadAll(Spending.class);

        for (Spending spending : dbSpendings) {
            if (spending.getCategoryId().equals(categoryId)) {
                if (spending.getCreationDateTimeAsDateTime().isAfter(start) && spending.getCreationDateTimeAsDateTime().isBefore(end)) {
                    sum += spending.getAmount();
                }
            }
        }

        return sum;
    }

}
