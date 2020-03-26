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

public class Experiment {

	public static void main(String[] args) throws UIMAException, IOException {
		runEnglish();
//		runGerman();
	}

	public static void runEnglish() throws UIMAException, IOException {
		String[] dicts_en = new String[] { "dictionaries/en-testDict1.txt", "dictionaries/en-testDict2.txt" };
		String[] types_to_exclude = new String[] {};
		
		String lmPath = "src/main/resources/LM.ser";

		FrequencyDistribution<String> fd = new FrequencyDistribution<String>();
		ConditionalFrequencyDistribution<Integer,String> cfd = new ConditionalFrequencyDistribution<Integer,String>();
		cfd.inc(2,"Hello there");
		cfd.inc(2,"this Frequency");
		cfd.inc(2,"Frequency Distrbution");
		cfd.inc(2,"Distrbution is");
		cfd.inc(2,"is about");
		cfd.inc(2,"about to");
		cfd.inc(2,"to be");
		cfd.inc(2,"be serialized");
		
        try
        {    
            FileOutputStream file = new FileOutputStream(lmPath); 
            ObjectOutputStream out = new ObjectOutputStream(file); 
            out.writeObject(fd); 
            out.close(); 
            file.close(); 
        } 
          
        catch(IOException e) 
        { 
            e.printStackTrace();
        } 
		
		
		CollectionReader reader = getReader("en-testData", "en");
		AnalysisEngine engine = createEngine(SpellingCorrector.class, SpellingCorrector.PARAM_LANGUAGE, "en",
				SpellingCorrector.PARAM_SCORE_THRESHOLD, 2,
				SpellingCorrector.PARAM_ADDITIONAL_DICTIONARIES, dicts_en,
				SpellingCorrector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, types_to_exclude,
				SpellingCorrector.PARAM_PHONETIC_CANDIDATE_GENERATION, true,
				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL,
				SpellingCorrector.PARAM_NGRAM_SIZE, 1,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
				SpellingCorrector.PARAM_LANGUAGE_MODEL_PATH, lmPath);
		SimplePipeline.runPipeline(reader, engine);
	}

	public static void runGerman() throws UIMAException, IOException {
		String[] dicts_de = new String[] { "dictionaries/de-testDict1.txt", "dictionaries/de-testDict2.txt" };
		String[] types_to_exclude = new String[] {};

		String languageModel = "/Volumes/Marie2/web1t_de/export/data/ltlab/data/web1t/EUROPEAN/data/test"; // Just

		CollectionReader reader = getReader("de-testData", "de");
		AnalysisEngine engine = createEngine(SpellingCorrector.class, SpellingCorrector.PARAM_LANGUAGE, "de",
				SpellingCorrector.PARAM_SCORE_THRESHOLD, 2,
				SpellingCorrector.PARAM_ADDITIONAL_DICTIONARIES, dicts_de,
//				SpellingCorrector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, types_to_exclude,
				SpellingCorrector.PARAM_PHONETIC_CANDIDATE_GENERATION, true,
				SpellingCorrector.PARAM_FIRST_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LANGUAGE_MODEL,
				SpellingCorrector.PARAM_NGRAM_SIZE, 2,
//				SpellingCorrector.PARAM_SECOND_LEVEL_SELECTION_METHOD, CandidateSelectionMethod.LEVENSHTEIN_DISTANCE,
				SpellingCorrector.PARAM_LANGUAGE_MODEL_PATH, languageModel);
		SimplePipeline.runPipeline(reader, engine);

	}

	public static CollectionReader getReader(String path, String language) throws ResourceInitializationException {
		return CollectionReaderFactory.createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, path,
				TextReader.PARAM_PATTERNS, "*.txt", TextReader.PARAM_LANGUAGE, language);
	}

}
