package de.unidue.ltl.spelling.normalization;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.dkpro.core.api.transform.JCasTransformerChangeBased_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.StartOfSentence;
import de.unidue.ltl.spelling.types.SuggestedActionWithOrigin;

/**
 * In case of multiple candidates with an equally well score, pick the one that
 * came up first
 */

public class SpellingAnomalyReplacer extends JCasTransformerChangeBased_ImplBase {

	@Override
	public void process(JCas aInput, JCas aOutput) throws AnalysisEngineProcessException {
		for (ExtendedSpellingAnomaly anomaly : select(aInput, ExtendedSpellingAnomaly.class)) {
			
			if (anomaly.getSuggestions() != null) {
				
				FSArray suggestions = anomaly.getSuggestions();
				Float minCost = Float.MAX_VALUE;
				Map<String,String> bestReplacements = new TreeMap<String,String>();

				for (int i = 0; i < suggestions.size(); i++) {
					SuggestedAction action = anomaly.getSuggestions(i);
					Float cost = action.getCertainty();
					if (cost < minCost) {
						bestReplacements.clear();
						minCost = cost;
						SuggestedActionWithOrigin suggestedAction = (SuggestedActionWithOrigin) action;
						bestReplacements.put(suggestedAction.getReplacement(),suggestedAction.getMethodThatGeneratedThisSuggestion());
					} else if (Math.abs(cost - minCost) < 0.00001) {
						SuggestedActionWithOrigin suggestedAction = (SuggestedActionWithOrigin) action;
						bestReplacements.put(suggestedAction.getReplacement(),suggestedAction.getMethodThatGeneratedThisSuggestion());
					}
				}

				Entry<String,String> replacementEntry = getBestReplacement(bestReplacements);
				String replacement = replacementEntry.getKey();
				if (replacement != null) {
					if (!JCasUtil.selectCovered(StartOfSentence.class, anomaly).isEmpty()) {
						replacement = replacement.substring(0, 1).toUpperCase() + replacement.substring(1);
					}
					System.out.println(
							"Replacing anomaly:\t'" + anomaly.getCoveredText() + "'\twith\t'" + replacement + "' (cost: "+minCost+").");
					replace(anomaly.getBegin(), anomaly.getEnd(), replacement);
					anomaly.setCorrected(true);
					anomaly.setMethodThatGeneratedTheCorrection(replacementEntry.getValue());
				}
			}
		}
	}

	protected Entry<String, String> getBestReplacement(Map<String, String> replacements) {
		try {
			return replacements.entrySet().iterator().next();
		}
		catch(IndexOutOfBoundsException e) {
			System.err.println("Cost of all correction candidates is infinite, not choosing any.");
			return null;
		}
	}
}