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

package org.entur.netex.conversion.osm.cli;

import org.apache.commons.cli.*;
import org.entur.netex.conversion.osm.transformer.NetexHelper;
import org.entur.netex.conversion.osm.transformer.OsmToNetexTransformer;
import org.entur.netex.conversion.osm.transformer.OsmUnmarshaller;
import org.openstreetmap.osm.Osm;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileOutputStream;
import java.io.IOException;
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

            transform(osmFile, netexOutputFile, targetEntity);
        } catch (UnrecognizedOptionException e) {
            logger.error("Unable to convert to NeTEx", e);
            printHelp(options);
            System.exit(1);
        }
    }

    private static void transform(String osmInputFile, String netexOutputFile, String targetEntity) throws JAXBException, IOException, ClassNotFoundException, SAXException, ParserConfigurationException {

        OsmUnmarshaller osmUnmarshaller = new OsmUnmarshaller(false);
        InputSource osmInputSource = new InputSource(osmInputFile);

        Osm osm = osmUnmarshaller.unmarshall(osmInputSource);

        logger.info("Unmarshalled OSM file. generator: {}, version: {}, nodes: {}, ways: {}, relations: {}",
                osm.getGenerator(), osm.getVersion(), osm.getNode().size(), osm.getWay().size(), osm.getRelation().size());
        ObjectFactory netexObjectFactory = new ObjectFactory();
        NetexHelper netexHelper = new NetexHelper(netexObjectFactory);

        OsmToNetexTransformer osmToNetexTransformer = new OsmToNetexTransformer(netexHelper);

        PublicationDeliveryStructure publicationDeliveryStructure = osmToNetexTransformer.map(osm, targetEntity);

        FileOutputStream fileOutputStream = new FileOutputStream("target/"+netexOutputFile);
        netexHelper.marshalNetex(publicationDeliveryStructure, fileOutputStream);

        logger.info("Done. Check the result in the file {}", netexOutputFile);
    }


    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar <path-to-jar-file>", options);
    }
}