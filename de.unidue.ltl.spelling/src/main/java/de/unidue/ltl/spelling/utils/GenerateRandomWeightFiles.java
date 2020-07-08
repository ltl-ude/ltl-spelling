package de.unidue.ltl.spelling.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class GenerateRandomWeightFiles {

	private final static String path = "src/main/resources/matrixes";
	private final static Character[] alphabet_EN = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
	private final static Character[] alphabet_DE = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z','ä','ö','ü','ß'};

	public static void main(String[] args) throws IOException {

		writeWeightFile(path, "insertion_DE", alphabet_DE, 1, 5);
		writeWeightFile(path, "deletion_DE", alphabet_DE, 1, 5);
		writeWeightFile2D(path, "substitution_DE", alphabet_DE, 1, 5);
		writeWeightFile2D(path, "transposition_DE", alphabet_DE, 1, 5);
		
		writeWeightFile(path, "insertion_EN", alphabet_EN, 1, 5);
		writeWeightFile(path, "deletion_EN", alphabet_EN, 1, 5);
		writeWeightFile2D(path, "substitution_EN", alphabet_EN, 1, 5);
		writeWeightFile2D(path, "transposition_EN", alphabet_EN, 1, 5);

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
}
