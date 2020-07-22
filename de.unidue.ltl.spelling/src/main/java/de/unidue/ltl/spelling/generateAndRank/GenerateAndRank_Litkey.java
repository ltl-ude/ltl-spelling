package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.json.JSONArray;
import org.json.JSONObject;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

/**
 * Generates candidates based on Liktey error sum. Only supports German input.
 */
public class GenerateAndRank_Litkey extends CandidateGeneratorAndRanker {

	// Location of the temporarily saved file containing anomaly text and correction
	// candidate
	private final String pathToTempFile = "src/main/resources/bodu-spell/temp.tsv";

	private List<String> dictionaryList = new ArrayList<String>();

	// Map indicating whether an error type is classified as systematic
	private final String pathToSystematicMap = "src/main/resources/bodu-spell/litkey_type_map_systematic.tsv";
	private Map<String, Boolean> systematicMap = new HashMap<String, Boolean>();

	// Litkey error type 'diffuse' indicates that no relation between error and
	// candidate could be found
	private final float diffuseCost = 1000.0f;
	
	private LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		if (!language.equals("de")) {
			getContext().getLogger().log(Level.WARNING,
					"Litkey candidate selection is only available for German ('de'), and not for the currently selected language '"
							+ language + "'.");
			System.exit(1);
		}

		readDictionaries(dictionaries);
		readSystematicMap(pathToSystematicMap, systematicMap);
		dictionaryList.addAll(dictionary);
	}

	private void readSystematicMap(String path, Map<String, Boolean> map) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(pathToSystematicMap)));
			while (br.ready()) {
				String line = br.readLine();
				String[] entries = line.split("\t");
				Boolean systematic = null;
				if (entries[1].equals("systematisch")) {
					systematic = true;
				} else if (entries[1].equals("unsystematisch")) {
					systematic = false;
				} else {
					System.out.println("Error initializing map from litkey types to boolean systematic: Type "
							+ entries[0] + " maps to " + entries[1]
							+ " (is neither \"systematisch\" nor \"unsystematisch\")");
					continue;
				}
				systematicMap.put(entries[0], systematic);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
//	For single error - correction - pair
	@Override
	protected float calculateCost(String misspelling, String candidate) {
		float errorScore = 0.0f;

		// Write anomaly text and correction candidate to file for processing
		FileWriter fw = null;
		File file = new File(pathToTempFile);
		try {
			fw = new FileWriter(file);
			fw.write(misspelling + "\t" + candidate + System.lineSeparator());
			System.out.println(
					"Litkey: processing candidate\t" + misspelling + "\twith suggestion candidate\t" + candidate);
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Execute python script to determine errors of anomaly respective the
		// current correction candidate
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(new String[] { "bash", "src/main/resources/bodu-spell/runLitkey.sh",
					file.getAbsolutePath(), "de" });
		} catch (Exception e) {
			e.printStackTrace();
		}

		// See if any errors occured during script execution, if yes: print them
		BufferedReader stdError = new BufferedReader(
				new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
		String line;
		try {
			while ((line = stdError.readLine()) != null) {
				System.err.println("Error executing Litkey python script: " + line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Process the result of the script: parse JSON object and read desired fields
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		String result;
		try {
			while ((result = reader.readLine()) != null) {
				System.out.println("LINE: " + result);
				if (!result.startsWith("unknown")) {
					JSONObject jObject = new JSONObject(result.substring(1, result.length() - 1));
					JSONArray errors = jObject.getJSONArray("errors");
					// Process errors one by one
					for (int i = 0; i < errors.length(); i++) {
						JSONObject error = errors.getJSONObject(i);
						String errorType = error.getString("category");
						System.out.println("Candidate:\t" + error.get("candidate").toString());
						System.out.println("Error:\t" + errorType);
						if (systematicMap.get(errorType)) {
							errorScore += 1.0;
						} else if (!systematicMap.get(errorType)) {
							// Diffuse errors: no mapping between incorrect and correct version possible,
							// therefore "worse" than other errors
							if (errorType.equals("diffuse")) {
								errorScore += 10.0;
							} else {
								errorScore += 2.0;
							}
						} else {
							System.out.println("Lookup of litkey error type " + errorType
									+ " yielded no results in systematicMap.");
						}
					}
				} else {
					System.out.println("unknown");
					return 10.0f;
				}
			}
		} catch (IOException e) {
			System.out.println("Exception in reading output" + e.toString());
		}
		return errorScore;
	}

	private float evaluateErrors(String errorString) {
		JSONArray errors = new JSONArray(errorString);

		float errorScore = 0.0f;

		for (int i = 0; i < errors.length(); i++) {
			JSONObject error = errors.getJSONObject(i);
			String errorType = error.getString("category");
			if (systematicMap.keySet().contains(errorType)) {
				if (systematicMap.get(errorType)) {
					errorScore += 1.0;
				} else {
					// Diffuse errors: no mapping between incorrect and correct version possible,
					// therefore "worse" than other errors
					if (errorType.equals("diffuse")) {
						errorScore += diffuseCost;
					} else {
						errorScore += 2.0;
					}
				} 
			}
			else {
				System.err.println(
						"Lookup of litkey error type " + errorType + " yielded no results in systematicMap.");
			}
		}
//		System.out.println("String: "+errorString+" errorScore: "+errorScore);
		return errorScore;
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {

			System.out.println("Processing anomaly: " + anomaly.getCoveredText());

			LocalTime startTime = java.time.LocalTime.now();

			Map<Float, List<String>> rankedCandidates = new TreeMap<Float, List<String>>();
			String misspelling = anomaly.getCoveredText();
			List<String> litkeyResultEntries = new ArrayList<String>();
			int consideredWords = 0;
			int nonDiffuse = 0;
			System.out.println();

			// Write file to be processed by litkey script
			// Must be processed in batches of 180 words, as larger sizes cannot be handled
			// by litkey
			int lengthDeviation = 2;
//			for (int i = 0; i < dictionaryList.size(); i += 180) {
//				System.out.println("Batch " + i);
//				FileWriter fw = null;
//				File file = new File(pathToTempFile);
//
//				try {
//					fw = new FileWriter(file);
//					for (int j = 0; j < 180; j++) {
//						String word = dictionaryList.get(i + j);
//						if (word.length() <= misspelling.length() + lengthDeviation
//								&& word.length() >= misspelling.length() - lengthDeviation) {
//							fw.write(misspelling + "\t" + word + System.lineSeparator());
//							consideredWords += 1;
//						}
//					}
//					fw.close();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//
//				System.out.println("Start Litkey script: " + java.time.LocalTime.now());
//
//				litkeyResultEntries.addAll(runLitkeyScript(file));
//
//				System.out.println("End Litkey script: " + java.time.LocalTime.now());
//
//				file.delete();
//			}

			int dictIndex = 0;
			int wordsInFile = 0;
			int batch = 1;
			int batchSize = 1000;
			int limit = dictionaryList.size();
			batchSize = limit;
			while (dictIndex < limit) {

				System.out.println("Batch " + batch);
				System.out.println("Starting from dict index " + dictIndex);
				FileWriter fw = null;
				File file = new File(pathToTempFile);

				try {
					fw = new FileWriter(file);
					while (wordsInFile < batchSize && dictIndex < limit) {
						String word = dictionaryList.get(dictIndex);
//						if (word.length() <= misspelling.length() + lengthDeviation
//								&& word.length() >= misspelling.length() - lengthDeviation) {
						if(!errorWillBeDiffuse(misspelling, word)) {
//							System.out.println("Writing to file: "+misspelling + "\t" + word);
							fw.write(misspelling + "\t" + word + System.lineSeparator());
							consideredWords += 1;
							wordsInFile += 1;
						}
						dictIndex++;
					}
					fw.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				System.out.println("Start Litkey script: " + java.time.LocalTime.now());

				litkeyResultEntries.addAll(runLitkeyScript(file));

//				System.out.println("End Litkey script: " + java.time.LocalTime.now());

				batch++;
				wordsInFile = 0;
				file.delete();
			}

			for (String entry : litkeyResultEntries) {
//				System.out.println("Entry: "+entry);
				String[] wordAndErrors = entry.split("\t");
				if (wordAndErrors.length < 2) {
					System.out.println("No result for: " + entry);
				} else {
					float cost = evaluateErrors(wordAndErrors[1]);
					if (cost < diffuseCost) {
						nonDiffuse += 1;
						List<String> rankList = rankedCandidates.get(cost);
						if (rankList == null) {
							rankedCandidates.put(cost, new ArrayList<String>());
							rankList = rankedCandidates.get(cost);
						}
						rankList.add(wordAndErrors[0]);
//						System.out.println("Added candidate and cost: " + wordAndErrors[0] + cost);
					}
				}
			}

			LocalTime endTime = java.time.LocalTime.now();

			System.out.println("Deviation: " + lengthDeviation);
			System.out.println("Considered: " + consideredWords);
			System.out.println("Non-diffuse: " + nonDiffuse);
			System.out.println("Result entries: " + litkeyResultEntries.size());
			System.out.println("Start: " + startTime);
			System.out.println("End: " + endTime);

			Iterator<Entry<Float, List<String>>> entries = rankedCandidates.entrySet().iterator();
			SuggestionCostTuples tuples = new SuggestionCostTuples();

			while (tuples.size() < numberOfCandidatesToGenerate) {
				if (entries.hasNext()) {
					Entry<Float, List<String>> entry = entries.next();
					List<String> currentRankList = entry.getValue();
					float rank = entry.getKey();
					for (int j = 0; j < currentRankList.size(); j++) {
						tuples.addTuple(currentRankList.get(j), rank);
					}
				} else {
					break;
				}
			}

			addSuggestedActions(aJCas, anomaly, tuples);
		}
		System.out.println();
		System.exit(0);
	}
	
	private boolean errorWillBeDiffuse(String misspelling, String candidate) {
//		System.out.println("Diffuse judgement: "+ candidate+"\t"+(double)(levenshteinDistance.apply(misspelling, candidate))/(double)(candidate.length()));
		return candidate.length()>4 && (double)(levenshteinDistance.apply(misspelling, candidate))/(double)(candidate.length()) > 0.66;
	}

	private List<String> runLitkeyScript(File file) {
		List<String> results = new ArrayList<String>();

		// Execute python script to determine errors of anomaly respective the
		// current correction candidate
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(new String[] { "bash", "src/main/resources/bodu-spell/runLitkey.sh",
					file.getAbsolutePath(), "de" });
		} catch (Exception e) {
			e.printStackTrace();
		}

		// See if any errors occured during script execution, if yes: print them
		// TODO: must thread reading errors and output
//		BufferedReader stdError = new BufferedReader(
//				new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
//		String line;
//		try {
//			while ((line = stdError.readLine()) != null) {
//				System.err.println("Error executing Litkey python script: " + line);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		// Process the result of the script: parse JSON object and read desired fields
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				results.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}
}