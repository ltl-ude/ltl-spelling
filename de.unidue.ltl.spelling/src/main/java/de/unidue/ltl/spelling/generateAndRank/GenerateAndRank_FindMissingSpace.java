package de.unidue.ltl.spelling.generateAndRank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.unidue.ltl.spelling.types.StartOfSentence;

/**
 * Generates candidates based on inserting a space into a token.
 */

public class GenerateAndRank_FindMissingSpace extends CandidateGeneratorAndRanker {

	/**
	 * The dictionaries based on which to generate the correction candidates.
	 */
	public static final String PARAM_MIN_WORD_LENGTH = "minWordLength";
	@ConfigurationParameter(name = PARAM_MIN_WORD_LENGTH, mandatory = true, defaultValue = "3")
	protected int minWordLength;

	// TODO: should this be accessible?
	private final int spaceCost = 4;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionaries(dictionaries);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {

			Map<Float, List<String>> rankedCandidates = new TreeMap<Float, List<String>>();
			String currentWord = anomaly.getCoveredText();
			Boolean isSentenceBeginning = !JCasUtil.selectCovered(StartOfSentence.class, anomaly).isEmpty();

			Map<Integer, Map<Integer, String>> wordsInWord = findWordsInWord(currentWord, isSentenceBeginning);
			Map<String, Integer> wordsSoFar = new HashMap<String, Integer>();
			Set<String> solutionSet = new HashSet<String>();
			Map<Integer, String> startWordsFromMap = wordsInWord.get(0);
			if (startWordsFromMap == null) {
				continue;
			} else {
				for (Integer key : startWordsFromMap.keySet()) {
					wordsSoFar.put(startWordsFromMap.get(key), key);
				}
				while (wordsSoFar.size() > 0) {
					Map<String, Integer> newWords = new HashMap<String, Integer>();
					for (String word : wordsSoFar.keySet()) {
						Map<String, Integer> results = processWord(wordsInWord, currentWord, word,
								wordsSoFar.get(word));
						if (results == null) {
							// Word could not be completed, ends here
						} else if (results.size() == 0) {
							solutionSet.add(word);
						} else {
							for (String key : results.keySet()) {
								newWords.put(key, results.get(key));
							}
						}
					}
					wordsSoFar = newWords;
				}
			}

			for (String solution : solutionSet) {
				float cost = spaceCost * StringUtils.countMatches(solution, " ");
				List<String> entriesForThisCost = rankedCandidates.get(cost);
				if (entriesForThisCost == null) {
					rankedCandidates.put(cost * 1.0f, new ArrayList<String>());
					entriesForThisCost = rankedCandidates.get(cost);
				}
				entriesForThisCost.add(solution);
			}
			SuggestionCostTuples tuples = getSuggestionCostTuples(rankedCandidates.entrySet().iterator());
			addSuggestedActions(aJCas, anomaly, tuples);
		}
	}

	private Map<String, Integer> processWord(Map<Integer, Map<Integer, String>> wordsInWord, String fullWord,
			String wordSoFar, int endIndex) {

		Map<String, Integer> wordMap = new HashMap<String, Integer>();
		if ((endIndex) == fullWord.length()) {
			// Word is done
			return wordMap;
		}
		Map<Integer, String> startMap = wordsInWord.get(endIndex);
		if (startMap == null) {
			return null;
		} else {
			for (Integer key : startMap.keySet()) {
				wordMap.put(wordSoFar + " " + startMap.get(key), key);
			}
			return wordMap;
		}
	}

	private Map<Integer, Map<Integer, String>> findWordsInWord(String word, boolean isBeginningOfSentence) {
		Map<Integer, Map<Integer, String>> indexMap = new HashMap<Integer, Map<Integer, String>>();
		for (int i = 0; i <= word.length() - minWordLength; i++) {
			for (int j = i + minWordLength; j <= word.length(); j++) {
				if (dictionary.contains(word.substring(i, j)) || (i == 0 && isBeginningOfSentence
						&& dictionary.contains(word.toLowerCase().substring(i, j)))) {
					Map<Integer, String> subMap = indexMap.get(i);
					if (subMap == null) {
						indexMap.put(i, new HashMap<Integer, String>());
						subMap = indexMap.get(i);
					}
					subMap.put(j, word.substring(i, j));
				}
			}
		}
		return indexMap;
	}
}