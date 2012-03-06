/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.ooomanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import javax.servlet.ServletException;
import java.io.IOException;

public class OOoManagerComponent extends DefaultComponent implements
        OOoManagerService {

    protected static final Log log = LogFactory.getLog(OOoManagerComponent.class);

    protected static String CONFIG_EP = "oooManagerConfig";

    private static OfficeManager officeManager;

    protected OOoManagerDescriptor descriptor = new OOoManagerDescriptor();

    protected boolean started = false;

    public OOoManagerDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void registerContribution(Object contribution,
                                     String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equals(extensionPoint)) {
            OOoManagerDescriptor desc = (OOoManagerDescriptor) contribution;
            descriptor = desc;
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        stopOOoManager();
    }

    public OfficeDocumentConverter getDocumentConverter() {
        if (isOOoManagerStarted()) {
            return new OfficeDocumentConverter(officeManager);
        } else {
            log.error("OfficeManager is not started.");
            return null;
        }
    }

    public void stopOOoManager() {
        if (started) {
            officeManager.stop();
            log.debug("Stoping ooo manager.");
        } else {
            log.debug("OOoManager already stoped..");
        }
    }

    public void startOOoManager() throws IOException {
        try {
            Class<?> builder = descriptor.getManagerBuilder();
            if (builder == null) {
                builder = OOoDefaultManagerBuilder.class;
            }
            OOoManagerBuilder managerBuilder = (OOoManagerBuilder) builder.newInstance();
            officeManager = managerBuilder.buildOfficeManager(descriptor);
            officeManager.start();
            started = true;
            log.debug("Starting ooo manager.");
        } catch (Exception e) {
            Throwable t = unwrapException(e);
            log.warn("OpenOffice was not found, JOD Converter "
                    + "won't be available: " + t.getMessage());
        }
    }

    public Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t instanceof ServletException) {
            cause = ((ServletException) t).getRootCause();
        } else if (t instanceof ClientException) {
            cause = t.getCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        try {
            startOOoManager();
        } catch (IOException e) {
            throw new RuntimeException("Could not start OOoManager.", e);
        }
    }

    public boolean isOOoManagerStarted() {
        return started;
    }

}
