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
    public boolean initialize(ResourceSpecifier aSpecifier, Map additionalParams)
        throws ResourceInitializationException
    {
        if (!super.initialize(aSpecifier, additionalParams)) {
            return false;
        }  
        System.out.println("Initialize LM: "+modelFile);
        try {
			web1tSearcher = new JWeb1TSearcher(new File(modelFile),1,1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("unigram count: "+web1tSearcher.getNrOfDistinctNgrams(1));
        return true;
    }
    
    //TODO: Should return normalized token frequency (probability) 
    public double getFrequency(String[] ngram){	
    	String ngramToQuery = String.join(" ", ngram);
    	double count = 0.0;
    	System.out.println("Attempting to get frequency of: "+ngramToQuery);
    	try {
			count = (web1tSearcher.getFrequency(ngramToQuery)*1.0);
    		System.out.println("Freuqency of "+ngramToQuery+": "+count);
			//TODO: This returns -1, something must be wrong with the web1t index files (ENGLISH)
//					/web1tSearcher.getNrOfDistinctNgrams(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//TODO: never return 0; set nonexisting ngrams to 1/ngramCount instead
    	if(count == 0.0) {
    		count = 1/10000;
    	}
    	return count;
    }

}
