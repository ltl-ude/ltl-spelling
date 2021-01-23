package de.unidue.ltl.spelling.experiments;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.pipeline.SimplePipeline;

import de.unidue.ltl.spelling.preprocessing.SpellingErrorListWriter;
import de.unidue.ltl.spelling.reader.SpellingReader;

public class CollectMisspellingPhonemes {

	public static void main(String[] args) throws UIMAException, IOException {

//		System.out.println("CItA");
//		evalCItA();
//		System.out.println();
//		System.out.println("Litkey");
//		evalLitkey();
//		System.out.println();
		System.out.println("Merlin DE");
		evalMerlinDE();
//		System.out.println();
//		System.out.println("Merlin IT");
//		evalMerlinIT();
//		System.out.println();
//		System.out.println("Merlin CZ");
//		evalMerlinCZ();
	}

	public static void evalCItA() throws UIMAException, IOException {
		String cita_lang = "it";
		String cita_corpus = "src/main/resources/corpora/CItA_spelling.xml";
		String outPath = "src/main/resources/dictionaries/CItA_misspellingPhonemes.tsv";
		writeMisspellingPhonemes(cita_lang, cita_corpus, outPath);
	}

	public static void evalLitkey() throws UIMAException, IOException {
		String litkey_lang = "de";
		String litkey_corpus = "src/main/resources/corpora/litkey_spelling.xml";
		String outPath = "src/main/resources/dictionaries/Litkey_misspellingPhonemes.tsv";
		writeMisspellingPhonemes(litkey_lang, litkey_corpus, outPath);
	}

	public static void evalMerlinDE() throws UIMAException, IOException {
		String merlin_lang = "de";
		String merlin_corpus = "src/main/resources/corpora/Merlin_spelling_german.xml";
		String outPath = "src/main/resources/dictionaries/MerlinDE_misspellingPhonemes.tsv";
		writeMisspellingPhonemes(merlin_lang, merlin_corpus, outPath);
	}

	public static void evalMerlinIT() throws UIMAException, IOException {
		String merlin_lang = "it";
		String merlin_corpus = "src/main/resources/corpora/Merlin_spelling_italian.xml";
		String outPath = "src/main/resources/dictionaries/MerlinIT_misspellingPhonemes.tsv";
		writeMisspellingPhonemes(merlin_lang, merlin_corpus, outPath);
	}

	public static void evalMerlinCZ() throws UIMAException, IOException {
		String merlin_lang = "cz";
		String merlin_corpus = "src/main/resources/corpora/Merlin_spelling_czech.xml";
		String outPath = "src/main/resources/dictionaries/MerlinCZ_misspellingPhonemes.tsv";
		writeMisspellingPhonemes(merlin_lang, merlin_corpus, outPath);
	}

	public static void writeMisspellingPhonemes(String lang, String corpus_path, String outPath)
			throws UIMAException, IOException {

		CollectionReader reader = getReader(corpus_path, lang);
		AnalysisEngineDescription misspellingsToPhonemes = createEngineDescription(SpellingErrorListWriter.class,
				SpellingErrorListWriter.PARAM_LANGUAGE, lang, SpellingErrorListWriter.PARAM_OUTPUT_PATH, outPath);
		SimplePipeline.runPipeline(reader, misspellingsToPhonemes);
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {

		return CollectionReaderFactory.createReader(SpellingReader.class, SpellingReader.PARAM_SOURCE_FILE, path,
				SpellingReader.PARAM_LANGUAGE_CODE, language, SpellingReader.PARAM_FOR_ERROR_DETECTION, true);
	}
}