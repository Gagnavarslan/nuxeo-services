package org.nuxeo.ecm.platform.convert.ooomanager;

import org.artofsolving.jodconverter.office.OfficeManager;

import java.io.IOException;

public interface OOoManagerBuilder {
    OfficeManager buildOfficeManager(OOoManagerDescriptor descriptor) throws IOException;
}
