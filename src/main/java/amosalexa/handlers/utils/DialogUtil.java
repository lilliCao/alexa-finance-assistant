package amosalexa.handlers.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogUtil {

    private static final Logger log = LoggerFactory.getLogger(DialogUtil.class);

    /**
     * checks the numerus for the noun "Transaktion"
     *
     * @param count count
     * @return String
     */
    public static String getTransactionNumerus(int count) {
        return count == 1 ? "Transaktion" : "Transaktionen";
    }

    /**
     * gutschrift oder abbuchung
     *
     * @param balance balance
     * @return String
     */
    public static String getBalanceVerb(double balance) {
        return balance >= 0 ? "gutgeschrieben" : "abgebucht";
    }


    public static String getIbanSsmlOutput(String iban) {
        if (iban == null) {
            return null;
        }
        String ssml = "<emphasis level=\"reduced\"><say-as interpret-as=\"characters\">";
        String firstPart = iban.startsWith(" ") ? iban.substring(0, 3).substring(1) : iban.substring(0, 2);
        ssml = ssml + firstPart + "<break time=\"10ms\"/>";
        ssml = ssml + "</say-as>";
        ssml = ssml + "<say-as interpret-as=\"digits\">";
        String firstNumberChar = iban.startsWith(" ") ? iban.substring(3, 4) : iban.substring(2, 3);
        ssml = ssml + firstNumberChar + "<break time=\"10ms\"/>";
        String remainingNumbers = iban.startsWith(" ") ? iban.substring(4) : iban.substring(3);
        for (char ch : remainingNumbers.toCharArray()) {
            ssml = ssml.concat(ch + "<break time=\"10ms\"/>");
        }
        ssml = ssml + "</say-as></emphasis>";
        return ssml;
    }

    public static String readNumberAsDigit(String number) {
        if (number == null) {
            return null;
        }
        String ssml = "<say-as interpret-as=\"digits\">";
        for (char ch : number.toCharArray()) {
            ssml = ssml.concat(ch + "<break time=\"10ms\"/>");
        }
        ssml = ssml + "</say-as>";
        return ssml;
    }


}
