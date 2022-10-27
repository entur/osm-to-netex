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

package org.entur.netex.conversion.osm;

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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OsmToNetexTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);


    private final NetexHelper netexHelper;
    private final String targetEntity;

    public OsmToNetexTransformer(NetexHelper netexHelper, String targetEntity) {
        this.netexHelper = netexHelper;
        this.targetEntity = targetEntity;
    }

    public void transform(String osmInputFile, String netexOutputFile) throws JAXBException, IOException, ClassNotFoundException, SAXException, ParserConfigurationException {

        OsmUnmarshaller osmUnmarshaller = new OsmUnmarshaller(false);
        InputSource osmInputSource = new InputSource(osmInputFile);

        Osm osm = osmUnmarshaller.unmarshall(osmInputSource);

        logger.info("Unmarshalled OSM file. generator: {}, version: {}, nodes: {}, ways: {}, relations: {}",
                osm.getGenerator(), osm.getVersion(), osm.getNode().size(), osm.getWay().size(), osm.getRelation().size());

        SiteFrame siteFrame = netexHelper.createSiteFrame();

        Class<? extends Zone_VersionStructure> clazz = validateAndGetDestinationClass(targetEntity);

        map(osm, siteFrame, clazz);

        PublicationDeliveryStructure publicationDeliveryStructure = netexHelper.createPublicationDelivery(siteFrame, osmInputFile);

        FileOutputStream fileOutputStream = new FileOutputStream("target/"+netexOutputFile);
        netexHelper.marshalNetex(publicationDeliveryStructure, fileOutputStream);

        logger.info("Done. Check the result in the file {}", netexOutputFile);
    }


    public void map(Osm osm, SiteFrame siteFrame, Class<? extends Zone_VersionStructure> clazz) {

        /*
         * 1. Parse relations to collect ways first
         * 2. Parse ways to collect nodes first
         * 3. Parse nodes and ways
         */

        Map<BigInteger, Node> mapOfNodes = osm.getNode().stream()
                .collect(Collectors.toMap(Node::getId
                        , node -> node));
        logger.info("Mapped {} nodes from osm file", mapOfNodes.size());

        if (clazz.isAssignableFrom(TariffZone.class)) {
            OsmToNetexMapper<TariffZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);

            final List<Map<BigInteger, TariffZone>> maps = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TariffZone.class);
            List<TariffZone> tariffZones= maps.stream().flatMap(map -> map.values().stream()).collect(Collectors.toList());

            List<JAXBElement<? extends Zone_VersionStructure>> tariffZones1 = tariffZones.stream()
                    .map(tariffZone -> new ObjectFactory().createTariffZone(tariffZone)).collect(Collectors.toList());
            siteFrame.withTariffZones(
                    new TariffZonesInFrame_RelStructure()
                            .withTariffZone(tariffZones1));
        } else if (clazz.isAssignableFrom(FareZone.class)) {
            OsmToNetexMapper<FareZone> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
            final List<Map<BigInteger,FareZone>> fareZoneMaps = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, FareZone.class);
            List<FareZone> fareZones = fareZoneMaps.stream().flatMap(map -> map.values().stream()).collect(Collectors.toList());
            List<JAXBElement<? extends Zone_VersionStructure>> fareZones1 = fareZones.stream()
                    .map(fareZone -> new ObjectFactory().createFareZone(fareZone)).collect(Collectors.toList());

            siteFrame.withTariffZones(new TariffZonesInFrame_RelStructure().withTariffZone(fareZones1));

            // Group of TariffZones

            if(osm.getRelation() != null && !osm.getRelation().isEmpty())  {

                List<GroupOfTariffZones> groupOfTariffZones =osmToNetexMapper.mapRelationsToGroupOfTariffZones(osm.getRelation(),fareZoneMaps);
                siteFrame.withGroupsOfTariffZones(new GroupsOfTariffZonesInFrame_RelStructure().withGroupOfTariffZones(groupOfTariffZones));
            }
        }
        else if (clazz.isAssignableFrom(TopographicPlace.class)) {
            OsmToNetexMapper<TopographicPlace> osmToNetexMapper = new OsmToNetexMapper<>(netexHelper);
            List<Map<BigInteger,TopographicPlace>> topographicPlacesMap = osmToNetexMapper.mapWaysToZoneList(osm.getWay(), mapOfNodes, TopographicPlace.class);
            List<TopographicPlace> topographicPlaces = topographicPlacesMap.stream().flatMap(tp -> tp.values().stream()).collect(Collectors.toList());
            topographicPlaces.forEach(tp -> tp.setDescriptor(new TopographicPlaceDescriptor_VersionedChildStructure().withName(tp.getName())));
            siteFrame.withTopographicPlaces(
                    new TopographicPlacesInFrame_RelStructure()
                            .withTopographicPlace(topographicPlaces));
        } else {
            throw new IllegalArgumentException(clazz + " is not supported");
        }
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
