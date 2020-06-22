package de.unidue.ltl.spelling.candidategeneration;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public abstract class CorrectionCandidateSelector extends JCasAnnotator_ImplBase {

	// Indicating whether a specific selector class calls for maximizing or
	// minimizing the values assigned to correction candidates
	protected boolean maximize;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {
			narrowDownSuggestions(aJCas, anomaly);
		}
	}

	protected void narrowDownSuggestions(JCas aJCas, SpellingAnomaly anomaly) {
		double bestValue = Integer.MIN_VALUE;
		if (!maximize) {
			bestValue = Integer.MAX_VALUE;
		}
		List<SuggestedAction> bestSuggestions = new ArrayList<SuggestedAction>();

		if (anomaly.getSuggestions() != null) {
			for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
				SuggestedAction currentSuggestion = anomaly.getSuggestions(i);
				double currentValue = getValue(aJCas, anomaly, currentSuggestion);

				// Case 1: suggestion is worse: no need to do anything
				if ((currentValue < bestValue && maximize) || (currentValue > bestValue && !maximize)) {

				}
				// Case 2: suggestion is better: replace previously best suggestions
				else if ((currentValue > bestValue && maximize) || (currentValue < bestValue && !maximize)) {
					bestSuggestions.clear();
					bestSuggestions.add(currentSuggestion);
					bestValue = currentValue;
				}
				// Case 3: suggestion is equal: add to current best suggestions
				else {
					bestSuggestions.add(currentSuggestion);
				}
			}
			FSArray actions = new FSArray(aJCas, bestSuggestions.size());
			for (int i = 0; i < actions.size(); i++) {
				actions.set(i, bestSuggestions.get(i));
			}

			// Replace suggestions with new, reduced set
			anomaly.getSuggestions().removeFromIndexes();
			anomaly.setSuggestions(actions);
		}
	}

	protected abstract double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action);

}