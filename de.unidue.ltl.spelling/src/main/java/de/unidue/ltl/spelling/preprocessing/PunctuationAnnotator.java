package de.unidue.ltl.spelling.preprocessing;

import java.util.Collection;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.Punctuation;

public class PunctuationAnnotator extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		//Iterate over tokens
		Collection<Token> tokens = JCasUtil.select(aJCas, Token.class);
		
		for(Token token : tokens) {
			System.out.println(token.getCoveredText());
			if(token.getCoveredText().matches("[!?\\.,\"\'-´`']")) {
				//Create new Punctuation annotation
				Punctuation punct = new Punctuation(aJCas);
				punct.setBegin(token.getBegin());
				punct.setEnd(token.getEnd());
				punct.addToIndexes();
			}
		}
		
	}

}
