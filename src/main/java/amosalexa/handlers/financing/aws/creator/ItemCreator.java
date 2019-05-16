package amosalexa.handlers.financing.aws.creator;

import amosalexa.handlers.financing.aws.model.Item;
import amosalexa.handlers.financing.aws.util.AWSUtil;
import amosalexa.handlers.financing.aws.util.XMLParser;

import java.sql.Timestamp;
import java.util.ArrayList;

public class ItemCreator {

    private static String itemXML;

    public static ArrayList<Item> createItems(String xml) {

        ArrayList<String> xmlItems = XMLParser.readFeatureListValues(xml, new String[]{"Items", "Item"});
        ArrayList<Item> items = new ArrayList<>();

        for (String xmlItem : xmlItems) {
            if (!xmlItem.isEmpty())
                items.add(createItem(xmlItem));
        }

        return items;
    }

    public static Item createItem(String itemXML) {

        Item item = new Item();

        // ASIN
        String ASIN = XMLParser.readValue(itemXML, new String[]{"ASIN"});
        item.setASIN(ASIN);

        //Timestamp
        java.util.Date date = new java.util.Date();
        Timestamp timestamp = new Timestamp(date.getTime());
        item.setAdded(timestamp);

        // Locale
        item.setLocale("de");

        // ParentASIN
        String parentASIN = XMLParser.readValue(itemXML, new String[]{"ParentASIN"});
        if (parentASIN != null && parentASIN.length() == "B00KAKPZYG".length()) {
            item.setParentASIN(parentASIN);
        }

        // DetailPageURL
        String detailPageURL = XMLParser.readValue(itemXML, new String[]{"DetailPageURL"});
        item.setDetailPageURL(detailPageURL);


        // Itemlink - WishList
        String itemLinkWishList = XMLParser.readValue(itemXML, new String[]{"ItemLinks", "ItemLink", "URL"});
        item.setItemLinkWishList(itemLinkWishList);

        // ImageURL
        String imageURL = XMLParser.readValue(itemXML, new String[]{"LargeImage", "URL"});
        item.setImageURL(imageURL);

        // Binding
        String binding = XMLParser.readValue(itemXML, new String[]{"ItemAttributes", "Binding"});
        if (binding.length() < 30) {
            item.setBinding(binding);
        }

        // Brand
        String brand = XMLParser.readValue(itemXML, new String[]{"ItemAttributes", "Brand"});
        if (brand.length() < 30) {
            item.setBrand(brand);
        }

        // EAN
        String ean = XMLParser.readValue(itemXML, new String[]{"ItemAttributes", "EAN"});
        if (ean.length() < 15) {
            item.setEan(ean);
        }

        // ProductGroup
        String productGroup = XMLParser.readValue(itemXML, new String[]{"ItemAttributes", "ProductGroup"});
        item.setProductGroup(productGroup);

        // ProductTypeName
        String productTypeName = XMLParser.readValue(itemXML, new String[]{"ItemAttributes", "ProductTypeName"});
        item.setProductTypeName(productTypeName);

        // Title
        String title = XMLParser.readValue(itemXML, new String[]{"ItemAttributes", "Title"});
        System.out.println("set title: " + title);
        item.setTitle(title);

        // Lowest New Price

        // Title
        String lowestNewPrice = XMLParser.readValue(itemXML, new String[]{"OfferSummary", "LowestNewPrice", "Amount"});
        if (AWSUtil.isNumeric(lowestNewPrice)) {
            item.setLowestNewPrice(Integer.parseInt(lowestNewPrice));
        }

        return item;
    }
}
