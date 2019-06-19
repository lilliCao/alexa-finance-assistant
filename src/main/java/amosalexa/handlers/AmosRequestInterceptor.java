package amosalexa.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;

public class AmosRequestInterceptor implements RequestInterceptor {
    public static String accessToken;

    @Override
    public void process(HandlerInput inputR) {
        accessToken = inputR.getRequestEnvelope().getContext().getSystem().getUser().getAccessToken();
    }
}
