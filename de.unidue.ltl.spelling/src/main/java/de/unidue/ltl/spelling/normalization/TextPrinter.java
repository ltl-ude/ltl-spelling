package de.unidue.ltl.spelling.normalization;

import java.util.Iterator;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;

public class TextPrinter extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		if(JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class).size() > 0) {
			System.out.println("Corrected Text:\n"+aJCas.getDocumentText());
		}
		
//		for(Token t : JCasUtil.select(aJCas, Token.class)) {
//			System.out.println("TOKEN: "+t.getCoveredText());
//		}
		for(ExtendedSpellingAnomaly anomaly : JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class)) {
			System.out.println("Anomaly: "+anomaly.getCoveredText()+"\t (was "+anomaly.getMisspelledTokenText()+")\t"+anomaly.getCorrected()+" correction from "+anomaly.getMethodThatGeneratedTheCorrection());;
		}
		
	}
}
