package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

public class BlobExtractorCache {

    Log log = LogFactory.getLog(BlobExtractorCache.class);

    protected Map<String, List<String>> blobFieldPaths = new HashMap<String, List<String>>();

    protected List<String> docTypeCached = new ArrayList<String>();

    protected SchemaManager schemaManager;

    protected SchemaManager getSchemaManager() throws Exception {
        if (schemaManager == null) {
            schemaManager = Framework.getService(SchemaManager.class);
        }
        return schemaManager;
    }

    /**
     * Get properties of the given document that contain a blob value. This
     * method use the cache engine to find these properties.
     *
     * @param doc
     * @return
     * @throws Exception
     */
    public List<Property> getBlobsProperties(DocumentModel doc)
            throws Exception {

        List<Property> result = new ArrayList<Property>();
        for (String path : getBlobFieldPathForDocumentType(doc.getType())) {
            List<String> pathSplitted = Arrays.asList(path.split("/[*]/"));
            if (pathSplitted.size() == 0) {
                throw new Exception("Path detected not wellformed: " + path);
            }
            Property prop = doc.getProperty(pathSplitted.get(0));

            if (pathSplitted.size() >= 1) {
                List<String> subPath = pathSplitted.subList(1,
                        pathSplitted.size());
                getBlobValue(prop, subPath, path, result);
            }
        }

        return result;
    }

    /**
     * Get path list of properties that may contain a blob for the given
     * document type.
     *
     * @param documentType document type name
     * @return return the property names that contain blob
     * @throws Exception
     */
    public List<String> getBlobFieldPathForDocumentType(String documentType)
            throws Exception {
        DocumentType docType = getSchemaManager().getDocumentType(documentType);

        if (!docTypeCached.contains(documentType)) {
            List<String> paths = new ArrayList<String>();
            blobFieldPaths.put(docType.getName(), paths);

            createCacheForDocumentType(docType);
        }

        return blobFieldPaths.get(documentType);
    }

    public void invalidateDocumentTypeCache(String docType) {
        if (docTypeCached.contains(docType)) {
            docTypeCached.remove(docType);
        }
    }

    public void invalidateCache() {
        docTypeCached = new ArrayList<String>();
    }

    protected void createCacheForDocumentType(DocumentType docType)
            throws Exception {

        for (Schema schema : docType.getSchemas()) {
            findInteresting(docType, schema, "", schema);
        }

        if (!docTypeCached.contains(docType.getName())) {
            docTypeCached.add(docType.getName());
        }
    }

    /**
     * Analyzes the document's schemas to find which fields and complex types
     * contain blobs. For each blob fields type found,
     * {@link BlobExtractorCache#blobMatched(DocumentType, Schema, String, Field)}
     * is called and for each property that contains a subProperty containing a
     * Blob,
     * {@link BlobExtractorCache#containsBlob(DocumentType, Schema, String, Field)}
     * is called
     *
     * @param parent The parent schema that contains the field
     * @param ct Current type parsed
     * @return {@code true} if the passed complex type contains at least one
     *         blob field
     * @throws Exception thrown if a field is named '*' (name forbidden)
     */
    protected boolean findInteresting(DocumentType docType, Schema parent,
            String path, ComplexType ct) throws Exception {
        boolean interesting = false;
        for (Field field : ct.getFields()) {
            Type type = field.getType();
            if (type.isSimpleType()) {
                continue; // not binary text
            } else if (type.isListType()) {
                Type ftype = ((ListType) type).getField().getType();
                if (ftype.isComplexType()) {
                    path = path + String.format("%s/*", field.getName());
                    if ("*".equals(field.getName())) {
                        throw new Exception(
                                "A field can't be named '*' please check this field: "
                                        + path);
                    }
                    if (findInteresting(docType, parent, path,
                            (ComplexType) ftype)) {
                        containsBlob(docType, parent, path, field);
                        interesting |= true;
                    }
                } else {
                    continue; // not binary text
                }
            } else { // complex type
                ComplexType ctype = (ComplexType) type;
                if (type.getName().equals(TypeConstants.CONTENT)) {
                    path = path + String.format("/%s", field.getName());
                    blobMatched(docType, parent, path, field);
                    interesting = true;
                } else {
                    path = path + String.format("%s/*", field.getName());
                    interesting |= findInteresting(docType, parent, path, ctype);
                }
            }
        }
        if (interesting) {
            containsBlob(docType, parent, path, null);
        }
        return interesting;
    }

    /**
     * Call during the parsing of the schema structure in
     * {@link BlobExtractorCache#findInteresting(Schema, ComplexType)} if field
     * is a Blob Type. This method stores the path to that Field.
     *
     * @param parent The parent schema that contains the field
     * @param field Field that is a BlobType
     */
    protected void blobMatched(DocumentType docType, Schema parent,
            String path, Field field) {
        List<String> paths = blobFieldPaths.get(docType.getName());
        if (paths == null) {
            paths = new ArrayList<String>();
            blobFieldPaths.put(docType.getName(), paths);
        }

        paths.add(path);

    }

    /**
     * Call during the parsing of the schema structure in
     * {@link BlobExtractorCache#findInteresting(Schema, ComplexType)} if field
     * contains a subfield of type Blob. This method do nothing.
     *
     * @param parent The parent schema that contains the field
     * @param field Field that contains a subField of type BlobType
     */
    protected void containsBlob(DocumentType docType, Schema parent,
            String path, Field field) {

    }

    protected void getBlobValue(Property prop, List<String> subPath,
            String completePath, List<Property> result) throws Exception {
        if (subPath.size() == 0) {
            if (!(prop.getValue() instanceof Blob)) {
                throw new Exception("Path Field not contains a blob value: "
                        + completePath);
            }
            result.add(prop);
            return;
        }

        for (Property childProp : prop.getChildren()) {
            if ("/*".equals(subPath.get(0))) {
                // TODO : if blob are in a list
                throw new Exception("TODO : BLOB IN A LIST NOT IMPLEMENTED for this path "
                        + completePath);
            }
            Property childSubProp = childProp.get(subPath.get(0));
            getBlobValue(childSubProp, subPath.subList(1, subPath.size()),
                    completePath, result);
        }
    }
}