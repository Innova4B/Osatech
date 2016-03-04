package com.innova4b.osatech.cda;

import java.io.IOException;
import java.io.InputStream;

import com.innova4b.osatech.cda.OsakidetzaSerializerHandler.Section;

public interface SerializerHandler {
	
	/**
	* Convierte un input stream en un fichero XML con formato CDA
	* @param is InputStream con el archivo a importar
	* @return String Cadena con el xml generado
	*/
	public String serialize(InputStream is)
			throws Exception;
	
	/**
	* Parsea una linea en busca de tags
	* @param line Linea a parsear
	*/
	public void process(String line)
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de información del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addInfo(String namespace)
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque del Paciente del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addPatient(String namespace, String birthDate)
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de alergias del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addAllergies(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de Antecedentes personales del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addMedicalHistory(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de Antecedentes familiares del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addFamilyHistory(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de medicamentos del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addMedications(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de Habitos del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addSocialHistory(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de visitas del CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addVisitStart(String namespace) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque del Médico de una visita en el CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addVisitAuthor(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque del Anamnesis en una visita en el CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addReasonAssessment(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque de la Exploración en una visita en el CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addPhysicalExamination(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque del Diagnóstico en una visita en el CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addAssessment(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;
	
	/**
	* Genera el bloque del Tratamiento en una visita en el CDA
	* @param namespace Namespace del XML a generar
	*/
	public void addPlan(String namespace, String text) 
			throws IllegalArgumentException, IllegalStateException, IOException;

}
