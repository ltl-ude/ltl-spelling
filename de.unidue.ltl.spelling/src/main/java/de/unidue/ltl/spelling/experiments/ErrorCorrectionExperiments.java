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

import de.unidue.ltl.spelling.candidateReranking.LanguageModelReranker;
import de.unidue.ltl.spelling.evaluation.EvaluateErrorCorrection;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_FindMissingSpace;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_KeyboardDistance;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinGrapheme;
import de.unidue.ltl.spelling.generateAndRank.GenerateAndRank_LevenshteinPhoneme;
import de.unidue.ltl.spelling.normalization.ApplyChanges;
import de.unidue.ltl.spelling.normalization.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.normalization.TextPrinter;
import de.unidue.ltl.spelling.reader.SpellingReader;

public class ErrorCorrectionExperiments{

	public static void main(String[] args) throws IOException, UIMAException{
		int num_candidates_per_method = 3;
//		int num_candidates_per_method = 10;
		int n_gram_size = 3;

		// CiTA
		String cita_lang = "it";
		String cita_path = "src/main/resources/corpora/cita_spelling.xml";
		String hunspell_it = "src/main/resources/dictionaries/hunspell_Italian.txt";
		String phonetic_hunspell_it = "src/main/resources/dictionaries/hunspell_Italian_phoneme_map.txt";
		String keyboard_distances_it = "src/main/resources/matrixes/keyboardDistance_IT-manual.txt";
		String web1t_path_it = System.getenv("DKPRO_HOME") + "web1t_it/";
		runErrorCorrection("", "full", cita_lang, cita_path, hunspell_it, phonetic_hunspell_it, keyboard_distances_it, web1t_path_it, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "grapheme", cita_lang, cita_path, hunspell_it, phonetic_hunspell_it, keyboard_distances_it, web1t_path_it, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "phoneme", cita_lang, cita_path, hunspell_it, phonetic_hunspell_it, keyboard_distances_it, web1t_path_it, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "keyboard", cita_lang, cita_path, hunspell_it, phonetic_hunspell_it, keyboard_distances_it, web1t_path_it, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "missing_spaces", cita_lang, cita_path, hunspell_it, phonetic_hunspell_it, keyboard_distances_it, web1t_path_it, num_candidates_per_method, n_gram_size);

		// Litkey
		String litkey_lang = "de";
		String litkey_path = "";
		String hunspell_de = "";
		String phonetic_hunspell_de = "";
		String keyboard_distances_de = "";
		String web1t_path_de = System.getenv("DKPRO_HOME") + "web1t_de_fixed/";
		runErrorCorrection("", "full", litkey_lang, litkey_path, hunspell_de, phonetic_hunspell_de, keyboard_distances_de, web1t_path_de, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "grapheme", litkey_lang, litkey_path, hunspell_de, phonetic_hunspell_de, keyboard_distances_de, web1t_path_de, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "phoneme", litkey_lang, litkey_path, hunspell_de, phonetic_hunspell_de, keyboard_distances_de, web1t_path_de, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "keyboard", litkey_lang, litkey_path, hunspell_de, phonetic_hunspell_de, keyboard_distances_de, web1t_path_de, num_candidates_per_method, n_gram_size);
		runErrorCorrection("", "missing_spaces", litkey_lang, litkey_path, hunspell_de, phonetic_hunspell_de, keyboard_distances_de, web1t_path_de, num_candidates_per_method, n_gram_size);
		
		// Merlin

		// ASAP?

	}

	private static void runErrorCorrection(String config_name, String setting, String lang, String corpus_path, String dict_path, String phonetic_dict_path,
			String keyboard_distance_path, String web1t_path, int num_candidates_per_method, int n_gram_size) throws UIMAException, IOException {

		// Create web1t language model to set via parameter
		ExternalResourceDescription web1t = createExternalResourceDescription(Web1TFrequencyCountResource.class,
				Web1TFrequencyCountResource.PARAM_LANGUAGE, "it", Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL,
				"1", Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "5",
				Web1TFrequencyCountResource.PARAM_INDEX_PATH, web1t_path);

		CollectionReader reader = getReader(corpus_path, lang);
		AnalysisEngineDescription generateRankGrapheme = createEngineDescription(
				GenerateAndRank_LevenshteinGrapheme.class,
				GenerateAndRank_LevenshteinGrapheme.PARAM_DICTIONARIES, dict_path,
				GenerateAndRank_LevenshteinGrapheme.PARAM_LOWERCASE, false,
				GenerateAndRank_LevenshteinGrapheme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, num_candidates_per_method,
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_EN.tsv",
//				GenerateAndRank_LevenshteinGrapheme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_EN.tsv",
				GenerateAndRank_LevenshteinGrapheme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankPhoneme = createEngineDescription(
				GenerateAndRank_LevenshteinPhoneme.class,
				GenerateAndRank_LevenshteinPhoneme.PARAM_DICTIONARIES, phonetic_dict_path,
				GenerateAndRank_LevenshteinPhoneme.PARAM_LANGUAGE, lang,
				GenerateAndRank_LevenshteinPhoneme.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, num_candidates_per_method,
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_DELETION,
//				"src/main/resources/matrixes/RDMatrix_deletion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_INSERTION,
//				"src/main/resources/matrixes/RDMatrix_insertion_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_SUBSTITUTION,
//				"src/main/resources/matrixes/RDMatrix_substitution_Sampa.tsv",
//				GenerateAndRank_LevenshteinPhoneme.PARAM_WEIGHT_FILE_TRANSPOSITION,
//				"src/main/resources/matrixes/RDMatrix_transposition_Sampa.tsv",
				GenerateAndRank_LevenshteinPhoneme.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankKeyboard = createEngineDescription(
				GenerateAndRank_KeyboardDistance.class,
				GenerateAndRank_KeyboardDistance.PARAM_DICTIONARIES, dict_path,
				GenerateAndRank_KeyboardDistance.PARAM_KEYBOARD_DISTANCES_FILE, keyboard_distance_path,
				GenerateAndRank_KeyboardDistance.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, num_candidates_per_method,
				GenerateAndRank_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, true);
		AnalysisEngineDescription generateRankMissingSpaces = createEngineDescription(
				GenerateAndRank_FindMissingSpace.class,
				GenerateAndRank_FindMissingSpace.PARAM_DICTIONARIES, dict_path,
				GenerateAndRank_FindMissingSpace.PARAM_NUM_OF_CANDIDATES_TO_GENERATE, num_candidates_per_method);
		AnalysisEngineDescription lmReranker = createEngineDescription(
				LanguageModelReranker.class,
				LanguageModelReranker.RES_LANGUAGE_MODEL, web1t,
				LanguageModelReranker.RES_LANGUAGE_MODEL_PROMPT_SPECIFIC, web1t,
				LanguageModelReranker.PARAM_NGRAM_SIZE, n_gram_size);
		AnalysisEngineDescription anomalyReplacer = createEngineDescription(
				SpellingAnomalyReplacer.class,
				SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY, new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		AnalysisEngineDescription correctionEvaluator = createEngineDescription(
				EvaluateErrorCorrection.class,
				EvaluateErrorCorrection.PARAM_CONFIG_NAME, config_name);
//		AnalysisEngineDescription textPrinter = createEngineDescription(TextPrinter.class);

		if(setting.equals("full")) {
			SimplePipeline.runPipeline(
					reader,
					generateRankGrapheme,
					generateRankPhoneme,
					generateRankKeyboard,
					generateRankMissingSpaces,
					lmReranker,
					anomalyReplacer,
					changeApplier,
					correctionEvaluator);
		}
		else if(setting.equals("grapheme")) {
			SimplePipeline.runPipeline(
					reader,
					generateRankGrapheme,
					lmReranker,
					anomalyReplacer,
					changeApplier,
					correctionEvaluator);
			
		}
		else if(setting.equals("phoneme")) {
			SimplePipeline.runPipeline(
					reader,
					generateRankPhoneme,
					lmReranker,
					anomalyReplacer,
					changeApplier,
					correctionEvaluator);
			
		}
		else if(setting.equals("keyboard")) {
			SimplePipeline.runPipeline(
					reader,
					generateRankKeyboard,
					lmReranker,
					anomalyReplacer,
					changeApplier,
					correctionEvaluator);
			
		}
		else if(setting.equals("missing_spaces")) {
			SimplePipeline.runPipeline(
					reader,
					generateRankMissingSpaces,
					lmReranker,
					anomalyReplacer,
					changeApplier,
					correctionEvaluator);
		}
	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {

		return CollectionReaderFactory.createReader(SpellingReader.class, SpellingReader.PARAM_SOURCE_FILE, path,
				SpellingReader.PARAM_LANGUAGE_CODE, language);
	}

}
