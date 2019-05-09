package api.banking;


import model.banking.Transaction;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.springframework.hateoas.client.Hop.rel;

public class TransactionAPI {

    /**
     * Logger
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TransactionAPI.class);

    private static BankingRESTClient bankingRESTClient = BankingRESTClient.getInstance();

    /**
     * Create transaction.
     *
     * @param amount          the amount
     * @param sourceIban      the source IBAN (NOT ACCOUNT NUMBER)
     * @param destinationIban the destination IBAN (NOT ACCOUNT NUMBER)
     * @param valueDate       the value date
     * @param description     the description
     * @param payee           the payee
     * @param remitter        the remitter
     * @return the transaction
     */
    public static Transaction createTransaction(Number amount, String sourceIban,
                                                String destinationIban, String valueDate, String description, String payee, String remitter) {
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(amount);
        newTransaction.setSourceAccount(sourceIban);
        newTransaction.setDestinationAccount(destinationIban);
        newTransaction.setValueDate(valueDate);
        newTransaction.setDescription(description);
        newTransaction.setPayee(payee);
        newTransaction.setRemitter(remitter);

        return createTransaction(newTransaction);
    }

    /**
     * Create transaction.
     *
     * @param newTransaction the new transaction
     * @return the transaction
     */
    public static Transaction createTransaction(Transaction newTransaction) {
        return (Transaction) bankingRESTClient.postBankingModelObject("/transactions", newTransaction, Transaction.class);
    }

    /**
     * Get all transactions for the given account.
     *
     * @param accountNumber Account number
     * @return Collection of Cards
     * @throws HttpClientErrorException
     */
    private static Collection<Transaction> getTransactions(String accountNumber) throws HttpClientErrorException {
        // TODO: Create a generic method for getting embedded JSON-HAL collections (in BankingRESTClient)
        Traverson traverson = null;
        String uri = BankingRESTClient.BANKING_API_ENDPOINT + BankingRESTClient.BANKING_API_BASEURL_V2 + "/accounts/" + accountNumber + "/transactions";
        log.info("URI: " + uri);
        try {
            traverson = new Traverson(new URI(uri),
                    MediaTypes.HAL_JSON);
        } catch (URISyntaxException e) {
            log.error("getTransactionsForAccount failed", e);
            return null;
        }

        ParameterizedTypeReference<Resources<Transaction>> typeRefDevices = new ParameterizedTypeReference<Resources<Transaction>>() {
        };
        Resources<Transaction> resResponses = traverson.follow(rel("$._links.self.href")).withHeaders(bankingRESTClient.generateHttpHeaders()).toObject(typeRefDevices);
        return resResponses.getContent();
    }

    /**
     * request api for list of transaction @{@link Transaction}
     *
     * @return list of tranactions
     */
    public static List<Transaction> getTransactionsForAccount(String accountNumber) {
        Collection<Transaction> transactions = getTransactions(accountNumber);
        List<Transaction> txs = new ArrayList<>(transactions);
        Collections.reverse(txs);
        return txs;
    }

    public static Transaction getTransactionForAccount(String accountNumber, String transactionId) {
        //Search for transaction
        //TODO this is only a temporary solution
        //TODO wait for API extension to get transaction detail request call /transactions/{transactionId}
        List<Transaction> allTransactions = getTransactionsForAccount(accountNumber);
        Number transactionNumber = Integer.valueOf(transactionId);
        Transaction transaction = null;
        for (Transaction t : allTransactions) {
            if (transactionNumber.equals(t.getTransactionId())) {
                transaction = t;
            }
        }
        return transaction;
    }

}
