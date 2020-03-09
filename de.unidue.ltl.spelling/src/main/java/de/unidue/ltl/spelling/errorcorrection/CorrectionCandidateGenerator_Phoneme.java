package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.Level;

import com.github.liblevenshtein.transducer.Candidate;
import com.github.liblevenshtein.transducer.ITransducer;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.AnnotationChecker;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator.CandidateSelectionMethod;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator.SuggestionCostTuple;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator.SuggestionCostTuples;
import de.unidue.ltl.spelling.utils.PhonemeUtils;

public class CorrectionCandidateGenerator_Phoneme extends CorrectionCandidateGenerator{
	
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;
	
	protected final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US_phoneme_map.txt";
	protected final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt";
	
	protected Map<String,Set<String>> phoneme2grapheme = new HashMap<String,Set<String>>();
	
	protected String getTokenText(SpellingAnomaly spell){
		String lang = null;
		String result = null;
		if(language.contentEquals("de")) {
			lang = "deu-DE";
		}
		else if(language.contentEquals("en")) {
			lang = "eng-US";
		}
		else {
			System.out.println("Unknown language: "+language);
		}
		
		try {
			result = PhonemeUtils.getPhoneme(spell.getCoveredText(),lang);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	//Create map and derive dictionary from its keys
	protected void initializeDefaultDictionary(Set<String> dictionarySet) {
		String[] entries;
		try {
			BufferedReader br = null;
			Set<String> graphemes;
			if(language.contentEquals("de")) {
				br = new BufferedReader(new FileReader(new File(defaultDictDE)));
	
			}
			else if(language.contentEquals("en")) {
				br = new BufferedReader(new FileReader(new File(defaultDictEN)));
			}
			else {
				getContext().getLogger().log(Level.WARNING,
	                    "Unknown language '" + language
	                    + "' was passed, defaulting to English dictionary.");
				br = new BufferedReader(new FileReader(new File(defaultDictEN)));
			}
			while(br.ready()) {
				entries = br.readLine().split("\t");
				graphemes = phoneme2grapheme.get(entries[1]);
				if(graphemes == null) {
					phoneme2grapheme.put(entries[1], new HashSet<String>());
					graphemes = phoneme2grapheme.get(entries[1]);
				}
				graphemes.add(entries[0]);
			}
			br.close();
			
			dictionarySet.addAll(phoneme2grapheme.keySet());
			System.out.println("Phonetic dictionary size: "+dictionarySet.size());
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void readAdditionalDictionaries(Set<String> dictionarySet) {
	}
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), Token.class);
		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), SpellingAnomaly.class);
		
		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {
				
				String tokenText = getTokenText(anomaly);
				SuggestionCostTuples tuples = new SuggestionCostTuples();
				
				for (ITransducer<Candidate> it : transducers){	
					for (Candidate candidate : it.transduce(tokenText,scoreThreshold)){
						String suggestionString = candidate.term();
						int cost;
						if(candidateSelectionMethod == CandidateSelectionMethod.LEVENSHTEIN_DISTANCE) {
							cost = candidate.distance();
						}
						else {
							cost = calculateCosts(tokenText,suggestionString);
						}
						for(String grapheme : phoneme2grapheme.get(suggestionString)) {
							tuples.addTuple(grapheme, cost);
						}
					}
				}
				
				if (tuples.size() > 0) {
					FSArray actions = new FSArray(aJCas, tuples.size());
					int i=0;
				//	System.out.print(anomaly.getCoveredText()+"\t");
					for (SuggestionCostTuple tuple : tuples) {
						SuggestedAction action = new SuggestedAction(aJCas);
						action.setReplacement(tuple.getSuggestion());
						//TODO: This is where custom matrixes would take effect
						action.setCertainty(tuple.getCertainty(tuples.getMaxCost()));
						actions.set(i, action);
						i++;
						System.out.println(tokenText+"\t"+action.getReplacement()+"\t"+action.getCertainty());
					}
				//	System.out.println();
					anomaly.setSuggestions(actions);
				}
				else {
					System.out.println("No correction found for: "+tokenText);
				}
		}
		
	};

}
