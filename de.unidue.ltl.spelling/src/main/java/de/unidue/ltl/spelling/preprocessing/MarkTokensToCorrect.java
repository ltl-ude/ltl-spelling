package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.KnownWord;
import de.unidue.ltl.spelling.types.TokenToConsider;

/**
 * Marks Tokens as SpellingAnomalies if they are a TokenToConsider and have not
 * been marked as a KnownWord.
 */

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
		"de.unidue.ltl.spelling.types.TokenToConsider", "de.unidue.ltl.spelling.types.KnownWord" }, outputs = {
				"de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly" })

public class MarkTokensToCorrect extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (Token token : JCasUtil.select(aJCas, Token.class)) {

			if (JCasUtil.selectCovered(TokenToConsider.class, token).size() > 0
					&& JCasUtil.selectCovered(KnownWord.class, token).size() == 0) {
				ExtendedSpellingAnomaly anomaly = new ExtendedSpellingAnomaly(aJCas);
				anomaly.setBegin(token.getBegin());
				anomaly.setEnd(token.getEnd());
				anomaly.setCorrected(false);
				anomaly.setMisspelledTokenText(token.getCoveredText());
				anomaly.addToIndexes();
				System.out.println("Marked as SpellingAnomaly:\t" + token.getCoveredText());
			}
		}
	}
}