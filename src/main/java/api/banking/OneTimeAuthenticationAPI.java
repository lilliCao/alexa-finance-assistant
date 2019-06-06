package api.banking;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

// TODO consider where to save secret seed and how to transfer the qr code to user
public class OneTimeAuthenticationAPI {
    private final static GoogleAuthenticator gAuth = new GoogleAuthenticator();
    public static String demoSecretSeed = "BIRAKXJN42SDFMMU";
    public static String demoQR = "https://chart.googleapis.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth%3A%2F%2Ftotp%2Fissuer%3AaccountName%3Fsecret%3DBIRAKXJN42SDFMMU%26issuer%3Dissuer";

    public static boolean validateOTP(int code) {
        //return gAuth.authorize(demoSecretSeed, code);
        return code==3;
    }

    // should be done by bank server when the user link the account in alexa
    public static void generateKey (String issuer, String account) {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        demoSecretSeed = key.getKey();
        demoQR = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, account, key);
    }

}
