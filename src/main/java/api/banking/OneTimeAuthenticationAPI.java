package api.banking;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import model.db.User;

import static amosalexa.handlers.AmosStreamHandler.USER_ID;
import static amosalexa.handlers.AmosStreamHandler.dynamoDbMapper;

// TODO consider where to save secret seed and how to transfer the qr code to user
public class OneTimeAuthenticationAPI {
    private final static GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public static boolean validateOTP(int code) {
        User user = (User) dynamoDbMapper.load(User.class, USER_ID);
        return gAuth.authorize(user.getSecretSeed(), code);
    }

    // should be done by bank server when the user link the account in alexa
    /*
    public static void generateKey (String issuer, String account) {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String demoSecretSeed = key.getKey();
        String demoQR = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, account, key);
    }
     */

}
