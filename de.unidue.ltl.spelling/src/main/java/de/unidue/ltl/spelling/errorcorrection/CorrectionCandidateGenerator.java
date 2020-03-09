package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import com.github.liblevenshtein.collection.dictionary.SortedDawg;
import com.github.liblevenshtein.transducer.Algorithm;
import com.github.liblevenshtein.transducer.Candidate;
import com.github.liblevenshtein.transducer.ITransducer;
import com.github.liblevenshtein.transducer.factory.TransducerBuilder;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

public abstract class CorrectionCandidateGenerator extends JCasAnnotator_ImplBase{
	
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;
	
	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES,
			mandatory = false)
	protected String[] dictionaries;
	
	public static final String PARAM_MODEL_ENCODING = ComponentParameters.PARAM_MODEL_ENCODING;
	@ConfigurationParameter(name = PARAM_MODEL_ENCODING, mandatory = false, defaultValue = "UTF-8")
	protected String dictEncoding;
	
	public static final String PARAM_SCORE_THRESHOLD = "ScoreThreshold";
	@ConfigurationParameter(name = PARAM_SCORE_THRESHOLD, mandatory = true, defaultValue = "1")
	protected int scoreThreshold;
	
	public static final String PARAM_METHOD = "candidateSelectionMethod";
	@ConfigurationParameter(name = PARAM_METHOD, mandatory = false, defaultValue = "LEVENSHTEIN_UNIFORM")
	protected CandidateSelectionMethod candidateSelectionMethod;
	
	public enum CandidateSelectionMethod {
		LEVENSHTEIN_UNIFORM, KEYBOARD_DISTANCE, PHONETIC
	}
	
	protected final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US.txt";
	protected final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE_unmunched.txt";
	
	ITransducer<Candidate>[] transducers;
	SortedDawg dictionary = null;
	
	Map<String, Integer> insert = new HashMap<String, Integer>();
	Map<String, Integer> delete = new HashMap<String, Integer>();
	//Outer map: char to replace; inner map: replacement, mapped to respective cost
	Map<Character, Map<Character,Integer>> substitute = new HashMap<Character, Map<Character, Integer>>();
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		
		transducers = new ITransducer[this.scoreThreshold];	
		Set<String> dictionarySet = new HashSet<String>();
		
		initializeDefaultDictionary(dictionarySet);
		readAdditionalDictionaries(dictionarySet);
		
		List<String> dictionaryList = new ArrayList<String>();
		dictionaryList.addAll(dictionarySet);
		dictionaryList.sort(null);
			
		dictionary = new SortedDawg();
		dictionary.addAll(dictionaryList);

		//TOOD: use standard or include transposing?
		for (int i = 1; i<= this.scoreThreshold; i++){
			ITransducer<Candidate> transducer = new TransducerBuilder()
					.dictionary(dictionary)
					.algorithm(Algorithm.STANDARD)
					.defaultMaxDistance(i)
					.includeDistance(true)
					.build();
			transducers[i-1] = transducer;
		}

		//TODO: init cost matrixes
		

	}
	
	private void initializeDefaultDictionary(Set<String> dictionarySet) {
		try {
			BufferedReader br = null;
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
				dictionarySet.add(br.readLine());
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readAdditionalDictionaries(Set<String> dictionarySet) {
		BufferedReader br = null;
		for (String path : dictionaries) {
			try {
				br = new BufferedReader(new FileReader(new File(path)));
				while (br.ready()) {
					dictionarySet.add(br.readLine());
				}
				br.close();
			} catch (FileNotFoundException e1) {
				getContext().getLogger().log(Level.WARNING,
	                    "Custom dictionary not found.");
				e1.printStackTrace();
			}
			catch (IOException e) {
				getContext().getLogger().log(Level.WARNING,
	                    "Unable to read custom dictionary.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {
				
				String tokenText = anomaly.getCoveredText();
				SuggestionCostTuples tuples = new SuggestionCostTuples();
				
				// merged words: if we split the word, both subparts exist in the lexicon
				for (int i = 0; i<tokenText.length(); i++){
					String word1 = tokenText.substring(0, i);
					String word2 = tokenText.substring(i, tokenText.length());
					if (dictionary.contains(word1) && dictionary.contains(word2)){
		//				System.out.println("Found\t"+tokenText+"\t"+word1+"\t"+word2);
						tuples.addTuple(word1+" "+word2, 1);
						break;
					}
				}
				
				// TODO: wir müssen Fälle wie "1.you" anders aufsplitten
				if (tokenText.contains(".")){
					String[] parts = tokenText.split("\\.");
					boolean allFound = true;
					for (String part : parts){
						if (!dictionary.contains(part) && (!(part.matches("\\d")))){
							allFound = false;
							break;
						}
					}
			//		System.out.println(tokenText+"\t"+allFound);
					if (allFound){
						String replacement = String.join(" . ", parts);
						replacement = replacement.replaceAll("(\\d) ", "$1");
						tuples.addTuple(replacement, parts.length-1);
					}
				}
				
				if (tokenText.contains(":")){
					String[] parts = tokenText.split(":");
					boolean allFound = true;
					for (String part : parts){
						if (!dictionary.contains(part) && (!(part.matches("\\d")))){
							allFound = false;
							break;
						}
					}
			//		System.out.println(tokenText+"\t"+allFound);
					if (allFound){
						String replacement = String.join(" : ", parts);
						replacement = replacement.replaceAll("(\\d) ", "$1");
						tuples.addTuple(replacement, parts.length-1);
					}
				}
				
				
				for (ITransducer<Candidate> it : transducers){	
					for (Candidate candidate : it.transduce(tokenText.toLowerCase())){
						String suggestionString = candidate.term();
						int cost;
						if(candidateSelectionMethod == CandidateSelectionMethod.LEVENSHTEIN_UNIFORM) {
							cost = candidate.distance();
						}
						else {
							cost = calculateCosts(tokenText,suggestionString);
						}
						tuples.addTuple(suggestionString, cost);
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
	
	public int calculateCosts (CharSequence lhs, CharSequence rhs) {                          
	    int len0 = lhs.length() + 1;                                                     
	    int len1 = rhs.length() + 1;                                                     
	                                                                                    
	    // the array of distances  
	    // cost: referring to s0
	    // newcost: referring to s1
	    int[] cost = new int[len0];                                                     
	    int[] newcost = new int[len0];                                                  
	                                                                                    
	    // initial cost of skipping prefix in String s0       
	    for (int i = 0; i < len0; i++) cost[i] = i;                                     
	                                                                                    
	    // dynamically computing the array of distances                                  
	                                                                                    
	    // transformation cost for each letter in s1                                    
	    for (int j = 1; j < len1; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	                                                                                    
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < len0; i++) {                                             
	            // matching current letters in both strings                             
	            int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;             
	                                                                                    
	            // computing cost for each transformation     
	            // if the chars do not match: look up substitution cost
	            int cost_replace = cost[i - 1] + match*substitute.get(lhs.charAt(i-1)).get(rhs.charAt(j-1));                                 
	            int cost_insert  = cost[i] + insert.get(rhs.charAt(j-1));                                         
	            int cost_delete  = newcost[i - 1] + delete.get(lhs.charAt(i-1));                                  
	                                                                                    
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	                                                                                    
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	                                                                                    
	    // the distance is the cost for transforming all letters in both strings        
	    return cost[len0 - 1];                                                          
	}
	
	//To be called by process to determine candidates; can be implemented here
	public void generateCandidates(int maxDistance, Set<String> dictionary) {
		
		
	}
	
	private class SuggestionCostTuple {
		private final String suggestion;
		private final Integer cost;

		public SuggestionCostTuple(String suggestion, Integer cost)
		{
			this.suggestion = suggestion;
			this.cost = cost;
		}

		public String getSuggestion()
		{
			return suggestion;
		}

		public float getCertainty(int maxCost)
		{
			if (maxCost > 0) {
				return (float) maxCost / cost;
			}
			else {
				return 0f;
			}
		}
	}
	
	class SuggestionCostTuples implements Iterable<SuggestionCostTuple> {
		private final List<SuggestionCostTuple> tuples;
		private int maxCost;

		public SuggestionCostTuples()
		{
			tuples = new ArrayList<SuggestionCostTuple>();
			maxCost = 0;
		}

		public void addTuple(String suggestion, int cost) {
			tuples.add(new SuggestionCostTuple(suggestion, cost));

			if (cost > maxCost) {
				maxCost = cost;
			}
		}

		public int getMaxCost() {
			return maxCost;
		}

		public int size() {
			return tuples.size();
		}

//		@Override
		public Iterator<SuggestionCostTuple> iterator()
		{
			return tuples.iterator();
		}
	}

}
