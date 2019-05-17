package api.banking;

import configuration.ConfigurationAMOS;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static amosalexa.handlers.AmosStreamHandler.USER_ID;

/**
 * Banking Rest Client
 * <p>
 * more information visit the api guide: https://s3.eu-central-1.amazonaws.com/amos-bank/api-guide.html
 */

public class BankingRESTClient {

    /**
     * Banking API Endpoint
     */
    public static final String BANKING_API_ENDPOINT = ConfigurationAMOS.bankApi;

    /**
     * Banking API base URL v2.0
     */
    public static final String BANKING_API_BASEURL_V2 = "/api/v2_0";

    /**
     * Logger
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BankingRESTClient.class);

    /**
     * REST template
     */
    private RestTemplate restTemplate = new RestTemplate();

    /**
     *
     */
    private static BankingRESTClient bankingRESTClient = new BankingRESTClient();

    /**
     * @return BankingRESTClient
     */
    public static BankingRESTClient getInstance() {
        return bankingRESTClient;
    }

    /**
     * Generate http headers that include the authorization token.
     *
     * @return the http headers
     */
    public static HttpHeaders generateHttpHeaders() {
        log.info("generateHttpHeaders");
        // Refresh the user's access token if necessary
        AuthenticationAPI.updateAccessToken(USER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + AuthenticationAPI.getAccessToken(USER_ID));
        return headers;
    }

    /**
     * GET HTTP request to the banking endpoint.
     *
     * @param objectPath object path of the API interface
     * @param cl         response mapping class
     * @return banking object
     */
    public Object getBankingModelObject(String objectPath, Class cl) throws RestClientException {
        String url = BANKING_API_ENDPOINT + BANKING_API_BASEURL_V2 + objectPath;
        log.info("GET from API: " + objectPath + " - Mapping to: " + cl);

        //return restTemplate.getForObject(url, cl);

        HttpEntity entity = new HttpEntity(null, generateHttpHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, cl).getBody();
    }

    /**
     * POST HTTP request to the banking endpoint.
     *
     * @param objectPath object path of the API interface
     * @param request    post object
     * @param cl         response mapping class
     * @return banking object
     */
    public Object postBankingModelObject(String objectPath, Object request, Class cl) throws RestClientException {
        String url = BANKING_API_ENDPOINT + BANKING_API_BASEURL_V2 + objectPath;
        log.info("POST to API: " + objectPath + " - Mapping to: " + cl);

        //return restTemplate.postForObject(url, request, cl);

        HttpEntity entity = new HttpEntity(request, generateHttpHeaders());
        return restTemplate.exchange(url, HttpMethod.POST, entity, cl).getBody();
    }

    /**
     * POST HTTP request to the banking endpoint.
     *
     * @param objectPath object path of the API interface
     * @param request    post object
     * @return banking object
     */
    public Object postBankingModelObject(String objectPath, Object request) throws RestClientException {
        String url = BANKING_API_ENDPOINT + BANKING_API_BASEURL_V2 + objectPath;
        log.info("POST to API: " + objectPath);

        //return restTemplate.postForObject(url, request, cl);

        HttpEntity entity = new HttpEntity(request, generateHttpHeaders());
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }

    /**
     * PUT HTTP request to the banking endpoint.
     *
     * @param objectPath endpoint object
     * @param request    post object
     */
    public void putBankingModelObject(String objectPath, Object request) throws RestClientException {
        String url = BANKING_API_ENDPOINT + BANKING_API_BASEURL_V2 + objectPath;
        log.info("PUT to API: " + objectPath);

        //restTemplate.put(url, request);

        HttpEntity entity = new HttpEntity(request, generateHttpHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    /**
     * DELETE HTTP request to the banking endpoint.
     *
     * @param objectPath endpoint object
     */
    public void deleteBankingModelObject(String objectPath) throws RestClientException {
        String url = BANKING_API_ENDPOINT + BANKING_API_BASEURL_V2 + objectPath;
        log.info("DELETE to API: " + objectPath);

        //restTemplate.delete(url);

        HttpEntity entity = new HttpEntity(null, generateHttpHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }
}
