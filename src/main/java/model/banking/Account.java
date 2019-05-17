package model.banking;

import amosalexa.handlers.utils.DialogUtil;

import java.util.HashMap;
import java.util.Map;

public class Account {

    private Number creditcardLimit;
    private final static String creditcardLimitSlot = "kreditkartenlimit";
    private String creditcardLimitText;

    private Number balance;
    private final static String balanceSlot = "kontostand";
    private String balanceText;

    private String openingDate;
    private final static String openingDateSlot = "eröffnungsdatum";
    private String openingDateText;

    private Number creditLimit;
    private final static String creditLimitSlot = "kreditlimit";
    private String creditLimitText;

    private String number;
    private final static String numberSlot = "kontonummer";
    private String numberText;

    private Number interestRate;
    private final static String interestRateSlot = "zinssatz";
    private String interestRateText;

    private Number withdrawalFee;
    private final static String withdrawalFeeSlot = "abhebegebühr";
    private String withdrawalFeeText;

    private String iban;
    private final static String ibanSlot = "iban";
    private String ibanText;


    private Map<String, String> speechTexts;

    public void setSpeechTexts() {
        creditcardLimitText = "Dein " + creditcardLimitSlot + " beträgt <say-as interpret-as=\"unit\">€" + creditcardLimit + "</say-as>";
        balanceText = "Dein " + balanceSlot + " beträgt <say-as interpret-as=\"unit\">€" + balance + "</say-as>";
        openingDateText = "Dein " + openingDateSlot + " war " + openingDate;
        creditLimitText = "Dein " + creditLimitSlot + " beträgt <say-as interpret-as=\"unit\">€" + creditcardLimit + "</say-as>";
        numberText = "Deine " + numberSlot + " lautet " + DialogUtil.readNumberAsDigit(number);
        interestRateText = "Dein " + interestRateSlot + " ist aktuell " + interestRate;
        withdrawalFeeText = "Deine " + withdrawalFeeSlot + " beträgt <say-as interpret-as=\"unit\">€" + withdrawalFee + "</say-as>";
        ibanText = "Deine " + ibanSlot + " lautet " + DialogUtil.getIbanSsmlOutput(iban);

        speechTexts = new HashMap<String, String>() {{
            put(creditcardLimitSlot, creditcardLimitText);
            put(balanceSlot, balanceText);
            put(openingDateSlot, openingDateText);
            put(creditLimitSlot, creditLimitText);
            put(numberSlot, numberText);
            put(interestRateSlot, interestRateText);
            put(withdrawalFeeSlot, withdrawalFeeText);
            put(ibanSlot, ibanText);
        }};

    }

    public Number getCreditcardLimit() {
        return creditcardLimit;
    }

    public void setCreditcardLimit(double creditcardLimit) {
        this.creditcardLimit = creditcardLimit;
    }

    public Number getBalance() {
        return balance;
    }

    public void setBalance(Number balance) {
        this.balance = balance;
    }

    public String getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(String openingDate) {
        this.openingDate = openingDate;
    }

    public Number getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Number getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public Number getWithdrawalFee() {
        return withdrawalFee;
    }

    public void setWithdrawalFee(double withdrawalFee) {
        this.withdrawalFee = withdrawalFee;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    @Override
    public String toString() {
        return "ClassPojo [creditcardLimit = " + creditcardLimit + ", balance = " + balance + ", openingDate = " + openingDate + ", creditLimit = " + creditLimit + ", number = " + number + ", interestRate = " + interestRate + ", withdrawalFee = " + withdrawalFee + ", iban = " + iban + "]";
    }

    public static String getCreditcardLimitSlot() {
        return creditcardLimitSlot;
    }

    public static String getBalanceSlot() {
        return balanceSlot;
    }

    public static String getOpeningDateSlot() {
        return openingDateSlot;
    }

    public static String getCreditLimitSlot() {
        return creditLimitSlot;
    }

    public static String getNumberSlot() {
        return numberSlot;
    }

    public static String getInterestRateSlot() {
        return interestRateSlot;
    }

    public static String getWithdrawalFeeSlot() {
        return withdrawalFeeSlot;
    }

    public static String getIbanSlot() {
        return ibanSlot;
    }

    public String getCreditcardLimitText() {
        return creditcardLimitText;
    }

    public String getBalanceText() {
        return balanceText;
    }

    public String getOpeningDateText() {
        return openingDateText;
    }

    public String getCreditLimitText() {
        return creditLimitText;
    }

    public String getNumberText() {
        return numberText;
    }

    public String getInterestRateText() {
        return interestRateText;
    }

    public String getWithdrawalFeeText() {
        return withdrawalFeeText;
    }

    public String getIbanText() {
        return ibanText;
    }

    public Map<String, String> getSpeechTexts() {
        return speechTexts;
    }
}