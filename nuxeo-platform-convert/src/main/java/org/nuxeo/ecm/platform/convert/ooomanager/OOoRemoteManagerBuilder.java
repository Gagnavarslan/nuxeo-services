package org.nuxeo.ecm.platform.convert.ooomanager;

import org.artofsolving.jodconverter.office.ExternalOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;

import java.io.IOException;

public class OOoRemoteManagerBuilder implements OOoManagerBuilder {
    public OfficeManager buildOfficeManager(OOoManagerDescriptor descriptor) throws IOException {
        ExternalOfficeManagerConfiguration configuration = new ExternalOfficeManagerConfiguration();
        configuration.setConnectionProtocol(OfficeConnectionProtocol.SOCKET);
        configuration.setConnectOnStart(true);

        int[] portNumbers = descriptor.getPortNumbers();
        if (portNumbers != null && portNumbers.length == 1) {
            configuration.setPortNumber(portNumbers[0]);
        } else {
            configuration.setPortNumber(2003);
        }
        return configuration.buildOfficeManager();
    }
}
