package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.apache.uima.fit.descriptor.ConfigurationParameter;
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

	/**
	 * The dictionaries based on which to generate the correction candidates.
	 */
	public static final String PARAM_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_DICTIONARIES, mandatory = true)
	protected String[] dictionaries;

	// Location of the temporarily saved file containing pairs of misspellings and
	// correction candidates
	private final String pathToTempFile = "src/main/resources/bodu-spell/temp.tsv";

	private List<String> dictionaryList = new ArrayList<String>();

	// Map indicating whether an error type is classified as systematic
	private final String pathToSystematicMap = "src/main/resources/bodu-spell/litkey_type_map_systematic.tsv";
	private Map<String, Boolean> systematicMap = new HashMap<String, Boolean>();

	// Litkey error type 'diffuse' indicates that no relation between error and
	// candidate could be found
	// TODO: we do not need this anymore if we decide to optimize Litkey by not
	// passing in any pairs that will yield a diffuse error
	private final float diffuseCost = 1000.0f;

	// To avoid calling Litkey with misspelling-correction pairs that will yield a
	// 'diffuse' error we need to calculate their Levenshtein Distance
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
		for (int key : sortedDictionary.keySet()) {
			dictionaryList.addAll(sortedDictionary.get(key));
		}
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

//	For single error - correction - pair: not needed in this implementation as we want to batch-process multiple candidates at once.
	@Override
	protected float calculateCost(String misspelling, String candidate) {
		return 0.0f;
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {

			Map<Float, List<String>> rankedCandidates = new TreeMap<Float, List<String>>();
			String misspelling = anomaly.getCoveredText();
			List<String> litkeyResultEntries = new ArrayList<String>();

			// Just in case an upper limit is to be imposed on the number of candidates
			// processed by Litkey script
			int batchSize = dictionaryList.size();
			int wordsInFile = 0;

			int dictIndex = 0;
			while (dictIndex < dictionaryList.size()) {

				FileWriter fw = null;
				File file = new File(pathToTempFile);

				try {
					fw = new FileWriter(file);
					while (wordsInFile < batchSize && dictIndex < dictionaryList.size()) {
						String word = dictionaryList.get(dictIndex);
						if (!errorWillBeDiffuse(misspelling, word)) {
							fw.write(misspelling + "\t" + word + System.lineSeparator());
							wordsInFile += 1;
						}
						dictIndex++;
					}
					fw.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				litkeyResultEntries.addAll(runLitkeyScript(file));

				wordsInFile = 0;
				file.delete();
			}

			for (String entry : litkeyResultEntries) {
				String[] wordAndErrors = entry.split("\t");
				if (wordAndErrors.length < 2) {
					System.out.println("No result for: " + entry);
				} else {
					float cost = evaluateErrors(wordAndErrors[1]);
					if (cost < diffuseCost) {
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

			Iterator<Entry<Float, List<String>>> entries = rankedCandidates.entrySet().iterator();
			SuggestionCostTuples tuples = getSuggestionCostTuples(entries);
			addSuggestedActions(aJCas, anomaly, tuples);
		}
	}

	// Calculate Litkey error sum from a JSONArray of errors
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
					// TODO: if we avoid diffuse errors this becomes irrelevant
					if (errorType.equals("diffuse")) {
						errorScore += diffuseCost;
					} else {
						errorScore += 2.0;
					}
				}
			} else {
				System.err
						.println("Lookup of litkey error type " + errorType + " yielded no results in systematicMap.");
			}
		}
		return errorScore;
	}

	private boolean errorWillBeDiffuse(String misspelling, String candidate) {
		return candidate.length() > 4
				&& (double) (levenshteinDistance.apply(misspelling, candidate)) / (double) (candidate.length()) > 0.66;
	}

	// Execute python script to determine errors of anomaly respective the current
	// correction candidate
	private List<String> runLitkeyScript(File file) {
		List<String> results = new ArrayList<String>();

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