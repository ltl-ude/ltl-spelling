package de.unidue.ltl.spelling.errorcorrection;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class CorrectionCandidateSelector_Distance extends CorrectionCandidateSelector {

	@Override
	protected float getValue(SuggestedAction currentSuggestion) {
		// Certainty of SuggestedActions is inverted Distance: higher -> less distance
		return currentSuggestion.getCertainty();
	}
}