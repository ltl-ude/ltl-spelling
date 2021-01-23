package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.StartOfSentence;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Annotator marking the first token after a line break as the beginning of a
 * sentence
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" }, outputs = {
		"de.unidue.ltl.spelling.types.Punctuation" })

public class LineBreakAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		String text = aJCas.getDocumentText();

		for (Token token : JCasUtil.select(aJCas, Token.class)) {

			if (JCasUtil.selectCovered(StartOfSentence.class, token).isEmpty()) {

				if (token.getBegin() > 0 && text.substring(token.getBegin() - 1, token.getBegin()).matches("\\n")) {
//					System.out.println(token.getCoveredText() + " Is the beginning of a new line");
					
					StartOfSentence sos = new StartOfSentence(aJCas);
					sos.setBegin(token.getBegin());
					sos.setEnd(token.getEnd());
					sos.addToIndexes();
				}
			}
		}
	}
}