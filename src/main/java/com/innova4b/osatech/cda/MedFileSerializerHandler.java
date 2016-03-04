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

public class MedFileSerializerHandler implements SerializerHandler{
	
	private XmlSerializer serializer;
	private boolean isFirstVisit;
	private Date visitDate;
	private String patientName;
	private String historyNumber;
	private String reasonAssessment;
	private String physicalExamination;
	
	// TAGS
	private static final String TAG_PATIENT 			= "Paciente:";
	private static final String TAG_HISTORY_NUM			= "Documento Nº:";
	private static final String TAG_BIRTH_DATE			= "Fecha Nac.:";
	private static final String TAG_ALLERGIES			= "Alergias:";
	private static final String TAG_MEDICAL_HISTORY 	= "Antecedentes personales:";
	private static final String TAG_FAMILY_HISTORY 		= "Antecedentes Familiares:";
	private static final String TAG_MEDICATION 			= "Prescripciones y Ordenes Médicas:";
	private static final String TAG_SOCIAL_HISTORY 		= "Otros Antecedentes y Hábitos:";
	private static final String TAG_PATIENT_NOTES 		= "NOTAS PACIENTE:";
	private static final String TAG_VISIT_START 		= "Evolución - Visitas:";
	private static final String TAG_VISIT_DATE 			= "FECHA EXPLORACIÓN:";
	private static final String TAG_VISIT_NUM 			= "N. EPISODIO:";
	private static final String TAG_VISIT_DOCTOR 		= "DOCTOR:";
	private static final String TAG_REASON_ASSESSMENT	= "Motivo de Consulta y Enfermedad Actual:";
	private static final String TAG_EXAMINATION 		= "Examen Físico:";
	private static final String TAG_ASSESSMENT 			= "DIAGNÓSTICO:";
	private static final String TAG_PLAN		 		= "TRATAMIENTO:";
	private static final String TAG_NOTES 				= "NOTAS:";
	
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
	
	public MedFileSerializerHandler() {
//		serializer = Xml.newSerializer();
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance(
			System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			serializer = factory.newSerializer();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		
		isFirstVisit = true;
	}
	
	// Converts TXT file to CDA file
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
			String multipleLines = "";
			boolean addLines = false;
			boolean visits = false;

			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				// Remove whitespaces and end of lines
				line = line.trim().replaceAll("/n", "");
				if( (!"".equals(line)) && (line != null) ) {
					if (line.startsWith(TAG_REASON_ASSESSMENT) || line.startsWith(TAG_SOCIAL_HISTORY) || line.startsWith(TAG_FAMILY_HISTORY) || line.startsWith(TAG_EXAMINATION) || line.startsWith(TAG_MEDICATION) || addLines) {
						addLines = true;
						if("".equals(multipleLines))
							multipleLines = line;
						else
							multipleLines += "\n" + line;
					} else if(line.startsWith(TAG_VISIT_START)) {
						visits = true;
						addLines = true;
						if("".equals(multipleLines))
							multipleLines = line;
						else
							multipleLines += "\n" + line;
					} else {
						multipleLines = "";
						process(line);
					}
					if(isValidFormat("dd/MM/yyyy", line) && visitDate==null) {
						try {
							visitDate = new SimpleDateFormat("dd/MM/yyyy").parse(line);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					
				}
				if( ("".equals(line) && addLines && !visits) || (line.startsWith(TAG_MEDICATION) && visits)) {
					addLines = false;
					visits = false;
					process(multipleLines);
					multipleLines = "";
				}
			}
			scan.close();
			is.close();
			/*END VISIT BLOCK*/
//				serializer.endTag(namespace, "section");
//				serializer.endTag(namespace, "component");
//				/*END EPISODE BLOCK*/
//				serializer.endTag(namespace, "section");
//				serializer.endTag(namespace, "component");
			/*END BODY DOCUMENT*/
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
		} else if (line.startsWith(TAG_HISTORY_NUM)) {
			line = line.replace(TAG_HISTORY_NUM, "").trim();
			historyNumber = line;
		} else if (line.startsWith(TAG_BIRTH_DATE)) {
			line = line.replace(TAG_BIRTH_DATE, "").trim();
			addPatient(namespace, line);
			/*START BODY DOCUMENT*/
			serializer.startTag(namespace, "component");
			serializer.startTag(namespace, "structuredBody");
		} else if (line.startsWith(TAG_ALLERGIES)) {
			line = line.replace(TAG_ALLERGIES, "").trim();
			addAllergies(namespace, line);
		} else if (line.startsWith(TAG_MEDICAL_HISTORY)) {
			line = line.replace(TAG_MEDICAL_HISTORY, "").trim();
			addMedicalHistory(namespace, line);
		} else if (line.startsWith(TAG_FAMILY_HISTORY)) {
			line = line.replace(TAG_FAMILY_HISTORY, "").trim();
			addFamilyHistory(namespace, line);
		} else if (line.startsWith(TAG_MEDICATION)) {
			line = line.replace(TAG_MEDICATION, "").trim();
			addMedications(namespace, line);
		} else if (line.startsWith(TAG_SOCIAL_HISTORY)) {
			line = line.replace(TAG_SOCIAL_HISTORY, "").trim();
			addSocialHistory(namespace, line);
		} else if (line.startsWith(TAG_PATIENT_NOTES)) {
			line = line.replace(TAG_PATIENT_NOTES, "").trim();
			//NOT PROCESSED
		} else if (line.startsWith(TAG_VISIT_START)) {
			line = line.replace(TAG_VISIT_START, "").trim();
			
			/*START EPISODE BLOCK*/
			serializer.startTag(namespace, "component");
			serializer.startTag(namespace, "section");
			serializer.startTag(namespace, "code");
	    	serializer.attribute(namespace, "code", CODE_ILLNESS_HISTORY);
	    	serializer.attribute(namespace, "codeSystem", CODE_SYSTEM);
	    	serializer.attribute(namespace, "codeSystemName", CODE_SYSTEM_NAME);
	    	serializer.endTag(namespace, "code");
	    	serializer.startTag(namespace, "title");
	    	serializer.text("History of Present Illness");
	    	serializer.endTag(namespace, "title");
	    	
	    	/*START VISIT BLOCK */
	    	addVisitStart(namespace);
	    	
	    	serializer.startTag(namespace,"id");
	    	serializer.attribute(namespace, "extension", "1");
	        serializer.attribute(namespace, "root", "2.2.2.2.2.0.0.0.1");
	    	serializer.endTag(namespace,"id");
	    	
	    	addReasonAssessment(namespace, reasonAssessment);
	    	
	    	addPhysicalExamination(namespace, physicalExamination);
	    	
	    	addVisitAuthor(namespace, "");
	    	
	    	/*END VISIT BLOCK*/
	    	serializer.endTag(namespace, "section");
			serializer.endTag(namespace, "component");
			
			String[] visits = line.split("\\d{2}/\\d{2}/\\d{4}");
			for(String visit:visits) {
				if(!"".equals(visit)) {
				
					try {
						Integer startPosition = line.indexOf(visit);
						String newVisitDate = line.substring(startPosition-10, startPosition);
						visitDate = new SimpleDateFormat("dd/MM/yyyy").parse(newVisitDate);
						
						/*START VISIT BLOCK */
				    	addVisitStart(namespace);
				    	
				    	serializer.startTag(namespace,"id");
				    	serializer.attribute(namespace, "extension", "2");
				        serializer.attribute(namespace, "root", "2.2.2.2.2.0.0.0.1");
				    	serializer.endTag(namespace,"id");
				    	
				    	addReasonAssessment(namespace, visit);
				    	
				    	addVisitAuthor(namespace, "");
				    	
				    	/*END VISIT BLOCK*/
				    	serializer.endTag(namespace, "section");
						serializer.endTag(namespace, "component");
						
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			
			}
			
	    	/*END EPISODE BLOCK*/
	    	serializer.endTag(namespace, "section");
			serializer.endTag(namespace, "component");
			
			
		} else if (line.startsWith(TAG_VISIT_NUM)) {
			line = line.replace(TAG_VISIT_NUM, "").trim();
			serializer.startTag(namespace,"id");
	    	serializer.attribute(namespace, "extension", line);
	        serializer.attribute(namespace, "root", "2.2.2.2.2.0.0.0.1");
	    	serializer.endTag(namespace,"id");
	    	
		} else if (line.startsWith(TAG_VISIT_DOCTOR)) {
			line = line.replace(TAG_VISIT_DOCTOR, "").trim();
			addVisitAuthor(namespace, line);	    	
		} else if (line.startsWith(TAG_REASON_ASSESSMENT)) {
			line = line.replace(TAG_REASON_ASSESSMENT, "").trim();
			reasonAssessment = line;
			//addReasonAssessment(namespace, line);
		} else if (line.startsWith(TAG_EXAMINATION)) {
			line = line.replace(TAG_EXAMINATION, "").trim();
			physicalExamination = line;
			//addPhysicalExamination(namespace, line);
		} else if (line.startsWith(TAG_ASSESSMENT)) {
			line = line.replace(TAG_ASSESSMENT, "").trim();
			addAssessment(namespace, line);
		} else if (line.startsWith(TAG_PLAN)) {
			line = line.replace(TAG_PLAN, "").trim();
			addPlan(namespace, line);
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
    	String name = patientName.substring(patientName.indexOf(", ")+1);
    	serializer.text(name);
    	serializer.endTag(namespace, "given");
    	serializer.startTag(namespace, "family");
    	String surname = patientName.substring(0, patientName.indexOf(", "));
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
	
	public static boolean isValidFormat(String format, String value) {
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
	
	private class Section {
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
