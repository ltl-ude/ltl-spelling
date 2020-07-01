package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.StartOfSentence;

public class MarkSentenceBeginnings extends JCasAnnotator_ImplBase{

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		for(Sentence sentence : JCasUtil.select(aJCas, Sentence.class)) {
			Token first = JCasUtil.selectCovered(Token.class, sentence).get(0);
			StartOfSentence startOfSentence = new StartOfSentence(aJCas);
			startOfSentence.setBegin(first.getBegin());
			startOfSentence.setEnd(first.getEnd());
			startOfSentence.addToIndexes();
			System.out.println("Marked\t"+first.getCoveredText()+"\t as sentence beginning.");
		}
	}
}