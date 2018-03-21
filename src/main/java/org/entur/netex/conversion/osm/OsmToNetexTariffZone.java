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
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

public class OsmToNetexTariffZone {


    public static void main(String[] args) throws FileNotFoundException, JAXBException {

        String file = args.length == 0 ? "osm.xml" : args[0];

        System.out.println("File arg = " + file);


        OsmToNetexTariffZone osmToNetexTariffZone = new OsmToNetexTariffZone();
        osmToNetexTariffZone.transform(file);
    }

    private void transform(String file) throws JAXBException, FileNotFoundException {

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

            TariffZone tariffZone = new TariffZone();

            String tariffZoneCodeSpace = null;
            String tariffZoneReference = null;

            for (Tag tag : way.getTag()) {

                if (tag.getK().equals("tariffZoneCodespace")) {
                    tariffZoneCodeSpace = tag.getV();
                } else if (tag.getK().startsWith("tariffZoneName")) {
                    String keyName = tag.getK();
                    String lang = keyName.substring(keyName.lastIndexOf(':') + 1);
                    tariffZone.setName(new MultilingualString().withValue(tag.getV()).withLang(lang));
                } else if (tag.getK().startsWith("tariffZoneReference")) {
                    tariffZoneReference = tag.getV();
                }
            }

            tagValueNotNullOrExit("tariffZoneCodeSpace", tariffZoneCodeSpace);
            tagValueNotNullOrExit("tariffZoneReference", tariffZoneReference);

            String id = tariffZoneCodeSpace + ":TariffZone:" + tariffZoneReference;

            System.out.println("created id: " + id);

            System.out.println(tariffZone);
            siteFrame.getTariffZones().getTariffZone().add(tariffZone);
        });


        ObjectFactory netexObjectFactory = new ObjectFactory();
        PublicationDeliveryStructure publicationDeliveryStructure = new PublicationDeliveryStructure()
                .withPublicationTimestamp(OffsetDateTime.now())
                .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(netexObjectFactory.createSiteFrame(siteFrame)));


    }

    private void tagValueNotNullOrExit(String name, String value) {

        if (value == null) {
            System.err.println(name + " from tag was null");
            System.exit(1);
        }
    }


}