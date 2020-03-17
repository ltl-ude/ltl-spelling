package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.transform.JCasTransformerChangeBased_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;

public abstract class CorrectionCandidateSelector extends JCasTransformerChangeBased_ImplBase {
	
	//To be able to use this for the double-purpose of reducing the candidate set with one method,
	//then determine the best replacement within it with a different method
	public static final String PARAM_CHOOSE_BEST = "chooseBest";
	@ConfigurationParameter(name = PARAM_CHOOSE_BEST, defaultValue = "false")
	private boolean chooseBest;

	@Override
	public void process(JCas aInput, JCas aOutput) throws AnalysisEngineProcessException {
		String replacement;
		for (ExtendedSpellingAnomaly anomaly : select(aInput, ExtendedSpellingAnomaly.class)) {
			replacement = null;
			replacement = getBestSuggestion(anomaly);
			if(replacement != null) {
				replace(anomaly.getBegin(), anomaly.getEnd(), replacement);
				anomaly.setCorrected(true);
			}
		}
	}

	public abstract String getBestSuggestion(SpellingAnomaly s);

}