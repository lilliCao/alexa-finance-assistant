package api.banking;

import model.banking.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import static org.springframework.hateoas.client.Hop.rel;

public class SecuritiesAccountAPI {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritiesAccountAPI.class);

    private static BankingRESTClient bankingRESTClient = BankingRESTClient.getInstance();

    /**
     * Create securities account.
     *
     * @param clearingAccountIban the clearing account´s IBAN number
     * @return the securities account
     */
    public static SecuritiesAccount createSecuritiesAccount(Number id, String clearingAccountIban) {
        SecuritiesAccount newSecuritiesAccount = new SecuritiesAccount();
        if (id != null) {
            newSecuritiesAccount.setSecuritiesAccountId(id);
        }
        newSecuritiesAccount.setClearingAccount(clearingAccountIban);
        //TODO opening date
        return (SecuritiesAccount) bankingRESTClient.postBankingModelObject("/securitiesAccounts", newSecuritiesAccount, SecuritiesAccount.class);
    }

    /**
     * Get securities account.
     *
     * @param securitiesAccountId the securities account id
     * @return the securities account
     */
    public static SecuritiesAccount getSecuritiesAccount(Number securitiesAccountId) {
        return (SecuritiesAccount) bankingRESTClient.getBankingModelObject("/securitiesAccounts/" + securitiesAccountId, SecuritiesAccount.class);
    }

    /**
     * Get all securities for the given securities account.
     *
     * @param securitiesAccountNumber Account number
     * @return Collection of securities
     * @throws HttpClientErrorException
     */
    public static Collection<Security> getSecuritiesForAccount(Number securitiesAccountNumber) throws HttpClientErrorException {
        // TODO: Create a generic method for getting embedded JSON-HAL collections (in BankingRESTClient)
        Traverson traverson = null;
        try {
            traverson = new Traverson(new URI(BankingRESTClient.BANKING_API_ENDPOINT + BankingRESTClient.BANKING_API_BASEURL_V2 + "/securitiesAccounts/" + securitiesAccountNumber),
                    MediaTypes.HAL_JSON);
        } catch (URISyntaxException e) {
            LOGGER.error("getSecuritiesForAccount failed", e);
            return null;
        }

        ParameterizedTypeReference<Resources<Security>> typeRefDevices = new ParameterizedTypeReference<Resources<Security>>() {
        };
        Resources<Security> resResponses = traverson.follow(rel("$._links.self.href")).withHeaders(bankingRESTClient.generateHttpHeaders()).toObject(typeRefDevices);
        return resResponses.getContent();
    }

    /**
     * Delete a securities account.
     *
     * @param securitiesAccountNumber Account number
     * @return True on success, False otherwise
     */
    public static boolean deleteSecuritiesAccount(Number securitiesAccountNumber) {
        try {
            bankingRESTClient.deleteBankingModelObject("/securitiesAccounts/" + securitiesAccountNumber);
            return true;
        } catch (RestClientException e) {
            LOGGER.error("deleteSecuritiesAccount failed", e);
            return false;
        }
    }


    /**
     * Add security to securities account.
     *
     * @param securitiesAccountId the securities account id
     * @param newSecurity         the security to add
     * @return the security
     */
    public static Security addSecurityToAccount(Number securitiesAccountId, Security newSecurity) {
        return (Security) bankingRESTClient.postBankingModelObject("/securitiesAccounts/" + securitiesAccountId + "/securities", newSecurity, Security.class);
    }

    /**
     * Update a security.
     *
     * @param securitiesAccountId Securities account id
     * @param security            Security
     * @return True on success, False otherwise
     */
    public static boolean updateSecurity(Number securitiesAccountId, Security security) {
        try {
            bankingRESTClient.putBankingModelObject("/securitiesAccounts/" + securitiesAccountId + "/securities/" + security.getSecurityId(), security);
            return true;
        } catch (RestClientException e) {
            LOGGER.error("updateSecurity failed", e);
            return false;
        }
    }

    /**
     * Get a security.
     *
     * @param securitiesAccountId Securities account number
     * @param securityId          Security id
     * @return Security
     */
    public static Security getSecurity(Number securitiesAccountId, Number securityId) {
        return (Security) bankingRESTClient.getBankingModelObject("/securitiesAccounts/" + securitiesAccountId + "/securities/" + securityId, Security.class);
    }

    /**
     * Delete a security.
     *
     * @param securitiesAccountId Securities account number
     * @param securityId          Security id
     * @return True on success, False otherwise
     */
    public static boolean deleteSecurity(Number securitiesAccountId, Number securityId) {
        try {
            bankingRESTClient.deleteBankingModelObject("/securitiesAccounts/" + securitiesAccountId + "/securities/" + securityId);
            return true;
        } catch (RestClientException e) {
            LOGGER.error("deleteSecurity failed", e);
            return false;
        }
    }
}
