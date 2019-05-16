package amosalexa.handlers.financing.aws.model;

import java.sql.Timestamp;

public class Item {

    private String ASIN;

    private String parentASIN;

    private String locale;

    private String detailPageURL;

    private String itemLinkWishList;

    private String imageURL;

    private String binding;

    private String brand;

    private String ean;

    private String productGroup;

    private String productTypeName;

    private Timestamp added;

    private String title;

    private String titleShort;

    private Integer lowestNewPrice;

    public Item() {
        this.locale = "de";
    }

    public String getASIN() {
        return ASIN;
    }

    public void setASIN(String ASIN) {
        this.ASIN = ASIN;
    }

    public String getParentASIN() {
        return parentASIN;
    }

    public void setParentASIN(String parentASIN) {
        this.parentASIN = parentASIN;
    }

    public String getDetailPageURL() {
        return detailPageURL;
    }

    public void setDetailPageURL(String detailPageURL) {
        this.detailPageURL = detailPageURL;
    }

    public String getItemLinkWishList() {
        return itemLinkWishList;
    }

    public void setItemLinkWishList(String itemLinkWishList) {
        this.itemLinkWishList = itemLinkWishList;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public void setProductGroup(String productGroup) {
        this.productGroup = productGroup;
    }

    public String getProductTypeName() {
        return productTypeName;
    }

    public void setProductTypeName(String productTypeName) {
        this.productTypeName = productTypeName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Timestamp getAdded() {
        return added;
    }

    public void setAdded(Timestamp added) {
        this.added = added;
    }


    public String getLocale() {
        return locale;
    }

    public void setLocale(String id) {
        this.locale = locale;
    }

    public String getTitleShort() {
        return titleShort;
    }

    public Item setTitleShort(String titleShort) {
        this.titleShort = titleShort;
        return this;
    }

    public Integer getLowestNewPrice() {
        return lowestNewPrice;
    }

    public Item setLowestNewPrice(Integer lowestNewPrice) {
        this.lowestNewPrice = lowestNewPrice;
        return this;
    }
}
