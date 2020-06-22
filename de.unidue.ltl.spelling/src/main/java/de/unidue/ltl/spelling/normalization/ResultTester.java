package de.unidue.ltl.spelling.normalization;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;

public class ResultTester extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		System.out.println("RESULTING TEXT: "+aJCas.getDocumentText());
//		for(Token t : JCasUtil.select(aJCas, Token.class)) {
//			System.out.println("TOKEN: "+t.getCoveredText());
//		}
//		for(ExtendedSpellingAnomaly anomaly : JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class)) {
//			System.out.println("Anomaly: "+anomaly.getCoveredText()+" "+anomaly.getCorrected());
//		}
	}
}
