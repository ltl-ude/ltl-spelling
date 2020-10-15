package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.unidue.ltl.spelling.evaluation.OverlapOfCorrectionsWithDictionary;
import de.unidue.ltl.spelling.normalization.TextPrinter;
import de.unidue.ltl.spelling.reader.SpellingReader;

public class CorpusStatisticsExperiment {

	public static void main(String[] args) throws UIMAException, IOException {
		
		System.out.println("CItA");
		evalCItA();
		System.out.println();
		System.out.println("Litkey");
		evalLitkey();
	}

	public static void evalCItA() throws UIMAException, IOException {
		String cita_lang = "it";
		String cita_corpus = "src/main/resources/corpora/CItA_spelling.xml";
		String cita_dict = "src/main/resources/dictionaries/hunspell_Italian_dict.txt";
		getErrorRate(cita_lang, cita_corpus);
		getOverlapBetweenCorrectionsAndDict(cita_lang, cita_corpus, cita_dict);
	}

	public static void evalLitkey() throws UIMAException, IOException {
		String litkey_lang = "de";
		String litkey_corpus = "src/main/resources/corpora/litkey_spelling.xml";
		String litkey_dict = "src/main/resources/dictionaries/hunspell_DE.txt";
		getErrorRate(litkey_lang, litkey_corpus);
		getOverlapBetweenCorrectionsAndDict(litkey_lang, litkey_corpus, litkey_dict);
	}

	public static void getErrorRate(String lang, String corpus_path) throws UIMAException, IOException {

		CollectionReader reader = getReader(corpus_path, lang);
		AnalysisEngineDescription segmenter;
		if (lang.equals("it")) {
			segmenter = createEngineDescription(CoreNlpSegmenter.class, CoreNlpSegmenter.PARAM_LANGUAGE, "en");
		} else {
			segmenter = createEngineDescription(CoreNlpSegmenter.class);
		}
		AnalysisEngineDescription textPrinter = createEngineDescription(TextPrinter.class);
		SimplePipeline.runPipeline(reader, segmenter, textPrinter);
	}

	public static void getOverlapBetweenCorrectionsAndDict(String lang, String corpus_path, String dict_path)
			throws UIMAException, IOException {
		CollectionReader reader = getReader(corpus_path, lang);
		AnalysisEngineDescription overlap = createEngineDescription(OverlapOfCorrectionsWithDictionary.class,
				OverlapOfCorrectionsWithDictionary.PARAM_DICTIONARY_FILE, dict_path);
		SimplePipeline.runPipeline(reader, overlap);
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {

		return CollectionReaderFactory.createReader(SpellingReader.class, SpellingReader.PARAM_SOURCE_FILE, path,
				SpellingReader.PARAM_LANGUAGE_CODE, language, SpellingReader.PARAM_FOR_ERROR_DETECTION, false);
	}
}