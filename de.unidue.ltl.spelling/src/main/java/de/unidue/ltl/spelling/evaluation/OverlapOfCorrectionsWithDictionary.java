package de.unidue.ltl.spelling.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.JCasUtil;

import de.unidue.ltl.spelling.types.SpellingError;

public class OverlapOfCorrectionsWithDictionary extends JCasAnnotator_ImplBase {

	public static final String PARAM_DICTIONARY_FILE = "dictionaryPath";
	@ConfigurationParameter(name = PARAM_DICTIONARY_FILE, mandatory = true)
	private String dictionaryPath;

	private Set<String> dictionaryWords = new HashSet<String>();

	int numberOfErrors = 0;
	int numberOfCorrectionsInDict = 0;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionary(dictionaryPath);
	};

	private void readDictionary(String dictionaryPath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(dictionaryPath)));
			while (br.ready()) {
				dictionaryWords.add(br.readLine());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingError error : JCasUtil.select(aJCas, SpellingError.class)) {
			if (!error.getErrorType().contains("merge_middle") && !error.getErrorType().contains("merge_right")) {
				numberOfErrors++;

				if (dictionaryWords.contains(error.getCorrection())) {
					numberOfCorrectionsInDict++;
				}
			}
		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		System.out.println("Number of spelling errors without merge_middle and merge_right:\t" + numberOfErrors);
		System.out.println("Number of errors whose corrections are found in dict:\t" + numberOfCorrectionsInDict);
		System.out.println("Overlap:\t"+ (numberOfCorrectionsInDict*1.0)/(numberOfErrors*1.0));
	}
}