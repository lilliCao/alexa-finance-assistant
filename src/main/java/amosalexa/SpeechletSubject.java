package amosalexa;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.*;

public interface SpeechletSubject extends SpeechletV2{

    void attachSpeechletObserver(SpeechletObserver speechletObserver, String intentName);

    SpeechletResponse notifyOnIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope);
}
