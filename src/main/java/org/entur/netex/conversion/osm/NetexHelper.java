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

import org.rutebanken.netex.model.*;
import org.rutebanken.netex.validation.NeTExValidator;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

public class NetexHelper {

    private final ObjectFactory netexObjectFactory;
    private final Marshaller marshaller;

    public NetexHelper(ObjectFactory netexObjectFactory) throws JAXBException, IOException, SAXException {
        this.netexObjectFactory = netexObjectFactory;
        marshaller = JAXBContext.newInstance(StopPlace.class).createMarshaller();
        marshaller.setSchema(new NeTExValidator().getSchema());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    }

    public void marshalNetex(PublicationDeliveryStructure publicationDeliveryStructure, OutputStream outputStream) throws JAXBException {
        marshaller.marshal(netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure), outputStream);
    }

    @SuppressWarnings("unchecked")
    public PublicationDeliveryStructure createPublicationDelivery(SiteFrame siteFrame, String osmInputFile) {
        PublicationDeliveryStructure publicationDeliveryStructure = new PublicationDeliveryStructure()
                .withPublicationTimestamp(LocalDateTime.now())
                .withDescription(new MultilingualString()
                        .withValue(generatePublicationDeliveryDescription(osmInputFile)))
                .withParticipantRef(OsmToNetexApp.class.getCanonicalName())
                .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(netexObjectFactory.createSiteFrame(siteFrame)));

        return publicationDeliveryStructure;
    }

    public <ZONE extends Zone_VersionStructure> ZONE createNetexObject(Class<ZONE> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create class from class name: " + clazz, e);
        }
    }


    private String generatePublicationDeliveryDescription(String osmInputFile) {
        return "Generated by osm-to-netex on host : " + hostname() + " from file " + osmInputFile + ". Tool: https://github.com/entur/osm-to-netex";
    }

    private String hostname() {

        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println("Cannot detect hostname for this computer " + e.getMessage());
        }

        return "unknown";
    }

    public SiteFrame createSiteFrame() {
        SiteFrame siteFrame = new SiteFrame();
        siteFrame.setVersion("1");
        siteFrame.setId("OSM:SiteFrame:" + System.currentTimeMillis());
        siteFrame.setCreated(LocalDateTime.now());
        siteFrame.withFrameDefaults(
                new VersionFrameDefaultsStructure()
                        .withDefaultLocale(
                                new LocaleStructure()
                                        .withTimeZone("Europe/Paris")));
        return siteFrame;
    }
}
