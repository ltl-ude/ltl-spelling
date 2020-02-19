package de.unidue.ltl.spelling.resources;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import com.googlecode.jweb1t.JWeb1TIndexer;
import com.googlecode.jweb1t.JWeb1TIterator;
import com.googlecode.jweb1t.JWeb1TSearcher;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;

public class LanguageModelResource extends Resource_ImplBase{
	
	public static final String PARAM_MODEL_FILE = "modelFile";
	@ConfigurationParameter(name = PARAM_MODEL_FILE, mandatory = true)
	private String modelFile;
	
//	private FrequencyDistribution<String> fd = new FrequencyDistribution<String>(); 
	private JWeb1TSearcher web1tSearcher;
//	private JWeb1TIterator web1tIterator;
	
    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
        throws ResourceInitializationException
    {
        if (!super.initialize(aSpecifier, aAdditionalParams)) {
            return false;
        }
        
        System.out.println("Initialize LM: "+modelFile);
        try {
			web1tSearcher = new JWeb1TSearcher(new File(modelFile),1,1);
//			web1tIterator = new JWeb1TIterator(modelFile,1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        System.out.println("unigram count: "+web1tSearcher.getNrOfDistinctNgrams(1));

        return true;
    }
    
    public long getFrequency(String token) {
    	
    	long count = 0;
    	try {
			count =  web1tSearcher.getFrequency(token);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return count;
    }
    
    public long getProbability(String sentence) {

    	return 0;
    }

}
