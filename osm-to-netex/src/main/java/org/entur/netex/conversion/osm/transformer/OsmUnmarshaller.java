package org.entur.netex.conversion.osm.transformer;

import org.openstreetmap.osm.Osm;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.UnmarshallerHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

/**
 * Unmarshaller that makes it possible to read OSM XML files without namespace.
 */
public class OsmUnmarshaller {

    private static final String OSM_NAMESPACE = "http://openstreetmap.org/osm/0.6";

    private final XMLFilter namespaceFilter;

    private final UnmarshallerHandler unmarshallerHandler;


    /**
     * Creates a new Marshaller to read OSM XML data into Java objects
     *
     * @param performValidation Indicates if marshall shall validate the OSM source
     */
    public OsmUnmarshaller(boolean performValidation) {
        try {
            JAXBContext osmContext = null;

            osmContext = JAXBContext.newInstance(Osm.class);

            namespaceFilter = new NamespaceFilter(OSM_NAMESPACE);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            namespaceFilter.setParent(xmlReader);
            Unmarshaller osmContextUnmarshaller = osmContext.createUnmarshaller();

            if (performValidation) {
                OsmSchemaValidator osmSchemaValidator = new OsmSchemaValidator();
                osmContextUnmarshaller.setSchema(osmSchemaValidator.getSchema());
            }
            unmarshallerHandler = osmContextUnmarshaller.getUnmarshallerHandler();
            namespaceFilter.setContentHandler(unmarshallerHandler);
        } catch (JAXBException | ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to create OSM UnMarshaller", e);
        }
    }


    /**
     * Unmarshalls an OSM XML into a Java Object
     *
     * @param source InputSource containing the XML
     * @return parsed OSM Object
     * @throws IOException if unable to read data from InputSource
     */
    public Osm unmarshall(InputSource source) throws IOException{
        try {
            namespaceFilter.parse(source);
            return (Osm) unmarshallerHandler.getResult();
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("Failed parsing XML", e);
        }
    }

}
