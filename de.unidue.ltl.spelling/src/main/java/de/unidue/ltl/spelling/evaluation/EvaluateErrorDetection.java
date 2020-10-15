package de.unidue.ltl.spelling.evaluation;

import de.unidue.ltl.spelling.constants.SpellingConstants;

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

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.types.GrammarError;
import de.unidue.ltl.spelling.types.SpellingError;

public class EvaluateErrorDetection extends JCasAnnotator_ImplBase {

	public static final String PARAM_CONFIG_NAME = "configName";
	@ConfigurationParameter(name = PARAM_CONFIG_NAME, mandatory = true)
	protected String configName;

	// First index: gold, second index: prediction
	// 0: not a spelling error, 1: a spelling error
	int[][] confusion_matrix = new int[2][2];
	int numberOfFPthatAreGrammarErrors = 0;

	FrequencyDistribution<String> FP = new FrequencyDistribution<String>();
	FrequencyDistribution<String> FN = new FrequencyDistribution<String>();
	FrequencyDistribution<String> TP = new FrequencyDistribution<String>();
	FrequencyDistribution<String> TN = new FrequencyDistribution<String>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (Token token : JCasUtil.select(aJCas, Token.class)) {

//			System.out.print("Checking for:\t"+token.getCoveredText());
			int error_gold = 0;
			int error_predicted = 0;

			if (!JCasUtil.selectCovered(SpellingError.class, token).isEmpty()) {
				error_gold = 1;
//				System.out.print("GOLD==ERROR");
			}
			if (!JCasUtil.selectCovered(SpellingAnomaly.class, token).isEmpty()) {
				error_predicted = 1;
//				System.out.print("PRED==ERROR");
			}

			if (error_gold == 1 && error_predicted == 0) {
				FN.inc(token.getCoveredText() + "\t("
						+ JCasUtil.selectCovered(SpellingError.class, token).get(0).getCorrection() + ")");
			}
			if (error_gold == 0 && error_predicted == 1) {
				if (!JCasUtil.selectCovered(GrammarError.class, token).isEmpty()) {
					numberOfFPthatAreGrammarErrors++;
					FP.inc("_grammar_error: " + token.getCoveredText() + "\t("
							+ JCasUtil.selectCovered(GrammarError.class, token).get(0).getCorrection() + ")");
				} else {
					FP.inc(token.getCoveredText());
				}
			}
			if (error_gold == 0 && error_predicted == 0) {
				TN.inc(token.getCoveredText());
			}
			if (error_gold == 1 && error_predicted == 1) {
				TP.inc(token.getCoveredText());
			}

//			System.out.println();
			confusion_matrix[error_gold][error_predicted] += 1;
		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		try {
			String eval_dir = SpellingConstants.EVALUATION_DATA_PATH + "ErrorDetection_" + configName;
			File dir = new File(eval_dir);
			dir.mkdir();

			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/prec_rec_f1.txt")));

			System.out.println(confusion_matrix[0][0] + " (" + TN.getKeys().size() + " distinct)\t"
					+ confusion_matrix[1][0] + " (" + FN.getKeys().size() + " distinct)");
			System.out.println(confusion_matrix[0][1] + " (" + FP.getKeys().size() + " distinct)\t"
					+ confusion_matrix[1][1] + " (" + TP.getKeys().size() + " distinct)");
			System.out.println(numberOfFPthatAreGrammarErrors + " of the false positives are grammar errors.");

			bw.write(confusion_matrix[0][0] + " (" + TN.getKeys().size() + " distinct)\t" + confusion_matrix[1][0]
					+ " (" + FN.getKeys().size() + " distinct)");
			bw.newLine();
			bw.write(confusion_matrix[0][1] + " (" + FP.getKeys().size() + " distinct)\t" + confusion_matrix[1][1]
					+ " (" + TP.getKeys().size() + " distinct)");
			bw.newLine();
			bw.write(numberOfFPthatAreGrammarErrors + " of the false positives are grammar errors.");
			bw.newLine();

			double precision = confusion_matrix[1][1] * 1.0
					/ (confusion_matrix[1][1] * 1.0 + confusion_matrix[0][1] * 1.0);
			double recall = confusion_matrix[1][1] * 1.0
					/ (confusion_matrix[1][1] * 1.0 + confusion_matrix[1][0] * 1.0);
			double f_1 = (2 * precision * recall) / (precision + recall);

			System.out.println("Precision:\t" + precision);
			System.out.println("Recall:\t" + recall);
			System.out.println("F1:\t" + f_1);

			bw.write("Precision:\t" + precision);
			bw.newLine();
			bw.write("Recall:\t" + recall);
			bw.newLine();
			bw.write("F1:\t" + f_1);
			bw.newLine();
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/false_positives.txt")));
			List<String> fp_list = new ArrayList<String>();
			fp_list.addAll(FP.getKeys());
			fp_list.sort(null);
			for (String fp : fp_list) {
				bw.write(fp + "\t(" + FP.getCount(fp) + " times)");
				bw.newLine();
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/false_negatives.txt")));
			List<String> fn_list = new ArrayList<String>();
			fn_list.addAll(FN.getKeys());
			fn_list.sort(null);
			for (String fn : fn_list) {
				bw.write(fn + "\t(" + FN.getCount(fn) + " times)");
				bw.newLine();
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/true_negatives.txt")));
			List<String> tn_list = new ArrayList<String>();
			tn_list.addAll(TN.getKeys());
			tn_list.sort(null);
			for (String tn : tn_list) {
				bw.write(tn + "\t(" + TN.getCount(tn) + " times)");
				bw.newLine();
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/true_positives.txt")));
			List<String> tp_list = new ArrayList<String>();
			tp_list.addAll(TP.getKeys());
			tp_list.sort(null);
			for (String tp : tp_list) {
				bw.write(tp + "\t(" + TP.getCount(tp) + " times)");
				bw.newLine();
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}