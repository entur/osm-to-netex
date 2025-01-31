/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.entur.netex.conversion.osm.transformer;

import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Osm;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsmToNetexTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);

    private final NetexHelper netexHelper;

    public OsmToNetexTransformer() {
        ObjectFactory netexObjectFactory = new ObjectFactory();
        this.netexHelper = new NetexHelper(netexObjectFactory);
    }

    public void marshallOsm(InputSource osmInput, OutputStream output, String targetEntity, String generatedFrom, String participantRef) throws ClassNotFoundException {
        try {
            OsmUnmarshaller osmUnmarshaller = new OsmUnmarshaller(false);

            Osm osm = osmUnmarshaller.unmarshall(osmInput);

            PublicationDeliveryStructure publicationDeliveryStructure = map(osm, targetEntity, generatedFrom, participantRef);
            netexHelper.marshalNetex(publicationDeliveryStructure, output);

            logger.info("Unmarshalled OSM file. generator: {}, version: {}, nodes: {}, ways: {}, relations: {}",
                    osm.getGenerator(), osm.getVersion(), osm.getNode().size(), osm.getWay().size(), osm.getRelation().size());
        } catch (JAXBException | SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PublicationDeliveryStructure map(Osm osm, String targetEntity, String generatedFrom, String participantRef) throws ClassNotFoundException {

        /*
         * 1. Parse relations to collect ways first
         * 2. Parse ways to collect nodes first
         * 3. Parse nodes and ways
         */

        Class<? extends Zone_VersionStructure> clazz = validateAndGetDestinationClass(targetEntity);

        SiteFrame siteFrame;

        Map<BigInteger, Node> mapOfNodes = osm.getNode().stream()
                .collect(Collectors.toMap(Node::getId, node -> node));
        logger.info("Mapped {} nodes from osm file", mapOfNodes.size());

        if (clazz.isAssignableFrom(TariffZone.class)) {
            siteFrame = generateSiteFrameFromTariffZone(osm, mapOfNodes);
        } else if (clazz.isAssignableFrom(FareZone.class)) {
            siteFrame = generateSiteFrameFromFareZone(osm, mapOfNodes);
        } else if (clazz.isAssignableFrom(TopographicPlace.class)) {
            siteFrame = generateSiteFrameFromTopographicPlace(osm, mapOfNodes);
        } else {
            throw new IllegalArgumentException(clazz + " is not supported");
        }
        return netexHelper.createPublicationDelivery(siteFrame, generatedFrom, participantRef);
    }


    private SiteFrame generateSiteFrameFromTariffZone(Osm osm, Map<BigInteger, Node> mapOfNodes) {
        SiteFrame siteFrame = netexHelper.createSiteFrame();
        OsmToNetexMapper<TariffZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
        TariffZonesInFrame_RelStructure tariffZonesInFrame = new TariffZonesInFrame_RelStructure();
        osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TariffZone.class)
                .map(Map.Entry::getValue)
                .map(tariffZone -> new ObjectFactory().createTariffZone(tariffZone))
                .forEach(tariffZonesInFrame.getTariffZone()::add);

        siteFrame.withTariffZones(tariffZonesInFrame);
        return siteFrame;
    }

    private SiteFrame generateSiteFrameFromFareZone(Osm osm, Map<BigInteger, Node> mapOfNodes) {
        SiteFrame siteFrame = netexHelper.createSiteFrame();
        OsmToNetexMapper<FareZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
        final TariffZonesInFrame_RelStructure tariffZonesInFrame = new TariffZonesInFrame_RelStructure();

        if (osm.getRelation() != null && !osm.getRelation().isEmpty()) {
            GroupsOfTariffZonesInFrame_RelStructure groupOfTariffZones = new GroupsOfTariffZonesInFrame_RelStructure();

            final Map<BigInteger, String> fareZoneMaps = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, FareZone.class)
                    .peek(fareZone -> {
                        JAXBElement<FareZone> fz = new ObjectFactory().createFareZone(fareZone.getValue());
                        tariffZonesInFrame.getTariffZone().add(fz);
                    }).collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()));

            osmToNetexMapper.mapRelationsToGroupOfTariffZones(osm.getRelation(), fareZoneMaps).forEach(g -> groupOfTariffZones.getGroupOfTariffZones().add(g));
            siteFrame.withGroupsOfTariffZones(groupOfTariffZones);

        } else {
            osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, FareZone.class)
                    .map(Map.Entry::getValue)
                    .map(fareZone -> new ObjectFactory().createFareZone(fareZone))
                    .forEach(fz -> tariffZonesInFrame.getTariffZone().add(fz));
        }

        siteFrame.withTariffZones(tariffZonesInFrame);
        return siteFrame;
    }

    private SiteFrame generateSiteFrameFromTopographicPlace(Osm osm, Map<BigInteger, Node> mapOfNodes) {
        SiteFrame siteFrame = netexHelper.createSiteFrame();
        OsmToNetexMapper<TopographicPlace> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);

        final TopographicPlacesInFrame_RelStructure topographicPlacesInFrame = new TopographicPlacesInFrame_RelStructure();
        osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TopographicPlace.class).
                map(Map.Entry::getValue)
                .forEach(tp -> {
                    tp.setDescriptor(new TopographicPlaceDescriptor_VersionedChildStructure().withName(tp.getName()));
                    topographicPlacesInFrame.getTopographicPlace().add(tp);
                });
        siteFrame.withTopographicPlaces(topographicPlacesInFrame);
        return siteFrame;
    }


    @SuppressWarnings("unchecked")
    private Class<? extends Zone_VersionStructure> validateAndGetDestinationClass(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(StopPlace.class.getPackage().getName() + "." + className);
        if (!Zone_VersionStructure.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("The class specified:" + className + ", is not a Zone !");
        }
        return (Class<? extends Zone_VersionStructure>) clazz;
    }


}
