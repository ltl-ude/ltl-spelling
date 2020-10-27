package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.SimpleNamedResourceManager;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;
import org.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import org.dkpro.core.io.text.TextReader;
import org.uimafit.factory.AnalysisEngineFactory;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;
import de.unidue.ltl.spelling.candidateReranking.LanguageModelReranker;
import de.unidue.ltl.spelling.evaluation.EvaluateErrorDetection;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_FindMissingSpace;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_KeyboardDistance;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinGrapheme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinPhoneme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_Litkey;
import de.unidue.ltl.spelling.normalization.ApplyChanges;
import de.unidue.ltl.spelling.normalization.RandomSpellingAnomalyReplacer;
import de.unidue.ltl.spelling.normalization.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.DictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.MarkSentenceBeginnings;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToConsider;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToCorrect;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.preprocessing.SimpleNamedEntityRecognizer;
import de.unidue.ltl.spelling.reader.SpellingReader;
import de.unidue.ltl.spelling.resources.CFDFrequencyCountProvider;
import de.unidue.ltl.spelling.resources.DummyFrequencyCountProvider;
import de.unidue.ltl.spelling.resources.Web1TFrequencyCountProvider;
import de.unidue.ltl.spelling.types.Numeric;

public class Experiment_usingComponentsDirectly {

	public static void main(String[] args) throws UIMAException, IOException {
//		runEnglish();
//		runGerman();
		runCita_errorDetection();
//		runCita_errorCorrection();
	}

	public static void runCita_errorDetection() throws UIMAException, IOException {
		String lang_code = "it";
		String corpus = "src/main/resources/corpora/cita_spelling.xml";
//		String citaEx = "src/main/resources/corpora/cita_example.xml";
		String hunspell_it = "src/main/resources/dictionaries/hunspell_unmunched.txt";
		String web1tPath = System.getenv("DKPRO_HOME") + "web1t_it/";

		// Create web1t language model to set via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, "it", Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL,
				"1", Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1tPath);

		CollectionReader reader = getReader(corpus, lang_code);
		AnalysisEngineDescription segmenter = createEngineDescription(OpenNlpSegmenter.class);
		// Get NEs via POS tagging
		AnalysisEngineDescription posTagger = createEngineDescription(OpenNlpPosTagger.class);
//		AnalysisEngineDescription posTagger = createEngineDescription(TreeTaggerPosTagger.class);
		AnalysisEngineDescription namedEntityRecognizer = createEngineDescription(SimpleNamedEntityRecognizer.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		// TODO: Find/create named entity component for Italian
//		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class);
		AnalysisEngineDescription dictionaryChecker = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_LANGUAGE, "it", DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_it,
				DictionaryChecker.RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP, web1t);
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		// TODO: Engine to evaluate
		AnalysisEngineDescription evaluate = createEngineDescription(EvaluateErrorDetection.class);

		SimplePipeline.runPipeline(reader, segmenter, markSentenceBeginnings, namedEntityRecognizer
//				posTagger, 
//				textPrinter
//				, 
//				markSentenceBeginnings, numericAnnotator, punctuationAnnotator,
////				namedEntityAnnotator,
//				markTokensToConsider, dictionaryChecker, markTokensToCorrect, evaluate
		);
	}

	public static void runCita_errorCorrection() throws UIMAException, IOException {
		String lang_code = "it";
		String corpus = "src/main/resources/corpora/cita_spelling.xml";
//		String citaEx = "src/main/resources/corpora/cita_example.xml";
		String hunspell_it = "src/main/resources/dictionaries/hunspell_unmunched_it.txt";
		String hunspell_it_phoneme = "";
		String italian_keyboard_distances = "";
		String web1tPath = System.getenv("DKPRO_HOME") + "web1t_it/";
		int numberOfCandidatesPerMethod = 3;

		// Create web1t language model to set via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, "it", Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL,
				"1", Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1tPath);

		CollectionReader reader = getReader(corpus, lang_code);
		AnalysisEngineDescription generateRankLevenshtein = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class, GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES,
				hunspell_it, GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, numberOfCandidatesPerMethod,
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_EN.tsv");
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class, GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES,
				hunspell_it_phoneme, GenerateAndRank_LevenshteinPhoneme.PARAM_LANGUAGE, lang_code,
				GenerateAndRank_LevenshteinPhoneme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, numberOfCandidatesPerMethod,
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_it,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE, italian_keyboard_distances,
				GenerateAndRank_KeyboardDistance.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, numberOfCandidatesPerMethod,
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankMissingSpaces = createEngineDescription(
				GenerateAndRank_FindMissingSpace.class, GenerateAndRank_FindMissingSpace.PARAM_DICTIONARIES,
				hunspell_it, GenerateAndRank_FindMissingSpace.PARAM_NUM_OF_CANDIDATES_TO_GENERATE,
				numberOfCandidatesPerMethod);
		AnalysisEngineDescription lmReranker = createEngineDescription(LanguageModelReranker.class,
				LanguageModelReranker.RES_LANGUAGE_MODEL, web1t,
				LanguageModelReranker.RES_LANGUAGE_MODEL_PROMPT_SPECIFIC, web1t, LanguageModelReranker.PARAM_NGRAM_SIZE,
				3);
//		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
//				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
//				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription anomalyReplacer_random = createEngineDescription(RandomSpellingAnomalyReplacer.class,
				RandomSpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		// TODO: Engine to evaluate

//		SimplePipeline.runPipeline(reader, );
	}

	public static void runEnglish() throws UIMAException, IOException {
		String hunspell_en = "src/main/resources/dictionaries/hunspell_en_US.txt";
		String dummy_dict_en_1 = "src/main/resources/dictionaries/testDict_en.txt";

		String[] types_to_exclude = new String[] { Numeric.class.getName() };
		String web1tPath = "/Volumes/Marie2/web1t_en/data";

		String testData = "en-testData";
		String MEWS5 = "MEWS5";

		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2, "Hello this");
		cfd.inc(2, "this is");
		cfd.inc(2, "is a");
		cfd.inc(2, "a bigram");
		cfd.inc(2, "bigram language");
		cfd.inc(2, "language model");

		// Create web1t language model to set via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, "en", Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL,
				"1", Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1tPath);

		CollectionReader reader = getReader(MEWS5, "en");

		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, types_to_exclude}
		);
		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_en, DictionaryChecker.PARAM_LANGUAGE, "en",
				DictionaryChecker.RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP, web1t);
//		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
//				DictionaryChecker.PARAM_DICTIONARY_FILE, dummy_dict_en_1,
//				DictionaryChecker.PARAM_LANGUAGE, "en");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRankLevenshtein = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class, GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES,
				hunspell_en, GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1,
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_EN.tsv");
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class, GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES,
				"src/main/resources/dictionaries/hunspell_en_US_phoneme_map.txt",
				GenerateAndRank_LevenshteinPhoneme.PARAM_LANGUAGE, "eng-US",
				GenerateAndRank_LevenshteinPhoneme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1,
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_en,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_EN-manual.txt",
				GenerateAndRank_KeyboardDistance.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1,
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankMissingSpaces = createEngineDescription(
				GenerateAndRank_FindMissingSpace.class, GenerateAndRank_FindMissingSpace.PARAM_DICTIONARIES,
				hunspell_en, GenerateAndRank_FindMissingSpace.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription anomalyReplacer_random = createEngineDescription(RandomSpellingAnomalyReplacer.class,
				RandomSpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription lmReranker = createEngineDescription(LanguageModelReranker.class,
				LanguageModelReranker.RES_LANGUAGE_MODEL, web1t,
				LanguageModelReranker.RES_LANGUAGE_MODEL_PROMPT_SPECIFIC, web1t, LanguageModelReranker.PARAM_NGRAM_SIZE,
				3);

		// Adding language model resources via SimpleResourceManager
		// TODO: Does not work if resources for more than one annotator are added
//		CFDFrequencyCountProvider cfdResource = new CFDFrequencyCountProvider(cfd, "en");
//		Map<String, Object> context = new HashMap<String, Object>();
//		context.put(LanguageModelReranker.RES_LANGUAGE_MODEL, cfdResource);
//		SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
//		resMgr.setAutoWireEnabled(true);
//		resMgr.setExternalContext(context);
//		lmReranker.setResourceManagerConfiguration(new ResourceManagerConfiguration_impl());

		// Create spelling corrector engine
//		AnalysisEngineDescription spellingCorrector = AnalysisEngineFactory.createAggregateDescription(
//				textPrinter,
//				segmenter,
//				markSentenceBeginnings,
//				numericAnnotator,
//				punctuationAnnotator,
//				namedEntityAnnotator,
//				markTokensToConsider,
//				dictionaryChecker1,
////				dictionaryChecker2,
//				markTokensToCorrect,
//				generateRankMissingSpaces,
//				generateRankKeyboard,
//				generateRankLevenshtein,
//				generateRankPhoneme,
//				lmReranker,
//				anomalyReplacer,
////				anomalyReplacer_random,
//				changeApplier,
//				segmenter);
//		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spellingCorrector, null, null);
//		while (reader.hasNext()) {
//			CAS cas = ae.newCAS();
//			reader.getNext(cas);
//			ae.process(cas);
//		}

		SimplePipeline.runPipeline(reader, segmenter, markSentenceBeginnings, numericAnnotator, punctuationAnnotator,
				namedEntityAnnotator, markTokensToConsider, dictionaryChecker1,
//				dictionaryChecker2,
				markTokensToCorrect, generateRankMissingSpaces, generateRankKeyboard, generateRankLevenshtein,
				generateRankPhoneme, lmReranker, anomalyReplacer,
//				anomalyReplacer_random,
				changeApplier, segmenter);
	}

	public static void runGerman() throws UIMAException, IOException {
		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
		String dummy_dict_de_1 = "src/main/resources/dictionaries/testDict_de.txt";
		String web1tPath = "/Volumes/Marie2/web1t_de/data";

		String testData = "de-testData";
		String skala = "Skala5";

		// Used as a language model
		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2, "Hallo ,");
		cfd.inc(2, ", dies");
		cfd.inc(2, "dies ist");
		cfd.inc(2, "ist ein");
		cfd.inc(2, "ein Bigramm");
		cfd.inc(2, "Bigramm Language");
		cfd.inc(2, "Language Model");

		// Set web1t language model via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, "de", Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL,
				"1", Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1tPath);

		SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();

		CollectionReader reader = getReader(testData, "de");
		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, types_to_exclude}
		);
		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_de, DictionaryChecker.PARAM_LANGUAGE, "de",
				DictionaryChecker.RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP, web1t);
//		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
//				DictionaryChecker.PARAM_DICTIONARY_FILE, dummy_dict_de_1,
//				DictionaryChecker.PARAM_LANGUAGE, "de");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_KeyboardDistance.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_DE-manual.txt",
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankLitkey = createEngineDescription(GenerateAndRank_Litkey.class,
				GenerateAndRank_Litkey.PARAM_LANGUAGE, "de", GenerateAndRank_Litkey.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_Litkey.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1);
		AnalysisEngineDescription generateRankLevenshtein = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class, GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES,
				hunspell_de, GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1,
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_DE_withUpper.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_DE_withUpper.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_DE_withUpper.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class, GenerateAndRank_LevenshteinPhoneme.PARAM_LANGUAGE, "deu-DE",
				GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES,
				"src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt",
				GenerateAndRank_LevenshteinPhoneme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1,
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv"
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankMissingSpaces = createEngineDescription(
				GenerateAndRank_FindMissingSpace.class, GenerateAndRank_FindMissingSpace.PARAM_DICTIONARIES,
				hunspell_de, GenerateAndRank_FindMissingSpace.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 1);
		AnalysisEngineDescription lmReranker = createEngineDescription(LanguageModelReranker.class,
				LanguageModelReranker.RES_LANGUAGE_MODEL, web1t,
				LanguageModelReranker.RES_LANGUAGE_MODEL_PROMPT_SPECIFIC, web1t, LanguageModelReranker.PARAM_NGRAM_SIZE,
				3);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription anomalyReplacer_random = createEngineDescription(RandomSpellingAnomalyReplacer.class,
				RandomSpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);

		// Add language model resources
//		CFDFrequencyCountProvider CFD_LM = new CFDFrequencyCountProvider(cfd, "de");
//		Web1TFrequencyCountProvider web1t_LM = new Web1TFrequencyCountProvider(web1tPath, "de", 1, 5);
//		Map<String, Object> context = new HashMap<String, Object>();
//		context.put(DictionaryChecker.RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP, new DummyFrequencyCountProvider());
//		context.put(LanguageModelReranker.RES_LANGUAGE_MODEL, new DummyFrequencyCountProvider());
//		context.put(LanguageModelReranker.RES_LANGUAGE_MODEL_PROMPT_SPECIFIC, new DummyFrequencyCountProvider());
//		resMgr.setAutoWireEnabled(true);
//		resMgr.setExternalContext(context);

//		ResourceManagerConfiguration config = new ResourceManagerConfiguration_impl();
//		dictionaryChecker1.setResourceManagerConfiguration(config);
//		lmReranker.setResourceManagerConfiguration(config);

		// Create spelling corrector engine
		AnalysisEngineDescription spellingCorrector = AnalysisEngineFactory.createAggregateDescription(segmenter,
				markSentenceBeginnings, numericAnnotator, punctuationAnnotator, namedEntityAnnotator,
				markTokensToConsider, dictionaryChecker1,
//				dictionaryChecker2,
				markTokensToCorrect,
//				generateRankPhoneme,
//				generateRankMissingSpaces,
//				generateRankKeyboard,
				generateRankLitkey,
//				generateRankLevenshtein,
//				lmReranker,
				anomalyReplacer,
//				anomalyReplacer_random,
				changeApplier, segmenter);
		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spellingCorrector, null, null);
//		while (reader.hasNext()) {
		CAS cas = ae.newCAS();
		reader.getNext(cas);
		ae.process(cas);
//		}

//		SimplePipeline.runPipeline(
//				reader,
//				textPrinter,
//				segmenter,
//				markSentenceBeginnings,
//				numericAnnotator,
//				punctuationAnnotator,
//				namedEntityAnnotator,
//				markTokensToConsider,
//				dictionaryChecker1,
////				dictionaryChecker2,
//				markTokensToCorrect,
////				generateRankMissingSpaces,
////				generateRankKeyboard,
////				generateRankLitkey,
////				generateRankLevenshtein,
//				generateRankPhoneme,
////				lmReranker,
//				anomalyReplacer,
////				anomalyReplacer_random,
//				changeApplier,
//				segmenter);
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
//		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
//				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);

		return CollectionReaderFactory.createReader(SpellingReader.class, SpellingReader.PARAM_SOURCE_FILE, path,
				SpellingReader.PARAM_LANGUAGE_CODE, language);
	}
}
