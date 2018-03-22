package org.entur.netex.conversion.osm;

import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Osm;
import org.openstreetmap.osm.Tag;
import org.rutebanken.netex.model.*;

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
import java.util.Map;
import java.util.stream.Collectors;

public class Converter {


    public static final String CODESPACE = "codespace";
    public static final String NAME = "name";

    /**
     * Reference, which is the postfix of the generated Netex ID
     */
    public static final String REFERENCE = "reference";
    public static final String DEFAULT_VERSION = "1";


    private final NetexHelper netexHelper;

    public Converter(NetexHelper netexHelper) {
        this.netexHelper = netexHelper;
    }

    public void transform(String file) throws JAXBException, FileNotFoundException {

        JAXBContext osmContext = JAXBContext.newInstance(Osm.class);

        Unmarshaller osmContextUnmarshaller = osmContext.createUnmarshaller();

        JAXBElement<Osm> osmJAXBElement = osmContextUnmarshaller.unmarshal(new StreamSource(new FileInputStream(new File(file))), Osm.class);
        Osm osm = osmJAXBElement.getValue();

        System.out.println(osm);

        Map<BigInteger, Node> nodeMap = osm.getNode().stream()
                .collect(Collectors.toMap(Node::getId
                        , node -> node));

        System.out.println("Read " + nodeMap.size() + "nodes");

        SiteFrame siteFrame = new SiteFrame();
        siteFrame.setVersion(DEFAULT_VERSION);
        siteFrame.setId("OSM:SiteFrame:1");

        final String className = "TariffZone";

        siteFrame.withTariffZones(new TariffZonesInFrame_RelStructure());

        osm.getWay().forEach(way -> {
            System.out.println("About to read way: " + way.getId());

            Zone_VersionStructure zoneVersionStructure = netexHelper.createNetexObject("TariffZone");

            zoneVersionStructure.setVersion(DEFAULT_VERSION);
            String codespace = null;
            String tariffZoneReference = null;

            for (Tag tag : way.getTag()) {

                if (tag.getK().equals(CODESPACE)) {
                    codespace = tag.getV();
                } else if (tag.getK().startsWith(NAME)) {
                    String keyName = tag.getK();
                    String lang = keyName.substring(keyName.lastIndexOf(':') + 1);
                    zoneVersionStructure.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
                } else if (tag.getK().startsWith(REFERENCE)) {
                    tariffZoneReference = tag.getV();
                }
            }

            tagValueNotNullOrExit(CODESPACE, codespace);
            tagValueNotNullOrExit(REFERENCE, tariffZoneReference);

            String id = codespace + ":" + className + ":" + tariffZoneReference;

            System.out.println("created id: " + id);
            zoneVersionStructure.setId(id);

            System.out.println(zoneVersionStructure);

            if (zoneVersionStructure instanceof TariffZone) {
                siteFrame.getTariffZones().getTariffZone().add((TariffZone) zoneVersionStructure);
            }

        });

        PublicationDeliveryStructure publicationDeliveryStructure = netexHelper.createPublicationDelivery(siteFrame);

        FileOutputStream fileOutputStream = new FileOutputStream("netex.xml");
        netexHelper.marshalNetex(publicationDeliveryStructure, fileOutputStream);

    }


    private void tagValueNotNullOrExit(String name, String value) {

        if (value == null) {
            throw new IllegalArgumentException("Cannot map " + name + " from tag.");
        }
    }


}
