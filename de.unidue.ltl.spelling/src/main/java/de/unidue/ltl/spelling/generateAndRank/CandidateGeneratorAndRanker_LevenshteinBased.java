package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

/**
 * Supertype for generate and rank methods based on levenshtein implementations
 */

public abstract class CandidateGeneratorAndRanker_LevenshteinBased extends CandidateGeneratorAndRanker {
	
	/**
	 * Whether to permit transposition as a modification operation, e.g. apply
	 * Damerau-Levenshtein distance as opposed to standard Levenshtein Distance.
	 */
	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "True")
	protected boolean includeTransposition;

	protected Map<Integer, Set<String>> sortedDictionary;

	@Override
	protected void readDictionaries(String[] dictionaries) {
		sortedDictionary = new HashMap<Integer, Set<String>>();
		for (String path : dictionaries) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(path)));
				while (br.ready()) {
					String word = br.readLine();
					int lengthOfCurrentWord = word.length();
					Set<String> wordsOfThisLength = sortedDictionary.get(lengthOfCurrentWord);
					if (wordsOfThisLength == null) {
						sortedDictionary.put(lengthOfCurrentWord, new HashSet<String>());
						wordsOfThisLength = sortedDictionary.get(lengthOfCurrentWord);
					}
					wordsOfThisLength.add(word);
				}
				br.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {
			String misspelling = getStringToCorrectFromAnomaly(anomaly);
			int lengthOfMisspelling = getLengthOfMisspelling(misspelling);
			Map<Float, List<String>> rankedCandidates = new TreeMap<Float, List<String>>();

			// First: only take words of same length as candidate
			calculateCostsForDict(sortedDictionary.get(lengthOfMisspelling), misspelling, rankedCandidates);

			// Second: see what the max cost is to fill the number of required candidates
			// Repeat cost calculation with candidates +- this length
			Iterator<Entry<Float, List<String>>> entries = rankedCandidates.entrySet().iterator();
			int testNumberOfCandidates = 0;
			float maxCostRequiredToFillCandidates = 0;
			while (testNumberOfCandidates < numberOfCandidatesToGenerate) {
				if (entries.hasNext()) {
					Entry<Float, List<String>> entry = entries.next();
					testNumberOfCandidates += entry.getValue().size();
					maxCostRequiredToFillCandidates = entry.getKey();
				} else {
					break;
				}
			}
			Set<String> subDict = new HashSet<String>();
			int plusMinusLength = (int) maxCostRequiredToFillCandidates;
			for (int i = -plusMinusLength; i <= plusMinusLength; i++) {
				if (i != 0) {
					if (lengthOfMisspelling + i > 0) {
						Set<String> entriesWithThisLength = sortedDictionary.get(lengthOfMisspelling + i);
						if (entriesWithThisLength != null) {
							subDict.addAll(entriesWithThisLength);
						}
					}
				}
			}
			calculateCostsForDict(subDict, misspelling, rankedCandidates);

			// Select the top n candidates
			entries = rankedCandidates.entrySet().iterator();
			SuggestionCostTuples tuples = getSuggestionCostTuples(entries);
			addSuggestedActions(aJCas, anomaly, tuples);
		}
	}

	// To accommodate phonetic Levenshtein
	protected String getStringToCorrectFromAnomaly(SpellingAnomaly anomaly) {
		return anomaly.getCoveredText();
	}

	protected int getLengthOfMisspelling(String misspelling) {
		return misspelling.length();
	}

	private void calculateCostsForDict(Set<String> dictionary, String misspelling,
			Map<Float, List<String>> rankedCandidates) {
		for (String word : dictionary) {
			float cost = calculateCost(misspelling, word);
			List<String> rankList = rankedCandidates.get(cost);
			if (rankList == null) {
				rankedCandidates.put(cost, new ArrayList<String>());
				rankList = rankedCandidates.get(cost);
			}
			rankList.add(word);
		}
	}

	protected abstract float calculateCost(String misspelling, String correction);
}