package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;
import org.dkpro.core.io.text.TextReader;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.Dictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.LinkingMorphemes;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.SimpleDictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.BananaSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DataDrivenSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.LeftToRightSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_KeyboardDistance;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinGrapheme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinPhoneme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_Litkey;
import de.unidue.ltl.spelling.normalization.ApplyChanges;
import de.unidue.ltl.spelling.normalization.ResultTester;
import de.unidue.ltl.spelling.normalization.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.DictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.MarkSentenceBeginnings;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToConsider;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToCorrect;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PrintText;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.types.Numeric;

public class Experiment_usingComponentsDirectly {

	public static void main(String[] args) throws UIMAException, IOException {
//		runEnglish();
		runGerman();
	}

	public static void runEnglish() throws UIMAException, IOException {
		String hunspell_en = "src/main/resources/dictionaries/hunspell_en_US.txt";
//		String dict_en_1 = "dictionaries/en-testDict1.txt";
//		String dict_en_2 = "dictionaries/en-testDict2.txt";
		String[] types_to_exclude = new String[] { Numeric.class.getName() };

		// Mock-LM
		// Issue: cannot serialize
//		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
//		cfd.inc(2, "Hello there");
//		cfd.inc(2, "this Frequency");
//		cfd.inc(2, "Frequency Distrbution");
//		cfd.inc(2, "Distrbution is");
//		cfd.inc(2, "is about");
//		cfd.inc(2, "about to");
//		cfd.inc(2, "to be");
//		cfd.inc(2, "be serialized");
		// TODO: figure out best implementation
//		String[] lmPaths = CFD_Serializer.serialize(cfd);

		CollectionReader reader = getReader("en-testData", "en");
		// Just for now
		AnalysisEngineDescription showText = createEngineDescription(PrintText.class);
		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,
//				MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, types_to_exclude}
		);

		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_en, DictionaryChecker.PARAM_LANGUAGE, "en");
//		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
//				DictionaryChecker.PARAM_DICTIONARY_FILE, dict_en_1, DictionaryChecker.PARAM_LANGUAGE, "en");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRank1 = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_en,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_EN-manual.txt",
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription testResult = createEngineDescription(ResultTester.class);

		SimplePipeline.runPipeline(reader, showText, segmenter, markSentenceBeginnings, numericAnnotator,
				punctuationAnnotator, namedEntityAnnotator, markTokensToConsider, dictionaryChecker1,
//				dictionaryChecker2,
				markTokensToCorrect, generateRank1, anomalyReplacer, changeApplier, segmenter, testResult);
	}

	public static void runGerman() throws UIMAException, IOException {
		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
//		String dict_1_de = "dictionaries/de-testDict1.txt";
//		String dict_2_de = "dictionaries/de-testDict2.txt";

		// Issue: cannot serialize directly
//		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
//		cfd.inc(2,"Hallo ,");
//		cfd.inc(2,", diese");
//		cfd.inc(2, "diese Frequency");
//		cfd.inc(2,"Frequency Distribution");
//		cfd.inc(2,"Distribution wird");
//		cfd.inc(2,"wird jetzt");
//		cfd.inc(2,"jetzt serialisiert");
//		cfd.inc(2,"serialisiert .");
		// Figure out how to
//		String[] lmPaths = CFD_Serializer.serialize(cfd);

//		Dictionary dict = new SimpleDictionary(Paths.get(hunspell_de).toFile());
//		LinkingMorphemes linkingMorphemesDE = new LinkingMorphemes(new String[] { "e", "s", "es", "n", "en", "er", "ens" });
////		LeftToRightSplitterAlgorithm splitter = new LeftToRightSplitterAlgorithm();
////		BananaSplitterAlgorithm splitter = new BananaSplitterAlgorithm();
//		DataDrivenSplitterAlgorithm splitter = new DataDrivenSplitterAlgorithm();
//		splitter.setDictionary(dict);
//		splitter.setLinkingMorphemes(linkingMorphemesDE);
//		System.out.println(splitter.split("Lehrstelle").getAllSplits());
//		System.exit(0);

		CollectionReader reader = getReader("de-testData", "de");
		AnalysisEngineDescription showText = createEngineDescription(PrintText.class);
		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,
//				MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, types_to_exclude}
		);

		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_de, DictionaryChecker.PARAM_LANGUAGE, "de");
//		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
//				DictionaryChecker.PARAM_DICTIONARY_FILE, dict_1_de, DictionaryChecker.PARAM_LANGUAGE, "de");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_KeyboardDistance.PARAM_NUM_OF_CANDIDATES_TO_GENERATE,10,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_DE-manual.txt",
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankLitkey = createEngineDescription(GenerateAndRank_Litkey.class,
				GenerateAndRank_Litkey.PARAM_LANGUAGE, "de", GenerateAndRank_Litkey.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_Litkey.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, 5);
		AnalysisEngineDescription generateRankLevenshtein = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class,
				GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES, hunspell_de,
				GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE,10,
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true
				,
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
				"src/main/resources/matrixes/RDMatrix_deletion_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
				"src/main/resources/matrixes/RDMatrix_insertion_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
				"src/main/resources/matrixes/RDMatrix_substitution_DE_withUpper.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
				"src/main/resources/matrixes/RDMatrix_transposition_DE_withUpper.tsv"
				);
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class, GenerateAndRank_LevenshteinPhoneme.PARAM_LANGUAGE, "deu-DE",
				GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES,
				"/Users/mariebexte/ltl-spelling/de.unidue.ltl.spelling/src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt",
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true,
				GenerateAndRank_LevenshteinPhoneme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE,10
				,
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv"
				);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription testResult = createEngineDescription(ResultTester.class);

		SimplePipeline.runPipeline(reader, showText, segmenter, markSentenceBeginnings, numericAnnotator,
				punctuationAnnotator, namedEntityAnnotator, markTokensToConsider, dictionaryChecker1,
//				dictionaryChecker2,
				markTokensToCorrect,
//				generateRankKeyboard,
//				generateRankLitkey,
//				generateRankLevenshtein,
				generateRankPhoneme,
				anomalyReplacer, changeApplier, segmenter, testResult);
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);
	}
}
