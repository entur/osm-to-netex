package org.entur.netex.conversion.osm;

import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;

public class NetexHelper {

    private final ObjectFactory netexObjectFactory;
    private final Marshaller marshaller;

    public NetexHelper(ObjectFactory netexObjectFactory) throws JAXBException {
        this.netexObjectFactory = netexObjectFactory;
        this.marshaller = JAXBContext.newInstance(StopPlace.class).createMarshaller();

    }


    public void marshalNetex(PublicationDeliveryStructure publicationDeliveryStructure, OutputStream outputStream) throws JAXBException {
        marshaller.marshal(netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure), outputStream);
    }


    public PublicationDeliveryStructure createPublicationDelivery(SiteFrame siteFrame) {
        PublicationDeliveryStructure publicationDeliveryStructure = new PublicationDeliveryStructure()
                .withPublicationTimestamp(OffsetDateTime.now())
                .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(netexObjectFactory.createSiteFrame(siteFrame)));

        return publicationDeliveryStructure;
    }

    public Zone_VersionStructure createNetexObject(String className) {

        String fullyQualifiedName = StopPlace.class.getPackageName() + "." + className;

        try {
            Class<? extends Zone_VersionStructure> clazz = (Class<? extends Zone_VersionStructure>) Class.forName(fullyQualifiedName);

            Constructor<? extends Zone_VersionStructure> constructor = clazz.getConstructor();

            return constructor.newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Cannot create class from class name: " + className, e);
        }
    }

}
