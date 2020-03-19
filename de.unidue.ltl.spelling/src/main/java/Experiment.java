import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.io.text.TextReader;

import de.unidue.ltl.spelling.engine.SpellingCorrector;
import de.unidue.ltl.spelling.engine.SpellingCorrector.CandidateSelectionMethod;

public class Experiment {

	public static void main(String[] args) throws UIMAException, IOException {
		runEnglish();

	}

	public static void runEnglish() throws UIMAException, IOException {
		String[] dicts_en = new String[] { "dictionaries/en-testDict1.txt", "dictionaries/en-testDict2.txt" };
		String[] types_to_exclude = new String[] {};

		String languageModel = "/Volumes/Marie2/web1t/en/data"; // does not give unigram count

		CollectionReader reader = getReader("en-testData", "en");
		AnalysisEngine engine = createEngine(SpellingCorrector.class, SpellingCorrector.PARAM_LANGUAGE, "en",
				SpellingCorrector.PARAM_SCORE_THRESHOLD, 2,
				SpellingCorrector.PARAM_ADDITIONAL_DICTIONARIES, dicts_en,
				SpellingCorrector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, types_to_exclude,
				SpellingCorrector.PARAM_PHONETIC_CANDIDATE_GENERATION, true,
				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL_PROBABILITY,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
				SpellingCorrector.PARAM_LANGUAGE_MODEL_PATH, languageModel);
		SimplePipeline.runPipeline(reader, engine);
	}

	public static void runGerman() throws UIMAException, IOException {
		String[] dicts_de = new String[] { "dictionaries/de-testDict1.txt", "dictionaries/de-testDict2.txt" };
		String[] types_to_exclude = new String[] {};

		String languageModel = "/Volumes/Marie2/web1t_de/export/data/ltlab/data/web1t/EUROPEAN/data/test"; // Just
																											// unigrams
																											// atm

		CollectionReader reader = getReader("de-testData", "de");
		AnalysisEngine engine = createEngine(SpellingCorrector.class, SpellingCorrector.PARAM_LANGUAGE, "de",
				SpellingCorrector.PARAM_SCORE_THRESHOLD, 2, SpellingCorrector.PARAM_ADDITIONAL_DICTIONARIES, dicts_de,
				SpellingCorrector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, types_to_exclude,
				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
				SpellingCorrector.PARAM_LANGUAGE_MODEL_PATH, languageModel);
		SimplePipeline.runPipeline(reader, engine);

	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);
	}

}
