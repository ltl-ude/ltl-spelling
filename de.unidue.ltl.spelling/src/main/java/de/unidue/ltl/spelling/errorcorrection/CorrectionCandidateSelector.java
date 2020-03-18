package de.unidue.ltl.spelling.errorcorrection;

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

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {
			narrowDownSuggestions(aJCas, anomaly);
		}
	}

//	public abstract void narrowDownSuggestions(JCas aJCas, SpellingAnomaly s);

	protected void narrowDownSuggestions(JCas aJCas, SpellingAnomaly anomaly) {
		Float highestValue = 0.0f;
		List<SuggestedAction> bestSuggestions = new ArrayList<SuggestedAction>();

		if (anomaly.getSuggestions() != null) {
			for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
				SuggestedAction currentSuggestion = anomaly.getSuggestions(i);
				Float currentValue = getValue(currentSuggestion);

				// Case 1: suggestion is worse
				if (currentValue < highestValue) {
					// No need to do anything
				}
				// Case 2: suggestion is better
				else if (currentValue > highestValue) {
					// Replace previously saved suggestions
					bestSuggestions.clear();
					bestSuggestions.add(currentSuggestion);
					highestValue = currentValue;
				}
				// Case 3: suggestion is equal
				else {
					// Add to currently best suggestions
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

	protected abstract float getValue(SuggestedAction action);

	protected void replaceSuggestions(JCas aJCas, SpellingAnomaly anomaly, List<SuggestedAction> bestSuggestions) {
		FSArray actions = new FSArray(aJCas, bestSuggestions.size());
		for (int i = 0; i < actions.size(); i++) {
			actions.set(i, bestSuggestions.get(i));
		}

		// Replace suggestions with new, reduced set
		anomaly.getSuggestions().removeFromIndexes();
		anomaly.setSuggestions(actions);
	}

}