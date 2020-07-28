package de.unidue.ltl.spelling.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple mapping tool from ISO639-1 codes to ISO639-3-ISO3166-1 codes and vice
 * versa. Necessary for g2p service and only including languages supported by
 * the BAS g2p webservice.
 * 
 * Mostly unambiguous, the only defaults are for 'en' (en-US) and 'eu' (eu-ES)
 */
public class G2P_LanguageCodeMapper {

	private static G2P_LanguageCodeMapper mapper = null;

	private String languageCodeFile = "src/main/resources/g2p/languageCodes_639-3_to_639-1.tsv";
	private String supportedBasCodesFile = "src/main/resources/g2p/supportedBasCodes.txt";

	private Map<String, String> fromBasTo639_1 = null;
	private Map<String, String> from639_1ToBas = null;
	private Set<String> supportedBasCodes = null;

	private G2P_LanguageCodeMapper() {
		fromBasTo639_1 = new HashMap<String, String>();
		from639_1ToBas = new HashMap<String, String>();
		supportedBasCodes = new HashSet<String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(languageCodeFile)));
			while (br.ready()) {
				String line = br.readLine();
				String[] codes = line.split("\t");
				if (codes.length == 2) {
					from639_1ToBas.put(codes[1], codes[0]);
					String iso639_3 = codes[0].substring(0, codes[0].indexOf("-"));
					fromBasTo639_1.put(iso639_3, codes[1]);
				}
			}
			br.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			br = new BufferedReader(new FileReader(new File(supportedBasCodesFile)));

			while (br.ready()) {
				supportedBasCodes.add(br.readLine());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static G2P_LanguageCodeMapper getInstance() {
		if (mapper == null) {
			mapper = new G2P_LanguageCodeMapper();
		}
		return mapper;
	}

	public String get639_1FromBas(String languageCode) {
		languageCode = languageCode.substring(0, languageCode.indexOf("-"));
		try {
			return fromBasTo639_1.get(languageCode);
		} catch (NullPointerException e) {
			return "";
		}
	}

	public String getBasFrom639_1(String languageCode) {
		try {
			return from639_1ToBas.get(languageCode);
		} catch (NullPointerException e) {
			return "";
		}
	}

	public boolean checkIfLanguageIsSupported(String languageCode) {
		return supportedBasCodes.contains(languageCode);
	}
}