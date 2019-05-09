package amosalexa.services;


import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.*;

public abstract class AbstractSpeechService {

    /**
     * To hold session dialog context (which dialog is the current active dialog)
     */
    protected static final String DIALOG_CONTEXT = "DialogContext";

    /**
     * To hold information about the context within one dialog.
     */
    protected static final String WITHIN_DIALOG_CONTEXT = "WithinDialogContext";

    protected static final String YES_INTENT = "AMAZON.YesIntent";
    protected static final String NO_INTENT = "AMAZON.NoIntent";
    protected static final String STOP_INTENT = "AMAZON.StopIntent";
    protected static final String HELP_INTENT = "AMAZON.HelpIntent";

    //Should be used for utterances that contain only a number
    protected static final String PLAIN_NUMBER_INTENT = "PlainNumberIntent";

    //Should be used for utterances that contain a number plus 'Euro'
    protected static final String PLAIN_EURO_INTENT = "PlainEuroIntent";

    //Should be used for utterances that contain a number plus 'Jahre' or 'Jahr'
    protected static final String PLAIN_YEARS_INTENT = "PlainYearsIntent";

    //Used as slot key for all plain intents (see above)
    protected static final String NUMBER_SLOT_KEY = "Number";

    /**
     * Helper method that will get the intent name from a provided Intent object. If a name does not
     * exist then this method will return null.
     *
     * @param intent intent object provided from a skill request.
     * @return intent name or null.
     */
    protected String getIntentName(Intent intent) {
        return (intent != null) ? intent.getName() : null;
    }

    /**
     * Helper method for retrieving an Ask response with a simple card and reprompt included.
     *
     * @param cardTitle  Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user.
     * @return the resulting card and speech text.
     */
    protected SpeechletResponse getAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(speech);

        SpeechletResponse response = SpeechletResponse.newAskResponse(speech, reprompt, card);
        response.setShouldEndSession(false);
        return response;
    }

    /**
     * Helper method for retrieving an response with a simple card and reprompt included.
     *
     * @param cardTitle  Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user.
     * @return the resulting card and speech text.
     */
    protected SpeechletResponse getResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);

        SpeechletResponse response = SpeechletResponse.newTellResponse(speech, card);
        response.setShouldEndSession(true);
        return response;
    }

    /**
     * Helper method for retrieving an OutputSpeech object when given a string of TTS.
     *
     * @param speechText the text that should be spoken out to the user.
     * @return an instance of SpeechOutput.
     */
    protected PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    /**
     * Helper method for retrieving an SsmlOutputSpeech object when given a string of TTS.
     *
     * @param ssmlText the text that should be spoken out to the user.
     * @return an instance of SsmlOutputSpeech.
     */
    protected SsmlOutputSpeech getSSMLOutputSpeech(String ssmlText) {
        SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
        outputSpeech.setSsml("<speak>" + ssmlText + "</speak>");

        return outputSpeech;
    }

    /**
     * Helper method that returns a reprompt object. This is used in Ask responses where you want
     * the user to be able to respond to your speech.
     *
     * @param outputSpeech The OutputSpeech object that will be said once and repeated if necessary.
     * @return Reprompt instance.
     */
    protected Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    /**
     * Helper method that creates a card object.
     *
     * @param title   title of the card
     * @param content body of the card
     * @return SimpleCard the display card to be sent along with the voice response.
     */
    protected SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }

    /**
     * response a ssml speech text (further information:
     * https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/speech-synthesis-markup-language-ssml-reference)
     *
     * @param cardTitle card title
     * @param ssmlText  string in ssml format
     * @return SpeechletResponse
     */
    protected SpeechletResponse getSSMLResponse(String cardTitle, String ssmlText) {
        SimpleCard card = getSimpleCard(cardTitle, ssmlText);
        SsmlOutputSpeech ssmlOutputSpeech = getSSMLOutputSpeech(ssmlText);

        SpeechletResponse response = SpeechletResponse.newTellResponse(ssmlOutputSpeech, card);
        response.setShouldEndSession(true);
        return response;
    }

    /**
     * Helper method for retrieving an Ask response with a simple card and reprompt included.
     *
     * @param cardTitle    Title of the card that you want displayed.
     * @param speechText   speech text that will be spoken to the user.
     * @param repromptText
     * @return the resulting card and speech text.
     */
    protected SpeechletResponse getSSMLAskResponse(String cardTitle, String speechText, String repromptText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        SsmlOutputSpeech ssmlOutputSpeech = getSSMLOutputSpeech(speechText);
        SsmlOutputSpeech repromptSpeech = getSSMLOutputSpeech(repromptText);
        Reprompt reprompt = getReprompt(repromptSpeech);

        SpeechletResponse response = SpeechletResponse.newAskResponse(ssmlOutputSpeech, reprompt, card);
        response.setShouldEndSession(false);
        return response;
    }

    /**
     * Helper method for retrieving an Ask response with a simple card and a speech text included.
     *
     * @param cardTitle  Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user (and also be used as reprompt)
     * @return the resulting card and speech text.
     */
    protected SpeechletResponse getSSMLAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        SsmlOutputSpeech ssmlOutputSpeech = getSSMLOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(ssmlOutputSpeech);

        SpeechletResponse response = SpeechletResponse.newAskResponse(ssmlOutputSpeech, reprompt, card);
        response.setShouldEndSession(false);
        return response;
    }

    /**
     * Gets error response.
     *
     * @param errorDetail error details
     * @return the error response
     */
    protected SpeechletResponse getErrorResponse(String errorDetail) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        //speech.setText("Ein Fehler ist aufgetreten. " + errorDetail);
        speech.setText("");

        SpeechletResponse response = SpeechletResponse.newTellResponse(speech);
        response.setShouldEndSession(true);
        return response;
    }

    /**
     * Gets error response.
     *
     * @return the error response
     */
    protected SpeechletResponse getErrorResponse() {
        return getErrorResponse("");
    }

}
