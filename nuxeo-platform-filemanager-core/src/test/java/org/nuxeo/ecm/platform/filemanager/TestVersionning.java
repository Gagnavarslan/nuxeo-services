/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", init = RepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template",
        "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.api",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.versioning.api",
        "org.nuxeo.ecm.platform.versioning" })
@LocalDeploy("org.nuxeo.ecm.platform.filemanager.core:ecm-types-test-contrib.xml")
public class TestVersionning {

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    @Inject
    protected CoreSession coreSession;

    private void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = coreSession.createDocument(ws);

        destWS = ws;
    }

    @Test
    public void testVersioning() throws Exception {
        createTestDocuments();
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = new StringBlob("Something");
        blob.setMimeType("something");
        blob.setFilename("mytest.something");
        DocumentModel doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytest.something");

        assertEquals("mytest.something", doc.getTitle());

        VersioningManager vm = Framework.getLocalService(VersioningManager.class);
        String vl = vm.getVersionLabel(doc);
        assertEquals("0.0", vl);

        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytest.something");

        String vl2 = vm.getVersionLabel(doc);
        assertEquals("0.1", vl2);

        blob.setFilename("mytest2.something");
        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytest2.something");
        vl = vm.getVersionLabel(doc);
        assertEquals("0.0", vl);

        blob.setFilename("mytxt.txt");
        blob.setMimeType("text/plain");
        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytxt.txt");
        assertEquals("Note", doc.getType());
        vl = vm.getVersionLabel(doc);
        assertEquals("0.0", vl);

        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytxt.txt");
        vl = vm.getVersionLabel(doc);
        assertEquals("0.1", vl);
    }

}
