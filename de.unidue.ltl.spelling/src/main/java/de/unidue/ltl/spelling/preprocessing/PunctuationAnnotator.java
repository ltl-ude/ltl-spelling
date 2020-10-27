package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.Punctuation;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Annotator matching and marking all tokens that are made up of nothing but
 * punctuation.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" }, outputs = {
		"de.unidue.ltl.spelling.types.Punctuation" })

public class PunctuationAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (Token token : JCasUtil.select(aJCas, Token.class)) {

			if (token.getCoveredText().matches("^[\\[\\];:!?\\.,=\\*\"'\\-´`'()<>\\+/\\\\]+$")) {
//				System.out.println("Found punctuation:\t" + token.getCoveredText());
				Punctuation punct = new Punctuation(aJCas);
				punct.setBegin(token.getBegin());
				punct.setEnd(token.getEnd());
				punct.addToIndexes();
			}
		}
	}
}