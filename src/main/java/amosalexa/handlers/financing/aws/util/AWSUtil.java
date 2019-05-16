package amosalexa.handlers.financing.aws.util;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AWSUtil {

    public static String encodeString(String str) {
        try {
            str = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String shortTitle(String title) {
        String[] parts = title.split(" ");
        StringBuilder shortTitle = new StringBuilder();
        if (parts.length > 3) {
            for (int i = 0; i < 3; i++) {
                shortTitle.append(parts[i]).append(" ");
            }
            return shortTitle.toString();
        }
        return title;
    }

    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
