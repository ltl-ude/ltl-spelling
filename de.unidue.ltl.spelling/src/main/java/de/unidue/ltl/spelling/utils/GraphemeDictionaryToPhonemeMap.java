package de.unidue.ltl.spelling.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//Takes a dictionary with one word per line (grapheme)
//and transforms it to one grapheme \t phoneme per line
//To be used for candidate generation based on phonetic distance
public class GraphemeDictionaryToPhonemeMap {

	public static void main(String[] args) throws IOException {

//		processDictionary(
//				"src/main/resources/dictionaries/hunspell_DE.txt",
//				"src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt",
//				"deu-DE");

//		processDictionary(
//				"src/main/resources/dictionaries/hunspell_en_US.txt",
//				"src/main/resources/dictionaries/hunspell_en_US_phoneme_map.txt",
//				"deu-DE");

//		processDictionary(
//				"src/main/resources/dictionaries/hunspell_Czech_dict.txt",
//				"/src/main/resources/dictionaries/hunspell_Czech_phoneme_map.txt",
//				"cze-CZ");

		processDictionary(
				"src/main/resources/dictionaries/hunspell_Italian_dict.txt",
				"src/main/resources/dictionaries/hunspell_Italian_phoneme_map.txt",
				"it");
	}

	private static void processDictionary(String path, String outputFileName, String language) throws IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(path));
		List<String> graphemes = new ArrayList<String>();
		while (br.ready()) {
			graphemes.add(br.readLine());
		}
		br.close();

		FileWriter writer = new FileWriter(outputFileName);
		List<String> result;
		// Cannot process all at once, make batches of 10000
		List<String> subList;
		int stepSize = 10000;
		int numSteps = (int) Math.floor(graphemes.size() / stepSize);
		for (int i = 0; i <= numSteps; i++) {
			System.out.println("Step: " + (i+1) + "/" + numSteps);
			if (i == numSteps) {
				subList = graphemes.subList(i * stepSize, graphemes.size() - 1);
			} else {
				subList = graphemes.subList(i * stepSize, (i + 1) * stepSize - 1);
			}
			result = PhonemeUtils.getPhonemes(subList, language);
			for (int j = 0; j < subList.size(); j++) {
				writer.write(subList.get(j) + "\t" + result.get(j) + System.lineSeparator());
			}
		}
		writer.close();
	}
}