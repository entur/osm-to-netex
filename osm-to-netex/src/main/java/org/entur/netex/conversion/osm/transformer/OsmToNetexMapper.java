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

import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.openstreetmap.osm.Member;
import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Relation;
import org.openstreetmap.osm.Tag;
import org.openstreetmap.osm.Way;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBElement;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Map osm Ways to Netex Zones
 *
 * @param <T>
 */
class OsmToNetexMapper<T extends Zone_VersionStructure> {

    /**
     * The prefix (usually three letters) in IDs.
     */
    protected static final String CODESPACE = "codespace";
    /**
     * The objects name that will be retrieved from a tag with the same value.
     */
    protected static final String NAME = "name";
    /**
     * Reference, which is the postfix of the generated Netex ID
     */
    protected static final String REFERENCE = "reference";  //privatecode
    protected static final String ZONE_TYPE = "zone_type";  //scoping method
    protected static final String DEFAULT_VERSION = "1";
    protected static final String VALID_FROM = "valid_from";
    protected static final String VALID_TO = "valid_to";
    protected static final String FAREZONEID = "id";
    protected static final String AUTHORITYREF = "authorityRef";
    protected static final String MEMBERS = "members";
    protected static final String NEIGHBOURS = "neighbours";
    protected static final String PRIVATECODE = "privateCode";
    protected static final String SCOPINGMETHOD = "scopingMethod";
    protected static final String TARIFFZONETYPE = "tariffZone";
    protected static final String ZONETOPOLOGY = "zoneTopology";
    protected static final String TZMAPPING = "tzMapping";


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

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexMapper.class);
    private static final net.opengis.gml._3.ObjectFactory openGisObjectFactory = new net.opengis.gml._3.ObjectFactory();
    private final NetexHelper netexHelper;

    protected OsmToNetexMapper(NetexHelper netexHelper) {
        this.netexHelper = netexHelper;
    }

    protected Stream<Map.Entry<BigInteger, T>> mapWaysToZoneList(List<Way> ways, Map<BigInteger, Node> mapOfNodes, Class<T> clazz) {
        return ways.stream().map(way -> mapWayToZone(way, mapOfNodes, clazz));
    }

    protected Map.Entry<BigInteger, T> mapWayToZone(Way way, Map<BigInteger, Node> mapOfNodes, Class<T> clazz) {
        T zone = netexHelper.createNetexObject(clazz);

        zone.setVersion(DEFAULT_VERSION);

        if (clazz.getSimpleName().equals("FareZone")) {
            mapFareZoneTags(way.getTag(), (FareZone) zone);
        } else {
            mapTags(way.getTag(), zone, clazz.getSimpleName());
        }

        zone.setPolygon(mapNodes(way, mapOfNodes));

        return Map.entry(way.getId(), zone);
    }

    protected void mapFareZoneTags(List<Tag> tags, FareZone zone) {
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
        List<IllegalArgumentException> tagErrors = new ArrayList<>();
        String codespace = null;
        String fareZoneId = null;
        String privateCode = null;
        String tzMapping = null;
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;


        for (Tag tag : tags) {

            if (tag.getK().equals(CODESPACE)) {
                codespace = tag.getV();
            } else if (tag.getK().startsWith(NAME)) {
                String keyName = tag.getK();
                String lang = extractLangFromNameTagKey(keyName);
                zone.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
            } else if (tag.getK().startsWith(AUTHORITYREF)) {
                String authorityRef = tag.getV();
                tagErrorCollector(AUTHORITYREF, authorityRef, tagErrors);
                zone.withTransportOrganisationRef(new ObjectFactory().createAuthorityRef(new AuthorityRef().withRef(authorityRef)));
            } else if (tag.getK().startsWith(PRIVATECODE)) {
                privateCode = tag.getV();
            } else if (tag.getK().startsWith(ZONETOPOLOGY)) {
                String value = tag.getV();
                tagErrorCollector(ZONETOPOLOGY, value, tagErrors);
                zone.withZoneTopology(ZoneTopologyEnumeration.fromValue(value));
            } else if (tag.getK().startsWith(SCOPINGMETHOD)) {
                final String value = tag.getV();
                tagErrorCollector(SCOPINGMETHOD, value, tagErrors);
                final ScopingMethodEnumeration scopingMethodEnumeration = ScopingMethodEnumeration.fromValue(value);
                zone.withScopingMethod(scopingMethodEnumeration);

            } else if (tag.getK().startsWith(MEMBERS)) {
                final String value = tag.getV();
                tagErrorCollector(MEMBERS, value, tagErrors);
                final String[] stopPlaces = value.split(";");
                List<JAXBElement<? extends PointRefStructure>> stopPoints = Arrays.stream(stopPlaces)
                        .map(stopPlace -> new ObjectFactory().createScheduledStopPointRef(new ScheduledStopPointRefStructure().withRef(stopPlace)))
                        .collect(Collectors.toList());
                if (!stopPoints.isEmpty()) {
                    zone.withMembers(new PointRefs_RelStructure().withPointRef(stopPoints));
                }
            } else if (tag.getK().startsWith(NEIGHBOURS)) {
                final String value = tag.getV();
                tagErrorCollector(NEIGHBOURS, value, tagErrors);
                final String[] neighbours = value.split(";");
                final List<FareZoneRefStructure> fareZoneRefs = Arrays.stream(neighbours)
                        .map(farezone -> new FareZoneRefStructure().withRef(farezone))
                        .collect(Collectors.toList());

                if (!fareZoneRefs.isEmpty()) {
                    zone.withNeighbours(new FareZoneRefs_RelStructure().withFareZoneRef(fareZoneRefs));
                }

            } else if (tag.getK().equals(VALID_FROM)) {
                String validFrom = tag.getV();
                tagErrorCollector(VALID_FROM, validFrom, tagErrors);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Instant instant = sdf.parse(validFrom).toInstant();
                    fromDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    logger.info("Unable to parse and set valid from date: {}", e.getMessage());
                }

            } else if (tag.getK().equals(VALID_TO)) {
                String validTo = tag.getV();
                tagErrorCollector(VALID_TO, validTo, tagErrors);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Instant instant = sdf.parse(validTo).toInstant();
                    toDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    logger.info("Unable to parse and set valid to date: {}", e.getMessage());
                }

            } else if (tag.getK().startsWith(FAREZONEID)) {
                fareZoneId = tag.getV();
            } else if (tag.getK().startsWith(TZMAPPING)) {
                tzMapping = tag.getV();
            }
        }

        if (fromDate != null && toDate != null && toDate.isAfter(fromDate)) {
            logger.info("Set validity from and to date");
            zone.withValidBetween(new ValidBetween().withFromDate(fromDate).withToDate(toDate));
        } else if (fromDate != null && toDate == null) {
            logger.info("Set validity only from date");
            zone.withValidBetween(new ValidBetween().withFromDate(fromDate));
        }


        tagErrorCollector(CODESPACE, codespace, tagErrors);
        tagErrorCollector(FAREZONEID, fareZoneId, tagErrors);
        tagErrorCollector(PRIVATECODE, privateCode, tagErrors);

        checkTagErrors(tagErrors);

        zone.withPrivateCode(new PrivateCodeStructure().withValue(privateCode));
        generateOrSetTzMapping(zone, tzMapping, privateCode, codespace);
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

        DirectPositionListType dplt = new DirectPositionListType();
        way.getNd().stream()
                .map(nd -> mapOfNodes.get(nd.getRef()))
                .forEachOrdered(node -> {
                    dplt.getValue().add(node.getLat());
                    dplt.getValue().add(node.getLon());
                });

        AbstractRingPropertyType abstractRingPropertyType = new AbstractRingPropertyType()
                .withAbstractRing(openGisObjectFactory.createLinearRing(new LinearRingType().withPosList(dplt)));

        return new PolygonType()
                .withId("GEN-PolygonType" + way.getId())
                .withExterior(abstractRingPropertyType);
    }

    private void mapTags(List<Tag> tags, Zone_VersionStructure zone, String className) {
        String codespace = null;
        String reference = null;
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        List<IllegalArgumentException> tagErrors = new ArrayList<>();

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
                tagErrorCollector(ZONE_TYPE, value, tagErrors);

                KeyValueStructure keyValueStructure = new KeyValueStructure().withKey(keyName).withValue(value);
                KeyListStructure keyListStructure = new KeyListStructure().withKeyValue(keyValueStructure);
                zone.setKeyList(keyListStructure);
            } else if (tag.getK().equals(VALID_FROM)) {
                String validFrom = tag.getV();
                tagErrorCollector(VALID_FROM, validFrom, tagErrors);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Instant instant = sdf.parse(validFrom).toInstant();
                    fromDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    logger.info("Unable to parse and set valid from date: {}", e.getMessage());
                }
            } else if (tag.getK().equals(VALID_TO)) {
                String validTo = tag.getV();
                tagErrorCollector(VALID_TO, validTo, tagErrors);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Instant instant = sdf.parse(validTo).toInstant();
                    toDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    logger.info("Unable to parse and set valid to date: {}", e.getMessage());
                }

            }
        }

        if (fromDate != null && toDate != null && toDate.isAfter(fromDate)) {
            logger.info("Set validity from and to date");
            zone.withValidBetween(new ValidBetween().withFromDate(fromDate).withToDate(toDate));
        } else if (fromDate != null && toDate == null) {
            logger.info("Set validity only from date");
            zone.withValidBetween(new ValidBetween().withFromDate(fromDate));
        }


        tagErrorCollector(CODESPACE, codespace, tagErrors);
        tagErrorCollector(REFERENCE, reference, tagErrors);

        checkTagErrors(tagErrors);

        zone.setId(generateId(codespace, className, reference));
    }

    private String generateId(String codespace, String className, String reference) {
        return codespace + ":" + className + ":" + reference;
    }

    private String extractLangFromNameTagKey(String osmTagName) {
        return osmTagName.substring(osmTagName.lastIndexOf(':') + 1);
    }

    private void tagErrorCollector(String name, String value, List<IllegalArgumentException> errors) {
        if (value == null) {
            errors.add(new IllegalArgumentException(String.format("Missing tag or tag value: %s", name)));
        }
    }

    private void checkTagErrors(List<IllegalArgumentException> errors) {
        if(!errors.isEmpty()) {
            IllegalArgumentException exception = new IllegalArgumentException("Some required tags are missing");
            errors.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    protected Stream<GroupOfTariffZones> mapRelationsToGroupOfTariffZones(List<Relation> relations, Map<BigInteger, String> fareZoneMaps) {
        return relations.stream().map(rel -> mapRelationToGroupOfTariffZones(rel, fareZoneMaps));
    }

    private GroupOfTariffZones mapRelationToGroupOfTariffZones(Relation relation, Map<BigInteger, String> fareZoneMaps) {
        final GroupOfTariffZones groupOfTariffZones = new ObjectFactory().createGroupOfTariffZones();
        mapRelationTags(relation.getTag(), groupOfTariffZones);

        final TariffZoneRefs_RelStructure tariffZoneRefsRelStructure = new TariffZoneRefs_RelStructure();

        relation.getMember().stream().map(Member::getRef)
                .map(member -> new ObjectFactory().createTariffZoneRef(
                        new TariffZoneRef()
                                .withRef(fareZoneMaps.get(member))
                                .withVersion(DEFAULT_VERSION)))
                .forEach(tariffZoneRefsRelStructure.getTariffZoneRef_()::add);

        groupOfTariffZones.withMembers(tariffZoneRefsRelStructure);

        return groupOfTariffZones;
    }

    private void mapRelationTags(List<Tag> tags, GroupOfTariffZones groupOfTariffZones) {

        String groupOfTariffZoneId = null;
        String privateCode = null;
        String purposeOfGroupingRef = null;

        for (Tag tag : tags) {
            if (tag.getK().equals("GroupOfTariffZoneId")) {
                groupOfTariffZoneId = tag.getV();
            } else if (tag.getK().startsWith(NAME)) {
                String keyName = tag.getK();
                String lang = extractLangFromNameTagKey(keyName);
                groupOfTariffZones.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
            } else if (tag.getK().startsWith(PRIVATECODE)) {
                privateCode = tag.getV();
            } else if (tag.getK().startsWith("PurposeOfGroupingRef")) {
                purposeOfGroupingRef = tag.getV();
            }
        }

        if(groupOfTariffZoneId == null) {
            throw new IllegalArgumentException(String.format("Missing tag or tag value: %s", "GroupOfTariffZoneId"));
        }

        groupOfTariffZones.setId(groupOfTariffZoneId);
        groupOfTariffZones.withPrivateCode(new PrivateCodeStructure().withValue(privateCode));
        groupOfTariffZones.setPurposeOfGroupingRef(new PurposeOfGroupingRefStructure().withRef(purposeOfGroupingRef));
        groupOfTariffZones.setVersion(DEFAULT_VERSION);

    }
}
