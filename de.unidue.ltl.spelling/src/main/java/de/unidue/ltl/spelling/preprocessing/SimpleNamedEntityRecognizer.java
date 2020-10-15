package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.StartOfSentence;

// Does not recognize NE at the beginning of a sentence.
public class SimpleNamedEntityRecognizer extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		for(Token t : JCasUtil.select(aJCas, Token.class)){
			if((JCasUtil.selectCovered(StartOfSentence.class, t).isEmpty()) && containsUpperCaseLetter(t.getCoveredText())) {
				
				NamedEntity ne = new NamedEntity(aJCas);
				ne.setBegin(t.getBegin());
				ne.setEnd(t.getEnd());
				ne.addToIndexes();
//				System.out.println("NE:\t"+t.getCoveredText());
			}
		}	
	};
	
	private boolean containsUpperCaseLetter(String text) {
		boolean containsUpper = false;
		for(int i=0; i<text.length(); i++) {
			if(Character.isUpperCase(text.charAt(i))) {
				containsUpper = true;
			}
		}
		return containsUpper;
	}
}