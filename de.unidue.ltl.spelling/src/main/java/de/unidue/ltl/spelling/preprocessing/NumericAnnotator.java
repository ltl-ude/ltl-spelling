package de.unidue.ltl.spelling.preprocessing;

import java.util.Collection;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.Numeric;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Annotator matching and marking all tokens consisting of nothing but
 * numbers.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" }, outputs = {
		"de.unidue.ltl.spelling.types.Numeric" })

public class NumericAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		Collection<Token> tokens = JCasUtil.select(aJCas, Token.class);

		for (Token token : tokens) {
			if (token.getCoveredText().matches("[:0-9-\\+\\*\\.,=x/\\\\]*[0-9]+[:0-9-\\+\\*\\.,=x/\\\\]*")) {
				System.out.println("Found a number:\t" + token.getCoveredText());
				Numeric numeric = new Numeric(aJCas);
				numeric.setBegin(token.getBegin());
				numeric.setEnd(token.getEnd());
				numeric.addToIndexes();
			}
		}
	}
}