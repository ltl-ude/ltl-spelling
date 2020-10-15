package de.unidue.ltl.spelling.normalization;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class TextPrinter extends JCasAnnotator_ImplBase {

	int numSentences = 0;
	int numTokens = 0;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		numTokens += JCasUtil.select(aJCas, Token.class).size();
		numSentences += JCasUtil.select(aJCas, Sentence.class).size();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		System.out.println("Total number of sentences: " + numSentences);
		System.out.println("Total number of tokens: " + numTokens);
	}
}