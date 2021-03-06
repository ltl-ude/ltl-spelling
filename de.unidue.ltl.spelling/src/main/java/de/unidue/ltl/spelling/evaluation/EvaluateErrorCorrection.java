package de.unidue.ltl.spelling.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.frequency.util.FrequencyDistribution;
import org.uimafit.util.JCasUtil;

import de.unidue.ltl.spelling.constants.SpellingConstants;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.StartOfSentence;

public class EvaluateErrorCorrection extends JCasAnnotator_ImplBase {

	public static final String PARAM_CONFIG_NAME = "configName";
	@ConfigurationParameter(name = PARAM_CONFIG_NAME, mandatory = true)
	protected String configName;

	FrequencyDistribution<String> correct = new FrequencyDistribution<String>();
	FrequencyDistribution<String> incorrect = new FrequencyDistribution<String>();

	List<String> numberedCorrect = new ArrayList<String>();

	int numberOfAnomalies = 0;
	int numberOfMatchingCorrections = 0;
	int rAt1;
	int rAt2;
	int rAt3;
	int rAt5;
	int rAt10;
	int lessThan1in1;
	int lessThan2in2;
	int lessThan3in3;
	int lessThan5in5;
	int lessThan10in10;
	int moreThan1in1;
	int moreThan2in2;
	int moreThan3in3;
	int moreThan5in5;
	int moreThan10in10;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (ExtendedSpellingAnomaly anomaly : JCasUtil.select(aJCas, ExtendedSpellingAnomaly.class)) {

			boolean isBeginningOfSentence = !JCasUtil.selectCovered(StartOfSentence.class, anomaly).isEmpty();
			if (isBeginningOfSentence) {
				System.out.println("Is beginning of sentence");
			}

			numberOfAnomalies++;
			if (anomaly.getCoveredText().equals(anomaly.getGoldStandardCorrection())) {
				numberOfMatchingCorrections++;
				correct.inc(anomaly.getMisspelledTokenText() + "\t(" + anomaly.getCoveredText() + ")");

				rAt1++;
				rAt2++;
				rAt3++;
				rAt5++;
				rAt10++;
				System.out.println("Correct: " + anomaly.getGoldStandardCorrection());

				numberedCorrect.add(
						numberOfAnomalies + "\t" + anomaly.getMisspelledTokenText() + "\t" + anomaly.getCoveredText());
			} else {
				incorrect.inc(anomaly.getMisspelledTokenText() + "\t(" + anomaly.getCoveredText()
						+ "), should have been: " + anomaly.getGoldStandardCorrection());
				System.out.println("Incorrect: " + anomaly.getGoldStandardCorrection());

				// R@3, 5, 10, accuracy is same as R@1
				// Ensure that there are no duplicates in ranked candidates
				String targetCorrection = anomaly.getGoldStandardCorrection();
				Map<Float, Set<String>> suggestions = new HashMap<Float, Set<String>>();
				if (anomaly.getSuggestions() != null) {
					for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
						float certainty = anomaly.getSuggestions(i).getCertainty();
						String candidate = anomaly.getSuggestions(i).getCoveredText();
						boolean alreadyPresent = false;
						for (Set<String> candidateSet : suggestions.values()) {
							if (candidateSet.contains(candidate)) {
								alreadyPresent = true;
							}
						}
						if (!alreadyPresent) {
							Set<String> suggestionRank = suggestions.get(certainty);
							if (suggestionRank == null) {
								suggestions.put(certainty, new HashSet<String>());
								suggestionRank = suggestions.get(certainty);
							}
							suggestionRank.add(anomaly.getSuggestions(i).getReplacement());
						} else {
							System.out.println("Was already present: " + candidate);
							System.exit(0);
						}
					}
				}

				List<Float> rankList = new ArrayList<Float>();
				rankList.addAll(suggestions.keySet());
				rankList.sort(null);

//				System.out.println("Rank list: " + rankList);

				Set<String> top1Candidates = new HashSet<String>();
				Set<String> top2Candidates = new HashSet<String>();
				Set<String> top3Candidates = new HashSet<String>();
				Set<String> top5Candidates = new HashSet<String>();
				Set<String> top10Candidates = new HashSet<String>();

				int currentIndex = 0;
				while (top1Candidates.size() < 1) {
					if (currentIndex < rankList.size()) {
						top1Candidates.addAll(suggestions.get(rankList.get(currentIndex)));
						currentIndex++;
					} else {
						break;
					}
				}
				// In case of too many candidates with equal rank, remove those
				if (top1Candidates.size() > 1) {
//					System.out.println("More than 3 candidates in top 3 set: " + top3Candidates.size());
					top1Candidates.removeAll(suggestions.get(rankList.get(currentIndex - 1)));
//					System.out.println("Size of top 3 after reducing: " + top3Candidates.size());
					if (top1Candidates.size() == 0) {
						top1Candidates.addAll(suggestions.get(rankList.get(0)));
						moreThan1in1++;
//						System.out.println(
//								"Had to increase again, number of candidates is now: " + top3Candidates.size());
					}
				}

				currentIndex = 0;
				while (top2Candidates.size() < 2) {
					if (currentIndex < rankList.size()) {
						top2Candidates.addAll(suggestions.get(rankList.get(currentIndex)));
						currentIndex++;
					} else {
						break;
					}
				}
				// In case of too many candidates with equal rank, remove those
				if (top2Candidates.size() > 2) {
//					System.out.println("More than 3 candidates in top 3 set: " + top3Candidates.size());
					top2Candidates.removeAll(suggestions.get(rankList.get(currentIndex - 1)));
//					System.out.println("Size of top 3 after reducing: " + top3Candidates.size());
					if (top2Candidates.size() == 0) {
						top2Candidates.addAll(suggestions.get(rankList.get(0)));
						moreThan2in2++;
//						System.out.println(
//								"Had to increase again, number of candidates is now: " + top3Candidates.size());
					}
				}

				currentIndex = 0;
				while (top3Candidates.size() < 3) {
					if (currentIndex < rankList.size()) {
						top3Candidates.addAll(suggestions.get(rankList.get(currentIndex)));
						currentIndex++;
					} else {
						break;
					}
				}

				for (String cand : top3Candidates) {
					System.out.println("in top 3: " + cand);
				}

				// In case of too many candidates with equal rank, remove those
				if (top3Candidates.size() > 3) {
//					System.out.println("More than 3 candidates in top 3 set: " + top3Candidates.size());
					top3Candidates.removeAll(suggestions.get(rankList.get(currentIndex - 1)));
//					System.out.println("Size of top 3 after reducing: " + top3Candidates.size());
					if (top3Candidates.size() == 0) {
						top3Candidates.addAll(suggestions.get(rankList.get(0)));
						moreThan3in3++;
//						System.out.println(
//								"Had to increase again, number of candidates is now: " + top3Candidates.size());
					}
				}

				for (String cand : top3Candidates) {
					System.out.println("in top 3: " + cand);
				}

				currentIndex = 0;
				while (top5Candidates.size() < 5) {
					if (currentIndex < rankList.size()) {
						top5Candidates.addAll(suggestions.get(rankList.get(currentIndex)));
						currentIndex++;
					} else {
						break;
					}

				}
				if (top5Candidates.size() > 5) {
//					System.out.println("More than 5 candidates in top 5 set: " + top5Candidates.size());
					top5Candidates.removeAll(suggestions.get(rankList.get(currentIndex - 1)));
//					System.out.println("Size of top 5 after reducing: " + top5Candidates.size());
					if (top5Candidates.size() == 0) {
						top5Candidates.addAll(suggestions.get(rankList.get(0)));
						moreThan5in5++;
//						System.out.println(
//								"Had to increase again, number of candidates is now: " + top5Candidates.size());
					}
				}

				currentIndex = 0;
				while (top10Candidates.size() < 10) {
					if (currentIndex < rankList.size()) {
						top10Candidates.addAll(suggestions.get(rankList.get(currentIndex)));
						currentIndex++;
					} else {
						break;
					}
				}
				if (top10Candidates.size() > 10) {
//					System.out.println("More than 10 candidates in top 10 set: " + top10Candidates.size());
					top10Candidates.removeAll(suggestions.get(rankList.get(currentIndex - 1)));
//					System.out.println("Size of top 10 after reducing: " + top10Candidates.size());
					if (top10Candidates.size() == 0) {
						top10Candidates.addAll(suggestions.get(rankList.get(0)));
						moreThan10in10++;
//						System.out.println(
//								"Had to increase again, number of candidates is now: " + top3Candidates.size());
					}
				}

				if (top1Candidates.size() != 0 && top1Candidates.size() < 1) {
//					System.err.println("WARN: less than 1 candidates in top 1: " + top1Candidates.size());
					lessThan1in1++;
				}
				if (top2Candidates.size() != 0 && top2Candidates.size() < 2) {
//					System.err.println("WARN: less than 2 candidates in top 2: " + top2Candidates.size());
					lessThan2in2++;
				}
				if (top3Candidates.size() != 0 && top3Candidates.size() < 3) {
//					System.err.println("WARN: less than 3 candidates in top 3: " + top3Candidates.size());
					lessThan3in3++;
				}
				if (top5Candidates.size() != 0 && top5Candidates.size() < 5) {
//					System.err.println("WARN: less than 5 candidates in top 5: " + top5Candidates.size());
					lessThan5in5++;
				}
				if (top10Candidates.size() != 0 && top10Candidates.size() < 10) {
//					System.err.println("WARN: less than 10 candidates in top 10: " + top10Candidates.size());
					lessThan10in10++;
				}

				if (top1Candidates.contains(targetCorrection)
						|| (isBeginningOfSentence && top1Candidates.contains(targetCorrection.toLowerCase()))) {
					rAt1++;
//					System.out.println("RANK LIST: "+rankList);
				}
				if (top2Candidates.contains(targetCorrection)
						|| (isBeginningOfSentence && top2Candidates.contains(targetCorrection.toLowerCase()))) {
					rAt2++;
//					System.out.println("RANK LIST: "+rankList);
				}
				if (top3Candidates.contains(targetCorrection)
						|| (isBeginningOfSentence && top3Candidates.contains(targetCorrection.toLowerCase()))) {
					System.out.println("Is in top3");
					rAt3++;
//					System.out.println("RANK LIST: "+rankList);
				}
				if (top5Candidates.contains(targetCorrection)
						|| (isBeginningOfSentence && top5Candidates.contains(targetCorrection.toLowerCase()))) {
					rAt5++;
				}
				if (top10Candidates.contains(targetCorrection)
						|| (isBeginningOfSentence && top10Candidates.contains(targetCorrection.toLowerCase()))) {
					rAt10++;
				}
			}

			System.out.println("at 3: " + rAt3);
			System.out.println("at 5: " + rAt5);
			System.out.println("at 10: " + rAt10);
			System.out.println("correct: " + numberOfMatchingCorrections);

		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		try {
			String eval_dir = SpellingConstants.EVALUATION_DATA_PATH + "ErrorCorrection_" + configName;
			File dir = new File(eval_dir);
			dir.mkdir();

			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/evaluation.txt")));
			System.out.println("Correct: " + numberOfMatchingCorrections + " Incorrect: "
					+ (numberOfAnomalies - numberOfMatchingCorrections));
			System.out.println("Accuracy:\t" + (numberOfMatchingCorrections * 1.0) / (numberOfAnomalies * 1.0));
			System.out.println("R@1:\t" + (rAt1 * 1.0) / (numberOfAnomalies * 1.0));
			System.out.println("R@2:\t" + (rAt2 * 1.0) / (numberOfAnomalies * 1.0));
			System.out.println("R@3:\t" + (rAt3 * 1.0) / (numberOfAnomalies * 1.0));
			System.out.println("R@5:\t" + (rAt5 * 1.0) / (numberOfAnomalies * 1.0));
			System.out.println("R@10:\t" + (rAt10 * 1.0) / (numberOfAnomalies * 1.0));

			bw.write("Correct: " + numberOfMatchingCorrections + " Incorrect: "
					+ (numberOfAnomalies - numberOfMatchingCorrections));
			bw.newLine();
			bw.write("Accuracy:\t" + (numberOfMatchingCorrections * 1.0) / (numberOfAnomalies * 1.0));
			bw.newLine();
			bw.write("R@1:\t" + (rAt1 * 1.0) / (numberOfAnomalies * 1.0) + "\t(" + lessThan1in1
					+ " times there were less than 1 candidates to choose from, " + moreThan1in1
					+ " times there were more than 1)");
			bw.newLine();
			bw.write("R@2:\t" + (rAt2 * 1.0) / (numberOfAnomalies * 1.0) + "\t(" + lessThan2in2
					+ " times there were less than 2 candidates to choose from, " + moreThan2in2
					+ " times there were more than 2)");
			bw.newLine();
			bw.write("R@3:\t" + (rAt3 * 1.0) / (numberOfAnomalies * 1.0) + "\t(" + lessThan3in3
					+ " times there were less than 3 candidates to choose from, " + moreThan3in3
					+ " times there were more than 3)");
			bw.newLine();
			bw.write("R@5:\t" + (rAt5 * 1.0) / (numberOfAnomalies * 1.0) + "\t(" + lessThan5in5
					+ " times there were less than 5 candidates to choose from, " + moreThan5in5
					+ " times there were more than 3)");
			bw.newLine();
			bw.write("R@10:\t" + (rAt10 * 1.0) / (numberOfAnomalies * 1.0) + "\t(" + lessThan10in10
					+ " times there were less than 10 candidates to choose from, " + moreThan10in10
					+ " times there were more than 10)");
			bw.newLine();
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/correct.txt")));
			List<String> correctList = new ArrayList<String>();
			correctList.addAll(correct.getKeys());
			correctList.sort(null);
			for (String c : correctList) {
				bw.write(c + "\t(" + correct.getCount(c) + " times)");
				bw.newLine();
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/incorrect.txt")));
			List<String> incorrectList = new ArrayList<String>();
			incorrectList.addAll(incorrect.getKeys());
			incorrectList.sort(null);
			for (String i : incorrectList) {
				bw.write(i + "\t(" + incorrect.getCount(i) + " times)");
				bw.newLine();
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(new File(eval_dir + "/correct_numbered.txt")));
			for (String i : numberedCorrect) {
				bw.write(i);
				bw.newLine();
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
