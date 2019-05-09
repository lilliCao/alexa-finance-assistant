package amosalexa;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;

import java.util.List;

public interface SpeechletObserver {
    SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException;

    void subscribe(SpeechletSubject speechletSubject);

    /**
     * Unique dialog name for this Service.
     *
     * @return the dialog name
     */
    String getDialogName();

    /**
     * List of Intents that start this Service.
     *
     * @return Array of start Intents
     */
    List<String> getStartIntents();

    /**
     * List of Intents that are handled by this Service.
     *
     * @return List of handled Intents
     */
    List<String> getHandledIntents();
}
