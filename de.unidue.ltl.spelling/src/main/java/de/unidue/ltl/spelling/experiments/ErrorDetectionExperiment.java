package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.frequency.resources.Web1TFrequencyCountResource;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.evaluation.EvaluateErrorDetection;
import de.unidue.ltl.spelling.normalization.TextPrinter;
import de.unidue.ltl.spelling.preprocessing.DictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.MarkSentenceBeginnings;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToConsider;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToCorrect;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.POStoNEAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.preprocessing.SimpleNamedEntityRecognizer;
import de.unidue.ltl.spelling.reader.SpellingReader;

public class ErrorDetectionExperiment {
	
	public static void main(String[] args) throws UIMAException, IOException {
		
		runCItA();
		runLitkey();		
		//Merlin
		//ASAP	
	}
	
	private static void runLitkey() throws UIMAException, IOException {
		
		String litkey_lang = "de";
		String litkey_path = "src/main/resources/corpora/litkey_spelling.xml";
		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
		String web1t_path_de = System.getenv("DKPRO_HOME") + "/web1t_de_fixed/data";
		runErrorDetection("Litkey_hunspell_web1t", litkey_lang, litkey_path, hunspell_de, null, web1t_path_de, false);	
	}
	
	private static void runCItA() throws UIMAException, IOException {
		
		String cita_lang = "it";
		String cita_path = "src/main/resources/corpora/cita_spelling.xml";
		String hunspell_it = "src/main/resources/dictionaries/hunspell_Italian_dict.txt";
		String italian_aux_dict = "src/main/resources/dictionaries/italian_include.txt";
		String web1t_path_it = System.getenv("DKPRO_HOME") + "web1t_it/";
		runErrorDetection("CItA_hunspell_auxiliary_web1t_POSNE_CoreNLPen", cita_lang, cita_path, hunspell_it, italian_aux_dict, web1t_path_it, true);	
	}
		
	private static void runErrorDetection(String config_name, String lang, String corpus_path, String dict_path, String aux_dict_path, String web1t_path, boolean use_simple_ne_detector) throws UIMAException, IOException {

		// Create web1t language model to set via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, lang,
				Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, "1",
				Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1t_path);

		CollectionReader reader = getReader(corpus_path, lang);
		AnalysisEngineDescription segmenter;
			if(lang.equals("it")){
				segmenter = createEngineDescription(CoreNlpSegmenter.class, CoreNlpSegmenter.PARAM_LANGUAGE, "en");
			}
			else {
				segmenter = createEngineDescription(CoreNlpSegmenter.class);			
			}
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityRecognizer;
			if(lang.equals("it")) {
				AnalysisEngineDescription posTagger = createEngineDescription(OpenNlpPosTagger.class);
				AnalysisEngineDescription posToNeAnnotator = createEngineDescription(POStoNEAnnotator.class,
						POStoNEAnnotator.PARAM_NE_POS_TAG, "SP");
				namedEntityRecognizer = createEngineDescription(posTagger, posToNeAnnotator);
			}
			else if(use_simple_ne_detector) {
				namedEntityRecognizer = createEngineDescription(SimpleNamedEntityRecognizer.class);
			}
			else {
				namedEntityRecognizer = createEngineDescription(StanfordNamedEntityRecognizer.class);
			}
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class);
		AnalysisEngineDescription dictionaryChecker = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_LANGUAGE, lang,
				DictionaryChecker.PARAM_DICTIONARY_FILE, dict_path,
				DictionaryChecker.PARAM_AUXILIARY_DICTIONARY_FILE, aux_dict_path,
				DictionaryChecker.RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP, web1t);
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription evaluate = createEngineDescription(EvaluateErrorDetection.class,
				EvaluateErrorDetection.PARAM_CONFIG_NAME, config_name);

		SimplePipeline.runPipeline(
				reader,
				segmenter,
				markSentenceBeginnings,
				numericAnnotator,
				punctuationAnnotator,
				namedEntityRecognizer,
				markTokensToConsider,
				dictionaryChecker,
				markTokensToCorrect,
				evaluate
				);
	}
	
	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {

		return CollectionReaderFactory.createReader(SpellingReader.class,
				SpellingReader.PARAM_SOURCE_FILE, path,
				SpellingReader.PARAM_LANGUAGE_CODE, language,
				SpellingReader.PARAM_FOR_ERROR_DETECTION, true);
	}
}