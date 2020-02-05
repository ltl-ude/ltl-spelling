package de.unidue.ltl.spelling.preprocessing;

import java.util.Collection;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.Numeric;

public class NumericAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		//Iterate over tokens: if token text matches number regex, annotate Type 'Number'
		Collection<Token> tokens = JCasUtil.select(aJCas, Token.class);
		
		for(Token token : tokens) {
			System.out.println(token.getCoveredText());
			if(token.getCoveredText().matches("[0-9]+")) {
				//Create new Number annotation
				System.out.println("Found a number: "+token.getCoveredText());
				Numeric numeric = new Numeric(aJCas);
				numeric.setBegin(token.getBegin());
				numeric.setEnd(token.getEnd());
				numeric.addToIndexes();
			}
		}
		
	}

}
