package amosalexa.handlers.financing.aws.util;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

public class XMLParser {

    final static Logger log = Logger.getLogger(XMLParser.class);

    public static String readValue(String xml, String[] path) {

        for (String elementPath : path) {
            String elementPathStart = "<" + elementPath + ">";
            String elementPathEnd = "</" + elementPath + ">";

            try {
                xml = xml.substring(xml.indexOf(elementPathStart) + elementPathStart.length(), xml.indexOf(elementPathEnd));
            } catch (NullPointerException | StringIndexOutOfBoundsException e) {
                return "";
            }
        }

        return xml;
    }


    // feature list
    public static ArrayList<String> readFeatureListValues(String xmlList, String[] path) {

        ArrayList<String> list = new ArrayList<String>();

        String elements = readValue(xmlList, new String[]{path[0]});

        try {
            while (elements.contains(path[1])) {
                String value = readValue(elements, new String[]{path[1]});
                list.add(value);
                String endTag = "</" + path[1] + ">";
                elements = elements.substring(elements.indexOf(endTag) + endTag.length());

            }
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            log.error(e.getMessage());
        }

        return list;
    }

    public static String getPrettyXML(String xmlString) {

        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes("utf-8"))));

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                    document,
                    XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                node.getParentNode().removeChild(node);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);

            transformer.transform(new DOMSource(document), streamResult);

            return stringWriter.toString();

        } catch (ParserConfigurationException |
                XPathExpressionException | TransformerException
                | IOException | SAXException e) {
            log.error(e.getMessage());
        }

        return null;
    }


    public static String xmlToString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

}
