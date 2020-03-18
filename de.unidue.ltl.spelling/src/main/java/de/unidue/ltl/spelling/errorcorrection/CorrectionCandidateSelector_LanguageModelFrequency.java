package de.unidue.ltl.spelling.errorcorrection;

import org.apache.uima.fit.descriptor.ExternalResource;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.unidue.ltl.spelling.resources.LanguageModelResource;

public class CorrectionCandidateSelector_LanguageModelFrequency extends CorrectionCandidateSelector {

	public static final String PARAM_LANGUAGE_MODEL = "languageModel";
	@ExternalResource(key = PARAM_LANGUAGE_MODEL)
	private LanguageModelResource languageModel;

	@Override
	public float getValue(SuggestedAction currentSuggestion) {
		return languageModel.getFrequency(currentSuggestion.getReplacement());
	}

}
