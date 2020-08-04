package de.unidue.ltl.spelling.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class GenerateRandomWeightFiles {

	private final static String path = "src/main/resources/matrixes";
	private final static Character[] alphabet_EN = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
	private final static Character[] alphabet_EN_upper = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
			'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
	private final static Character[] alphabet_DE = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'ä', 'ö', 'ü', 'ß', 'A',
			'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
			'X', 'Y', 'Z', 'Ä', 'Ö', 'Ü' };
	private final static Character[] alphabet_DE_upper = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'ä', 'ö', 'ü', 'ß' };
	private final static String[] alphabet_Sampa = new String[] {
			"A", "C", "I", "E", "M", "N", "O", "Q", "S", "T", "U", "V", "Y", "Z",
			"a", "b", "d", "i", "e", "f", "g", "h", "j", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "v", "x", "y", "z",
			"1", "2", "3", "6", "7", "8", "9", "O6", "66", "96", "2:",
			"@", "{", "}", "&", "?",
			"a:", "E:", "e:", "i:", "o:", "u:", "y:",
			"a~", "aI", "aU", "dZ", "OY", "tS", "ts",
			"a6", "E6", "i6", "I6", "U6", "u6", "o6", "Y6", "y6", "@6",
			"a:6", "E:6", "e:6", "i:6", "o:6", "u:6", "y:6", "2:6" };

	public static void main(String[] args) throws IOException {

		writeWeightFile(path, "insertion_DE", alphabet_DE, 1, 5);
		writeWeightFile(path, "deletion_DE", alphabet_DE, 1, 5);
		writeWeightFile2D(path, "substitution_DE", alphabet_DE, 1, 5);
		writeWeightFile2D(path, "transposition_DE", alphabet_DE, 1, 5);

		writeWeightFile(path, "insertion_DE_withUpper", alphabet_DE_upper, 1, 5);
		writeWeightFile(path, "deletion_DE_withUpper", alphabet_DE_upper, 1, 5);
		writeWeightFile2D(path, "substitution_DE_withUpper", alphabet_DE_upper, 1, 5);
		writeWeightFile2D(path, "transposition_DE_withUpper", alphabet_DE_upper, 1, 5);

		writeWeightFile(path, "insertion_EN", alphabet_EN, 1, 5);
		writeWeightFile(path, "deletion_EN", alphabet_EN, 1, 5);
		writeWeightFile2D(path, "substitution_EN", alphabet_EN, 1, 5);
		writeWeightFile2D(path, "transposition_EN", alphabet_EN, 1, 5);

		writeWeightFile(path, "insertion_EN_withUpper", alphabet_EN_upper, 1, 5);
		writeWeightFile(path, "deletion_EN_withUpper", alphabet_EN_upper, 1, 5);
		writeWeightFile2D(path, "substitution_EN_withUpper", alphabet_EN_upper, 1, 5);
		writeWeightFile2D(path, "transposition_EN_withUpper", alphabet_EN_upper, 1, 5);

		writeWeightFile(path, "insertion_Sampa", alphabet_Sampa, 1, 5);
		writeWeightFile(path, "deletion_Sampa", alphabet_Sampa, 1, 5);
		writeWeightFile2D(path, "substitution_Sampa", alphabet_Sampa, 1, 5);
		writeWeightFile2D(path, "transposition_Sampa", alphabet_Sampa, 1, 5);

	}

	private static void writeWeightFile2D(String locationToWriteTo, String purpose, Character[] alphabet, int min,
			int max) throws IOException {
		File file = new File(locationToWriteTo + "/RDMatrix_" + purpose + ".tsv");
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (char from : alphabet) {
			for (char to : alphabet) {
				bw.write(from + "\t" + to + "\t" + ThreadLocalRandom.current().nextInt(min, max + 1));
				bw.newLine();
			}
		}
		bw.close();
	}

	private static void writeWeightFile(String locationToWriteTo, String purpose, Character[] alphabet, int min,
			int max) throws IOException {
		File file = new File(locationToWriteTo + "/RDMatrix_" + purpose + ".tsv");
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (char from : alphabet) {
			bw.write(from + "\t" + ThreadLocalRandom.current().nextInt(min, max + 1));
			bw.newLine();
		}
		bw.close();
	}

	private static void writeWeightFile2D(String locationToWriteTo, String purpose, String[] alphabet, int min, int max)
			throws IOException {
		File file = new File(locationToWriteTo + "/RDMatrix_" + purpose + ".tsv");
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (String from : alphabet) {
			for (String to : alphabet) {
				bw.write(from + "\t" + to + "\t" + ThreadLocalRandom.current().nextInt(min, max + 1));
				bw.newLine();
			}
		}
		bw.close();
	}

	private static void writeWeightFile(String locationToWriteTo, String purpose, String[] alphabet, int min, int max)
			throws IOException {
		File file = new File(locationToWriteTo + "/RDMatrix_" + purpose + ".tsv");
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (String from : alphabet) {
			bw.write(from + "\t" + ThreadLocalRandom.current().nextInt(min, max + 1));
			bw.newLine();
		}
		bw.close();
	}
}
