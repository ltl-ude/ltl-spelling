package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.StartOfSentence;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Marks first token of each sentence to enable treating sentence beginnings
 * differently.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" }, outputs = {
				"de.unidue.ltl.spelling.types.StartOfSentence" })

public class MarkSentenceBeginnings extends JCasAnnotator_ImplBase {

	public static final String PARAM_INCLUDE_PERIOD = "includePeriod";
	@ConfigurationParameter(name = PARAM_INCLUDE_PERIOD, mandatory = true)
	private boolean includePeriod;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		for (Sentence sentence : JCasUtil.select(aJCas, Sentence.class)) {
//			System.out.println("Sentence:" + sentence.getCoveredText()+"END");
			Token first = JCasUtil.selectCovered(Token.class, sentence).get(0);
			StartOfSentence startOfSentence = new StartOfSentence(aJCas);
			startOfSentence.setBegin(first.getBegin());
			startOfSentence.setEnd(first.getEnd());
			startOfSentence.addToIndexes();
//			System.out.println("Marked\t"+first.getCoveredText()+"\t as sentence beginning, because it is the first token of a sentence.");
		}
		
		boolean previousTokenWasPunctuation = false;
		boolean foundClosingPunctuation = true;
		for (Token token : JCasUtil.select(aJCas, Token.class)) {
			if (token.getCoveredText().matches("[\"]")) {
				if (foundClosingPunctuation) {
					previousTokenWasPunctuation = true;
					foundClosingPunctuation = false;
				} else {
					foundClosingPunctuation = true;
				}
			} else if (previousTokenWasPunctuation) {
				if (JCasUtil.selectCovered(StartOfSentence.class, token).isEmpty()) {
					StartOfSentence startOfSentence = new StartOfSentence(aJCas);
					startOfSentence.setBegin(token.getBegin());
					startOfSentence.setEnd(token.getEnd());
					startOfSentence.addToIndexes();
					previousTokenWasPunctuation = false;
//				    System.out.println("Marked\t"+token.getCoveredText()+"\t as sentence beginning, because of the previous token.");
				}
			}
		}

		String regex = "";
		if (includePeriod) {
			regex = "[\\.?!:„]+";
		} else {
			regex = "[?!:„]+";
		}

		previousTokenWasPunctuation = false;
		for (Token token : JCasUtil.select(aJCas, Token.class)) {
			if (token.getCoveredText().matches(regex)) {
				previousTokenWasPunctuation = true;
			} else if (previousTokenWasPunctuation) {
				if (JCasUtil.selectCovered(StartOfSentence.class, token).isEmpty()) {
					StartOfSentence startOfSentence = new StartOfSentence(aJCas);
					startOfSentence.setBegin(token.getBegin());
					startOfSentence.setEnd(token.getEnd());
					startOfSentence.addToIndexes();
					previousTokenWasPunctuation = false;
//				    System.out.println("*** Marked\t"+token.getCoveredText()+"\t as sentence beginning, because of the previous token.");
				}
			}
		}
	}
}