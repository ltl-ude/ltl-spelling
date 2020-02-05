package de.unidue.ltl.spelling.experiment;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.impl.compatibility.CollectionReaderAdapter;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.io.text.TextReader;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.errorcorrection.AnnotateChanges_SpellingNormalizer;
import de.unidue.ltl.spelling.errorcorrection.ApplyChanges;
import de.unidue.ltl.spelling.errorcorrection.ErrorDetector;
import de.unidue.ltl.spelling.errorcorrection.Levenshtein_CandidateGenerator;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;

public class SpellingCorrector {
	
	public static void main(String[] args) throws UIMAException, IOException {
		String[] dicts_de = new String[] {"dictionaries/de-testDict1.txt","dictionaries/de-testDict2.txt"};
		String[] dicts_en = new String[] {"dictionaries/en-testDict1.txt","dictionaries/en-testDict2.txt"};
		
		String[] types_to_exclude = new String[] {};
		
//		CollectionReader reader = getReader("en-testData","en");
//		AnalysisEngine engine = getSpellingCorrector(1,dicts_en);
//		SimplePipeline.runPipeline(reader, engine);
		
		
		CollectionReader reader = getReader("de-testData","de");
		AnalysisEngine engine = getSpellingCorrector(2,dicts_de);
		SimplePipeline.runPipeline(reader, engine);
	}
	
	public static CollectionReader getReader(String path,String language) throws ResourceInitializationException {
		 return CollectionReaderFactory.createReader(TextReader.class,
        		TextReader.PARAM_SOURCE_LOCATION, path,
        		TextReader.PARAM_PATTERNS, "*.txt",
                TextReader.PARAM_LANGUAGE, language
                );
	}
	
	public static AnalysisEngine getSpellingCorrector(int levenshtein_threshold, String[] dicts) throws ResourceInitializationException {
		
//		CollectionReader reader =  CollectionReaderFactory.createReader(TextReader.class,
//        		TextReader.PARAM_SOURCE_LOCATION, path,
//        		TextReader.PARAM_PATTERNS, "*.txt",
//                TextReader.PARAM_LANGUAGE, language
//                );
//		CollectionReaderAdapter textReader = new CollectionReaderAdapter(reader, null);
		
		//Preprocessing
		AnalysisEngineDescription segmenter = createEngineDescription(OpenNlpSegmenter.class);
        AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
        AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
		AnalysisEngineDescription namedEntities = createEngineDescription(StanfordNamedEntityRecognizer.class);
		
		//Error Detection
		AnalysisEngineDescription errorDetector = createEngineDescription(ErrorDetector.class,
				ErrorDetector.PARAM_DICTIONARIES, dicts);
		AnalysisEngineDescription levenshtein = createEngineDescription(Levenshtein_CandidateGenerator.class,
				Levenshtein_CandidateGenerator.PARAM_SCORE_THRESHOLD,levenshtein_threshold,
				Levenshtein_CandidateGenerator.PARAM_DICTIONARIES, dicts);
		
		//Normalization
		AnalysisEngineDescription changeAnnotator = createEngineDescription(AnnotateChanges_SpellingNormalizer.class,
				AnnotateChanges_SpellingNormalizer.PARAM_TYPES_TO_COPY,new String[]{"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token"});
		AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
		
		List<AnalysisEngineDescription> spellingComponents = new ArrayList<AnalysisEngineDescription>();
		spellingComponents.add(segmenter);
		spellingComponents.add(numericAnnotator);
		spellingComponents.add(punctuationAnnotator);
		spellingComponents.add(namedEntities);
		spellingComponents.add(errorDetector);
		spellingComponents.add(levenshtein);
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
		
		return AnalysisEngineFactory.createEngine(spellingComponents, componentNames, null, null);
		
//		List<Class< ? extends AnalysisComponent>> spellingComponents = new ArrayList<Class< ? extends AnalysisComponent>>();
//		spellingComponents.add(CollectionReaderAdapter.class);
//		spellingComponents.add(OpenNlpSegmenter.class);
//		spellingComponents.add(NumericAnnotator.class);
//		spellingComponents.add(PunctuationAnnotator.class);
//		spellingComponents.add(StanfordNamedEntityRecognizer.class);
//		spellingComponents.add(ErrorDetector.class);
//		spellingComponents.add(Levenshtein_CandidateGenerator.class);
//		spellingComponents.add(AnnotateChanges_SpellingNormalizer.class);
//		spellingComponents.add(ApplyChanges.class);
//		
//		List<String> componentParameters = new ArrayList<String>();
//		
//		return AnalysisEngineFactory.createEngine(spellingComponents,null,null,null,componentParameters);
		
	}

}
