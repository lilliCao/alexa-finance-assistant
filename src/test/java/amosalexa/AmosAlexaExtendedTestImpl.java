package amosalexa;

import amosalexa.server.Launcher;
import api.banking.SecuritiesAccountAPI;
import model.banking.SecuritiesAccount;
import model.banking.Security;
import org.eclipse.jetty.server.Server;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * Use extended mytest for tests that connect to an external API and thus need internet connection and/or
 * have limited API requests and should not be executed with every local or travis build.
 * (Call extended tests explicitly by executing 'gradle extendedTests')
 */
@Category(AmosAlexaExtendedTest.class)
public class AmosAlexaExtendedTestImpl extends AbstractAmosAlexaSpeechletTest implements AmosAlexaExtendedTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmosAlexaExtendedTestImpl.class);


    /**************************** Test bank contact ****************************************/
    @Test
    public void bankContactTelephoneNumberTest() throws Exception {
        newSession();

        // pretend local environment
        Launcher.server = new Server();
        Launcher.server.start();

        testIntentMatches("BankTelephone", "(.*) hat die Telefonnummer (.*)");

        Launcher.server.stop();
    }

    @Test
    public void bankContactOpeningHoursTest() throws Exception {

        // pretend local environment
        Launcher.server = new Server();
        Launcher.server.start();

        newSession();
        testIntentMatches("BankOpeningHours", "Sparkasse Nürnberg - Geschäftsstelle hat am (.*)");
        testIntentMatches("BankOpeningHours", "OpeningHoursDate:2017-06-13", "Sparkasse Nürnberg - Geschäftsstelle Geöffnet am Dienstag von (.*) bis (.*)");

        Launcher.server.stop();
    }

    @Test
    public void bankContactAddressTest() throws Exception {

        // pretend local environment
        Launcher.server = new Server();
        Launcher.server.start();

        newSession();

        testIntentMatches("BankAddress", "Sparkasse Nürnberg - Geschäftsstelle hat die Adresse: Lorenzer Pl. 12, 90402 Nürnberg, Germany");
        testIntentMatches("BankAddress", "BankNameSlots:Deutsche Bank", "Deutsche Bank Filiale hat die Adresse: Landgrabenstraße 144, 90459 Nürnberg, Germany");

        Launcher.server.stop();
    }

    /**************************** Test budget tracker ****************************************/
    @Test
    public void securitiesAccountTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        //Securities Account with ID 2 is the one we use for mytest purpose!

        SecuritiesAccount testSecuritiesAccount = SecuritiesAccountAPI.getSecuritiesAccount(2);
        Set<Security> securitiesBefore = new HashSet<>(SecuritiesAccountAPI.getSecuritiesForAccount(2));
        LOGGER.debug("List Before: " + securitiesBefore);
        for (Security s : securitiesBefore) {
            SecuritiesAccountAPI.deleteSecurity(2, s.getSecurityId());
        }

        //Test speech answer with empty depot
        testIntent("SecuritiesAccountInformationIntent", "Keine Depotinformationen vorhanden.");

        //Add some securities
        Security testSecurity1 = new Security("DE000A1EWWW0", "A1EWWW", "Adidas Aktie", 10, 174.19, new Date(), Security.SecurityType.STOCK);
        Security testSecurity2 = new Security("US1912161007", "850663", "The Coca-Cola Company Aktie", 3, 2200, new Date(), Security.SecurityType.STOCK);
        Security testSecurity3 = new Security("US0378331005", "865985", "Apple Inc. Aktie", 2, 9999, new Date(), Security.SecurityType.STOCK);
        Security testSecurity4 = new Security("DE0007164600", "716460 ", "SAP Aktie", 4, 3636, new Date(), Security.SecurityType.STOCK);
        testSecurity1 = SecuritiesAccountAPI.addSecurityToAccount(testSecuritiesAccount.getSecuritiesAccountId(), testSecurity1);
        testSecurity2 = SecuritiesAccountAPI.addSecurityToAccount(testSecuritiesAccount.getSecuritiesAccountId(), testSecurity2);
        testSecurity3 = SecuritiesAccountAPI.addSecurityToAccount(testSecuritiesAccount.getSecuritiesAccountId(), testSecurity3);
        testSecurity4 = SecuritiesAccountAPI.addSecurityToAccount(testSecuritiesAccount.getSecuritiesAccountId(), testSecurity4);

        //Test speech answer with some securities in the depot
        String response = performIntent("SecuritiesAccountInformationIntent");
        Pattern p = Pattern.compile(
                "Du hast momentan 4 Wertpapiere in deinem Depot. " +
                        "Wertpapier Nummer ([0-9]+): Adidas Aktie, ([0-9]+) Stueck, mit einem momentanen Wert von ([0-9\\\\.]+) Euro. " +
                        "Wertpapier Nummer ([0-9]+): The Coca-Cola Company Aktie, ([0-9]+) Stueck, mit einem momentanen Wert von ([0-9\\.]+) Euro. " +
                        "Moechtest du einen weiteren Eintrag hoeren?");
        Matcher m = p.matcher(response);
        if (!m.find()) {
            fail("Unexpected speech output.\nExcpected: " + p.pattern() + "\nAcutual: " + response);
        }

        response = performIntent("AMAZON.YesIntent");
        p = Pattern.compile("Wertpapier Nummer ([0-9]+): " +
                "Apple Inc. Aktie, ([0-9]+) Stueck, mit einem momentanen Wert von ([0-9\\\\.]+) Euro. " +
                "Moechtest du einen weiteren Eintrag hoeren?");
        m = p.matcher(response);
        if (!m.find()) {
            fail("Unexpected speech output.\nExcpected: " + p.pattern() + "\nAcutual: " + response);
        }

        testIntent("AMAZON.NoIntent", "Okay, tschuess!");

        SecuritiesAccountAPI.deleteSecurity(2, testSecurity1.getSecurityId());
        SecuritiesAccountAPI.deleteSecurity(2, testSecurity2.getSecurityId());
        SecuritiesAccountAPI.deleteSecurity(2, testSecurity3.getSecurityId());

//        Restore securities as before mytest
        for (Security s : securitiesBefore) {
            SecuritiesAccountAPI.addSecurityToAccount(2, s);
        }
    }
}