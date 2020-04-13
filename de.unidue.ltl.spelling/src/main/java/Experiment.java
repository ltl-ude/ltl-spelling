import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;
import org.dkpro.core.api.frequency.util.FrequencyDistribution;
import org.dkpro.core.io.text.TextReader;

import de.unidue.ltl.spelling.engine.SpellingCorrector;
import de.unidue.ltl.spelling.engine.SpellingCorrector.CandidateSelectionMethod;
import de.unidue.ltl.spelling.utils.CFD_Serializer;

public class Experiment {

	public static void main(String[] args) throws UIMAException, IOException {
		runEnglish();
//		runGerman();
	}

	public static void runEnglish() throws UIMAException, IOException {
		String[] dicts_en = new String[] { "dictionaries/en-testDict1.txt", "dictionaries/en-testDict2.txt" };
		String[] types_to_exclude = new String[] {};

		// Issue: cannot serialize
		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2,"Hello there");
		cfd.inc(2,"this Frequency");
		cfd.inc(2,"Frequency Distrbution");
		cfd.inc(2,"Distrbution is");
		cfd.inc(2,"is about");
		cfd.inc(2,"about to");
		cfd.inc(2,"to be");
		cfd.inc(2,"be serialized");
		
		String[] lmPaths = CFD_Serializer.serialize(cfd);

		CollectionReader reader = getReader("en-testData", "en");
		AnalysisEngine engine = createEngine(SpellingCorrector.class, SpellingCorrector.PARAM_LANGUAGE, "en",
				SpellingCorrector.PARAM_ADDITIONAL_DICTIONARIES, dicts_en,

				/* ErrorDetector */

				// Only address if necessary (default: true)
//				SpellingCorrector.PARAM_EXCLUDE_NAMED_ENTITIES, false,
//				SpellingCorrector.PARAM_EXCLUDE_NUMERIC, false,
//				SpellingCorrector.PARAM_EXCLUDE_PUNCTUATION, false,
				// Can pass additional types
//				SpellingCorrector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, types_to_exclude,

				/* Candidate Generation */

				// Only address if candidates should be generated based on phonemes (as opposed
				// to graphemes); default: false
//				SpellingCorrector.PARAM_PHONETIC_CANDIDATE_GENERATION, true,
				// Default: 1
//				SpellingCorrector.PARAM_SCORE_THRESHOLD, 2,
				// Default: false
				SpellingCorrector.PARAM_INCLUDE_TRANSPOSITION, true,

				/* Candidate Selection */

//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.CUSTOM_LEVENSHTEIN,
//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.KEYBOARD_DISTANCE,
				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL,
//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.DEFAULT_LEVENSHTEIN,
//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.PHONETIC,
				// A second method is optional
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.CUSTOM_LEVENSHTEIN,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.KEYBOARD_DISTANCE,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.DEFAULT_LEVENSHTEIN,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.PHONETIC,

				/* In case candidates are to be selected based on custom weights */
				SpellingCorrector.PARAM_MATRIX_DELETION, null, SpellingCorrector.PARAM_MATRIX_INSERTION, null,
				SpellingCorrector.PARAM_MATRIX_SUBSTITUTION, null, SpellingCorrector.PARAM_MATRIX_TRANSPOSITION, null,

				/* If candidates are to be ranked based on language model information */
				SpellingCorrector.PARAM_NGRAM_SIZE, 1,
				SpellingCorrector.PARAM_CUSTOM_LANGUAGE_MODEL_PATHS, lmPaths,
				SpellingCorrector.PARAM_CUSTOM_LM_WEIGHT, 0.3f);

		SimplePipeline.runPipeline(reader, engine);
	}

	public static void runGerman() throws UIMAException, IOException {
		String[] dicts_de = new String[] { "dictionaries/de-testDict1.txt", "dictionaries/de-testDict2.txt" };
		String[] types_to_exclude = new String[] {};

		// Issue: cannot serialize
		ConditionalFrequencyDistribution<Integer, String> cfd = new ConditionalFrequencyDistribution<Integer, String>();
		cfd.inc(2,"Hallo ,");
		cfd.inc(2,", diese");
		cfd.inc(2, "diese Frequency");
		cfd.inc(2,"Frequency Distrbution");
		cfd.inc(2,"Distribution wird");
		cfd.inc(2,"wird jetzt");
		cfd.inc(2,"jetzt serialisiert");
		cfd.inc(2,"serialisiert .");

		String[] lmPaths = CFD_Serializer.serialize(cfd);
		
		CollectionReader reader = getReader("de-testData", "de");
		AnalysisEngine engine = createEngine(SpellingCorrector.class, SpellingCorrector.PARAM_LANGUAGE, "de",
				SpellingCorrector.PARAM_ADDITIONAL_DICTIONARIES, dicts_de,

				/* ErrorDetector */

				// Only address if necessary (default: true)
//				SpellingCorrector.PARAM_EXCLUDE_NAMED_ENTITIES, false,
//				SpellingCorrector.PARAM_EXCLUDE_NUMERIC, false,
//				SpellingCorrector.PARAM_EXCLUDE_PUNCTUATION, false,
				// Can pass additional types
//				SpellingCorrector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, types_to_exclude,

				/* Candidate Generation */

				// Only address if candidates should be generated based on phonemes (as opposed
				// to graphemes); default: false
				SpellingCorrector.PARAM_PHONETIC_CANDIDATE_GENERATION, true,
				// Default: 1
				SpellingCorrector.PARAM_SCORE_THRESHOLD, 2,
				// Default: false
				SpellingCorrector.PARAM_INCLUDE_TRANSPOSITION, true,

				/* Candidate Selection */

//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.CUSTOM_MATRIX,
//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.KEYBOARD_DISTANCE,
				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL,
//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
//				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.PHONETIC,
				// A second method is optional
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.CUSTOM_MATRIX,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.KEYBOARD_DISTANCE,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.PHONETIC,

				/* In case candidates are to be selected based on custom weights */
				SpellingCorrector.PARAM_MATRIX_DELETION, null, SpellingCorrector.PARAM_MATRIX_INSERTION, null,
				SpellingCorrector.PARAM_MATRIX_SUBSTITUTION, null, SpellingCorrector.PARAM_MATRIX_TRANSPOSITION, null,

				/* If candidates are to be ranked based on language model information */
				SpellingCorrector.PARAM_NGRAM_SIZE, 1, SpellingCorrector.PARAM_CUSTOM_LANGUAGE_MODEL_PATHS, lmPaths,
				SpellingCorrector.PARAM_CUSTOM_LM_WEIGHT, 0.3f);

		SimplePipeline.runPipeline(reader, engine);

	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);
	}
}
