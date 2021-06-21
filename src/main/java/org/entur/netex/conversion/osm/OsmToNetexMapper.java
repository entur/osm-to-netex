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

import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Tag;
import org.openstreetmap.osm.Way;
import org.rutebanken.netex.model.AuthorityRefStructure;
import org.rutebanken.netex.model.FareZone;
import org.rutebanken.netex.model.FareZoneRefStructure;
import org.rutebanken.netex.model.FareZoneRefs_RelStructure;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.OrganisationRefStructure;
import org.rutebanken.netex.model.PointRefStructure;
import org.rutebanken.netex.model.PointRefs_RelStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ScopingMethodEnumeration;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.ValidBetween;
import org.rutebanken.netex.model.ZoneTopologyEnumeration;
import org.rutebanken.netex.model.Zone_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Map osm Ways to Netex Zones
 *
 * @param <T>
 */
public class OsmToNetexMapper<T extends Zone_VersionStructure> {

    /**
     * The prefix (usually three letters) in IDs.
     */
    public static final String CODESPACE = "codespace";
    /**
     * The objects name that will be retrieved from a tag with the same value.
     */
    public static final String NAME = "name";
    /**
     * Reference, which is the postfix of the generated Netex ID
     */
    public static final String REFERENCE = "reference";  //privatecode
    public static final String ZONE_TYPE = "zone_type";  //scoping method
    public static final String DEFAULT_VERSION = "1";
    public static final String VALID_FROM = "valid_from";
    public static final String FAREZONEID = "id";
    public static final String AUTHORITYREF = "authorityRef";
    public static final String MEMBERS = "members";
    public static final String NEIGHBOURS = "neighbours";
    public static final String PRIVATECODE = "privateCode";
    public static final String SCOPINGMETHOD = "scopingMethod";
    public static final String TARIFFZONETYPE ="tariffZone";
    public static final String ZONETOPOLOGY ="zoneTopology";
    public static final String TZMAPPING ="tzMapping";


    /*
    <tag k='area' v='tariffZone' />
    <tag k='authorityRef' v='VOT:Authority:VTFK_ID' />**
    <tag k='codespace' v='VOT' />  **
    <tag k='id' v='VOT:FareZone:19' /> **
    <tag k='members' v='NSR:StopPlace:16845;NSR:StopPlace:16848' /> **
    <tag k='name:nor' v='Kongsberg' /> **
    <tag k='neighbours' v='VOT:FareZone:17;VOT:FareZone:14' /> **
    <tag k='privateCode' v='630' /> **
    <tag k='scopingMethod' v='explicit' />**
    <tag k='tariffZone' v='fareZone' />
    <tag k='valid_from' v='2021-02-01' />**
    <tag k='zoneTopology' v='tiled' />**
     */

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);
    private static final net.opengis.gml._3.ObjectFactory openGisObjectFactory = new net.opengis.gml._3.ObjectFactory();
    private final NetexHelper netexHelper;

    public OsmToNetexMapper(NetexHelper netexHelper) {
        this.netexHelper = netexHelper;
    }

    public List<T> mapWaysToZoneList(List<Way> ways, Map<BigInteger, Node> mapOfNodes, Class<T> clazz) {
        List<T> zones = ways
                .stream()
                .map(way -> mapWayToZone(way, mapOfNodes, clazz))
                .collect(Collectors.toList());
        logger.info("Mapped {} zones of type {} from osm to netex", zones.size(), clazz);
        return zones;
    }

    public T mapWayToZone(Way way, Map<BigInteger, Node> mapOfNodes, Class<T> clazz) {

        T zone = netexHelper.createNetexObject(clazz);

        zone.setVersion(DEFAULT_VERSION);
    
        if (clazz.getSimpleName().equals("FareZone")){
            mapFareZoneTags(way.getTag(), (FareZone) zone);
            
        } else {
            mapTags(way.getTag(), zone, clazz.getSimpleName());
        }

        zone.setPolygon(mapNodes(way, mapOfNodes));

        return zone;
    }

    private void mapFareZoneTags(List<Tag> tags, FareZone zone) {
         /*
    <tag k='area' v='tariffZone' />
    <tag k='authorityRef' v='VOT:Authority:VTFK_ID' />**
    <tag k='codespace' v='VOT' />  **
    <tag k='id' v='VOT:FareZone:19' /> **
    <tag k='members' v='NSR:StopPlace:16845;NSR:StopPlace:16848' /> **
    <tag k='name:nor' v='Kongsberg' /> **
    <tag k='neighbours' v='VOT:FareZone:17;VOT:FareZone:14' /> **
    <tag k='privateCode' v='630' /> **
    <tag k='scopingMethod' v='explicit' />**
    <tag k='tariffZone' v='fareZone' />
    <tag k='valid_from' v='2021-02-01' />**
    <tag k='zoneTopology' v='tiled' />**
     */
        String codespace = null;
        String fareZoneId = null;
        String privateCode = null;
        String tzMapping = null;


        for (Tag tag : tags) {

            if (tag.getK().equals(CODESPACE)) {
                codespace = tag.getV();
            } else if (tag.getK().startsWith(NAME)) {
                String keyName = tag.getK();
                String lang = extractLangFromNameTagKey(keyName);
                zone.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
            } else if (tag.getK().startsWith(AUTHORITYREF)) {
                String authorityRef = tag.getV();
                tagValueNotNull(AUTHORITYREF,authorityRef);
                zone.withTransportOrganisationRef(new ObjectFactory().createAuthorityRef(new AuthorityRefStructure().withRef(authorityRef)));
            } else if (tag.getK().startsWith(PRIVATECODE)) {
                privateCode = tag.getV();
            } else if (tag.getK().startsWith(ZONETOPOLOGY)) {
                String value = tag.getV();
                tagValueNotNull(ZONETOPOLOGY, value);
                zone.withZoneTopology(ZoneTopologyEnumeration.fromValue(value));
            } else if (tag.getK().startsWith(SCOPINGMETHOD)) {
                final String value = tag.getV();
                tagValueNotNull(SCOPINGMETHOD, value);
                final ScopingMethodEnumeration scopingMethodEnumeration = ScopingMethodEnumeration.fromValue(value);
                zone.withScopingMethod(scopingMethodEnumeration);

            } else if (tag.getK().startsWith(MEMBERS)) {
                final String value = tag.getV();
                tagValueNotNull(MEMBERS, value);
                final String[] stopPlaces = value.split(";");
                List<JAXBElement<? extends PointRefStructure>> stopPoints = Arrays.stream(stopPlaces)
                        .map(stopPlace -> new ObjectFactory().createScheduledStopPointRef(new ScheduledStopPointRefStructure().withRef(stopPlace)))
                        .collect(Collectors.toList());
                if (!stopPoints.isEmpty()) {
                    zone.withMembers(new PointRefs_RelStructure().withPointRef(stopPoints));
                }
            } else if (tag.getK().startsWith(NEIGHBOURS)) {
                final String value = tag.getV();
                tagValueNotNull(NEIGHBOURS, value);
                final String[] neighbours = value.split(";");
                final List<FareZoneRefStructure> fareZoneRefs = Arrays.stream(neighbours)
                        .map(farezone -> new FareZoneRefStructure().withRef(farezone))
                        .collect(Collectors.toList());

                if (!fareZoneRefs.isEmpty()) {
                    zone.withNeighbours(new FareZoneRefs_RelStructure().withFareZoneRef(fareZoneRefs));
                }

            } else if (tag.getK().equals(VALID_FROM)) {
                String validFrom = tag.getV();
                tagValueNotNull(VALID_FROM, validFrom);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Instant instant = sdf.parse(validFrom).toInstant();
                    final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    zone.withValidBetween(new ValidBetween().withFromDate(localDateTime));
                } catch (ParseException e) {
                    logger.info("Unable to parse and set valid from date: {}", e.getMessage());
                }

            } else if (tag.getK().startsWith(FAREZONEID)) {
                fareZoneId =tag.getV();
            } else if (tag.getK().startsWith(TZMAPPING)) {
                tzMapping = tag.getV();
            }
        }

        tagValueNotNull(CODESPACE, codespace);
        tagValueNotNull(FAREZONEID,fareZoneId);
        tagValueNotNull(PRIVATECODE,privateCode);
        zone.withPrivateCode(new PrivateCodeStructure().withValue(privateCode));
        generateOrSetTzMapping(zone,tzMapping,privateCode,codespace);
        zone.setId(fareZoneId);
    }

    private void generateOrSetTzMapping(FareZone zone, String tariffZoneRef, String privateCode, String codespace) {
        if (tariffZoneRef == null) {
            tariffZoneRef = codespace + ":" + TariffZone.class.getSimpleName() + ":" + privateCode;
        }
        KeyValueStructure keyValueStructure = new KeyValueStructure().withKey(TZMAPPING).withValue(tariffZoneRef);
        KeyListStructure keyListStructure = new KeyListStructure().withKeyValue(keyValueStructure);
        zone.setKeyList(keyListStructure);

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
            } else if (tag.getK().startsWith(ZONE_TYPE)) {
                String keyName = tag.getK();
                String value = tag.getV();
                tagValueNotNull(ZONE_TYPE,value);

                KeyValueStructure keyValueStructure = new KeyValueStructure().withKey(keyName).withValue(value);
                KeyListStructure keyListStructure = new KeyListStructure().withKeyValue(keyValueStructure);
                zone.setKeyList(keyListStructure);
            } else if (tag.getK().equals(VALID_FROM)) {
                String validFrom = tag.getV();
                tagValueNotNull(VALID_FROM,validFrom);

                SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Instant instant = sdf.parse(validFrom).toInstant();
                    final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    zone.withValidBetween(new ValidBetween().withFromDate(localDateTime));
                } catch (ParseException e) {
                    logger.info("Unable to parse and set valid from date: {}",e.getMessage());
                }

            }
        }

        tagValueNotNull(CODESPACE, codespace);
        tagValueNotNull(REFERENCE, reference);


        zone.setId(generateId(codespace, className, reference));
    }

    private String generateId(String codespace, String className, String reference) {
        return codespace + ":" + className + ":" + reference;
    }

    private String extractLangFromNameTagKey(String osmTagName) {
        return osmTagName.substring(osmTagName.lastIndexOf(':') + 1);
    }

    private void tagValueNotNull(String name, String value) {

        if (value == null) {
            throw new IllegalArgumentException("Cannot map '" + name + "' from tag. Value not present. Seems like there are no tags with name " + name);
        }
    }

}
