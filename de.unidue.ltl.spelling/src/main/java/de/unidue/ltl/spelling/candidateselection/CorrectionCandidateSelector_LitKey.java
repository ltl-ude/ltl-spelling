package de.unidue.ltl.spelling.candidateselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.json.JSONArray;
import org.json.JSONObject;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class CorrectionCandidateSelector_LitKey extends CorrectionCandidateSelector {
	
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;
	
	// Location of the temporarily saved file containing anomaly text and correction candidate
	private final String filePath = "src/main/resources/bodu-spell/temp.csv";
	
	// A map inidicating whether an error type is classified as systematic
	private Map<String, Boolean> systematicMap = new HashMap<String, Boolean>();
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		// Aim is to determine candidates with lowest errorScore
		maximize = false;
		
		// TODO: initialize systematicMap
	}

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
		double errorScore = 0.0; 

		// Write anomaly text and correction candidate to file for processing
		FileWriter fw = null;
		File file = new File(filePath);
		try {
			fw = new FileWriter(file);
			fw.write(anomaly.getCoveredText() + "\t" + action.getReplacement() + System.lineSeparator());
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Execute python script to determine errors of anonaly respective the correction candidate
		// Is executed within a virtualenv
		Process process = null;
		try {
			process = Runtime.getRuntime()
					.exec(new String[] { "bash", "src/main/resources/bodu-spell/runLitkey.sh",file.getAbsolutePath(),language });
		} catch (Exception e) {
			System.out.println("Exception Raised" + e.toString());
		}

		//See if any errors occured during script execution, if yes: print them
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
		String line;
		try {
			while ((line = stdError.readLine()) != null) {
				System.err.println("Error executing python script: " + line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Process the result of the script: parse JSON object and read desired fields
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		try {
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("unknown")) {
					JSONObject jObject = new JSONObject(line.substring(1, line.length() - 1));
					JSONArray errors = jObject.getJSONArray("errors");
					//Process errors one by one
					for(int i = 0; i<errors.length(); i++) {
						JSONObject error = errors.getJSONObject(i);
//						Boolean phonOk = error.getBoolean("phon_ok");
						String errorType = error.getString("category");
//						if(systematicMap.get(errorType)) {
//							errorScore += 1.0;
//						}
//						else {
//							errorScore += 10;
//						}
					}
				}
				else {
					//Script has returned "unknown", therefore return high error score
					return 100.0;
				}
			}
		} catch (IOException e) {
			System.out.println("Exception in reading output" + e.toString());
		}
		return errorScore;
	}
}
