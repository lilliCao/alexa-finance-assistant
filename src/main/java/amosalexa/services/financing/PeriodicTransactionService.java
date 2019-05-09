package amosalexa.services.financing;

import amosalexa.AmosAlexaSpeechlet;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import api.aws.DynamoDbClient;
import api.aws.DynamoDbMapper;
import api.banking.TransactionAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.banking.Transaction;
import model.db.TransactionDB;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PeriodicTransactionService extends AbstractSpeechService implements SpeechService {

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                PERIODIC_TRANSACTION_LIST_INTENT,
                PERIODIC_TRANSACTION_ADD_INTENT,
                PERIODIC_TRANSACTION_DELETE_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                PERIODIC_TRANSACTION_LIST_INTENT,
                PERIODIC_TRANSACTION_ADD_INTENT,
                PERIODIC_TRANSACTION_DELETE_INTENT,
                YES_INTENT,
                NO_INTENT,
                STOP_INTENT
        );
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicTransactionService.class);

    /**
     * Default value for cards
     */
    private static final String PERIODIC_TRANSACTION = "Periodische Transaktionen";

    /**
     * Intent names
     */
    private static final String PERIODIC_TRANSACTION_LIST_INTENT = "PeriodicTransactionListIntent";
    private static final String PERIODIC_TRANSACTION_ADD_INTENT = "PeriodicTransactionAddIntent";
    private static final String PERIODIC_TRANSACTION_DELETE_INTENT = "PeriodicTransactionDeleteIntent";

    private static final String ACCOUNT_NUMBER = AmosAlexaSpeechlet.ACCOUNT_ID; //AmosAlexaSpeechlet.ACCOUNT_ID;

    // Key for the transaction number (used as slot key as well as session key!)
    private static final String TRANSACTION_NUMBER_KEY = "TransactionNumber";

    private static final String NEXT_TRANSACTION_KEY = "NextTransaction";

    /**
     * Number of transactions responded within one speech answer step
     */
    private static final int TRANSACTION_LIMIT = 3;

    private DynamoDbMapper dynamoDbMapper = new DynamoDbMapper(DynamoDbClient.getAmazonDynamoDBClient());

    public PeriodicTransactionService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        Intent intent = requestEnvelope.getRequest().getIntent();
        String intentName = intent.getName();
        Session session = requestEnvelope.getSession();
        LOGGER.info("Intent Name: " + intentName);
        String context = (String) session.getAttribute(DIALOG_CONTEXT);

        if (PERIODIC_TRANSACTION_LIST_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, intentName);
            markPeriodicTransactions();
            return listPeriodicTransactions(session);
        } else if (PERIODIC_TRANSACTION_ADD_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, intentName);
            return savePeriodicTransaction(intent, session, false);
        } else if (context != null && context.equals(PERIODIC_TRANSACTION_LIST_INTENT) && YES_INTENT.equals(intentName)) {
            return listNextTransactions(session);
        } else if (context != null && context.equals(PERIODIC_TRANSACTION_LIST_INTENT) && NO_INTENT.equals(intentName)) {
            return getResponse(PERIODIC_TRANSACTION, "Okay, tschuess!");
        } else if (context != null && context.equals(PERIODIC_TRANSACTION_ADD_INTENT) && YES_INTENT.equals(intentName)) {
            return savePeriodicTransaction(intent, session, true);
        } else if (context != null && context.equals(PERIODIC_TRANSACTION_ADD_INTENT) && NO_INTENT.equals(intentName)) {
            return getResponse(PERIODIC_TRANSACTION, "Okay, tschuess!");
        } else if (PERIODIC_TRANSACTION_DELETE_INTENT.equals(intentName)) {
            session.setAttribute(DIALOG_CONTEXT, intentName);
            return deletePeriodicTransaction(intent, session, false);
        } else if (context != null && context.equals(PERIODIC_TRANSACTION_DELETE_INTENT) && YES_INTENT.equals(intentName)) {
            return deletePeriodicTransaction(intent, session, true);
        } else if (context != null && context.equals(PERIODIC_TRANSACTION_DELETE_INTENT) && NO_INTENT.equals(intentName)) {
            return getResponse(PERIODIC_TRANSACTION, "Okay, tschuess!");
        } else {
            throw new SpeechletException("Unhandled intent: " + intentName);
        }
    }

    private SpeechletResponse listPeriodicTransactions(Session session) {
        List<TransactionDB> allTransactionsDb = dynamoDbMapper.loadAll(TransactionDB.class);

        List<TransactionDB> transactionsDb = new ArrayList<>();
        for(TransactionDB transactionDb : allTransactionsDb){
            if(transactionDb.getAccountNumber().equals(ACCOUNT_NUMBER))
                transactionsDb.add(transactionDb);
        }


        transactionsDb = transactionsDb.stream().filter(t -> t.isPeriodic()).collect(Collectors.toList());
        LOGGER.info("TransactionsDB: " + transactionsDb);

        if (transactionsDb == null || transactionsDb.isEmpty()) {
            LOGGER.warn("No periodic transactions have been found.");
            return getResponse(PERIODIC_TRANSACTION, "Du hast momentan keine Transaktionen als periodisch markiert.");
        }

        StringBuilder stringBuilder = new StringBuilder(Transaction.getTransactionSizeText(transactionsDb.size()));
        int i;
        for (i = 0; i < TRANSACTION_LIMIT; i++) {

            if (i < transactionsDb.size()) {
                Transaction transaction = TransactionAPI.getTransactionForAccount(ACCOUNT_NUMBER, transactionsDb.get(i).getTransactionId());
                LOGGER.info("TRANSACTION: " + transaction);
                stringBuilder.append(Transaction.getTransactionText(transaction));
            } else {
                break;
            }
        }

        if (i - 1 < transactionsDb.size()) {
            stringBuilder.append(Transaction.getAskMoreTransactionText());
            session.setAttribute(NEXT_TRANSACTION_KEY, i);
        } else {
            return getResponse(PERIODIC_TRANSACTION, "Das waren alle periodischen Transaktionen.");
        }

        return getSSMLAskResponse(PERIODIC_TRANSACTION, stringBuilder.toString());
    }

    private SpeechletResponse listNextTransactions(Session session) {
        String nextTransaction = (String) session.getAttribute(NEXT_TRANSACTION_KEY);
        if (nextTransaction != null) {
            int nextTransactionId = Integer.valueOf(nextTransaction);
            List<Transaction> transactions = TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER);
            String transactionText = Transaction.getTransactionText(transactions.get(Integer.valueOf(nextTransaction)));
            if (nextTransactionId - 1 < transactions.size()) {
                transactionText = transactionText + Transaction.getAskMoreTransactionText();
                session.setAttribute(NEXT_TRANSACTION_KEY, nextTransaction + 1);
            }
            return getSSMLAskResponse(PERIODIC_TRANSACTION, transactionText);
        }
        return null;
    }

    private SpeechletResponse savePeriodicTransaction(Intent intent, Session session, boolean confirmed) {
        if (!confirmed) {
            if (intent.getSlot(TRANSACTION_NUMBER_KEY) == null || StringUtils.isBlank(intent.getSlot(TRANSACTION_NUMBER_KEY).getValue())) {
                session.getAttributes().clear();
                return getAskResponse(PERIODIC_TRANSACTION, "Das habe ich nicht verstanden. Bitte wiederhole deine Eingabe.");
            } else {
                String transactionId = intent.getSlot(TRANSACTION_NUMBER_KEY).getValue();

                //Search for transaction
                //TODO wait for API extension to get transaction detail request call
                Transaction transaction = TransactionAPI.getTransactionForAccount(ACCOUNT_NUMBER, transactionId);
                LOGGER.info("Found transaction: " + transaction);
                if (transaction == null) {
                    session.getAttributes().clear();
                    return getAskResponse(PERIODIC_TRANSACTION, "Ich kann Transaktion Nummer " + transactionId + " nicht finden." +
                            " Bitte aendere deine Eingabe.");
                }

                session.setAttribute(TRANSACTION_NUMBER_KEY, transactionId);
                return getAskResponse(PERIODIC_TRANSACTION, "Moechtest du die Transaktion mit der Nummer " + transactionId
                        + " wirklich als periodisch markieren?");
            }
        } else {
            String transactionId = (String) session.getAttribute(TRANSACTION_NUMBER_KEY);
            if (transactionId != null) {
                TransactionDB transactionDb = (TransactionDB) dynamoDbMapper.load(TransactionDB.class, transactionId);
                LOGGER.info("TransactionDb: " + transactionDb);

                if (transactionDb == null) {
                    transactionDb = new TransactionDB(transactionId);
                    transactionDb.setPeriodic(true);
                    transactionDb.setAccountNumber(ACCOUNT_NUMBER);
                }

                dynamoDbMapper.save(transactionDb);
                List<TransactionDB> allDbts = dynamoDbMapper.loadAll(TransactionDB.class);
                LOGGER.info("AllDBTransactions: " + allDbts);
                return getResponse(PERIODIC_TRANSACTION, "Transaktion Nummer " + transactionId + " wurde als periodisch markiert.");
            }
        }
        return null;
    }

    private SpeechletResponse deletePeriodicTransaction(Intent intent, Session session, boolean confirmed) {
        if (!confirmed) {
            if (intent.getSlot(TRANSACTION_NUMBER_KEY) == null || StringUtils.isBlank(intent.getSlot(TRANSACTION_NUMBER_KEY).getValue())) {
                session.getAttributes().clear();
                return getAskResponse(PERIODIC_TRANSACTION, "Das habe ich nicht verstanden. Bitte wiederhole deine Eingabe.");
            } else {
                String transactionId = intent.getSlot(TRANSACTION_NUMBER_KEY).getValue();
                TransactionDB transactionDb = (TransactionDB) dynamoDbMapper.load(TransactionDB.class, transactionId);
                LOGGER.info("TransactionDb: " + transactionDb);

                if (transactionDb == null) {
                    return getAskResponse(PERIODIC_TRANSACTION, "Ich kann Transaktion Nummer " + transactionId + " nicht finden." +
                            " Bitte aendere deine Eingabe.");
                }

                session.setAttribute(TRANSACTION_NUMBER_KEY, transactionId);
                return getAskResponse(PERIODIC_TRANSACTION, "Moechtest du die Markierung als periodisch fuer die" +
                        " Transaktion mit der Nummer " + transactionId + " wirklich entfernen?");
            }
        } else {
            String transactionId = (String) session.getAttribute(TRANSACTION_NUMBER_KEY);
            if (transactionId != null) {
                TransactionDB transactionDb = (TransactionDB) dynamoDbMapper.load(TransactionDB.class, transactionId);
                dynamoDbMapper.delete(transactionDb);
                List<TransactionDB> allDbts = dynamoDbMapper.loadAll(TransactionDB.class);
                LOGGER.info("AllDBTransactions: " + allDbts);
                return getResponse(PERIODIC_TRANSACTION, "Transaktion Nummer " + transactionId + " ist nun nicht mehr als periodisch markiert.");
            }
        }
        return null;
    }

    private int markPeriodicTransactions() {
        List<Transaction> transactions = new ArrayList(TransactionAPI.getTransactionsForAccount(ACCOUNT_NUMBER));
        LOGGER.info("Size: " + transactions.size());

        Map<Number, Set<Transaction>> candidates = new HashMap<>();

        for (Transaction t : transactions) {
            LOGGER.info("Transaction: " + t);
            if (!candidates.containsKey(t.getAmount())) {
                LOGGER.info("Not contains");
                Set<Transaction> tList = new HashSet<>();
                tList.add(t);
                candidates.put(t.getAmount(), tList);
            } else {
                candidates.get(t.getAmount()).add(t);
            }
        }

        Iterator<Number> iter = candidates.keySet().iterator();
        while (iter.hasNext()) {
            Number key = iter.next();
            if (candidates.get(key).size() <= 1) {
                iter.remove();
            }
        }

        LOGGER.info("Candidates: " + candidates);
        return candidates.size();
    }
}