package de.unidue.ltl.spelling.utils;

/**
 * Simple mapping tool from ISO639-1 codes to ISO639-3-ISO3166-1 codes and vice
 * versa. Necessary for g2p service and only including languages supported by
 * the BAS g2p webservice.
 */
public class G2P_LanguageCodeMapper {

	public static String getISO_6319_1from639_3_3166_1(String code) {
		switch (code) {
		case "deu-DE":
			return "de";
		case "eng-US":
			return "en";
		case "eng-AU":
			return "en";
		case "eng-GB":
			return "en";
		case "eng-NZ":
			return "en";
		case "afr-ZA":
			return "af";
		case "sqi-AL":
			return "sq";
		case "eus-ES":
			return "eu";
		case "eus-FR":
			return "eu";
		case "cat-ES":
			return "ca";
		case "nld-NL":
			return "nl";
		case "fin-FI":
			return "fi";
		case "fra-FR":
			return "fr";
		case "kat-GE":
			return "ka";
		case "hat-HT":
			return "ht";
		case "hun-HU":
			return "hu";
		case "isl-IS":
			return "is";
		case "ita-IT":
			return "it";
		case "jpn-JP":
			return "ja";
		case "ltz-LU":
			return "lb";
		case "mlt-MT":
			return "mt";
		case "nor-NO":
			return "no";
		case "pol-PL":
			return "pl";
		case "ron-RO":
			return "ro";
		case "rus-RU":
			return "ru";
		case "slk-SK":
			return "sk";
		case "spa-ES":
			return "es";
		case "swe-SE":
			return "sv";
		case "tha-TH":
			return "th";
		default:
			return "";
		}
	}

	/**
	 * Mostly unambiguous, only need defaults for 'en' (en-US) and 'eu' (eu-ES)
	 */
	public static String getISO_6393_3_3166_1from639_1(String code) {
		switch (code) {
		case "de":
			return "deu-DE";
		case "en":
			return "eng-US";
		case "af":
			return "afr-ZA";
		case "sq":
			return "sqi-AL";
		case "eu":
			return "eus-ES";
		case "ca":
			return "cat-ES";
		case "nl":
			return "nld-NL";
		case "fi":
			return "fin-FI";
		case "fr":
			return "fra-FR";
		case "ka":
			return "kat-GE";
		case "ht":
			return "hat-HT";
		case "hu":
			return "hun-HU";
		case "is":
			return "isl-IS";
		case "it":
			return "ita-IT";
		case "ja":
			return "jpn-JP";
		case "lb":
			return "ltz-LU";
		case "mt":
			return "mlt-MT";
		case "no":
			return "nor-NO";
		case "pl":
			return "pol-PL";
		case "ro":
			return "ron-RO";
		case "ru":
			return "rus-RU";
		case "sk":
			return "slk-SK";
		case "es":
			return "spa-ES";
		case "sv":
			return "swe-SE";
		case "th":
			return "tha-TH";
		default:
			return "";
		}
	}
}