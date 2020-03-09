package de.unidue.ltl.spelling.errorcorrection;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ResultTester extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		// TODO Auto-generated method stub
		System.out.println("RESULTING TEXT: ");
		for(Token t : JCasUtil.select(aJCas, Token.class)) {
			System.out.println(t.getCoveredText());
		}
		for(SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {
			System.out.println(anomaly.getCoveredText());
//			anomaly.setFixed(true);
		}
	}
	
	

}
