package model.banking;


public class SecuritiesAccount {

    private Number securitiesAccountId;

    /**
     * IBAN of the clearing account. Must be known to the system.
     */
    private String clearingAccount;
    private String openingDate;

    public Number getSecuritiesAccountId() {
        return securitiesAccountId;
    }

    public void setSecuritiesAccountId(Number securitiesAccountId) {
        this.securitiesAccountId = securitiesAccountId;
    }

    public String getClearingAccount() {
        return clearingAccount;
    }

    public void setClearingAccount(String clearingAccount) {
        this.clearingAccount = clearingAccount;
    }

    public String getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(String openingDate) {
        this.openingDate = openingDate;
    }

}
