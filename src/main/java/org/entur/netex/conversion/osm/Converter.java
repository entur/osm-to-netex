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
import java.io.*;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

public class Converter {


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
        siteFrame.withTariffZones(new TariffZonesInFrame_RelStructure());

        osm.getWay().forEach(way -> {
            System.out.println("About to read way: " + way.getId());

            Zone_VersionStructure zoneVersionStructure = netexHelper.createNetexObject("TariffZone");

            String tariffZoneCodeSpace = null;
            String tariffZoneReference = null;

            for (Tag tag : way.getTag()) {

                if (tag.getK().equals("tariffZoneCodespace")) {
                    tariffZoneCodeSpace = tag.getV();
                } else if (tag.getK().startsWith("tariffZoneName")) {
                    String keyName = tag.getK();
                    String lang = keyName.substring(keyName.lastIndexOf(':') + 1);
                    zoneVersionStructure.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
                } else if (tag.getK().startsWith("tariffZoneReference")) {
                    tariffZoneReference = tag.getV();
                }
            }

            tagValueNotNullOrExit("tariffZoneCodeSpace", tariffZoneCodeSpace);
            tagValueNotNullOrExit("tariffZoneReference", tariffZoneReference);

            String id = tariffZoneCodeSpace + ":TariffZone:" + tariffZoneReference;

            System.out.println("created id: " + id);

            System.out.println(zoneVersionStructure);

            if(zoneVersionStructure instanceof TariffZone) {

                siteFrame.getTariffZones().getTariffZone().add((TariffZone) zoneVersionStructure);
            }

        });

        PublicationDeliveryStructure publicationDeliveryStructure = netexHelper.createPublicationDelivery(siteFrame);

        FileOutputStream fileOutputStream = new FileOutputStream("netex.xml");
        netexHelper.marshalNetex(publicationDeliveryStructure, fileOutputStream);

    }


    private void tagValueNotNullOrExit(String name, String value) {

        if (value == null) {
            System.err.println(name + " from tag was null");
            System.exit(1);
        }
    }


}
