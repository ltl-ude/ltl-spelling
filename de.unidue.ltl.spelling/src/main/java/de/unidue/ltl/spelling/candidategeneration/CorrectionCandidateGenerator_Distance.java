package de.unidue.ltl.spelling.candidategeneration;

import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class CorrectionCandidateGenerator_Distance extends CorrectionCandidateSelector {
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		// Certainty of SuggestedActions is inverted Distance: higher -> less distance
		maximize = true;
	}

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction currentSuggestion) {
		return currentSuggestion.getCertainty();
	}
}