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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Entry point for running.
 */
public class OsmToNetexApp {

    private static final Logger logger = LoggerFactory.getLogger(OsmToNetexApp.class);

    public static final String OSM_FILE = "osmFile";
    public static final String NETEX_OUTPUT_FILE = "netexOutputFile";
    public static final String NETEX_OUTPUT_FILE_DEFAULT_VALUE = "netex.xml";
    public static final String TARGET_ENTITY = "targetEntity";

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(OSM_FILE, true, "Osm file to convert from");
        options.addOption(NETEX_OUTPUT_FILE, true, "Netex file name to write");
        options.addOption(TARGET_ENTITY, true, "Target entity. TariffZone, FareZone or TopographicPlace");


        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String osmFile = cmd.getOptionValue(OSM_FILE);
            if (osmFile == null) {
                printHelp(options);
                System.exit(1);
            }

            logger.info("got osm file: {}", osmFile);

            final String baseFileName = FilenameUtils.removeExtension(Paths.get(osmFile).getFileName().toString());


            String outPutFileName = new SimpleDateFormat("'"+baseFileName+"_'yyyyMMddHHmmss'.xml'").format(new Date());


            String netexOutputFile = cmd.getOptionValue(NETEX_OUTPUT_FILE, outPutFileName);

            ObjectFactory netexObjectFactory = new ObjectFactory();
            NetexHelper netexHelper = new NetexHelper(netexObjectFactory);


            String targetEntity = cmd.getOptionValue(TARGET_ENTITY);

            OsmToNetexTransformer osmToNetexTransformer = new OsmToNetexTransformer(netexHelper, targetEntity);
            osmToNetexTransformer.transform(osmFile, netexOutputFile);
        } catch (UnrecognizedOptionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            printHelp(options);
            System.exit(1);
        }
    }


    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar <path-to-jar-file>", options);
    }
}