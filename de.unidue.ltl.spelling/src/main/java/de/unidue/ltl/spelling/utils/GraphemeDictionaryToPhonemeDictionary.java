package de.unidue.ltl.spelling.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Replaced by GraphemeDictionaryToPhonemeMap

//Takes a dictionary with one word per line (grapheme) and transforms it to one word per line (phoneme)
//To be used for candidate generation based on phonetic distance
public class GraphemeDictionaryToPhonemeDictionary {
	
	public static void main(String[] args) throws IOException {
		
		//deu-DE OR eng-US
		processDictionary(
				"/Users/mariebexte/ltl-spelling/de.unidue.ltl.spelling/src/main/resources/dictionaries/hunspell_DE.txt",
				"/Users/mariebexte/ltl-spelling/de.unidue.ltl.spelling/src/main/resources/dictionaries/hunspell_DE_bas_270720.txt",
				"deu-DE");
	}
	
	private static void processDictionary(String path,String outputFileName, String language) throws IOException {
		//Read tokens into list
		FileReader fr = new FileReader(path);
		BufferedReader br = new BufferedReader(fr);
		List<String> graphemes = new ArrayList<String>();
		while(br.ready()) {
			graphemes.add(br.readLine());
		}
		br.close();
		
		//Determine phonemes for all tokens
		Set<String> phonemes = new HashSet<String>();
		
		//Cannot process all at once, make batches of 10000
		List<String> subList;
		int stepSize = 10000;
		int numSteps = (int) Math.floor(graphemes.size()/stepSize);
		for(int i = 0; i<=numSteps; i++) {
			System.out.println("Processing batch "+(i+1)+"/"+(numSteps+1));
			if(i == numSteps) {
				subList = graphemes.subList(i*stepSize, graphemes.size()-1);
			}
			else{
				subList = graphemes.subList(i*stepSize, (i+1)*stepSize);
				System.out.println("from "+(i*stepSize)+" to "+ ((i+1)*stepSize-1));
			}
			
			phonemes.addAll(PhonemeUtils.getPhonemes(subList,language));
		}
		
		//Sort resulting set and write it to file
		List<String> result = new ArrayList<String>();
		result.addAll(phonemes);
		result.sort(null);
		PhonemeUtils.writeListToFile(result,outputFileName);	
	}
}