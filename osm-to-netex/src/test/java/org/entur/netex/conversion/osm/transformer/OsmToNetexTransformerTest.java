package org.entur.netex.conversion.osm.transformer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

import javax.xml.transform.Source;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Map.entry;

public class OsmToNetexTransformerTest {
    private static final Set<String> ignoredNodes = Set.of("PublicationTimestamp", "Description", "ParticipantRef");
    private static final Set<String> ignoredAttributes = Set.of("created");
    private static final Map<String, Set<String>> ignoredAttributesForSpecificNodes = Map.ofEntries(entry("SiteFrame", Set.of("id")));

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("expected_result_tariff.xml", "TariffZone"),
                Arguments.of("expected_result_topographic_place.xml", "TopographicPlace")
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testConversion(String expectedResultFile, String targetEntity) throws ClassNotFoundException {
        OsmToNetexTransformer osmToNetexTransformer = new OsmToNetexTransformer();

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputSource osmInputSource = new InputSource(classloader.getResourceAsStream("osm.xml"));
        InputStream is = classloader.getResourceAsStream(expectedResultFile);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        osmToNetexTransformer.marshallOsm(osmInputSource, byteArrayOutputStream, targetEntity,"osm.xml",  "test");

        Source expected = Input.fromStream(is).build();

        Source result = Input.fromString(byteArrayOutputStream.toString()).build();

        final Diff documentDiff = compareResults(expected, result);
        Assertions.assertFalse(documentDiff.hasDifferences(), documentDiff.fullDescription());
    }

    private Diff compareResults(Source expected, Source result) {
        return DiffBuilder
                .compare(expected)
                .withTest(result)
                .withNodeFilter(node -> !ignoredNodes.contains(node.getNodeName()))
                .withAttributeFilter(attr -> !(ignoredAttributes.contains(attr.getNodeName()) ||
                        ignoredAttributesForSpecificNodes.containsKey(attr.getOwnerElement().getNodeName()) && ignoredAttributesForSpecificNodes.get(attr.getOwnerElement().getNodeName()).contains(attr.getNodeName())))
                .build();
    }
}
