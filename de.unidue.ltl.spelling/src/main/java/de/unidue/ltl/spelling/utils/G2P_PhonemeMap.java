package de.unidue.ltl.spelling.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class G2P_PhonemeMap {

	private static G2P_PhonemeMap mapper = null;

	private String phonemeLocation = "src/main/resources/g2p_maps/";
	private Map<String, Map<String, String>> phonemeMaps;

	private G2P_PhonemeMap() {
		phonemeMaps = new HashMap<String, Map<String, String>>();
		File dir = new File(phonemeLocation);
		// Every dir becomes a key, read all files contained in it into respective map
		for (File phonemeDir : dir.listFiles()) {
			if (phonemeDir.isDirectory()) {
				phonemeMaps.put(phonemeDir.getName(), new HashMap<String, String>());
				Map<String, String> phonemeMapForCurrentLanguage = phonemeMaps.get(phonemeDir.getName());
				for (File phonemeMapFile : phonemeDir.listFiles()) {
					BufferedReader br = null;
					try {
						br = new BufferedReader(new FileReader(phonemeMapFile));
						while (br.ready()) {
							String line = br.readLine();
							String[] entries = line.split("\t");
							if (entries.length < 2) {
								phonemeMapForCurrentLanguage.put(entries[0], "");
							} else {
								phonemeMapForCurrentLanguage.put(entries[0], entries[1]);
							}
						}
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	static G2P_PhonemeMap getInstance() {
		if (mapper == null) {
			mapper = new G2P_PhonemeMap();
		}
		return mapper;
	}

	public String lookupPhonemesInMaps(String graphemes, String language) {
		Map<String, String> languageMap = phonemeMaps.get(language);
		return languageMap.get(graphemes);
	}
}