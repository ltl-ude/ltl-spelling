package de.unidue.ltl.spelling.normalization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.StartOfSentence;

public class TextPrinter extends JCasAnnotator_ImplBase{
	
	int numSentences = 0;
	int numTokens = 0;
	Set<String> namedEntities = new HashSet<String>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
//		if(JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class).size() > 0) {
//			System.out.println("Corrected Text:\n"+aJCas.getDocumentText());
//		}
		
//		for(Token t : JCasUtil.select(aJCas, Token.class)) {
//			System.out.println("TOKEN: "+t.getCoveredText());
//		}
//		for(ExtendedSpellingAnomaly anomaly : JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class)) {
//			System.out.println("Anomaly: "+anomaly.getCoveredText()+"\t (was "+anomaly.getMisspelledTokenText()+")\t"+anomaly.getCorrected()+" correction from "+anomaly.getMethodThatGeneratedTheCorrection());;
//		}
		
		for(Sentence sentence : JCasUtil.select(aJCas, Sentence.class)) {
			System.out.println("Sentence:\t"+sentence.getCoveredText());
//			for(Token t : JCasUtil.selectCovered(Token.class, sentence)){
////				t.getCoveredText().matches("[A-Z].*")
//				if((JCasUtil.selectCovered(StartOfSentence.class, t).isEmpty()) && Character.isUpperCase(t.getCoveredText().charAt(0))) {
//					namedEntities.add(t.getCoveredText());
//					System.out.println("NE:\t"+t.getCoveredText());
//				}
//			}
		}
//		
		for(Token token : JCasUtil.select(aJCas, Token.class)) {
//			System.out.println("POS of token "+token.getCoveredText()+": "+token.getPosValue());
			if(token.getPosValue().equals("SP")) {
				System.out.println(token.getCoveredText());
				namedEntities.add(token.getCoveredText());
			}
		}

//		System.out.println("Number of Sentences: "+JCasUtil.select(aJCas, Sentence.class).size());
//		System.out.println("Number of Tokens: "+JCasUtil.select(aJCas, Token.class).size());
		
		
		
		numTokens += JCasUtil.select(aJCas, Token.class).size();
		numSentences += JCasUtil.select(aJCas, Sentence.class).size();
		
	}
	
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		System.out.println("Total number of sentences: "+numSentences);
		System.out.println("Total number of tokens: "+numTokens);
		
		List<String> ne_list = new ArrayList<String>();
		ne_list.addAll(namedEntities);
		ne_list.sort(null);
		for(String ne : ne_list) {
			System.out.println(ne);
		}
	}
}
