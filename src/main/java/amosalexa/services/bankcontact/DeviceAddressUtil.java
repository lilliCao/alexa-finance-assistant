package amosalexa.services.bankcontact;


import amosalexa.services.bankcontact.exceptions.DeviceAddressClientException;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.Context;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.interfaces.system.SystemInterface;
import com.amazon.speech.speechlet.interfaces.system.SystemState;
import model.location.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceAddressUtil {

    private static final Logger log = LoggerFactory.getLogger(DeviceAddressUtil.class);

    /**
     * tries to get the device address
     * @param requestEnvelope SpeechletRequestEnvelope
     */
    public static Address getDeviceAddress(SpeechletRequestEnvelope<IntentRequest> requestEnvelope){

        try {
            String consentToken = requestEnvelope.getSession().getUser().getPermissions().getConsentToken();
            SystemState systemState = getSystemState(requestEnvelope.getContext());
            String deviceId = systemState.getDevice().getDeviceId();
            String apiEndpoint = systemState.getApiEndpoint();
            AlexaDeviceAddressClient alexaDeviceAddressClient = new AlexaDeviceAddressClient(deviceId, consentToken, apiEndpoint);

            return alexaDeviceAddressClient.getFullAddress();
        } catch (DeviceAddressClientException e) {
            log.error("Device address client failed to successfully return the address.");
        } catch (NullPointerException e){
            log.warn("No Permission to request device address!");
        }

        return null;
    }

    /**
     * Helper method that retrieves the system state from the request context.
     *
     * @param context request context.
     * @return SystemState the systemState
     */
    private static SystemState getSystemState(Context context) {
        return context.getState(SystemInterface.class, SystemState.class);
    }
}
