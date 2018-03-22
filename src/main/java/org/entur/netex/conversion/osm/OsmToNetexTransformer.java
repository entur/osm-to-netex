package org.entur.netex.conversion.osm;

import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Osm;
import org.openstreetmap.osm.Tag;
import org.openstreetmap.osm.Way;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsmToNetexTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);


    public static final String CODESPACE = "codespace";
    public static final String NAME = "name";


    /**
     * Reference, which is the postfix of the generated Netex ID
     */
    public static final String REFERENCE = "reference";
    public static final String DEFAULT_VERSION = "1";

    private static final AtomicLong idCounter = new AtomicLong();
    private static final net.opengis.gml._3.ObjectFactory openGisObjectFactory = new net.opengis.gml._3.ObjectFactory();

    private final NetexHelper netexHelper;

    public OsmToNetexTransformer(NetexHelper netexHelper) {
        this.netexHelper = netexHelper;
    }

    public void transform(String file) throws JAXBException, FileNotFoundException {

        JAXBContext osmContext = JAXBContext.newInstance(Osm.class);

        Unmarshaller osmContextUnmarshaller = osmContext.createUnmarshaller();

        JAXBElement<Osm> osmJAXBElement = osmContextUnmarshaller.unmarshal(new StreamSource(new FileInputStream(new File(file))), Osm.class);
        Osm osm = osmJAXBElement.getValue();

        SiteFrame siteFrame = netexHelper.createSiteFrame();

        final String className = "TariffZone";

        map(osm, siteFrame, className);

        PublicationDeliveryStructure publicationDeliveryStructure = netexHelper.createPublicationDelivery(siteFrame);

        FileOutputStream fileOutputStream = new FileOutputStream("netex.xml");
        netexHelper.marshalNetex(publicationDeliveryStructure, fileOutputStream);
    }


    public void map(Osm osm, SiteFrame siteFrame, String className) {

        Map<BigInteger, Node> mapOfNodes = osm.getNode().stream()
                .collect(Collectors.toMap(Node::getId
                        , node -> node));

        logger.info("Found {} nodes from osm file", mapOfNodes.size());

        if (className.equals(TariffZone.class.getSimpleName())) {
            List<TariffZone> zones = osm.getWay()
                    .stream()
                    .map(way -> mapWayToZone(way, mapOfNodes, TariffZone.class))
                    .collect(Collectors.toList());
            logger.info("Mapped {} zones from osm to netex", zones.size());
            siteFrame.withTariffZones(new TariffZonesInFrame_RelStructure()
                    .withTariffZone(zones));
        }
    }


    public <ZONE extends Zone_VersionStructure> ZONE mapWayToZone(Way way, Map<BigInteger, Node> mapOfNodes, Class<ZONE> clazz) {

        ZONE zone = netexHelper.createNetexObject(clazz);

        zone.setVersion(DEFAULT_VERSION);

        mapTags(way.getTag(), zone, clazz.getSimpleName());

        zone.setPolygon(mapNodes(way, mapOfNodes));

        return zone;
    }

    private PolygonType mapNodes(Way way, Map<BigInteger, Node> mapOfNodes) {

        List<Double> coordinates = way.getNd().stream()
                .map(nd -> mapOfNodes.get(nd.getRef()))
                .flatMap(node -> Stream.of(node.getLat(), node.getLon()))
                .collect(Collectors.toList());

        AbstractRingPropertyType abstractRingPropertyType = new AbstractRingPropertyType()
                .withAbstractRing(openGisObjectFactory.createLinearRing(
                        new LinearRingType()
                                .withPosList(
                                        new DirectPositionListType().withValue(coordinates))));

        return new PolygonType()
                .withId("GEN-PolygonType" + way.getId())
                .withExterior(abstractRingPropertyType);
    }

    private void mapTags(List<Tag> tags, Zone_VersionStructure zone, String className) {
        String codespace = null;
        String reference = null;

        for (Tag tag : tags) {

            if (tag.getK().equals(CODESPACE)) {
                codespace = tag.getV();
            } else if (tag.getK().startsWith(NAME)) {
                String keyName = tag.getK();
                String lang = extractLangFromNameTagKey(keyName);
                zone.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
            } else if (tag.getK().startsWith(REFERENCE)) {
                reference = tag.getV();
            }
        }

        tagValueNotNullOrExit(CODESPACE, codespace);
        tagValueNotNullOrExit(REFERENCE, reference);


        zone.setId(generateId(codespace, className, reference));
    }

    private String generateId(String codespace, String className, String reference) {
        return codespace + ":" + className + ":" + reference;
    }

    private String extractLangFromNameTagKey(String osmTagName) {
        return osmTagName.substring(osmTagName.lastIndexOf(':') + 1);
    }

    private void tagValueNotNullOrExit(String name, String value) {

        if (value == null) {
            throw new IllegalArgumentException("Cannot map " + name + " from tag.");
        }
    }


}
