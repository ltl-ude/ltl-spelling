package de.unidue.ltl.spelling.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.dkpro.core.api.frequency.util.FrequencyDistribution;

public class FrequencyDistributionLanguageModel extends LanguageModelResource{
	
	public static final String PARAM_NGRAM_SIZE = "ngramSize";
	@ConfigurationParameter(name = PARAM_NGRAM_SIZE, mandatory = true)
	private String ngramSize_String;
	
	//TODO: is there a better way to get this in here other than via serialization?
	//TODO: do we just assume that the ngram size corresponds to what was requested as selection size and warn if it is not?
	FrequencyDistribution<String> fd;
	//TODO: this should work with an int configuration parameter (but it didnt, hence the String workaround)
	int ngramSize;

    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map additionalParams)
        throws ResourceInitializationException
    {
        if (!super.initialize(aSpecifier, additionalParams)) {
            return false;
        }  
    	int ngramSize = Integer.parseInt(ngramSize_String);
        try
        {    
            FileInputStream file = new FileInputStream(modelFile); 
            ObjectInputStream in = new ObjectInputStream(file); 
            fd = (FrequencyDistribution<String>)in.readObject();           
            in.close(); 
            file.close();          
			getUimaContext().getLogger().log(Level.INFO, "Frequency distrbution '" + modelFile
					+ "' was succesfully deseralized.");
//			Check if fd has proposed size
			for(String key : fd.getKeys()) {
				if(key.split(" ").length != ngramSize ) {
					getUimaContext().getLogger().log(Level.WARNING, "You selected ngram size '" + ngramSize
							+ "', but provided a frequency distribution containing '"+key+"'.");
				}
			}
        } 
          
        catch(IOException e) 
        {  
            e.printStackTrace();
			getUimaContext().getLogger().log(Level.WARNING, "Unable to read frequency distribution from '" + modelFile
					+ "'.");
        } 
          
        catch(ClassNotFoundException e) 
        { 
            e.printStackTrace();
        } 
        return true;
    }
	
	@Override
	public double getFrequency(String[] ngram) {
		String ngramJoined = String.join(" ", ngram);
		double result = fd.getCount(ngramJoined)/(double)fd.getN();
		if(result == 0) {
			result = 1.0/fd.getN();
		}
		return result;
	}

}
