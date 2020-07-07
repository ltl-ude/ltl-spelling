package de.unidue.ltl.spelling.generateAndRank;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

/**
 * Generates candidates based on inserting a space into a token.
 */

// TODO: include insertion of more than one whitespace?
public class GenerateAndRank_FindMissingSpace extends CandidateGeneratorAndRanker {

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
			SuggestionCostTuples tuples = new SuggestionCostTuples();

			String currentWord = anomaly.getCoveredText();
			String firstHalf = "";
			String secondHalf = "";

			for (int i = 0; i < currentWord.length() - 1; i++) {
				firstHalf = currentWord.substring(0, i + 1);
				secondHalf = currentWord.substring(i + 1);
				if (dictionary.contains(firstHalf) && dictionary.contains(secondHalf)) {
					tuples.addTuple(firstHalf + " " + secondHalf, spaceCost);
					System.out.println("Added SpellingAnomaly for:\t" + anomaly.getCoveredText()
							+ "\t(could be split into\t" + firstHalf + "\tand\t" + secondHalf + ")");
				}
			}
			// TODO: as long as we only allow one space to be inserted, the threshold of n
			// candidates to generate will not take effect
			if (tuples.size() > 0) {
				addSuggestedActions(aJCas, anomaly, tuples);
			}
		}
	}
	
	// Not required for this annotator
	@Override
	protected float calculateCost(String misspelling, String correction) {
		return 0;
	}
}