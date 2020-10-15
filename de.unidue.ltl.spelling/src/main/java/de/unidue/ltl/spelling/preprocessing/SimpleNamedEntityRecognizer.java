package de.unidue.ltl.spelling.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.StartOfSentence;

public class SimpleNamedEntityRecognizer extends JCasAnnotator_ImplBase{
	
//	Set<String> neSet = new HashSet<String>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		for(Token t : JCasUtil.select(aJCas, Token.class)){
			if((JCasUtil.selectCovered(StartOfSentence.class, t).isEmpty()) && containsUpperCaseLetter(t.getCoveredText())) {
				NamedEntity ne = new NamedEntity(aJCas);
				ne.setBegin(t.getBegin());
				ne.setEnd(t.getEnd());
				ne.addToIndexes();
//				System.out.println("NE:\t"+t.getCoveredText());
//				neSet.add(t.getCoveredText());
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
	
//	@Override
//	public void collectionProcessComplete() throws AnalysisEngineProcessException {
//		
//		List<String> ne_list = new ArrayList<String>();
//		ne_list.addAll(neSet);
//		ne_list.sort(null);
//		for(String ne : ne_list) {
//			System.out.println(ne);
//		}
//	}

}
