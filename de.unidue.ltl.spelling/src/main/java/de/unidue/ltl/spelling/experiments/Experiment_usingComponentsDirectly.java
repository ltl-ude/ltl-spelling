package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;
import org.dkpro.core.io.text.TextReader;

import de.drni.bananasplit.BananaSplit;
import de.drni.bananasplit.xmldict.XmlDictionary;
import de.tudarmstadt.ukp.dkpro.core.bananasplit.BananaSplitter;
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.Dictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.LinkingMorphemes;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.SimpleDictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.AsvToolboxSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.BananaSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DataDrivenSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DecompoundingTree;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.JWordSplitterAlgorithm;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.candidategeneration.CandidateGeneratorAndRanker_KeyboardDistance;
import de.unidue.ltl.spelling.normalization.ApplyChanges;
import de.unidue.ltl.spelling.normalization.PrintText;
import de.unidue.ltl.spelling.normalization.ResultTester;
import de.unidue.ltl.spelling.normalization.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.DictionaryChecker;
import de.unidue.ltl.spelling.preprocessing.MarkSentenceBeginnings;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToConsider;
import de.unidue.ltl.spelling.preprocessing.MarkTokensToCorrect;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;

public class Experiment_usingComponentsDirectly {

	public static void main(String[] args) throws UIMAException, IOException {
		runEnglish();
//		runGerman();
	}

	public static void runEnglish() throws UIMAException, IOException {
		String hunspell_en = "src/main/resources/dictionaries/hunspell_en_US.txt";
		String dict_en_1 = "dictionaries/en-testDict1.txt";
		String dict_en_2 = "dictionaries/en-testDict2.txt";
		
//		String[] types_to_exclude = new String[] {};

		// Mock-LM
		// Issue: cannot serialize
		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2, "Hello there");
		cfd.inc(2, "this Frequency");
		cfd.inc(2, "Frequency Distrbution");
		cfd.inc(2, "Distrbution is");
		cfd.inc(2, "is about");
		cfd.inc(2, "about to");
		cfd.inc(2, "to be");
		cfd.inc(2, "be serialized");

		// TODO: figure out best implementation
//		String[] lmPaths = CFD_Serializer.serialize(cfd);

		CollectionReader reader = getReader("en-testData", "en");
		
		// Just for now
		AnalysisEngineDescription showText = createEngineDescription(PrintText.class);
		
//		AnalysisEngineDescription lemmatizer = createEngineDescription(MateLemmatizer.class);
//		AnalysisEngineDescription bananaSplit = createEngineDescription(BananaSplitter.class,
//				BananaSplitter.PARAM_DICT_PATH, iGermanXML);

//		BananaSplitterAlgorithm banana = new BananaSplitterAlgorithm();
//		banana.setDictionary(new SimpleDictionary(hunspell_en));
//		DecompoundingTree dt = banana.split("homeworkhours");
//		System.out.println("Splits: "+dt.getSplits().size());
		
//		AsvToolboxSplitterAlgorithm splitter = new AsvToolboxSplitterAlgorithm(null, null, null);

		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		AnalysisEngineDescription markSentenceBeginnings = createEngineDescription(MarkSentenceBeginnings.class);
		AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
		AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);

		AnalysisEngineDescription markTokensToConsider = createEngineDescription(MarkTokensToConsider.class
//				,
//				MarkTokensToConsider.PARAM_TYPES_TO_IGNORE, new String[]{Numeric.class.getName()}
		);

		AnalysisEngineDescription dictionaryChecker1 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_en,
				DictionaryChecker.PARAM_LANGUAGE, "de");
		AnalysisEngineDescription dictionaryChecker2 = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, dict_en_1,
				DictionaryChecker.PARAM_LANGUAGE, "de");
		AnalysisEngineDescription markTokensToCorrect = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRank1 = createEngineDescription(
				CandidateGeneratorAndRanker_KeyboardDistance.class,
				CandidateGeneratorAndRanker_KeyboardDistance.PARAM_DICTIONARIES, hunspell_en,
				CandidateGeneratorAndRanker_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_EN-manual.txt",
				CandidateGeneratorAndRanker_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription testResult = createEngineDescription(ResultTester.class);

		SimplePipeline.runPipeline(
				reader,
				showText,
				segmenter,
				markSentenceBeginnings,
				numericAnnotator,
				punctuationAnnotator,
				namedEntityAnnotator,
				markTokensToConsider,
				dictionaryChecker1,
				dictionaryChecker2,
				markTokensToCorrect,
				generateRank1,
				anomalyReplacer,
				changeApplier,
				segmenter,
				testResult);
	}

	public static void runGerman() throws UIMAException, IOException {
		String hunspell_de = "src/main/resources/dictionaries/hunspell_DE.txt";
		String dict_1_de = "dictionaries/de-testDict1.txt";
		String dict_2_de = "dictionaries/de-testDict2.txt";
		String dict_dec = "dictionaries/de_decompounding_text.txt";
		String iGermanXML = "src/main/resources/dictionaries/igerman98_all.xml";
		
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

		CollectionReader reader = getReader("de-testData", "de");
		
		AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
		
		Path dictionaryPath = Paths.get(hunspell_de);
		Dictionary dic = new SimpleDictionary(dictionaryPath.toFile());
		System.out.println("size: "+dic.getAll().size());
//		System.out.println("haus: "+dic.contains("haus"));
//		System.out.println("haus: "+dic.contains("banane"));
//		System.out.println("haus: "+dic.contains("apfel"));
//		System.out.println("4th: "+dic.getAll().get(4));
		
//		BananaSplitterAlgorithm banana = new BananaSplitterAlgorithm();
//		banana.setDictionary(dic);
//		DecompoundingTree dt = banana.split("Butterbrot");
		
		JWordSplitterAlgorithm splitter = new JWordSplitterAlgorithm();
		splitter.setDictionary(dic);
		
		List<String> morphemes = new ArrayList<String>();
		morphemes.add("s");
		morphemes.add("ung");
//		DataDrivenSplitterAlgorithm splitter = new DataDrivenSplitterAlgorithm();
//		splitter.setDictionary(dic);
//		splitter.setLinkingMorphemes(new LinkingMorphemes(morphemes));
		
		DecompoundingTree dt = splitter.split("Gruppe");
		System.out.println("Splits: "+dt.getSplits().size()); //1
		System.out.println("Splits: "+dt.getSplits().toString()); //array
		System.out.println("Splits: "+dt.getSplits().get(0).getSplits().size()); //
		System.out.println("Splits: "+dt.getSplits().get(0).isCompound()); //
		System.out.println("Splits: "+dt.getSplits().get(0).getSplits().get(0).toString());
		System.exit(0);
		
//		BananaSplit split = new BananaSplit(new XmlDictionary(iGermanXML));
//		try {
//			System.out.println(split.splitCompound("eigenernte"));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		AnalysisEngineDescription dictionaryChecker = createEngineDescription(DictionaryChecker.class,
				DictionaryChecker.PARAM_DICTIONARY_FILE, hunspell_de);
		AnalysisEngineDescription markTokensToCheck = createEngineDescription(MarkTokensToCorrect.class);
		AnalysisEngineDescription generateRank1 = createEngineDescription(
				CandidateGeneratorAndRanker_KeyboardDistance.class,
				CandidateGeneratorAndRanker_KeyboardDistance.PARAM_DICTIONARIES, hunspell_de,
				CandidateGeneratorAndRanker_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE,
				"src/main/resources/matrixes/keyboardDistance_DE-manual.txt",
				CandidateGeneratorAndRanker_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
				new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription testResult = createEngineDescription(ResultTester.class);

		SimplePipeline.runPipeline(reader, segmenter, dictionaryChecker, markTokensToCheck, generateRank1,
				anomalyReplacer, changeApplier, segmenter, testResult);
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);
	}
}
