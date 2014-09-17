/**
 *
 */

package org.nuxeo.deloitte;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * @author mikeobrebski
 */
@Operation(id=MakePDF.ID, category=Constants.CAT_CONVERSION, label="MakePDF", description="Fills selected PDF form with data from Document by matching fields")
public class MakePDF {

    public static final String ID = "MakePDF";

    protected final Log log = LogFactory.getLog(MakePDF.class);

    @Param(name = "formName", required = true)
    protected String formName;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(DocumentModel input) {

        Properties mappings = new Properties();

        try {
            mappings.load(new FileInputStream("USCIS_Forms/"+formName + ".properties"));
        } catch (Exception e) {
            log.error("Cannot open PDF mapping properties for - "+formName);
            e.printStackTrace();
            return null;
        }

        // Open PDF
        InputStream fileInput;
        try {
            fileInput = new FileInputStream("USCIS_Forms/"+formName + ".pdf");
        } catch (FileNotFoundException e) {
            log.error("Cannot open PDF file template for - "+formName);
            e.printStackTrace();
            return null;
        }

        // Open and prepare PDF Doc

        // Loop through Doc and populate fields

        // Close
        try {
            fileInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File("tmp/"+formName+".pdf");

        try {
            file.createNewFile();
        } catch (IOException e) {
            log.error("Cannot open PDF file template");
            e.printStackTrace();
            return null;
        }

        log.warn(file.getAbsolutePath());

        FileBlob pdf = new FileBlob(file);
        return pdf;
    }


}
