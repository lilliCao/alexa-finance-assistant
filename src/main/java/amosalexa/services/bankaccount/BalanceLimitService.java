package amosalexa.services.bankaccount;


import amosalexa.Service;
import amosalexa.SessionStorage;
import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.help.HelpService;
import api.aws.DynamoDbMapper;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import model.db.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static amosalexa.AmosAlexaSpeechlet.USER_ID;

@Service(
		functionName = "Kontolimit setzen",
		functionGroup = HelpService.FunctionGroup.BUDGET_TRACKING,
		example = "Setze mein Kontolimit auf 800 Euro",
		description = "Diese Funktion erlaubt es dir ein Kontolimit zu setzen, sodass du nicht aus Versehen zu viel Geld ausgibst."
)
public class BalanceLimitService extends AbstractSpeechService implements SpeechService {
	@Override
	public String getDialogName() {
		return this.getClass().getName();
	}

	@Override
	public List<String> getStartIntents() {
		return Arrays.asList(
				SET_BALANCE_LIMIT_INTENT,
				GET_BALANCE_LIMIT_INTENT
		);
	}

	@Override
	public List<String> getHandledIntents() {
		return Arrays.asList(
				SET_BALANCE_LIMIT_INTENT,
				GET_BALANCE_LIMIT_INTENT,
				YES_INTENT,
				NO_INTENT
		);
	}

	private static final String SET_BALANCE_LIMIT_INTENT = "SetBalanceLimitIntent";
	private static final String GET_BALANCE_LIMIT_INTENT = "GetBalanceLimitIntent";
	private static final String CARD_TITLE = "Kontolimit";
	private static final String NEW_BALANCE_LIMIT = "NewBalanceLimit";

	public BalanceLimitService(SpeechletSubject speechletSubject) {
		subscribe(speechletSubject);
	}

	@Override
	public void subscribe(SpeechletSubject speechletSubject) {
		for(String intent : getHandledIntents()) {
			speechletSubject.attachSpeechletObserver(this, intent);
		}
	}

	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws SpeechletException {
		Intent intent = requestEnvelope.getRequest().getIntent();
		Session session = requestEnvelope.getSession();

		SessionStorage.Storage sessionStorage = SessionStorage.getInstance().getStorage(session.getSessionId());

		model.db.User user = (User)DynamoDbMapper.getInstance().load(model.db.User.class, USER_ID);

		if(intent.getName().equals(SET_BALANCE_LIMIT_INTENT)) {
			Map<String, Slot> slots = intent.getSlots();
			Slot balanceLimitAmountSlot = slots.get("BalanceLimitAmount");

			if(balanceLimitAmountSlot == null || balanceLimitAmountSlot.getValue() == null) {
				return getAskResponse(CARD_TITLE, "Auf welchen Betrag möchtest du dein Kontolimit setzen?");
			}

			String balanceLimitAmount = balanceLimitAmountSlot.getValue();

			if(balanceLimitAmount.equals("?")) {
				return getErrorResponse("Der angegebene Betrag ist ungültig.");
			}

			sessionStorage.put(NEW_BALANCE_LIMIT, balanceLimitAmount);
			return getAskResponse(CARD_TITLE, "Möchtest du dein Kontolimit wirklich auf " + balanceLimitAmount + " Euro setzen?");
		} else if(intent.getName().equals(GET_BALANCE_LIMIT_INTENT)) {
			return getResponse(CARD_TITLE, "Dein aktuelles Kontolimit beträgt " + user.getBalanceLimit() + " Euro.");
		} else if(intent.getName().equals(YES_INTENT)) {
			if(!sessionStorage.containsKey(NEW_BALANCE_LIMIT)) {
				return getErrorResponse();
			}
			String balanceLimitAmount = (String)sessionStorage.get(NEW_BALANCE_LIMIT);
			user.setBalanceLimit(Integer.parseInt(balanceLimitAmount));
			DynamoDbMapper.getInstance().save(user);
			return getResponse(CARD_TITLE, "Okay, dein Kontolimit wurde auf " + balanceLimitAmount + " Euro gesetzt.");
		} else if(intent.getName().equals(NO_INTENT)) {
			return getResponse(CARD_TITLE, "");
		}

		return null;
	}

}
