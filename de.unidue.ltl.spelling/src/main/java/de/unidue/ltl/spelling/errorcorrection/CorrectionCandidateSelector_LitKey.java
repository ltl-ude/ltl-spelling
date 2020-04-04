package de.unidue.ltl.spelling.errorcorrection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.json.JSONArray;
import org.json.JSONObject;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class CorrectionCandidateSelector_LitKey extends CorrectionCandidateSelector {
	
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;
	
	private final String filePath = "src/main/resources/bodu-spell/temp.csv";

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
		double errorScore = 0.0; 

		FileWriter fw = null;
		File file = new File(filePath);
		try {
			fw = new FileWriter(file);
			fw.write(anomaly.getCoveredText() + "\t" + action.getReplacement() + System.lineSeparator());
			fw.close();
//			System.out.println(anomaly.getCoveredText() + "\t" + action.getReplacement());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Process process = null;
		try {
			process = Runtime.getRuntime()
					.exec(new String[] { "bash", "src/main/resources/bodu-spell/runLitkey.sh",file.getAbsolutePath(),language });
		} catch (Exception e) {
			System.out.println("Exception Raised" + e.toString());
		}

		//See if any errors occured during script execution, print if yes
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
		String line;
		try {
			while ((line = stdError.readLine()) != null) {
				System.err.println("Error: " + line);
			}
		} catch (IOException e) {
			System.out.println("Exception in reading output" + e.toString());
		}

		//Process result of script: parse JSON object and read desired fields
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		try {
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("unknown")) {
					JSONObject jObject = new JSONObject(line.substring(1, line.length() - 1));
					JSONArray errors = jObject.getJSONArray("errors");
					//Process errors one by one
					for(int i = 0; i<errors.length(); i++) {
						JSONObject error = errors.getJSONObject(i);
						Boolean phonOk = error.getBoolean("phon_ok");
						String errorType = error.getString("category");
						System.out.println(errorType + " "+phonOk);
						if(phonOk) {
							errorScore += 1.0;
						}
						else {
							errorScore += 10;
						}
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
