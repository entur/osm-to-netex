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

import org.apache.commons.cli.*;
import org.rutebanken.netex.model.ObjectFactory;

public class OsmToNetex {

    public static final String OSM_FILE = "osmFile";
    public static final String NETEX_OUTPUT_FILE = "netexOutputFile";
    public static final String NETEX_OUTPUT_FILE_DEFAULT_VALUE = "netex.xml";

    public static void main(String[] args) throws Exception {

        String file = args.length == 0 ? "osm.xml" : args[0];

        System.out.println("File arg = " + file);

        Options options = new Options();
        options.addOption(OSM_FILE, true, "Osm file to convert from");
        options.addOption(NETEX_OUTPUT_FILE, true, "Netex file name to write");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        String osmFile = cmd.getOptionValue(OSM_FILE);

        if(osmFile == null) {

            HelpFormatter formatter = new HelpFormatter();

            formatter.printHelp( "java -jar ascdf", options);
            System.exit(1);
        }

        String netexOutputFile = cmd.getOptionValue(NETEX_OUTPUT_FILE, NETEX_OUTPUT_FILE_DEFAULT_VALUE);


        ObjectFactory netexObjectFactory = new ObjectFactory();
        NetexHelper netexHelper = new NetexHelper(netexObjectFactory);

        OsmToNetexTransformer osmToNetexTransformer = new OsmToNetexTransformer(netexHelper);
        osmToNetexTransformer.transform(osmFile, netexOutputFile);
    }


}