package amosalexa.handlers.securitiesAccount;

import api.banking.BankingRESTClient;
import model.banking.Security;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FinanceApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinanceApi.class);

    OkHttpClient client = new OkHttpClient();

    private String run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private static String getTickerSymbolForIsin(Security security) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("idType", "ID_ISIN");
            jsonObject.put("idValue", security.getIsin());
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }
        jsonArray.put(jsonObject);

        BankingRESTClient bankingRESTClient = BankingRESTClient.getInstance();
        String requestResponse = bankingRESTClient.postAnyObject("https://api.openfigi.com/v1/mapping", jsonArray.toString());
        requestResponse = requestResponse.substring(10, requestResponse.length() - 1);
        //LOGGER.info("Request Response: " + requestResponse);
        try {
            final JSONObject obj = new JSONObject(requestResponse);
            String tickerSymbol = obj.getString("ticker");
            return tickerSymbol;
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public static String getStockPrice(Security security) {
        return "100";
        // TODO api was shut down -> find other
        /*
        String tickerSymbol = getTickerSymbolForIsin(security);

        try {
            FinanceApi finance = new FinanceApi();
            String response = finance.run("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22" + tickerSymbol + "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=l");

            //FIXME magic number...
            response = response.substring(93, response.length() - 2);

            final JSONObject jsonObject = new JSONObject(response);
            //LOGGER.info("Stock Price Object: " + jsonObject);

            String stockPrice = jsonObject.getString("LastTradePriceOnly");
            LOGGER.info("Stock price for " + tickerSymbol + ": " + stockPrice);
            return stockPrice;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        return null;
        */
    }
}