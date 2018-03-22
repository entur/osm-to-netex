package org.entur.netex.conversion.osm;

import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.openstreetmap.osm.Node;
import org.openstreetmap.osm.Tag;
import org.openstreetmap.osm.Way;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Zone_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OsmToNetexMapper<T extends Zone_VersionStructure> {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexTransformer.class);
    public static final String CODESPACE = "codespace";
    public static final String NAME = "name";


    /**
     * Reference, which is the postfix of the generated Netex ID
     */
    public static final String REFERENCE = "reference";
    public static final String DEFAULT_VERSION = "1";

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
            throw new IllegalArgumentException("Cannot map " + name + " from tag.");
        }
    }

}
