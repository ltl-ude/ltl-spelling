package de.unidue.ltl.spelling.errorcorrection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ErrorDetector extends JCasAnnotator_ImplBase{
	
    public static final String PARAM_DICTIONARIES = "dictionaries";
    @ConfigurationParameter(name = PARAM_DICTIONARIES, mandatory = false,
    	defaultValue = {"dictionaries/de-testDict1.txt","dictionaries/de-testDict2.txt","dictionaries/en-testDict1.txt","dictionaries/en-testDict2.txt"})
    	private String[] dictionaries;
	
	
    public static final String PARAM_TYPES_TO_EXCLUDE = "typesToExclude";
    @ConfigurationParameter(name = PARAM_TYPES_TO_EXCLUDE, mandatory = false,
    	defaultValue = {"de.unidue.ltl.spelling.types.Numeric",
    			"de.unidue.ltl.spelling.types.Punctuation",
    			"de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity"})
    	private String[] typesToExclude;
    
    private Set<String> dictionaryWords = new HashSet<String>();
	
	//Initialize dictionaries
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException
    {
        super.initialize(context);

        try {
            readDictionaries();
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    };
    
    private void readDictionaries() throws IOException{
    	BufferedReader br = null;
    	for(String path:dictionaries) {
    		br = new BufferedReader(new FileReader(new File(path)));
    		while(br.ready()){
    			dictionaryWords.add(br.readLine());
    		}
    		br.close();
    	} 	
    }
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		boolean isCandidate = true;
		Collection<Token> tokens = JCasUtil.select(aJCas, Token.class);
		for(Token token : tokens) {
			isCandidate = true;
			for(String type : typesToExclude) {
				try {
					if(JCasUtil.contains(aJCas, token, (Class<? extends Annotation>) Class.forName(type))) {
						System.out.println("Marked as ignore: "+token.getCoveredText());
						isCandidate = false;
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					System.out.println("Failed to find class "+type);
					e.printStackTrace();
				}
			}
			//Check if present in dictionaries
			if(isCandidate) {
				if(!dictionaryWords.contains(token.getCoveredText())) {
					SpellingAnomaly spell = new SpellingAnomaly(aJCas);
					spell.setBegin(token.getBegin());
					spell.setEnd(token.getEnd());
					spell.addToIndexes();
					System.out.println("Found Error: "+token.getCoveredText());
				}
			}
			
		}
		
	}

}
