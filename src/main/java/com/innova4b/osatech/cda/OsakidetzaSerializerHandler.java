package com.innova4b.osatech.cda;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class OsakidetzaSerializerHandler implements SerializerHandler{
	
	private XmlSerializer serializer;
	private Date visitDate;
	private String patientName;
	private String historyNumber;
	private String birthDate;
	
	// TAGS
	private static final String TAG_PATIENT 			= "Nombre:";
	private static final String TAG_PATIENT_SURNAME1	= "Apellido 1:";
	private static final String TAG_PATIENT_SURNAME2	= "Apellido 2:";
	private static final String TAG_HISTORY_NUM			= "DNI/NIE:";
	private static final String TAG_BIRTH_DATE			= "Fecha nacimiento:";
	private static final String TAG_NOTES 				= "* (IDC) Informacion complementada por el usuario";
	
	// LOINC Codes for Sections inside Record
	private static final String CODE_SYSTEM				= "2.16.840.1.113883.6.1";
	private static final String CODE_SYSTEM_NAME		= "LOINC";
	private static final String CODE_EPISODE_SUMMARY	= "34133-9";
	private static final String CODE_ALLERGIES 			= "10155-0";
	private static final String CODE_MEDICAL_HISTORY 	= "10153-2";
	private static final String CODE_FAMILY_HISTORY 	= "10157-2";
	private static final String CODE_MEDICATIONS 		= "10160-0";
	private static final String CODE_SOCIAL_HISTORY 	= "29762-2";
	private static final String CODE_ILLNESS_HISTORY 	= "10164-2";
	private static final String CODE_VISIT 				= "66456-5";
	private static final String CODE_REASON_ASSESSMENT 	= "70160-7";
	private static final String CODE_PHYSICAL_EXAM		= "11384-5";
	private static final String CODE_ASSESSMENT 		= "11496-7";
	private static final String CODE_PLAN 				= "18776-5";
	private static final String CODE_NOTES 				= "34109-9";
	
	public OsakidetzaSerializerHandler() {
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance(
			System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			serializer = factory.newSerializer();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
	}
	
	// Converts RTF file to CDA file
	public String serialize(InputStream is) throws Exception {
		Scanner scan = new Scanner(is, "UTF-8");
		String namespace = "";
		
		StringWriter writer = new StringWriter();
	    try {
	        serializer.setOutput(writer);
	        
	        serializer.startDocument("UTF-8", true);
	        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	        serializer.processingInstruction("xml-stylesheet type=\"text/xsl\" href=\"CDA.xsl\"");
	        
	        serializer.startTag(namespace, "ClinicalDocument");
	        serializer.attribute(namespace, "xmlns", "urn:hl7-org:v3");
	        serializer.attribute(namespace, "xmlns:voc", "urn:hl7-org:v3/voc");
	        serializer.attribute(namespace, "xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
	        serializer.attribute(namespace, "xsi:schemaLocation","urn:hl7-org:v3 CDA.xsd");
	        addInfo(namespace);

			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				// Remove whitespaces and end of lines
				line = line.trim().replaceAll("/n", "");
				if( (!"".equals(line)) && (line != null) ) {
					process(line);
					
				}
			}
			scan.close();
			is.close();
			
			serializer.endTag(namespace, "structuredBody");
			serializer.endTag(namespace, "component");
			serializer.endTag(namespace, "ClinicalDocument");
	        serializer.endDocument();
	        
	        return writer.toString();
	        
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    } 
	}
	
	// Parses the line
	public void process(String line) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String namespace = "";
		
		if (line.startsWith(TAG_PATIENT)) {
			line = line.replace(TAG_PATIENT, "").trim();
			patientName = line;
		} else if (line.startsWith(TAG_PATIENT_SURNAME1)) {
			line = line.replace(TAG_PATIENT_SURNAME1, "").trim();
			patientName += "-" + line;
		} else if (line.startsWith(TAG_PATIENT_SURNAME2)) {
			line = line.replace(TAG_PATIENT_SURNAME2, "").trim();
			patientName += " " + line;
		} else if (line.startsWith(TAG_HISTORY_NUM)) {
			line = line.replace(TAG_HISTORY_NUM, "").trim();
			historyNumber = line.substring(0,8);
			addPatient(namespace, birthDate);
			
			/*START BODY DOCUMENT*/
			serializer.startTag(namespace, "component");
			serializer.startTag(namespace, "structuredBody");
		} else if (line.startsWith(TAG_BIRTH_DATE)) {
			line = line.replace(TAG_BIRTH_DATE, "").trim();
			birthDate = line;
		} else if (line.startsWith(TAG_NOTES)) {
			line = line.replace(TAG_NOTES, "").trim();
			addNotes(namespace, line);
		}
	}
	
	public void addInfo(String namespace) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(namespace, "typeId");
        serializer.attribute(namespace, "root", "2.16.840.1.113883.1.3");
        serializer.attribute(namespace, "extension", "POCD_HD000040");
        serializer.endTag(namespace, "typeId");
        serializer.startTag(namespace, "id");
        String currentFormattedDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        serializer.attribute(namespace, "extension", currentFormattedDate);
        serializer.attribute(namespace, "root", "1.1.1.1.1.0.0.0.1");
        serializer.endTag(namespace, "id");
        serializer.startTag(namespace, "code");
    	serializer.attribute(namespace, "code", CODE_EPISODE_SUMMARY);
    	serializer.attribute(namespace, "codeSystem", CODE_SYSTEM);
    	serializer.attribute(namespace, "codeSystemName", CODE_SYSTEM_NAME);
    	serializer.attribute(namespace, "displayName", "Summary of episode note");
    	serializer.endTag(namespace, "code");
    	serializer.startTag(namespace,"title");
    	serializer.text("Innova4B Consultation Note");
    	serializer.endTag(namespace, "title");
    	serializer.startTag(namespace, "effectiveTime");
    	serializer.attribute(namespace, "value", currentFormattedDate);
    	serializer.endTag(namespace, "effectiveTime");
    	serializer.startTag(namespace, "confidentialityCode");
    	serializer.attribute(namespace, "code", "N");
    	serializer.attribute(namespace, "codeSystem", "2.16.840.1.113883.5.25");
    	serializer.endTag(namespace, "confidentialityCode");
    	serializer.startTag(namespace, "languageCode");
    	serializer.attribute(namespace, "code", "es-ES");
    	serializer.endTag(namespace, "languageCode");
    	/*ŜTART AUTHOR BLOCK*/
    	serializer.startTag(namespace, "author");
    	serializer.startTag(namespace, "time");
    	serializer.attribute(namespace, "value", currentFormattedDate);
    	serializer.endTag(namespace, "time");
    	serializer.startTag(namespace, "assignedAuthor");
    	serializer.startTag(namespace,"id");
    	serializer.attribute(namespace, "extension", "XXX");
        serializer.attribute(namespace, "root", "XXX");
    	serializer.endTag(namespace,"id");
    	serializer.startTag(namespace, "assignedAuthoringDevice");
    	serializer.startTag(namespace, "code");
    	serializer.attribute(namespace, "code", "Middleware Innova4B");
    	serializer.attribute(namespace, "codeSystem", "1.1.1.1.1.0.0.0.1");
    	serializer.attribute(namespace, "displayName", "Sistema o dispositivo Middleware Innova4B");
    	serializer.endTag(namespace, "code");
    	serializer.endTag(namespace, "assignedAuthoringDevice");
    	serializer.endTag(namespace, "assignedAuthor");
    	serializer.endTag(namespace, "author");
    	/*END AUTHOR BLOCK*/
    	/*START CUSTODIAN BLOCK*/
    	serializer.startTag(namespace, "custodian");
    	serializer.startTag(namespace, "assignedCustodian");
    	serializer.startTag(namespace, "representedCustodianOrganization");
    	serializer.startTag(namespace,"id");
    	serializer.attribute(namespace, "extension", "ORL Gipuzkoa");
        serializer.attribute(namespace, "root", "2.2.2.2.2");
    	serializer.endTag(namespace,"id");
    	serializer.startTag(namespace, "name");
    	serializer.text("ORL Gipuzkoa");
    	serializer.endTag(namespace, "name");
    	serializer.endTag(namespace, "representedCustodianOrganization");
    	serializer.endTag(namespace, "assignedCustodian");
    	serializer.endTag(namespace, "custodian");
    	/*END CUSTODIAN BLOCK*/
	}
	
	public void addPatient(String namespace, String birthDate) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(namespace, "recordTarget");
		serializer.startTag(namespace, "patientRole");
		serializer.startTag(namespace,"id");
    	serializer.attribute(namespace, "extension", historyNumber);
        serializer.attribute(namespace, "root", "2.2.2.2.2.0.0.0.1");
    	serializer.endTag(namespace,"id");
    	serializer.startTag(namespace, "patient");
    	serializer.startTag(namespace, "name");
    	serializer.startTag(namespace, "given");
    	String name = patientName.substring(0, patientName.indexOf("-"));
    	serializer.text(name);
    	serializer.endTag(namespace, "given");
    	serializer.startTag(namespace, "family");
    	String surname = patientName.substring(patientName.indexOf("-")+1);
    	serializer.text(surname);
    	serializer.endTag(namespace, "family");
    	serializer.endTag(namespace, "name");
    	serializer.startTag(namespace, "administrativeGenderCode");
    	serializer.attribute(namespace, "code", "UN");
    	serializer.attribute(namespace, "codeSystem", "2.16.840.1.113883.5.1");
    	serializer.endTag(namespace, "administrativeGenderCode");
    	serializer.startTag(namespace, "birthTime");
    	String formattedDate = "yyyyMMdd";
    	try {
			Date birth = new SimpleDateFormat("dd/MM/yyyy").parse(birthDate);
			formattedDate = new SimpleDateFormat("yyyyMMdd").format(birth);
		} catch (ParseException e) {
			e.printStackTrace();
		}
    	serializer.attribute(namespace, "value", formattedDate);
    	serializer.endTag(namespace, "birthTime");
    	serializer.endTag(namespace, "patient");
		serializer.endTag(namespace, "patientRole");
		serializer.endTag(namespace, "recordTarget");		
	}
	
	public void addAllergies(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_ALLERGIES;
		String title = "Allergies and Adverse Reactions";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addMedicalHistory(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_MEDICAL_HISTORY;
		String title = "Past Medical History";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addFamilyHistory(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_FAMILY_HISTORY;
		String title = "Family History";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}

	public void addMedications(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_MEDICATIONS;
		String title = "Medications";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addSocialHistory(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_SOCIAL_HISTORY;
		String title = "Social History";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addVisitStart(String namespace) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_VISIT;
		String title = "Visit";
		
		Section section = new Section (code, title, "");
		
		serializer.startTag(namespace, "component");
		serializer.startTag(namespace, "section");
		serializer.startTag(namespace, "code");
		serializer.attribute(namespace, "code", section.getCode());
    	serializer.attribute(namespace, "codeSystem", section.getCodeSystem());
    	serializer.attribute(namespace, "codeSystemName", section.getCodeSystemName());
    	serializer.endTag(namespace, "code");
    	serializer.startTag(namespace, "title");
    	serializer.text(section.getTitle());
    	serializer.endTag(namespace, "title");
	}
	
	public void addVisitAuthor(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(namespace, "author");
		serializer.startTag(namespace, "time");
		String formattedDate = new SimpleDateFormat("yyyyMMdd").format(visitDate);
		serializer.attribute(namespace, "value", formattedDate);
		serializer.endTag(namespace, "time");
		serializer.startTag(namespace, "assignedAuthor");
		serializer.startTag(namespace,"id");
		serializer.attribute(namespace, "extension", "XXXXX");
	    serializer.attribute(namespace, "root", "XXXXX");
		serializer.endTag(namespace,"id");
		serializer.startTag(namespace, "assignedPerson");
		serializer.startTag(namespace, "name");
    	serializer.startTag(namespace, "given");
    	serializer.text(text);
    	serializer.endTag(namespace, "given");
    	serializer.startTag(namespace, "family");
    	serializer.text("");
    	serializer.endTag(namespace, "family");
    	serializer.startTag(namespace, "suffix");
    	serializer.text("");
    	serializer.endTag(namespace, "suffix");
    	serializer.endTag(namespace, "name");
		serializer.endTag(namespace, "assignedPerson");
		serializer.endTag(namespace, "assignedAuthor");
		serializer.endTag(namespace, "author");
	}
	
	public void addReasonAssessment(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_REASON_ASSESSMENT;
		String title = "Reason for Assessment";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addPhysicalExamination(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_PHYSICAL_EXAM;
		String title = "Physical Examination";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}

	public void addAssessment(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_ASSESSMENT;
		String title = "Assessment";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addPlan(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_PLAN;
		String title = "Plan";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	public void addNotes(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String code = CODE_NOTES;
		String title = "Notes";
		
		Section section = new Section (code, title, text);
		addSection(namespace, section);
	}
	
	// Create a new section with some parameters
	public void addSection(String namespace, Section section) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(namespace, "component");
		serializer.startTag(namespace, "section");
		serializer.startTag(namespace, "code");
    	serializer.attribute(namespace, "code", section.getCode());
    	serializer.attribute(namespace, "codeSystem", section.getCodeSystem());
    	serializer.attribute(namespace, "codeSystemName", section.getCodeSystemName());
    	serializer.endTag(namespace, "code");
    	serializer.startTag(namespace, "title");
    	serializer.text(section.getTitle());
    	serializer.endTag(namespace, "title");
    	serializer.startTag(namespace, "text");
    	serializer.text(section.getText());
    	serializer.endTag(namespace, "text");
		serializer.endTag(namespace, "section");
		serializer.endTag(namespace, "component");
	}
	
	private static boolean isValidFormat(String format, String value) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            date = sdf.parse(value);
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return date != null;
    }
	
	public class Section {
		private String code;
		private String codeSystem = CODE_SYSTEM;
		private String codeSystemName = CODE_SYSTEM_NAME;
		private String title;
		private String text;
				
		Section (String code, String title, String text) {
			this.setCode(code);
			this.setTitle(title);
			this.setText(text);
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getCodeSystem() {
			return codeSystem;
		}

		public void setCodeSystem(String codeSystem) {
			this.codeSystem = codeSystem;
		}

		public String getCodeSystemName() {
			return codeSystemName;
		}

		public void setCodeSystemName(String codeSystemName) {
			this.codeSystemName = codeSystemName;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
		
		
		
	}

}
