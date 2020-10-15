package de.unidue.ltl.spelling.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Should be executed via terminal, given that hunspell has been installed.
//.aff-File should be located in same folder this script is executed from.
public class CombineDicAndAffHunspell {

	public static void main(String[] args) {
		try {
			// .dic and .aff files were obtained from https://github.com/wooorm/dictionaries
			// (GNU 2.0/3.0)

			// GERMAN
			getDict("src/main/resources/dictionaries/hunspell/German_de_DE.dic",
					"src/main/resources/dictionaries/hunspell/German_de_DE.aff",
					"src/main/resources/dictionaries/hunspell/hunspell_DE_dict.txt");

			// CZECH
//			getDict("src/main/resources/dictionaries/hunspell/Czech.aff",
//					"src/main/resources/dictioanries/hunspell/Czech.dic",
//					"src/main/resources/dictionaries/hunspell/hunspell_Czech_dict.txt"
//					);

			// ITALIAN
//			getDict("src/main/resources/dictionaries/hunspell/Italian.aff",
//					"src/main/resources/dictioanries/hunspell/Italian.dic",
//					"src/main/resources/dictionaries/hunspell/hunspell_Italian_dict.txt");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void getDict(String aff, String dic, String dicName) throws IOException {

		Set<String> words = new HashSet<String>();
		String wordforms = "/usr/local/bin/wordforms";

		String word = null;
		BufferedReader br = new BufferedReader(new FileReader(new File(dic)));
		while (br.ready()) {
			word = br.readLine();
			if (word.startsWith("/")) {
				continue;
			}
			if (word.contains("/")) {
				word = word.substring(0, word.indexOf("/"));

				// System.out.println("aff:\t"+aff);
				// System.out.println("dic:\t"+dic);
				System.out.println("word:\t" + word);

				Process p = Runtime.getRuntime().exec(new String[] { "/bin/bash", wordforms, aff, dic, word });

				BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

				BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

				String s;
				while ((s = stdInput.readLine()) != null) {
//					System.out.println("out:\t" + s);
					words.add(s);
				}

				while ((s = stdError.readLine()) != null) {
					System.out.println("error:\t" + s);
					System.exit(0);
				}
			} else {
				System.out.println(word);
				words.add(word);
			}

		}
		br.close();

		List<String> dict = new ArrayList<String>();
		dict.addAll(words);
		dict.sort(null);

		FileWriter writer = new FileWriter(dicName);
		for (String str : dict) {
			writer.write(str + System.lineSeparator());
		}
		writer.close();
	}
}