package de.unidue.ltl.spelling.normalization;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.transform.JCasTransformerChangeBased_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;

public class SpellingAnomalyReplacer extends JCasTransformerChangeBased_ImplBase {

	@Override
	public void process(JCas aInput, JCas aOutput) throws AnalysisEngineProcessException {
		for (ExtendedSpellingAnomaly anomaly : select(aInput, ExtendedSpellingAnomaly.class)) {
			if (anomaly.getSuggestions() != null) {
				Float minValue = Float.MAX_VALUE;
				List<String> bestReplacements = new ArrayList<String>();
				for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
					SuggestedAction action = anomaly.getSuggestions(i);
					Float certainty = action.getCertainty();
					if (certainty < minValue) {
						bestReplacements.clear();
						minValue = certainty;
						bestReplacements.add(action.getReplacement());
					} else if (certainty == minValue) {
						bestReplacements.add(action.getReplacement());
					}
				}
				System.out.println(
						"Replacing anomaly:\t'" + anomaly.getCoveredText() + "'\twith\t'" + bestReplacements.get(0)+"'.");
				replace(anomaly.getBegin(), anomaly.getEnd(), bestReplacements.get(0));
				anomaly.setCorrected(true);
			}
		}
	}
}