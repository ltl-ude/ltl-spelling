package de.unidue.ltl.spelling.engine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.normalization.ApplyChanges;
import de.unidue.ltl.spelling.candidategeneration.CorrectionCandidateGenerator_Grapheme;
import de.unidue.ltl.spelling.candidategeneration.CorrectionCandidateGenerator_Phoneme;
import de.unidue.ltl.spelling.candidategeneration.ErrorDetector;
import de.unidue.ltl.spelling.candidateselection.CorrectionCandidateSelector_CustomMatrixes;
import de.unidue.ltl.spelling.candidateselection.CorrectionCandidateSelector_Distance;
import de.unidue.ltl.spelling.candidateselection.CorrectionCandidateSelector_KeyboardDistance;
import de.unidue.ltl.spelling.candidateselection.CorrectionCandidateSelector_LanguageModel;
import de.unidue.ltl.spelling.candidateselection.CorrectionCandidateSelector_LitKey;
import de.unidue.ltl.spelling.normalization.ResultTester;
import de.unidue.ltl.spelling.normalization.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.resources.FrequencyDistributionLanguageModel;
import de.unidue.ltl.spelling.resources.LanguageModelResource;
import de.unidue.ltl.spelling.resources.Web1TLanguageModel;

public class SpellingCorrector extends JCasAnnotator_ImplBase {

	/*
	 * General Parameters: 
	 * - Language ("en" and "de" are supported) 
	 * - [Optional] Passing a list of paths to additional dictionaries
	 */

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	// Custom dictionaries for error detection and candidate generation
	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false)
	private String[] dictionaries;

	/*
	 * For ErrorDetector: Parameters determining which types are to be disregarded as errors
	 * - Boolean switches for types that are by default not considered (default: false)
	 * - [Optional] Passing a list of additional types to exclude
	 */

	// Referring to de.unidue.ltl.spelling.types.Numeric
	public static final String PARAM_EXCLUDE_NUMERIC = "excludeNumeric";
	@ConfigurationParameter(name = PARAM_EXCLUDE_NUMERIC, mandatory = true, defaultValue = "true")
	private boolean excludeNumeric;

	// Referring to de.unidue.ltl.spelling.types.Punctuation
	public static final String PARAM_EXCLUDE_PUNCTUATION = "excludePunctuation";
	@ConfigurationParameter(name = PARAM_EXCLUDE_PUNCTUATION, mandatory = true, defaultValue = "true")
	private boolean excludePunctuation;

	// Referring to de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
	public static final String PARAM_EXCLUDE_NAMED_ENTITIES = "excludeNamedEntities";
	@ConfigurationParameter(name = PARAM_EXCLUDE_NAMED_ENTITIES, mandatory = true, defaultValue = "true")
	private boolean excludeNamedEntities;

	public static final String PARAM_ADDITIONAL_TYPES_TO_EXCLUDE = "additionalTypesToExclude";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, mandatory = false)
	private String[] additionalTypesToExclude;

	/*
	 * For CandidateGeneration:
	 * - Whether to generate based on phonemes (default:false)
	 * - If transposition should be included as an additional operation (Damerau-Levenshtein Distance; default: false)
	 * - Up to which distance threshold candidates are to be considered (default: 1)
	 */

	public static final String PARAM_PHONETIC_CANDIDATE_GENERATION = "phoneticCandidateGeneration";
	@ConfigurationParameter(name = PARAM_PHONETIC_CANDIDATE_GENERATION, mandatory = true, defaultValue = "false")
	private boolean phoneticCandidateGeneration;

	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "false")
	private boolean includeTransposition;

	public static final String PARAM_SCORE_THRESHOLD = "scoreThreshold";
	@ConfigurationParameter(name = PARAM_SCORE_THRESHOLD, mandatory = true, defaultValue = "1")
	private int scoreThreshold;

	/*
	 * For CandidateSelection: 
	 * - Selection of a first-level candidate selection method from CandidateSelectionMethod enum 
	 * - [Optional] selection of a second-level candidate selection method 
	 * - [If CandidateSelectionMethod.CUSTOM_MATRIX] Passing Matrixes for insertion, deletion, substitution, transposition
	 * - [If CandidateSelectionMethod.LANGUAGE_MODEL] Ngram size based on which probabilities should be determined
	 * - [If CandidateSelectionMethod.LANGUAGE_MODEL] [Optional] Path to serialized FrequencyDistribution (addtional custom language model)
	 * - [If CandidateSelectionMethod.LANGUAGE_MODEL && custom FrequencyDistribution passed] Weight of custom LM vs default
	 */

	public static final String PARAM_FIRST_LEVEL_SELECTION_METHOD = "candidateSelectionMethod_firstLevel";
	@ConfigurationParameter(name = PARAM_FIRST_LEVEL_SELECTION_METHOD, mandatory = true, defaultValue = "LEVENSHTEIN_DISTANCE")
	protected CandidateSelectionMethod candidateSelectionMethod_firstLevel;

	public static final String PARAM_SECOND_LEVEL_SELECTION_METHOD = "candidateSelectionMethod_secondLevel";
	@ConfigurationParameter(name = PARAM_SECOND_LEVEL_SELECTION_METHOD, mandatory = false)
	protected CandidateSelectionMethod candidateSelectionMethod_secondLevel;

	public static final String PARAM_MATRIX_INSERTION = "levenshteinMatrixInsertion";
	@ConfigurationParameter(name = PARAM_MATRIX_INSERTION, mandatory = false)
	private String filePathInsertion;

	public static final String PARAM_MATRIX_DELETION = "levenshteinMatrixDeletion";
	@ConfigurationParameter(name = PARAM_MATRIX_DELETION, mandatory = false)
	private String filePathDeletion;

	public static final String PARAM_MATRIX_SUBSTITUTION = "levenshteinMatrixSubstitution";
	@ConfigurationParameter(name = PARAM_MATRIX_SUBSTITUTION, mandatory = false)
	private String filePathSubstitution;

	public static final String PARAM_MATRIX_TRANSPOSITION = "levenshteinMatrixTransposition";
	@ConfigurationParameter(name = PARAM_MATRIX_TRANSPOSITION, mandatory = false)
	private String filePathTransposition;

	public static final String PARAM_NGRAM_SIZE = "ngramSize";
	@ConfigurationParameter(name = PARAM_NGRAM_SIZE, mandatory = true, defaultValue = "1")
	private int ngramSize;

	public static final String PARAM_CUSTOM_LANGUAGE_MODEL_PATHS = "languageModelPaths";
	@ConfigurationParameter(name = PARAM_CUSTOM_LANGUAGE_MODEL_PATHS, mandatory = false)
	private String[] languageModelPaths;

	public static final String PARAM_CUSTOM_LM_WEIGHT = "customLM_weight";
	@ConfigurationParameter(name = PARAM_CUSTOM_LM_WEIGHT, mandatory = false)
	private float customLMWeight;

	public enum CandidateSelectionMethod {
		DEFAULT_LEVENSHTEIN, KEYBOARD_DISTANCE, CUSTOM_LEVENSHTEIN, PHONETIC, LANGUAGE_MODEL
	}

	private AnalysisEngine spellingCorrectorEngine = null;
	ExternalResourceDescription customLanguageModel;
	ExternalResourceDescription defaultLanguageModel;
	// TODO: have to be adapted
	private final String web1tPathGerman = "/Volumes/Marie2/web1t_de/export/data/ltlab/data/web1t/EUROPEAN/data/test";
	private final String web1tPathEnglish = "/Volumes/Marie2/web1t/en/data";

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {

		// To ensure that parameters of this class are set before using them below
		super.initialize(context);

		if (!(language.contentEquals("de") || language.contentEquals("en"))) {
			getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
					+ "' was passed, as of now only English ('en') and German ('de') are supported.");
			System.exit(1);
		}

		// Build engine
		try {
			if (language.contentEquals("de")) {
				defaultLanguageModel = createExternalResourceDescription(Web1TLanguageModel.class,
						Web1TLanguageModel.PARAM_MODEL_FILES, web1tPathGerman);
			} else if (language.contentEquals("en")) {
				defaultLanguageModel = createExternalResourceDescription(Web1TLanguageModel.class,
						Web1TLanguageModel.PARAM_MODEL_FILES, web1tPathEnglish);
			}
			// Will never happen because language has already been checked above
			else {
				getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
						+ "' was passed, as of now only English ('en') and German ('de') are supported.");
				System.exit(1);
			}

			List<AnalysisEngineDescription> spellingComponents = new ArrayList<AnalysisEngineDescription>();
			List<String> componentNames = new ArrayList<String>();

			// Preprocessing
			AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
			spellingComponents.add(segmenter);
			componentNames.add("segmenter");

			AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
			spellingComponents.add(numericAnnotator);
			componentNames.add("numericAnnotator");

			AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
			spellingComponents.add(punctuationAnnotator);
			componentNames.add("punctuationAnnotator");

			AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(
					StanfordNamedEntityRecognizer.class);
			spellingComponents.add(namedEntityAnnotator);
			componentNames.add("namedEntityAnnotator");

			// Error Detection
			AnalysisEngineDescription errorDetector = createEngineDescription(ErrorDetector.class,
					ErrorDetector.PARAM_ADDITIONAL_DICTIONARIES, dictionaries,
					ErrorDetector.PARAM_EXCLUDE_NAMED_ENTITIES, excludeNamedEntities,
					ErrorDetector.PARAM_EXCLUDE_NUMERIC, excludeNumeric, ErrorDetector.PARAM_EXCLUDE_PUNCTUATION,
					excludePunctuation, ErrorDetector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, additionalTypesToExclude,
					ErrorDetector.PARAM_LANGUAGE, language);
			spellingComponents.add(errorDetector);
			componentNames.add("errorDetector");

			// Candidate Generation
			AnalysisEngineDescription candidateGenerator;
			if (!phoneticCandidateGeneration) {
				candidateGenerator = createEngineDescription(CorrectionCandidateGenerator_Grapheme.class,
						CorrectionCandidateGenerator_Grapheme.PARAM_LANGUAGE, language,
						CorrectionCandidateGenerator_Grapheme.PARAM_DISTANCE_THRESHOLD, scoreThreshold,
						CorrectionCandidateGenerator_Grapheme.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
						CorrectionCandidateGenerator_Grapheme.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
			} else {
				candidateGenerator = createEngineDescription(CorrectionCandidateGenerator_Phoneme.class,
						CorrectionCandidateGenerator_Phoneme.PARAM_LANGUAGE, language,
						CorrectionCandidateGenerator_Phoneme.PARAM_DISTANCE_THRESHOLD, scoreThreshold,
						CorrectionCandidateGenerator_Phoneme.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
						CorrectionCandidateGenerator_Phoneme.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
			}
			spellingComponents.add(candidateGenerator);
			componentNames.add("candidateGenerator");

			// If a custom LM was passed, create the corresponding resource
			if (languageModelPaths != null) {
				System.out.println(languageModelPaths);
				customLanguageModel = createExternalResourceDescription(FrequencyDistributionLanguageModel.class,
						LanguageModelResource.PARAM_MODEL_FILES, languageModelPaths);
			}

			// If no custom LM provided, but a weight for it: warn
			if (customLMWeight > 0 && languageModelPaths == null) {
				getContext().getLogger().log(Level.WARNING,
						"You did not provide a custom language model via the 'LANGUAGE_MODEL_PATH' parameter, but passed a weight that should be assigned to this nonexistent language model."
								+ "The weight will be ignored and language model probability will be determined based on the default language model alone.");
			}
			// Check whether the weight is in the expected range
			if (customLMWeight > 1.0) {
				getContext().getLogger().log(Level.WARNING, "You set 'PARAM_CUSTOM_LM_WEIGHT' to " + customLMWeight
						+ ", which is greater than 1. A probability between 0.0 and 1.0 was expected, the value is set to its default of 0.5.");
				customLMWeight = 0.5f;
			}

			// CandidateSelection
			AnalysisEngineDescription candidateSelector_firstLevel = getCandidateSelector(
					candidateSelectionMethod_firstLevel);
			spellingComponents.add(candidateSelector_firstLevel);
			componentNames.add("candidateSelector_firstLevel");

			if (candidateSelectionMethod_secondLevel != null) {
				AnalysisEngineDescription candidateSelector_secondLevel = getCandidateSelector(
						candidateSelectionMethod_secondLevel);
				spellingComponents.add(candidateSelector_secondLevel);
				componentNames.add("candidateSelector_secondLevel");
			}
			
			//Warnings if implausible param values were passed
			if (candidateSelectionMethod_firstLevel != CandidateSelectionMethod.CUSTOM_LEVENSHTEIN
					&& candidateSelectionMethod_secondLevel != CandidateSelectionMethod.CUSTOM_LEVENSHTEIN) {
				if (filePathDeletion != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for deletion was passed as '"
							+ filePathDeletion
							+ ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
				if (filePathInsertion != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for insertion was passed as '"
							+ filePathInsertion
							+ ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
				if (filePathSubstitution != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for substitution was passed as '"
							+ filePathSubstitution
							+ ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
				if (filePathTransposition != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for transposition was passed as '"
							+ filePathTransposition
							+ ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
			}

			// Normalization: no need to offer customization of TYPES_TO_COPY
			AnalysisEngineDescription changeAnnotator = createEngineDescription(SpellingAnomalyReplacer.class,
					SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
					new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
			spellingComponents.add(changeAnnotator);
			componentNames.add("changeAnnotator");

			AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
			spellingComponents.add(changeApplier);
			componentNames.add("changeApplier");

			// Repeat tokenization to ensure correct tokens after replacements of
			// misspellings may have introduced blank spaces within tokens
			spellingComponents.add(segmenter);
			componentNames.add("segmenter");

			//TODO: just for development
			AnalysisEngineDescription resultTester = createEngineDescription(ResultTester.class);
			spellingComponents.add(resultTester);
			componentNames.add("resultTester");

			spellingCorrectorEngine = AnalysisEngineFactory.createEngine(spellingComponents, componentNames, null,
					null);

		} catch (ResourceInitializationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private AnalysisEngineDescription getCandidateSelector(CandidateSelectionMethod method)
			throws ResourceInitializationException {
		switch (method) {
		case DEFAULT_LEVENSHTEIN:
			return createEngineDescription(CorrectionCandidateSelector_Distance.class);
		case CUSTOM_LEVENSHTEIN:
			return createEngineDescription(CorrectionCandidateSelector_CustomMatrixes.class,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_DELETION, filePathDeletion,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_INSERTION, filePathInsertion,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_SUBSTITUTION, filePathSubstitution,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_TRANSPOSITION, filePathTransposition);
		case KEYBOARD_DISTANCE:
			return createEngineDescription(CorrectionCandidateSelector_KeyboardDistance.class,
					CorrectionCandidateSelector_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
					CorrectionCandidateSelector_KeyboardDistance.PARAM_LANGUAGE, language);
		case LANGUAGE_MODEL:
			return createEngineDescription(CorrectionCandidateSelector_LanguageModel.class,
					CorrectionCandidateSelector_LanguageModel.PARAM_DEFAULT_LANGUAGE_MODEL, defaultLanguageModel,
					CorrectionCandidateSelector_LanguageModel.PARAM_CUSTOM_LANGUAGE_MODEL, customLanguageModel,
					// TODO: this gives weird type error when param is type double
					CorrectionCandidateSelector_LanguageModel.PARAM_CUSTOM_LM_WEIGHT, customLMWeight,
					CorrectionCandidateSelector_LanguageModel.PARAM_NGRAM_SIZE, ngramSize);
		case PHONETIC:
			return createEngineDescription(CorrectionCandidateSelector_LitKey.class,
					CorrectionCandidateSelector_LitKey.PARAM_LANGUAGE, language);
		default:
			getContext().getLogger().log(Level.WARNING,
					"Selected unknown selection type '" + method + "' , defaulting to levenshtein distance.");
			return createEngineDescription(CorrectionCandidateSelector_Distance.class);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		spellingCorrectorEngine.process(aJCas);
	}

}