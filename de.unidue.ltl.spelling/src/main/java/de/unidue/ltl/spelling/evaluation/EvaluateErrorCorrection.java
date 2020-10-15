package de.unidue.ltl.spelling.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.frequency.util.FrequencyDistribution;
import org.uimafit.util.JCasUtil;

import de.unidue.ltl.spelling.constants.SpellingConstants;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;

public class EvaluateErrorCorrection extends JCasAnnotator_ImplBase {

	public static final String PARAM_CONFIG_NAME = "configName";
	@ConfigurationParameter(name = PARAM_CONFIG_NAME, mandatory = true)
	protected String configName;
	
	FrequencyDistribution<String> correct = new FrequencyDistribution<String>();
	FrequencyDistribution<String> incorrect = new FrequencyDistribution<String>();
	
	int numberOfAnomalies = 0;
	int numberOfMatchingCorrections = 0;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		for(ExtendedSpellingAnomaly anomaly : JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class)) {
			
			numberOfAnomalies++;
			if(anomaly.getCoveredText().equals(anomaly.getGoldStandardCorrection())) {
				numberOfMatchingCorrections++;
				correct.inc(anomaly.getCoveredText()+"\t(" + anomaly.getMisspelledTokenText()+")");
			}
			else {
				incorrect.inc(anomaly.getCoveredText()+"\t(" + anomaly.getMisspelledTokenText()+"), should have been: " + anomaly.getGoldStandardCorrection());
			}
		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		try {
			String eval_dir = SpellingConstants.EVALUATION_DATA_PATH + "ErrorDetection_" + configName;
			File dir = new File(eval_dir);
			dir.mkdir();

			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/correct.txt")));
			List<String> correctList = new ArrayList<String>();
			correctList.addAll(correct.getKeys());
			correctList.sort(null);
			for(String c : correctList) {
				bw.write(c+"\t("+correct.getCount(c)+" times)");
			}
			bw.close();
			
			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/incorrect.txt")));
			List<String> incorrectList = new ArrayList<String>();
			incorrectList.addAll(incorrect.getKeys());
			incorrectList.sort(null);
			for(String i : incorrectList) {
				bw.write(i+"\t("+incorrect.getCount(i)+" times)");
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
