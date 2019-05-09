package amosalexa.services.demo;

import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.ui.SimpleCard;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Service for dummy intents for demo purposes. It just gives an answer that will be part of our story and potentially sets up
 * some database entries to match with the story.
 */
public class TimeTravelService extends AbstractSpeechService implements SpeechService {

    private static final String CARD_TITLE = "Zeitreise";
    private static final String FUTURE_INTENT = "FutureIntent";
    private static final String PAST_INTENT = "PastIntent";

    public TimeTravelService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                FUTURE_INTENT,
                PAST_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                FUTURE_INTENT,
                PAST_INTENT
        );
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
        IntentRequest request = requestEnvelope.getRequest();
        Intent intent = request.getIntent();

        if (intent.getName().equals(FUTURE_INTENT)) {
            // TODO: Insert code that sets up data

            String text = "Natürlich, nichts leichter als das. Bitte anschnallen!";

            SpeechletResponse response = new SpeechletResponse();

            // Back to the future song
            List<Directive> directives = new LinkedList<>();
            PlayDirective directive = new PlayDirective();
            AudioItem audioItem = new AudioItem();
            Stream stream = new Stream();
            stream.setToken("this-is-the-audio-token");
            stream.setUrl("https://files.bitspark.de/bttf.mp3"); // temporarily hosted on Julian's server
            audioItem.setStream(stream);
            directive.setAudioItem(audioItem);
            directive.setPlayBehavior(PlayBehavior.REPLACE_ALL);
            directives.add(directive);
            response.setDirectives(directives);

            // Card
            SimpleCard simpleCard = new SimpleCard();
            simpleCard.setTitle(CARD_TITLE);
            simpleCard.setContent(text);
            response.setCard(simpleCard);

            // Speech
            response.setOutputSpeech(getSSMLOutputSpeech(text));

            return response;
        } else if (intent.getName().equals(PAST_INTENT)) {
            // Create new demo account
            //AccountFactory accountFactory = AccountFactory.getInstance();
            //Account account = accountFactory.createDemo();

            String text = "<prosody rate=\"fast\">Okay! Es geht wieder zurueck. Halte dich fest!</prosody>";

            SpeechletResponse response = new SpeechletResponse();

            // Back to the future song
            List<Directive> directives = new LinkedList<>();
            PlayDirective directive = new PlayDirective();
            AudioItem audioItem = new AudioItem();
            Stream stream = new Stream();
            stream.setToken("this-is-the-audio-token");
            stream.setUrl("https://files.bitspark.de/bttf.mp3"); // temporarily hosted on Julian's server
            audioItem.setStream(stream);
            directive.setAudioItem(audioItem);
            directive.setPlayBehavior(PlayBehavior.REPLACE_ALL);
            directives.add(directive);
            response.setDirectives(directives);

            // Card
            SimpleCard simpleCard = new SimpleCard();
            simpleCard.setTitle(CARD_TITLE);
            simpleCard.setContent(text);
            response.setCard(simpleCard);

            // Speech
            response.setOutputSpeech(getSSMLOutputSpeech(text));

            return response;
        }

        // Shouldn't happen
        assert (false);
        return null;
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        for (String intent : getHandledIntents()) {
            speechletSubject.attachSpeechletObserver(this, intent);
        }
    }

}
