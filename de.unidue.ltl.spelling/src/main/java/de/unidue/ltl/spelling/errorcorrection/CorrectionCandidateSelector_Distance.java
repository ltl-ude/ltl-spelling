package de.unidue.ltl.spelling.errorcorrection;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class CorrectionCandidateSelector_Distance extends CorrectionCandidateSelector {

	@Override
	protected double getValue(JCas aJCas, String anomalyText, SuggestedAction currentSuggestion) {
		// Certainty of SuggestedActions is inverted Distance: higher -> less distance
		return currentSuggestion.getCertainty();
	}
}