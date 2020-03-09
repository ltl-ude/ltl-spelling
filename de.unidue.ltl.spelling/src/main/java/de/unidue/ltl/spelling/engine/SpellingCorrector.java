package de.unidue.ltl.spelling.engine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
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

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.errorcorrection.AnnotateChanges_SpellingNormalizer;
import de.unidue.ltl.spelling.errorcorrection.ApplyChanges;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator.CandidateSelectionMethod;
import de.unidue.ltl.spelling.errorcorrection.ErrorDetector;
import de.unidue.ltl.spelling.errorcorrection.LevenshteinCorrectionCandidateGenerator;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.resources.LanguageModelResource;

public class SpellingCorrector extends JCasAnnotator_ImplBase{
	
	//For Error Detector
	
		public static final String PARAM_LANGUAGE = "language";
		@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
		private String language;
	
		//Same dictionary will also be passed to candidate generation
		public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
		@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false, defaultValue = {
				"dictionaries/de-testDict1.txt", "dictionaries/de-testDict2.txt", "dictionaries/en-testDict1.txt",
				"dictionaries/en-testDict2.txt" })
		private String[] dictionaries;
		
		//Referring to de.unidue.ltl.spelling.types.Numeric
		public static final String PARAM_EXCLUDE_NUMERIC = "excludeNumeric";
		@ConfigurationParameter(name = PARAM_EXCLUDE_NUMERIC, defaultValue = "true")
		private boolean excludeNumeric;
		
		//Referring to de.unidue.ltl.spelling.types.Punctuation
		public static final String PARAM_EXCLUDE_PUNCTUATION = "excludePunctuation";
		@ConfigurationParameter(name = PARAM_EXCLUDE_PUNCTUATION, defaultValue = "true")
		private boolean excludePunctuation;
		
		//Referring to de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
		public static final String PARAM_EXCLUDE_NAMED_ENTITIES = "excludeNamedEntities";
		@ConfigurationParameter(name = PARAM_EXCLUDE_NAMED_ENTITIES, defaultValue = "true")
		private boolean excludeNamedEntities;
		
		public static final String PARAM_ADDITIONAL_TYPES_TO_EXCLUDE = "additionalTypesToExclude";
		@ConfigurationParameter(name = PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, mandatory = false)
		private String[] additionalTypesToExclude;
	
	//For Candidate Generation (classic Levenshtein or phonetic)
	
		public static final String PARAM_MODEL_ENCODING = ComponentParameters.PARAM_MODEL_ENCODING;
		@ConfigurationParameter(name = PARAM_MODEL_ENCODING, mandatory = true, defaultValue = "UTF-8")
		private String dictEncoding;
		
		public static final String PARAM_SCORE_THRESHOLD = "ScoreThreshold";
		@ConfigurationParameter(name = PARAM_SCORE_THRESHOLD, mandatory = true, defaultValue = "1")
		private int scoreThreshold;

	//For Candidate Selection
		
		public static final String PARAM_SELECTION_METHOD = "CandidateSelectionMethod";
		@ConfigurationParameter(name = PARAM_SELECTION_METHOD, mandatory = false, defaultValue = "LEVENSHTEIN_UNIFORM")
		protected CandidateSelectionMethod candidateSelectionMethod;
	
		//For Normalization
		//TODO: this does not have to be accessible from the outside does it?
	//	public static final String PARAM_TYPES_TO_COPY = "typesToCopy";
	//	@ConfigurationParameter(name = PARAM_TYPES_TO_COPY, mandatory = true)
		
	//	public static final String PARAM_LANGUAGE_MODEL = "languageModel";
	//	@ExternalResource(key=PARAM_LANGUAGE_MODEL)
	//	private LanguageModelResource languageModel;
	
	//For language model
		public static final String PARAM_LANGUAGE_MODEL_PATH = "languageModelPath";
		@ConfigurationParameter(name = PARAM_LANGUAGE_MODEL_PATH, mandatory = true)
		private String languageModelPath;

	private AnalysisEngine spellingCorrectorEngine = null;
		
		@Override
		public void initialize(UimaContext context) throws ResourceInitializationException {
			
			//To ensure that parameters of this class have been set to be able to use them in creating engines below
			super.initialize(context);
			
			ExternalResourceDescription languageModel = createExternalResourceDescription(LanguageModelResource.class, 
					LanguageModelResource.PARAM_MODEL_FILE, languageModelPath
					);

			try {
				//Preprocessing
//				segmenter = createEngineDescription(OpenNlpSegmenter.class);
				AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
				AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
				AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
				AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(StanfordNamedEntityRecognizer.class);
			
				//Error Detection
				AnalysisEngineDescription errorDetector = createEngineDescription(ErrorDetector.class,
						ErrorDetector.PARAM_ADDITIONAL_DICTIONARIES, dictionaries
						,
						ErrorDetector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, additionalTypesToExclude
						,
						ErrorDetector.PARAM_LANGUAGE,language
						);

				AnalysisEngineDescription candidateGenerator = createEngineDescription(LevenshteinCorrectionCandidateGenerator.class,
						LevenshteinCorrectionCandidateGenerator.PARAM_SCORE_THRESHOLD,scoreThreshold,
						LevenshteinCorrectionCandidateGenerator.PARAM_LANGUAGE,language,
						LevenshteinCorrectionCandidateGenerator.PARAM_METHOD,candidateSelectionMethod,
						LevenshteinCorrectionCandidateGenerator.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
		
				//Normalization
				AnalysisEngineDescription changeAnnotator = createEngineDescription(AnnotateChanges_SpellingNormalizer.class,
					AnnotateChanges_SpellingNormalizer.PARAM_TYPES_TO_COPY,new String[]{"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token"},
					AnnotateChanges_SpellingNormalizer.PARAM_LANGUAGE_MODEL,languageModel
					);
				AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
			
				List<AnalysisEngineDescription> spellingComponents = new ArrayList<AnalysisEngineDescription>();
				spellingComponents.add(segmenter);
				spellingComponents.add(numericAnnotator);
				spellingComponents.add(punctuationAnnotator);
				spellingComponents.add(namedEntityAnnotator);
				spellingComponents.add(errorDetector);
				spellingComponents.add(candidateGenerator);
				spellingComponents.add(changeAnnotator);
				spellingComponents.add(changeApplier);
				
				List<String> componentNames = new ArrayList<String>();
				componentNames.add("segmenter");
				componentNames.add("numericAnnotator");
				componentNames.add("punctuationAnnotator");
				componentNames.add("namedEntityAnnotator");
				componentNames.add("errorDetector");
				componentNames.add("candidateGenerator");
				componentNames.add("changeAnnotator");
				componentNames.add("changeApplier");
				
				spellingCorrectorEngine =  AnalysisEngineFactory.createEngine(spellingComponents, componentNames, null, null);
			
			} catch (ResourceInitializationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}	

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		spellingCorrectorEngine.process(aJCas);
		
	}

}
