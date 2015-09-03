/**
 *
 */

package org.nuxeo.deloitte;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author mikeobrebski
 */
@Operation(id = MakePDF.ID, category = Constants.CAT_CONVERSION, label = "MakePDF", description = "Fills selected PDF form with data from Document by matching fields")
public class MakePDF {

    public static final String ID = "MakePDF";

    protected final Log log = LogFactory.getLog(MakePDF.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext context;

    @Context
    protected AutomationService as;

    @Param(name = "formName", required = true)
    protected String formName;

    private Properties mappings;

    private PDDocument pdfDoc;

    private DocumentModel inputDoc;

    private File savedFile;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(DocumentModel input) throws Exception {

        mappings = new Properties();
        inputDoc = input;

        try {
            FileInputStream mappingInput = new FileInputStream("USCIS_Forms/"
                    + formName + ".properties");
            mappings.load(mappingInput);
            mappingInput.close();
        } catch (Exception e) {
            log.error("Cannot open PDF mapping - USCIS_Forms/" + formName
                    + ".properties");
            e.printStackTrace();
            throw e;
        }

        // Open PDF
        pdfDoc = PDDocument.load(new File("USCIS_Forms/" + formName + ".pdf"));
        if (pdfDoc.isEncrypted()) {
            try {
                pdfDoc.decrypt("");
                pdfDoc.setAllSecurityToBeRemoved(true);
            } catch (Exception e) {
                throw new Exception("Cannot Decrypt", e);
            }
        }

        //fillFields();

        savedFile = new File("tmp/" + inputDoc.getId() + "_" + formName
                + ".pdf");
        pdfDoc.save(savedFile);
        pdfDoc.close();


        FileBlob pdf = new FileBlob(savedFile, "application/pdf", null, formName + ".pdf", null);
        context.put("currentDocument", inputDoc.getId());

        Framework.trackFile(savedFile, pdf);

        BlobHolder bh = inputDoc.getAdapter(BlobHolder.class);

        bh.setBlob(pdf);

        session.saveDocument(inputDoc);

        return pdf;
    }

    @SuppressWarnings("rawtypes")
    protected void fillFields() throws IOException {
        PDDocumentCatalog docCatalog = pdfDoc.getDocumentCatalog();
        PDAcroForm acroForm = docCatalog.getAcroForm();
        List fields = acroForm.getFields();
        Iterator fieldsIter = fields.iterator();

        while (fieldsIter.hasNext()) {
            PDField field = (PDField) fieldsIter.next();
            processField(field);
        }

    }

    @SuppressWarnings("rawtypes")
    protected void processField(PDField field) throws IOException {
        List kids = field.getKids();
        if (kids != null) {
            Iterator kidsIter = kids.iterator();

            while (kidsIter.hasNext()) {
                Object pdfObj = kidsIter.next();
                if (pdfObj instanceof PDField) {
                    PDField kid = (PDField) pdfObj;
                    processField(kid);
                }
            }
        } else {

            String  nuxeoFieldName;
            Object value = null;
            try {
                nuxeoFieldName = mappings.getProperty(field.getFullyQualifiedName());
                value = inputDoc.getProperty("applicant_data", nuxeoFieldName);
            } catch (PropertyNotFoundException ex) {
                return;
            }

            if (value instanceof String) {
                String fieldValue = (String)value;
                field.setValue(fieldValue);

                log.warn(field.getFullyQualifiedName() + " = " + fieldValue
                        + " --- " + nuxeoFieldName);
            } else if (value instanceof GregorianCalendar) {
                GregorianCalendar cal = (GregorianCalendar)value;
                String date = ""+cal.get(Calendar.MONTH)+" / "+cal.get(Calendar.DAY_OF_MONTH)+" / "+cal.get(Calendar.YEAR);
                field.setValue(date);
                log.warn(field.getFullyQualifiedName() + " = " + date
                        + " --- " + nuxeoFieldName);
            } else if (value instanceof Boolean) {
                Boolean bool = (Boolean)value;
                if (bool.booleanValue()) {
                    field.setValue("On");
                } else {
                    field.setValue("Off");
                }
                log.warn(field.getFullyQualifiedName() + " = " + bool
                        + " --- " + nuxeoFieldName);
            }

        }
    }

}
