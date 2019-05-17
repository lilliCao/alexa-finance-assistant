package amosalexa.handlers.securitiesAccount;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.ConfigurationAMOS;
import model.banking.Security;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class FinanceApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinanceApi.class);
    private static RestTemplate restTemplate = new RestTemplate();
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * POST http request to the endpoint given by parameter URL
     *
     * @param objectPath endpoint object
     * @param json       body
     * @return banking object
     */
    private static JsonNode post(String objectPath, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(objectPath, entity, String.class);
        try {
            return mapper.readTree(response.getBody());
        } catch (IOException e) {
            return null;
        }
    }

    private static JsonNode get(String objectPath) {
        ResponseEntity<String> response = restTemplate.getForEntity(objectPath, String.class);
        try {
            return mapper.readTree(response.getBody());
        } catch (IOException e) {
            return null;
        }
    }
    private static String getExchSymbolForIsin(Security security) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("idType", "ID_ISIN");
            jsonObject.put("idValue", security.getIsin());
            jsonArray.put(jsonObject);
            JsonNode responseNode = post(ConfigurationAMOS.standardFinanceSymbolApi, jsonArray.toString());
            JsonNode data = responseNode.get(0).get("data").get(0);
            return data.get("exchCode").asText();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    public static String getStockPrice(Security security) {
        String exchCode = getExchSymbolForIsin(security);

        if(exchCode!=null) {
            JsonNode jsonNode = get(ConfigurationAMOS.stockPriceApiEndpoint
                    + "/query?function=GLOBAL_QUOTE&symbol=" + exchCode
                    + "&apikey=" + ConfigurationAMOS.stockPriceApiKey);
            if(jsonNode!=null) {
                return jsonNode.get("Global Quote").get("05. price").asText();
            }
        }
        return null;
    }
}