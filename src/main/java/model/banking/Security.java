package model.banking;


import java.util.Date;

public class Security {

    private Number securityId;
    private String isin;
    private String wkn;
    private String description;
    private Number quantity;
    private Number costPrice;
    private Date purchasingDate;
    private SecurityType securityType;

    public Security() {
    }

    public Security(String isin, String wkn, String description, Number quantity, Number costPrice, Date purchasingDate,
                    SecurityType securityType) {
        this.isin = isin;
        this.wkn = wkn;
        this.description = description;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.purchasingDate = purchasingDate;
        this.securityType = securityType;
    }

    public Number getSecurityId() {
        return securityId;
    }

    public void setSecurityId(Number securityId) {
        this.securityId = securityId;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getWkn() {
        return wkn;
    }

    public void setWkn(String wkn) {
        this.wkn = wkn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Number getQuantity() {
        return quantity;
    }

    public void setQuantity(Number quantity) {
        this.quantity = quantity;
    }

    public Number getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Number costPrice) {
        this.costPrice = costPrice;
    }

    public Date getPurchasingDate() {
        return purchasingDate;
    }

    public void setPurchasingDate(Date purchasingDate) {
        this.purchasingDate = purchasingDate;
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

    public enum SecurityType {
        STOCK,
        BOND,
        FUND
    }
}
