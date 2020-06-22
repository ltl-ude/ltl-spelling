//package de.unidue.ltl.spelling.candidategeneration;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.uima.UimaContext;
//import org.apache.uima.fit.descriptor.ConfigurationParameter;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.resource.ResourceInitializationException;
//import org.apache.uima.util.Level;
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
//import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
//
//public class CorrectionCandidateGenerator_LitKey extends CorrectionCandidateSelector{
//
//	public static final String PARAM_LANGUAGE = "language";
//	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
//	private String language;
//
//	// Location of the temporarily saved file containing anomaly text and correction
//	// candidate
//	private final String filePath = "src/main/resources/bodu-spell/temp.csv";
//
//	// A map indicating whether an error type is classified as systematic
//	private final String systematicMapPath = "src/main/resources/bodu-spell/litkey_type_map_systematic.tsv";
//	private Map<String, Boolean> systematicMap = new HashMap<String, Boolean>();
//
//	@Override
//	public void initialize(UimaContext context) throws ResourceInitializationException {
//		super.initialize(context);
//		// Aim is to determine candidates with lowest errorScore
//		maximize = false;
//
//		if (!language.equals("de")) {
//			getContext().getLogger().log(Level.WARNING,
//					"Phonetic candidate selection is only available for German ('de'), not with the currently selected language '"
//							+ language + "'");
//			System.exit(1);
//		}
//
//		try {
//			String line;
//			String[] entries = null;
//			Boolean systematic = null;
//			BufferedReader br = new BufferedReader(new FileReader(new File(systematicMapPath)));
//			while (br.ready()) {
//				line = br.readLine();
//				entries = line.split("\t");
//				if (entries[1].contentEquals("systematisch")) {
//					systematic = true;
//				} else if (entries[1].contentEquals("unsystematisch")) {
//					systematic = false;
//				} else {
//					System.out.println("Error initializing map from litkey types to boolean systematic: Type "
//							+ entries[0] + " maps to " + entries[1]
//							+ " (is neither \"systematisch\" nor \"unsystematisch\"");
//					continue;
//				}
//				systematicMap.put(entries[0], systematic);
////				System.out.println("Added "+entries[0]+" as "+systematic);
//			}
//			br.close();
//		} catch (FileNotFoundException e) {
//			System.out.println("Unable to initialize (litkey type) - (systematic) map from " + systematicMapPath);
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	@Override
//	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
//		double errorScore = 0.0;
//
//		// If token contains whitespace = was split during generation; now
//		// whitespace-separated tokens that were found in dictionary
//		// Do not process these here
//		if (action.getReplacement().contains(" ")) {
////			System.out.println("Found token containing whitespaces, assign cost "+StringUtils.countMatches(action.getReplacement(), " ")*2+" to token "+action.getReplacement());
//			return StringUtils.countMatches(action.getReplacement(), " ");
//		}
//
//		// Write anomaly text and correction candidate to file for processing
//		FileWriter fw = null;
//		File file = new File(filePath);
//		try {
//			fw = new FileWriter(file);
//			fw.write(anomaly.getCoveredText() + "\t" + action.getReplacement() + System.lineSeparator());
//			System.out.println("Litkey: processing candidate\t" + anomaly.getCoveredText()
//					+ "\twith suggestion candidate\t" + action.getReplacement());
//			fw.close();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//
//		// Execute python script to determine errors of anonaly respective the
//		// correction candidate
//		// Is executed within a virtualenv, see readme of ltl-spelling for setup
//		Process process = null;
//		try {
//			process = Runtime.getRuntime().exec(new String[] { "bash", "src/main/resources/bodu-spell/runLitkey.sh",
//					file.getAbsolutePath(), "de" });
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		// See if any errors occured during script execution, if yes: print them
//		BufferedReader stdError = new BufferedReader(
//				new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
//		String line;
//		try {
//			while ((line = stdError.readLine()) != null) {
//				System.err.println("Error executing python script: " + line);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		// Process the result of the script: parse JSON object and read desired fields
//		BufferedReader reader = new BufferedReader(
//				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
//		String result;
//		try {
//			while ((result = reader.readLine()) != null) {
////				System.out.println("LINE: " + result);
//				if (!result.startsWith("unknown")) {
//					JSONObject jObject = new JSONObject(result.substring(1, result.length() - 1));
//					JSONArray errors = jObject.getJSONArray("errors");
//					// Process errors one by one
//					for (int i = 0; i < errors.length(); i++) {
//						JSONObject error = errors.getJSONObject(i);
//						String errorType = error.getString("category");
//						System.out.println("Candidate:\t" + error.get("candidate").toString());
//						System.out.println("Error:\t" + errorType);
//						if (systematicMap.get(errorType)) {
//							errorScore += 1.0;
//						} else if (!systematicMap.get(errorType)) {
//							// Diffuse errors: no mapping between incorrect and correct version possible,
//							// therefore "worse" than other errors
//							if (errorType.contentEquals("diffuse")) {
//								errorScore += 4.0;
//							} else {
//								errorScore += 2.0;
//							}
//						} else {
//							System.out.println("Lookup of litkey error type " + errorType
//									+ " yielded no results in systematicMap.");
//						}
//					}
//				} else {
//					// Script has returned "unknown", therefore return high error score, should
//					// rarely happen
//					System.out.println("unknown");
//					return 10.0;
//				}
//			}
//		} catch (IOException e) {
//			System.out.println("Exception in reading output" + e.toString());
//		}
//		file.delete();
//		return errorScore;
//	}
//}
