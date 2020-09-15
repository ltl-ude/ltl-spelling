package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
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
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;
import org.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import org.dkpro.core.io.text.TextReader;
import org.uimafit.factory.AnalysisEngineFactory;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.candidateReranking.LanguageModelReranker;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_FindMissingSpace;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_KeyboardDistance;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinGrapheme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinPhoneme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_Litkey;
import de.unidue.ltl.spelling.normalization.ApplyChanges;
import de.unidue.ltl.spelling.normalization.RandomSpellingAnomalyReplacer;
import de.unidue.ltl.spelling.normalization.ResultTester;
import de.unidue.ltl.spelling.normalization.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.DictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.MarkSentenceBeginnings;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToConsider;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToCorrect;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PrintText;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.resources.CFDFrequencyCountProvider;
import de.unidue.ltl.spelling.resources.DummyFrequencyCountProvider;
import de.unidue.ltl.spelling.types.Numeric;

public class Experiment_usingComponentsDirectly {

	public static void main(String[] args) throws UIMAException, IOException {
//		runEnglish();
		runGerman();
	}

	public static void runEnglish() throws UIMAException, IOException {
		String hunspell_en = "src/main/resources/dictionaries/hunspell_en_US.txt";
		String dict_en_1 = "dictionaries/testDict_en.txt";
		String[] types_to_exclude = new String[] { Numeric.class.getName() };

		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2, "Hello there");
		cfd.inc(2, "this Frequency");
		cfd.inc(2, "Frequency Distrbution");
		cfd.inc(2, "Distrbution is");
		cfd.inc(2, "is about");
		cfd.inc(2, "about to");
		cfd.inc(2, "to be");
		cfd.inc(2, "be serialized");

		CollectionReader reader = getReader("en-testData", "en");

		AnalysisEngineDescription showText = createEngineDescription(PrintText.class);
		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, types_to_exclude}
		);
		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_en, DictionaryChecker.PARAM_LANGUAGE, "en");
		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, dict_en_1, DictionaryChecker.PARAM_LANGUAGE, "en");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRankLevenshtein = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class, GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES,
				hunspell_en, GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 10,
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true,
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
				"src/main/resources/matrixes/RDMatrix_deletion_EN.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
				"src/main/resources/matrixes/RDMatrix_insertion_EN.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
				"src/main/resources/matrixes/RDMatrix_substitution_EN.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
				"src/main/resources/matrixes/RDMatrix_transposition_EN.tsv");
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class, GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES,
				hunspell_en, GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_en,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_EN-manual.txt",
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankMissingSpaces = createEngineDescription(
				GenerateAndRank_FindMissingSpace.class, GenerateAndRank_FindMissingSpace.PARAM_DICTIONARIES,
				hunspell_en);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription anomalyReplacer_random = createEngineDescription(RandomSpellingAnomalyReplacer.class,
				RandomSpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription resultTester = createEngineDescription(ResultTester.class);
		AnalysisEngineDescription lmReranker = createEngineDescription(LanguageModelReranker.class,
//				LanguageModelReranker.RES_INJECTED_CFD, web1t,
				LanguageModelReranker.PARAM_NGRAM_SIZE, 3);

		// Add language model resources
		CFDFrequencyCountProvider cfdResource = new CFDFrequencyCountProvider(cfd);
		Map<String, Object> context = new HashMap<String, Object>();
		context.put(LanguageModelReranker.RES_LANGUAGE_MODEL, cfdResource);
		SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
		resMgr.setAutoWireEnabled(true);
		resMgr.setExternalContext(context);
		lmReranker.setResourceManagerConfiguration(new ResourceManagerConfiguration_impl());

		// Create spelling corrector engine
		AnalysisEngineDescription spellingCorrector = AnalysisEngineFactory.createAggregateDescription(showText,
				segmenter, markSentenceBeginnings, numericAnnotator, punctuationAnnotator, namedEntityAnnotator,
				markTokensToConsider, dictionaryChecker1, dictionaryChecker2, markTokensToCorrect,
				generateRankMissingSpaces, generateRankKeyboard, generateRankLevenshtein, generateRankPhoneme,
				anomalyReplacer, lmReranker, anomalyReplacer_random, changeApplier, segmenter, resultTester);
		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spellingCorrector, resMgr, null);
		while (reader.hasNext()) {
			CAS cas = ae.newCAS();
			reader.getNext(cas);
			ae.process(cas);
		}

		SimplePipeline.runPipeline(reader, showText, segmenter, markSentenceBeginnings, numericAnnotator,
				punctuationAnnotator, namedEntityAnnotator, markTokensToConsider, dictionaryChecker1,
				dictionaryChecker2, markTokensToCorrect, generateRankMissingSpaces, generateRankKeyboard,
				generateRankLevenshtein, generateRankPhoneme, anomalyReplacer, lmReranker, anomalyReplacer_random,
				changeApplier, segmenter, resultTester);
	}

	public static void runGerman() throws UIMAException, IOException {
		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
		String dict_1_de = "src/main/resources/dictionaries/testDict_de.txt";

		// Used as a language model
		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2, "Hallo ,");
		cfd.inc(2, ", diese");
		cfd.inc(2, "diese Frequency");
		cfd.inc(2, "Frequency Distribution");
		cfd.inc(2, "Distribution wird");
		cfd.inc(2, "wird jetzt");
		cfd.inc(2, "jetzt serialisiert");
		cfd.inc(2, "serialisiert .");

		// Set web1t language model via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, "de", Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL,
				"1", Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "3",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, new File("/Volumes/Marie2/web1t/en/data").getAbsolutePath());

		CollectionReader reader = getReader("de-testData", "de");
		AnalysisEngineDescription showText = createEngineDescription(PrintText.class);
		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, types_to_exclude}
		);
		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_de, DictionaryChecker.PARAM_LANGUAGE, "de");
		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, dict_1_de, DictionaryChecker.PARAM_LANGUAGE, "de");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_KeyboardDistance.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 5,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_DE-manual.txt",
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankLitkey = createEngineDescription(GenerateAndRank_Litkey.class,
				GenerateAndRank_Litkey.PARAM_LANGUAGE, "de", GenerateAndRank_Litkey.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_Litkey.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 5);
		AnalysisEngineDescription generateRankLevenshtein = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class, GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES,
				hunspell_de, GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 10,
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true,
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
				"src/main/resources/matrixes/RDMatrix_deletion_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
				"src/main/resources/matrixes/RDMatrix_insertion_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
				"src/main/resources/matrixes/RDMatrix_substitution_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
				"src/main/resources/matrixes/RDMatrix_transposition_DE_withUpper.tsv");
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class, GenerateAndRank_LevenshteinPhoneme.PARAM_LANGUAGE, "deu-DE",
				GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES,
				"/Users/mariebexte/ltl-spelling/de.unidue.ltl.spelling/src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt",
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true,
				GenerateAndRank_LevenshteinPhoneme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 10,
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv");
		AnalysisEngineDescription generateRankMissingSpaces = createEngineDescription(
				GenerateAndRank_FindMissingSpace.class, GenerateAndRank_FindMissingSpace.PARAM_DICTIONARIES,
				hunspell_de);
		AnalysisEngineDescription lmReranker = createEngineDescription(LanguageModelReranker.class,
				LanguageModelReranker.RES_LANGUAGE_MODEL, web1t,
				LanguageModelReranker.PARAM_NGRAM_SIZE, 3);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription anomalyReplacer_random = createEngineDescription(RandomSpellingAnomalyReplacer.class,
				RandomSpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription resultTester = createEngineDescription(ResultTester.class);

		// Add language model resources
		CFDFrequencyCountProvider cfdResource = new CFDFrequencyCountProvider(cfd);
		Map<String, Object> context = new HashMap<String, Object>();
//		context.put(LanguageModelReranker.RES_LANGUAGE_MODEL, cfdResource);
		context.put(LanguageModelReranker.RES_LANGUAGE_MODEL_PROMPT_SPECIFIC, cfdResource);		
		SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
		resMgr.setAutoWireEnabled(true);
		resMgr.setExternalContext(context);
//		lmReranker.setResourceManagerConfiguration(new ResourceManagerConfiguration_impl());

		// Create spelling corrector engine
		AnalysisEngineDescription spellingCorrector = AnalysisEngineFactory.createAggregateDescription(showText,
				segmenter, markSentenceBeginnings, numericAnnotator, punctuationAnnotator, namedEntityAnnotator,
				markTokensToConsider, dictionaryChecker1, dictionaryChecker2, markTokensToCorrect,
//				generateRankMissingSpaces, 
				generateRankKeyboard,
//				generateRankLitkey, generateRankLevenshtein,
//				generateRankPhoneme, 
				lmReranker, anomalyReplacer,
//				anomalyReplacer_random,
				changeApplier, segmenter,
				resultTester);
		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spellingCorrector, resMgr, null);
//		while (reader.hasNext()) {
			CAS cas = ae.newCAS();
			reader.getNext(cas);
			ae.process(cas);
//		}

//		SimplePipeline.runPipeline(reader, showText, segmenter, markSentenceBeginnings, numericAnnotator,
//				punctuationAnnotator, namedEntityAnnotator, markTokensToConsider, dictionaryChecker1,
//				dictionaryChecker2, markTokensToCorrect, findMissingSpaces, generateRankKeyboard, generateRankLitkey,
//				generateRankLevenshtein, generateRankPhoneme, anomalyReplacer, lmReranker, anomalyReplacer_random,
//				changeApplier, segmenter, testResult);
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);
	}
}
