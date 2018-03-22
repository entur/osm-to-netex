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

import org.rutebanken.netex.model.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

public class OsmToNetex {

    public static void main(String[] args) throws FileNotFoundException, JAXBException {

        String file = args.length == 0 ? "osm.xml" : args[0];

        System.out.println("File arg = " + file);

        ObjectFactory netexObjectFactory = new ObjectFactory();
        NetexHelper netexHelper = new NetexHelper(netexObjectFactory);

        Converter converter = new Converter(netexHelper);
        converter.transform(file);
    }


}