package de.unidue.ltl.spelling.resources;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

public abstract class LanguageModelResource extends Resource_ImplBase{
	
	public static final String PARAM_MODEL_FILES = "modelFiles";
	@ConfigurationParameter(name = PARAM_MODEL_FILES, mandatory = true)
	protected String modelFile;
    
    // Should return normalized token frequency (probability) 
    public abstract double getFrequency(String[] ngram);
  
}