package de.unidue.ltl.spelling.preprocessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.SpellingError;
import de.unidue.ltl.spelling.types.StartOfSentence;
import de.unidue.ltl.spelling.utils.GraphemeDictionaryToPhonemeMap;
import de.unidue.ltl.spelling.utils.G2P_LanguageCodeMapper;
import de.unidue.ltl.spelling.utils.GraphemeDictionaryToPhonemeDictionary;

public class SpellingErrorListWriter extends JCasAnnotator_ImplBase {

	public static final String PARAM_OUTPUT_PATH = "outputPath";
	@ConfigurationParameter(name = PARAM_OUTPUT_PATH, mandatory = true)
	protected String outPath;

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	protected String language;

	Set<String> misspellings = new HashSet<String>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		String text = aJCas.getDocumentText();

		for (SpellingError error : JCasUtil.select(aJCas, SpellingError.class)) {

			if(!error.getCoveredText().equals("")) {
				misspellings.add(error.getCoveredText());
			}
		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		List<String> misspellingList = new ArrayList<String>();
		for (String missp : misspellings) {
			if(!missp.contains(" ")) {
				misspellingList.add(missp);
			}
			else {
				System.out.println("SKIP THIS ONE: "+missp);
			}
		}
		System.exit(0);
		misspellingList.sort(null);
		
		try {
			GraphemeDictionaryToPhonemeMap.processDictionary(misspellingList, outPath, language);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}