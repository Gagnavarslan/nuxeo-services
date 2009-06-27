package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.platform.filemanager.core.listener.BlobExtractorCache;
import org.nuxeo.runtime.api.Framework;

public class TestBlobExtractor extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        openRepository();
        typeMgr = getTypeManager();
    }

    public void testCaching() throws Exception {

        BlobExtractorCache bec = new BlobExtractorCache();

        List<String> paths = bec.getBlobFieldPathForDocumentType("NoBlobDocument");
        assertEquals(0, paths.size());

        paths = bec.getBlobFieldPathForDocumentType("SimpleBlobDocument");
        assertEquals(1, paths.size());
        assertEquals("/sb:blob", paths.get(0));

        paths = bec.getBlobFieldPathForDocumentType("BlobInListDocument");
        assertEquals(1, paths.size());
        assertEquals("bil:files/*/file", paths.get(0));

    }

    public void testGetBlobsFromDocumentModelNoBlob() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractorCache bec = new BlobExtractorCache();

        DocumentModel noBlob = getCoreSession().createDocumentModel("/",
                "testNoBlob", "NoBlobDocument");
        noBlob.setProperty("dublincore", "title", "NoBlobDocument");
        noBlob = getCoreSession().createDocument(noBlob);
        getCoreSession().saveDocument(noBlob);

        getCoreSession().save();

        List<Property> blobProperties = bec.getBlobsProperties(noBlob);
        assertEquals(0, blobProperties.size());

    }

    public void testGetBlobsFromDocumentModelSimpleBlob() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractorCache bec = new BlobExtractorCache();

        DocumentModel simpleBlob = getCoreSession().createDocumentModel("/",
                "testSimpleBlob", "SimpleBlobDocument");
        simpleBlob.setProperty("dublincore", "title", "SimpleBlobDocument");
        simpleBlob.setProperty("simpleblob", "blob", createTestBlob(false,
                "test.pdf"));

        simpleBlob = getCoreSession().createDocument(simpleBlob);
        getCoreSession().saveDocument(simpleBlob);
        getCoreSession().save();

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(simpleBlob);
        assertEquals(1, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test.pdf", blob.getFilename());
    }

    @SuppressWarnings("unchecked")
    public void testGetBlobsFromBlobInListDocument() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener.test");
        BlobExtractorCache bec = new BlobExtractorCache();

        DocumentModel blobInListEmpty = getCoreSession().createDocumentModel(
                "/", "testBlobInListDocumentEmpty", "BlobInListDocument");

        DocumentModel blobInListWithBlobs = getCoreSession().createDocumentModel(
                "/", "testBlobInListDocument1", "BlobInListDocument");
        blobInListWithBlobs.setProperty("dublincore", "title",
                "BlobInListDocument");
        Collection files = (Collection) blobInListWithBlobs.getProperty(
                "blobinlist", "files");

        HashMap<String, Object> blob1Map = new HashMap<String, Object>(2);
        blob1Map.put("file", createTestBlob(false, "test1.pdf"));
        blob1Map.put("filename", "test1.pdf");

        HashMap<String, Object> blob2Map = new HashMap<String, Object>(2);
        blob2Map.put("file", createTestBlob(false, "test2.pdf"));
        blob2Map.put("filename", "test2.pdf");

        files.add(blob1Map);
        files.add(blob2Map);
        blobInListWithBlobs.setProperty("blobinlist", "files", files);

        blobInListWithBlobs = getCoreSession().createDocument(
                blobInListWithBlobs);
        getCoreSession().saveDocument(blobInListWithBlobs);
        getCoreSession().save();

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(blobInListEmpty);
        assertEquals(0, blobs.size());

        blobs = bec.getBlobsProperties(blobInListWithBlobs);
        assertEquals(2, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test1.pdf", blob.getFilename());
        blob = (Blob) blobs.get(1).getValue();
        assertEquals("test2.pdf", blob.getFilename());

    }

    protected Blob createTestBlob(boolean setMimeType, String filename) {
        Blob blob = new StringBlob("SOMEDUMMYDATA");
        blob.setFilename(filename);
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        return blob;

    }

    protected SchemaManager typeMgr;

    public static SchemaManagerImpl getTypeManager() {
        return (SchemaManagerImpl) getTypeService().getTypeManager();
    }

    public static TypeService getTypeService() {
        return (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
    }

}