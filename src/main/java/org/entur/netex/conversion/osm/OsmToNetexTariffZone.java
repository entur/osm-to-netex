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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Collectors;

public class OsmToNetexTariffZone {




    public static void main(String[] args) throws FileNotFoundException, XMLStreamException, JAXBException {

        String file = args.length == 0 ? "osm.xml" : args[0];

        System.out.println("File arg = " + file);


        JAXBContext osmContext = JAXBContext.newInstance(Osm.class);

        Unmarshaller osmContextUnmarshaller = osmContext.createUnmarshaller();

        JAXBElement<Osm> osmJAXBElement = osmContextUnmarshaller.unmarshal(new StreamSource(new FileInputStream(new File(file))), Osm.class);
        Osm osm = osmJAXBElement.getValue();

        System.out.println(osm);

        Map<BigInteger, Node> nodeMap = osm.getNode().stream()
                .collect(Collectors.toMap(Node::getId
                , node -> node));

        System.out.println("Read "+ nodeMap.size() + "nodes");

        osm.getWay().forEach(way -> {
            System.out.println("About to read way: " + way.getId());
            System.out.println(way);
            System.out.println(way.getId());
        });



    }


}