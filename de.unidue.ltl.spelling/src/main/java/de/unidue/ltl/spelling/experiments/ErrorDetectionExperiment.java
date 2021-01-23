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
import de.tudarmstadt.ukp.dkpro.core.tokit.RegexSegmenter;
import de.unidue.ltl.spelling.evaluation.EvaluateErrorDetection;
import de.unidue.ltl.spelling.preprocessing.DictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.HunspellDictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.LineBreakAnnotator;
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
		
//		runCItA();
//		runLitkey();		
		runMerlinDE();
//		runMerlinIT();
//		runMerlinCZ();
	
	}
	
	private static void runLitkey() throws UIMAException, IOException {
		
		String litkey_lang = "de";
		String litkey_path = "src/main/resources/corpora/litkey_spelling.xml";
		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
		String de_dic = "src/main/resources/dictionaries/hunspell/German_de_DE_aux.dic";
		String de_aff = "src/main/resources/dictionaries/hunspell/German_de_DE.aff";
		String web1t_path_de = System.getenv("DKPRO_HOME") + "/web1t_de/data";
//		runErrorDetection("Litkey_hunspellORIGadd_web1t_WhitespaceSegmentation", litkey_lang, litkey_path, hunspell_de, de_dic, de_aff, true, null, web1t_path_de, false, true);
//		runErrorDetection("Litkey_hunspell_web1t_WhitespaceSegmentation", litkey_lang, litkey_path, hunspell_de, de_dic, de_aff, false, null, web1t_path_de, false, true);	
//		runErrorDetection("Litkey_hunspell_web1t_lowercase_WhitespaceSegmentation", litkey_lang, litkey_path, hunspell_de, de_dic, de_aff, false, null, web1t_path_de, true, true);	
		hunspell_de = "src/main/resources/dictionaries/childlex_all.txt";
		runErrorDetection("Litkey_childlex_web1t_WhitespaceSegmentation", litkey_lang, litkey_path, hunspell_de, de_dic, de_aff, false, null, web1t_path_de, false, true);	

	}
	
	private static void runCItA() throws UIMAException, IOException {
		
		String cita_lang = "it";
		String cita_path = "src/main/resources/corpora/cita_spelling.xml";
		String hunspell_it = "src/main/resources/dictionaries/hunspell_Italian_dict.txt";
		String it_dic = "src/main/resources/dictionaries/hunspell/Italian_aux.dic";
		String it_aff = "src/main/resources/dictionaries/hunspell/Italian.aff";
		String italian_aux_dict = "src/main/resources/dictionaries/italian_include.txt";
		String web1t_path_it = System.getenv("DKPRO_HOME") + "web1t_it/";
		runErrorDetection("CItA_hunspellORIGadd_web1t_POSNE_OpenNLPit", cita_lang, cita_path, hunspell_it, it_dic, it_aff, true, italian_aux_dict, web1t_path_it, false, false);
//		runErrorDetection("CItA_hunspell_web1t_POSNE_OpenNLPit_lowercase", cita_lang, cita_path, hunspell_it, it_dic, it_aff, false, italian_aux_dict, web1t_path_it, true, false);
	}
	
	private static void runMerlinDE() throws UIMAException, IOException {
		
		String merlin_lang = "de";
		String merlin_path = "src/main/resources/corpora/Merlin_spelling_german.xml";
//		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
		String hunspell_de = "src/main/resources/dictionaries/childlex_litkey.txt";
		String de_dic = "src/main/resources/dictionaries/hunspell/German_de_DE_aux.dic";
		String de_aff = "src/main/resources/dictionaries/hunspell/German_de_DE.aff";
		String web1t_path_de = System.getenv("DKPRO_HOME") + "web1t_de/data";
//		runErrorDetection("MerlinDE_hunspellORIGaux_web1t", merlin_lang, merlin_path, hunspell_de, de_dic, de_aff, true, null, web1t_path_de, false, false);	
//		runErrorDetection("MerlinDE_hunspell_web1t", merlin_lang, merlin_path, hunspell_de, de_dic, de_aff, false, null, web1t_path_de, false, false);	
//		runErrorDetection("MerlinDE_hunspell_web1t_lowercase", merlin_lang, merlin_path, hunspell_de, de_dic, de_aff, false, null, web1t_path_de, true, false);	
		runErrorDetection("MerlinDE_childlex_web1t", merlin_lang, merlin_path, hunspell_de, de_dic, de_aff, false, null, web1t_path_de, false, false);	
	}
	
	private static void runMerlinIT() throws UIMAException, IOException {
		
		String merlin_lang = "it";
		String merlin_path = "src/main/resources/corpora/merlin_spelling.xml";
		String hunspell_it = "src/main/resources/dictionaries/hunspell_Italian_dict.txt";
		String it_dic = "src/main/resources/dictionaries/hunspell/Italian_aux.dic";
		String it_aff = "src/main/resources/dictionaries/hunspell/Italian.aff";
		String italian_aux_dict = "src/main/resources/dictionaries/italian_include.txt";
		String web1t_path_it = System.getenv("DKPRO_HOME") + "web1t_it/";
		runErrorDetection("MerlinIT_hunspellORIGaux_web1t_POSNE_OpenNLPit", merlin_lang, merlin_path, hunspell_it, it_dic, it_aff, true, italian_aux_dict, web1t_path_it, false, false);
//		runErrorDetection("MerlinIT_hunspell_web1t_POSNE_OpenNLPit", merlin_lang, merlin_path, hunspell_it, it_dic, it_aff, false, italian_aux_dict, web1t_path_it, false, false);
//		runErrorDetection("MerlinIT_hunspell_web1t_POSNE_OpenNLPit_lowercase", merlin_lang, merlin_path, hunspell_it, it_dic, it_aff, false, italian_aux_dict, web1t_path_it, true, false);
	}
	
	private static void runMerlinCZ() throws UIMAException, IOException {
		
		String merlin_lang = "cz";
		String merlin_path = "src/main/resources/corpora/merlin_spelling.xml";
		String hunspell_cz = "src/main/resources/dictionaries/hunspell_Czech_dict.txt";
		String cz_dic = "src/main/resources/dictionaries/hunspell/Czech.dic";
		String cz_aff = "src/main/resources/dictionaries/hunspell/Czech.aff";
		String web1t_path_cz = System.getenv("DKPRO_HOME") + "web1t_cz/";
		runErrorDetection("MerlinCZ_hunspellORIG_web1t_StanfordNEen_CoreNLPen", merlin_lang, merlin_path, hunspell_cz, cz_dic, cz_aff, true, null, web1t_path_cz, false, false);
		runErrorDetection("MerlinCZ_hunspell_web1t_StanfordNEen_CoreNLPen", merlin_lang, merlin_path, hunspell_cz, cz_dic, cz_aff, false, null, web1t_path_cz, false, false);
		runErrorDetection("MerlinCZ_hunspell_web1t_StanfordNEen_CoreNLPen_lowercase", merlin_lang, merlin_path, hunspell_cz, cz_dic, cz_aff, false, null, web1t_path_cz, true, false);
	}
	
	private static void runErrorDetection(String config_name, String lang, String corpus_path, String dict_path, String dic_path, String aff_path, boolean useHunspell, String aux_dict_path, String web1t_path, boolean lowercase, boolean periodsAreSentenceBoundaries) throws UIMAException, IOException {

		// Create web1t language model to set via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, lang,
				Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, "1",
				Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1t_path);

		CollectionReader reader = getReader(corpus_path, lang);
		AnalysisEngineDescription segmenter;
			if(lang.equals("it")) {
				segmenter = createEngineDescription(OpenNlpSegmenter.class);
			}
			else if (lang.equals("cz")){
				segmenter = createEngineDescription(CoreNlpSegmenter.class, CoreNlpSegmenter.PARAM_LANGUAGE, "en");
			}
			else if (config_name.startsWith("Litkey")) {
				segmenter = createEngineDescription(RegexSegmenter.class,
						RegexSegmenter.PARAM_SENTENCE_BOUNDARY_REGEX, " [\\.!?]+ ");
			}
			else {
				segmenter = createEngineDescription(CoreNlpSegmenter.class);			
			}
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class,
				MarkSentenceBeginnings.PARAM_INCLUDE_PERIOD, periodsAreSentenceBoundaries);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription lineBreakAnnotator = createEngineDescription(LineBreakAnnotator.class);
		AnalysisEngineDescription namedEntityRecognizer;
			if(lang.equals("it")) {
				AnalysisEngineDescription posTagger = createEngineDescription(OpenNlpPosTagger.class);
				AnalysisEngineDescription posToNeAnnotator = createEngineDescription(POStoNEAnnotator.class,
						POStoNEAnnotator.PARAM_NE_POS_TAG, "SP");
				namedEntityRecognizer = createEngineDescription(posTagger, posToNeAnnotator);
			}
			else if(lang.equals("cz")) {
				namedEntityRecognizer = createEngineDescription(StanfordNamedEntityRecognizer.class,
						StanfordNamedEntityRecognizer.PARAM_LANGUAGE, "en");
			}
			else {
				namedEntityRecognizer = createEngineDescription(StanfordNamedEntityRecognizer.class);
			}
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class);
		AnalysisEngineDescription dictionaryChecker = null;
			if(!useHunspell) {
				dictionaryChecker = createEngineDescription(DictionaryChecker.class,
					DictionaryChecker.PARAM_LANGUAGE, lang,
					DictionaryChecker.PARAM_DICTIONARY_FILE, dict_path,
					DictionaryChecker.PARAM_AUXILIARY_DICTIONARY_FILE, aux_dict_path,
					DictionaryChecker.RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP, web1t,
					DictionaryChecker.PARAM_LOWERCASE, lowercase,
					DictionaryChecker.PARAM_CHECK_FOR_COMPOUNDS, false);
			}
			else {
			dictionaryChecker = createEngineDescription(
					HunspellDictionaryChecker.class,
					HunspellDictionaryChecker.PARAM_LANGUAGE, lang,
					HunspellDictionaryChecker.PARAM_DIC_FILE,dic_path,
					HunspellDictionaryChecker.PARAM_AFF_FILE,aff_path);
			}
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription evaluate = createEngineDescription(EvaluateErrorDetection.class,
				EvaluateErrorDetection.PARAM_CONFIG_NAME, config_name);

		SimplePipeline.runPipeline(
				reader,
				segmenter,
				markSentenceBeginnings,
				numericAnnotator,
				punctuationAnnotator,
				lineBreakAnnotator,
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