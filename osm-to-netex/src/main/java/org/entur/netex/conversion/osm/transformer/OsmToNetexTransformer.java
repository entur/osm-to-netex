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
import org.rutebanken.netex.model.FareZone;
import org.rutebanken.netex.model.GroupOfTariffZones;
import org.rutebanken.netex.model.GroupsOfTariffZonesInFrame_RelStructure;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.TariffZonesInFrame_RelStructure;
import org.rutebanken.netex.model.TopographicPlace;
import org.rutebanken.netex.model.TopographicPlaceDescriptor_VersionedChildStructure;
import org.rutebanken.netex.model.TopographicPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.Zone_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsmToNetexTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);

    private final NetexHelper netexHelper;

    public OsmToNetexTransformer(NetexHelper netexHelper) {
        this.netexHelper = netexHelper;
    }

    public PublicationDeliveryStructure map(Osm osm, String targetEntity) throws ClassNotFoundException {

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
        return netexHelper.createPublicationDelivery(siteFrame, "Hello");
    }

    private SiteFrame generateSiteFrameFromTariffZone(Osm osm, Map<BigInteger, Node> mapOfNodes) {
        SiteFrame siteFrame = netexHelper.createSiteFrame();
        OsmToNetexMapper<TariffZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);

        final Stream<Map<BigInteger, TariffZone>> maps = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TariffZone.class);

            /*Stream<TariffZone> tariffZones = osm.getWay().parallelStream().map(way -> {
                TariffZone zone = netexHelper.createNetexObject(TariffZone.class);
                zone.setVersion(DEFAULT_VERSION);

                osmToNetexMapper.mapTags(way.getTag(), zone, clazz.getSimpleName());

                zone.setPolygon(osmToNetexMapper.mapNodes(way, mapOfNodes));
                return zone;
            });*/

        Stream<TariffZone> tariffZones = maps.flatMap(map -> map.values().stream());

        List<JAXBElement<? extends Zone_VersionStructure>> tariffZones1 = tariffZones
                .map(tariffZone -> new ObjectFactory().createTariffZone(tariffZone)).collect(Collectors.toList());
        siteFrame.withTariffZones(
                new TariffZonesInFrame_RelStructure()
                        .withTariffZone(tariffZones1));
        return siteFrame;
    }

    private SiteFrame generateSiteFrameFromFareZone(Osm osm, Map<BigInteger, Node> mapOfNodes) {
        SiteFrame siteFrame = netexHelper.createSiteFrame();
        OsmToNetexMapper<FareZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
        final List<Map<BigInteger, FareZone>> fareZoneMaps = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, FareZone.class).toList();
        Stream<FareZone> fareZones = fareZoneMaps.stream().flatMap(map -> map.values().stream());
        List<JAXBElement<? extends Zone_VersionStructure>> fareZones1 = fareZones.map(fareZone -> new ObjectFactory().createFareZone(fareZone)).collect(Collectors.toList());

        siteFrame.withTariffZones(new TariffZonesInFrame_RelStructure().withTariffZone(fareZones1));

        // Group of TariffZones

        if(osm.getRelation() != null && !osm.getRelation().isEmpty())  {

            List<GroupOfTariffZones> groupOfTariffZones =osmToNetexMapper.mapRelationsToGroupOfTariffZones(osm.getRelation(),fareZoneMaps);
            siteFrame.withGroupsOfTariffZones(new GroupsOfTariffZonesInFrame_RelStructure().withGroupOfTariffZones(groupOfTariffZones));
        }
        return siteFrame;
    }

    private SiteFrame generateSiteFrameFromTopographicPlace(Osm osm, Map<BigInteger, Node> mapOfNodes) {
        SiteFrame siteFrame = netexHelper.createSiteFrame();
        OsmToNetexMapper<TopographicPlace> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
        Stream<Map<BigInteger, TopographicPlace>> topographicPlacesMap = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TopographicPlace.class);
        List<TopographicPlace> topographicPlaces = topographicPlacesMap.flatMap(tp -> tp.values().stream()).collect(Collectors.toList());
        topographicPlaces.forEach(tp -> tp.setDescriptor(new TopographicPlaceDescriptor_VersionedChildStructure().withName(tp.getName())));
        siteFrame.withTopographicPlaces(
                new TopographicPlacesInFrame_RelStructure()
                        .withTopographicPlace(topographicPlaces));
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
