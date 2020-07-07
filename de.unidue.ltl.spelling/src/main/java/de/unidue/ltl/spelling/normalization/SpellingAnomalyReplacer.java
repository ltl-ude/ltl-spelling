package de.unidue.ltl.spelling.normalization;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.dkpro.core.api.transform.JCasTransformerChangeBased_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.StartOfSentence;

public class SpellingAnomalyReplacer extends JCasTransformerChangeBased_ImplBase {

	@Override
	public void process(JCas aInput, JCas aOutput) throws AnalysisEngineProcessException {
		for (ExtendedSpellingAnomaly anomaly : select(aInput, ExtendedSpellingAnomaly.class)) {
			if (anomaly.getSuggestions() != null) {
				FSArray suggestions = anomaly.getSuggestions();
				Float minCost = Float.MAX_VALUE;
				List<String> bestReplacements = new ArrayList<String>();

				for (int i = 0; i < suggestions.size(); i++) {
					SuggestedAction action = anomaly.getSuggestions(i);
					Float certainty = action.getCertainty();
					if (certainty < minCost) {
						bestReplacements.clear();
						minCost = certainty;
						bestReplacements.add(action.getReplacement());
					} else if (certainty == minCost) {
						bestReplacements.add(action.getReplacement());
					}
				}

				String replacement = bestReplacements.get(0);
				if (!JCasUtil.selectCovered(StartOfSentence.class, anomaly).isEmpty()) {
					replacement = replacement.substring(0, 1).toUpperCase() + replacement.substring(1);
				}
				System.out.println(
						"Replacing anomaly:\t'" + anomaly.getCoveredText() + "'\twith\t'" + replacement + "'.");
				replace(anomaly.getBegin(), anomaly.getEnd(), replacement);
				anomaly.setCorrected(true);
			}
		}
	}
}