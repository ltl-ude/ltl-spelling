package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Generates candidates based on Liktey error sum. Only supports German input.
 */
public class GenerateAndRank_Litkey extends CandidateGeneratorAndRanker {
	
	// Location of the temporarily saved file containing anomaly text and correction
	// candidate
	private final String pathToTempFile = "src/main/resources/bodu-spell/temp.csv";

	// Map indicating whether an error type is classified as systematic
	private final String pathToSystematicMap = "src/main/resources/bodu-spell/litkey_type_map_systematic.tsv";
	private Map<String, Boolean> systematicMap = new HashMap<String, Boolean>();

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
//			System.out.println("LINE: " + result);
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
								errorScore += 4.0;
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
		file.delete();
		return errorScore;
	}
}