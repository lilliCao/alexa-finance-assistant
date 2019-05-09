/**
 * Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0/
 * <p>
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package amosalexa;

import amosalexa.services.AbstractSpeechService;
import amosalexa.services.bankaccount.BalanceLimitService;
import amosalexa.services.bankaccount.BankAccountService;
import amosalexa.services.bankaccount.ContactTransferService;
import amosalexa.services.bankaccount.StandingOrderService;
import amosalexa.services.bankcontact.BankContactService;
import amosalexa.services.budgetreport.BudgetReportService;
import amosalexa.services.budgettracker.BudgetTrackerService;
import amosalexa.services.cards.BlockCardService;
import amosalexa.services.cards.ReplacementCardService;
import amosalexa.services.contacts.ContactService;
import amosalexa.services.budgettracker.EditCategoriesService;
import amosalexa.services.demo.TimeTravelService;
import amosalexa.services.financing.*;
import amosalexa.services.help.HelpService;
import amosalexa.services.securitiesAccount.SecuritiesAccountInformationService;
import amosalexa.services.transfertemplates.TransferTemplateService;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import model.banking.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base speechlet to register services that handle the intents.
 */
public class AmosAlexaSpeechlet extends AbstractSpeechService implements SpeechletSubject {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmosAlexaSpeechlet.class);

    public static final String USER_ID = "4711";
    private static final Account demoAccount = AccountFactory.getInstance().createDemo();
    public static final String ACCOUNT_ID = demoAccount.getNumber(); //"0000000001";
    public static final String ACCOUNT_IBAN = demoAccount.getIban();

    private static AmosAlexaSpeechlet amosAlexaSpeechlet = new AmosAlexaSpeechlet();
    private Map<String, List<SpeechletObserver>> speechServiceObservers = new HashMap<>();

    public static AmosAlexaSpeechlet getInstance() {

        new BankAccountService(amosAlexaSpeechlet);
        new StandingOrderService(amosAlexaSpeechlet);
        new AffordabilityService(amosAlexaSpeechlet);
        new BankContactService(amosAlexaSpeechlet);
        new SavingsPlanService(amosAlexaSpeechlet);
        new BlockCardService(amosAlexaSpeechlet);
        new ReplacementCardService(amosAlexaSpeechlet);
        new TransferTemplateService(amosAlexaSpeechlet);
        new SecuritiesAccountInformationService(amosAlexaSpeechlet);
        new BalanceLimitService(amosAlexaSpeechlet);
        new BudgetReportService(amosAlexaSpeechlet);
        new ContactService(amosAlexaSpeechlet);
        new ContactTransferService(amosAlexaSpeechlet);
        new BudgetTrackerService(amosAlexaSpeechlet);
        new HelpService(amosAlexaSpeechlet);
        new EditCategoriesService(amosAlexaSpeechlet);
        new PeriodicTransactionService(amosAlexaSpeechlet);
        new TransactionForecastService(amosAlexaSpeechlet);
        new AccountBalanceForecastService(amosAlexaSpeechlet);
        new TimeTravelService(amosAlexaSpeechlet);

        LOGGER.info("observers list: ---------------------------------------");
        for (Map.Entry<String, List<SpeechletObserver>> item: amosAlexaSpeechlet.speechServiceObservers.entrySet()) {
            LOGGER.info("Key="+item.getKey());
            LOGGER.info("Values=");
            for (SpeechletObserver ob: item.getValue()) {
                LOGGER.info("Observer="+ob.getClass().getName());
            }
        }
        return amosAlexaSpeechlet;
    }

    /**
     * attach a speechlet observer - observer will be notified if the intent name matches the key
     *
     * @param speechletObserver
     * @param intentName
     */
    @Override
    public void attachSpeechletObserver(SpeechletObserver speechletObserver, String intentName) {
        // Check for duplicate start Intents
        for (List<SpeechletObserver> observerList : speechServiceObservers.values()) {
            for (SpeechletObserver observer : observerList) {
                for (String startIntent : speechletObserver.getStartIntents()) {
                    if (observer.getStartIntents().contains(startIntent) && !observer.getDialogName().equals(speechletObserver.getDialogName())) {
                        // Oh no, duplicate start Intent!
                        throw new IllegalArgumentException("Duplicate start Intent [" + startIntent + "], defined by both [" + observer.getDialogName() + "] " +
                                "and [" + speechletObserver.getDialogName() + "]!");
                    }
                }
            }
        }

        List<SpeechletObserver> list = speechServiceObservers.get(intentName);

        if (list == null) {
            list = new LinkedList<>();
            speechServiceObservers.put(intentName, list);
        }

        list.add(speechletObserver);
    }

    /**
     * notifies the speechlet observer by the requested intent name - invokes method of observer
     *
     * @param requestEnvelope request from amazon
     * @return SpeechletResponse
     */
    @Override
    public SpeechletResponse notifyOnIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        String intentName = requestEnvelope.getRequest().getIntent().getName();
        String sessionId = requestEnvelope.getSession().getSessionId();
        SessionStorage.Storage sessionStorage = SessionStorage.getInstance().getStorage(sessionId);

        List<SpeechletObserver> list = speechServiceObservers.get(intentName);

        if (list == null) {
            return null;
        }

        for (SpeechletObserver speechService : list) {
            boolean validIntent = false;

            // Check if this Service should handle this Intent
            if (!speechService.getHandledIntents().contains(intentName)) {
                continue;
            }

            // Check if a dialog is active
            String currentDialogContext = (String) sessionStorage.get(SessionStorage.CURRENTDIALOG);
            if (currentDialogContext != null && !currentDialogContext.equals(speechService.getDialogName())) {
                // A dialog is active, but not for this service
                continue;
            }

            // Check if this Intent starts this Service
            if (currentDialogContext == null && speechService.getStartIntents().contains(intentName)) {
                // Set the dialog context in the current session
                sessionStorage.put(SessionStorage.CURRENTDIALOG, speechService.getDialogName());
                validIntent = true;
            }

            // Check if the active dialog is intended for this Service
            if (!validIntent && currentDialogContext != null && currentDialogContext.equals(speechService.getDialogName())) {
                validIntent = true;
            }

            if (!validIntent) {
                // Invalid intent, we should not let the Service handle it
                LOGGER.error("Invalid intent [" + intentName + "]!");
                continue;
            }

            SpeechletResponse response = null;
            try {
                response = speechService.onIntent(requestEnvelope);
            } catch (SpeechletException e) {
                LOGGER.error(e.getMessage());
            }

            if (response != null) {
                if (response.getShouldEndSession()) {
                    SessionStorage.getInstance().removeStorage(sessionId);
                }
                return response;
            }
        }
        return null;
    }

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        LOGGER.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        LOGGER.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        return null;
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();

        LOGGER.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : "";

        SessionStorage.Storage sessionStorage = SessionStorage.getInstance().getStorage(session.getSessionId());
        String currentDialogContext = (String) sessionStorage.get(SessionStorage.CURRENTDIALOG);

        LOGGER.info("Intent: " + intentName);
        LOGGER.info("DialogContext: " + currentDialogContext);

        if(intentName.equals(STOP_INTENT)){
            return getResponse("Stop", "");
        }

        SpeechletResponse response = notifyOnIntent(requestEnvelope);

        if (response == null) {
            SessionStorage.getInstance().removeStorage(session.getSessionId());

            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            //speech.setText("Ein Fehler ist aufgetreten.");
            speech.setText("");

            return SpeechletResponse.newTellResponse(speech);
        }
        return response;
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        LOGGER.info("onSessionEnded");
    }

}
