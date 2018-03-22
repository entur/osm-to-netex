package org.entur.netex.conversion.osm;

import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Osm;
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
import java.util.stream.Collectors;

public class OsmToNetexTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);


    private final NetexHelper netexHelper;

    public OsmToNetexTransformer(NetexHelper netexHelper) {
        this.netexHelper = netexHelper;
    }

    public void transform(String file) throws JAXBException, FileNotFoundException, ClassNotFoundException {

        JAXBContext osmContext = JAXBContext.newInstance(Osm.class);

        Unmarshaller osmContextUnmarshaller = osmContext.createUnmarshaller();

        JAXBElement<Osm> osmJAXBElement = osmContextUnmarshaller.unmarshal(new StreamSource(new FileInputStream(new File(file))), Osm.class);
        Osm osm = osmJAXBElement.getValue();

        SiteFrame siteFrame = netexHelper.createSiteFrame();

        final String className = "TopographicPlace";


        Class<? extends Zone_VersionStructure> clazz = validateAndGetDestinationClass(className);

        map(osm, siteFrame, clazz);

        PublicationDeliveryStructure publicationDeliveryStructure = netexHelper.createPublicationDelivery(siteFrame);

        FileOutputStream fileOutputStream = new FileOutputStream("netex.xml");
        netexHelper.marshalNetex(publicationDeliveryStructure, fileOutputStream);
    }


    public void map(Osm osm, SiteFrame siteFrame, Class<? extends Zone_VersionStructure> clazz) {

        Map<BigInteger, Node> mapOfNodes = osm.getNode().stream()
                .collect(Collectors.toMap(Node::getId
                        , node -> node));
        logger.info("Found {} nodes from osm file", mapOfNodes.size());

        if (clazz.isAssignableFrom(TariffZone.class)) {
            OsmToNetexMapper<TariffZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
            List<TariffZone> tariffZones = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TariffZone.class);
            siteFrame.withTariffZones(
                    new TariffZonesInFrame_RelStructure()
                            .withTariffZone(tariffZones));
        } else if (clazz.isAssignableFrom(TopographicPlace.class)) {
            OsmToNetexMapper<TopographicPlace> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
            List<TopographicPlace> topographicPlaces = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TopographicPlace.class);
            topographicPlaces.forEach(tp -> tp.setDescriptor(new TopographicPlaceDescriptor_VersionedChildStructure().withName(tp.getName())));
            siteFrame.withTopographicPlaces(
                    new TopographicPlacesInFrame_RelStructure()
                            .withTopographicPlace(topographicPlaces));
        } else {
            throw new IllegalArgumentException(clazz + " is not supported");
        }
    }

    private Class<? extends Zone_VersionStructure> validateAndGetDestinationClass(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(StopPlace.class.getPackageName() + "." + className);
        if (!Zone_VersionStructure.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("The class specified:" + className + ", is not a Zone !");
        }
        return (Class<? extends Zone_VersionStructure>) clazz;
    }


}
