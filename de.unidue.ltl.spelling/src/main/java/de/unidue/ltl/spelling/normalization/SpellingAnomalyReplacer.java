package de.unidue.ltl.spelling.normalization;

import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.transform.JCasTransformerChangeBased_ImplBase;

import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;

public class SpellingAnomalyReplacer extends JCasTransformerChangeBased_ImplBase {

	@Override
	public void process(JCas aInput, JCas aOutput) throws AnalysisEngineProcessException {
		for (ExtendedSpellingAnomaly anomaly : select(aInput, ExtendedSpellingAnomaly.class)) {

			if (anomaly.getSuggestions() != null) {

				if (anomaly.getSuggestions().size() > 0) {
					replace(anomaly.getBegin(), anomaly.getEnd(), anomaly.getSuggestions(0).getReplacement());
					anomaly.setCorrected(true);
				}
			}
		}
	}
}