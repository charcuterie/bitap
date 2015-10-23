package bitap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for any string-matching algorithms related to the Bitap algorithm.
 * Currently contains implementations of the Baeza-Yates-Gonnet algorithm
 * for exact string matching, as well as the Wu-Manber modification for
 * approximate string matching.
 * 
 * These implementations use zeroes for matching bits and ones for non-matching
 * bits. These implementations also use left-shifts rather than right-shifts.
 * 
 * @author Mason M Lai
 */
public class Bitap {
	private String needle;
	private String haystack;
	private Map<Character, Long> alphabetMasks;
	private Set<Character> alphabet;
	
	public Bitap(String needle, String haystack, Set<Character> alphabet) {
		this.needle = needle;
		this.haystack = haystack + "&";
		this.alphabet = alphabet;
		alphabet.add('&');
		alphabetMasks = generateAlphabetMasks();
	}
	
	public Bitap(String needle, String haystack, Character[] alphabet) {
		this(needle, haystack, new HashSet<Character>(Arrays.asList(alphabet)));
	}
	
	public String getHaystack() {
		return haystack.substring(0, haystack.length() - 1); // Remove trailing '&'
	}

	public void setHaystack(String haystack) {
		this.haystack = haystack + "&";
	}
	
	/**
	 * Initialize the alphabet masks, one for each character of the alphabet.
	 * Each alphabet mask is the length of the needle, plus a zero at the
	 * right-most position. Aside from this zero, other zeroes mark locations
	 * where the corresponding letter appears. For example, if the needle were
	 * "Mississippi", the alphabet masks would be:
	 *
	 *		M i s s i s s i p p i
	 *  
	 *  M : 0 1 1 1 1 1 1 1 1 1 1 0
	 *  i : 1 0 1 1 0 1 1 0 1 1 0 0
	 *  s : 1 1 0 0 1 0 0 1 1 1 1 0
	 *  p : 1 1 1 1 1 1 1 1 0 0 1 0
	 *  
	 */
	private Map<Character, Long> generateAlphabetMasks() {
		Map<Character, Long> masks = new HashMap<Character, Long>();
		for (Character letter : alphabet) {
			long mask = ~0;
			for (int pos = 0; pos < needle.length(); pos++) {
				if (letter.equals(needle.charAt(needle.length() - 1 - pos))) {
					mask &= ~(1L << pos);
				}
			}
			masks.put(letter, (mask << 1));
		}
		return masks;
	}
	
	/**
	 * The starting bit array, commonly denoted as 'R' in the literature.
	 * Commonly thought of as a 2D matrix with dimensions
	 * max-Levenshtein-distance by needle-length, with the top-most row
	 * corresponding to a Levenshtein distance of 0, and the bottom-most
	 * corresponding to a distance of k. Initialized as all-ones, except
	 * for the right-most column, which is all-zeroes. This array is
	 * updated dynamically as the algorithm progresses through the search
	 * corpus. This implementation is just a 1D array of longs, where each
	 * long, in binary, functions as a row of the matrix. Also, since
	 * longs are 64-bit, the extraneous columns on the left are just all-
	 * ones.
	 * 
	 * An example with a max-Levenshtein distance of two:
	 * 
	 *  1 1 1 1 1 1 1 1 1 1 ... 1 1 1 1 1 1 1 1 1 0
	 *  1 1 1 1 1 1 1 1 1 1 ... 1 1 1 1 1 1 1 1 1 0
	 *  1 1 1 1 1 1 1 1 1 1 ... 1 1 1 1 1 1 1 1 1 0
	 * |<---------------- 64-bits ---------------->|
	 * 
	 * @param lev - the maximum Levenshtein distance for a substring match
	 * @return the starting bit array
	 */
	private long[] generateBitArray(int lev) {
		long[] bitArray = new long[lev + 1];
		for (int k = 0; k <= lev; k++) {
			bitArray[k] = ~1;
		}
		return bitArray;
	}
	
	/**
	 * Baeza-Yates-Gonnet algorithm. Finds all exact matches of a needle
	 * within a haystack.
	 * 
	 * @return an ArrayList<Integer> containing the positions where a
	 * valid substring match can start
	 */
	public List<Integer> baezaYatesGonnet() {
		List<Integer> locatedPositions = new ArrayList<Integer>();
		long bitArray = ~1;

		for (int i = haystack.length() - 1; i >= 0; i--) {
			bitArray = (bitArray << 1) | alphabetMasks.get(haystack.charAt(i));
			if (0 == (bitArray & (1 << needle.length()))) {
				locatedPositions.add(i);
			}
		}
		
		Collections.sort(locatedPositions);
		return locatedPositions;
	}
	
	/**
	 * Wu-Manber algorithm. Finds all approximate (within a given
	 * Levenshtein distance) matches of a needle within a haystack.
	 * 
	 * Most implementations online start from the beginning of the haystack
	 * and proceed towards the end. When a zero bubbles up to the end of the
	 * bit array, that position (the end position) is recorded, and the
	 * needle length subtracted to get the start position. Due to indels,
	 * this start position is often incorrect. As a consequence, this
	 * implementation starts from the end of the haystack, with a reversed
	 * needle, and proceeds to the start of the haystack. The "end position"
	 * that this implementation finds is really the start position, since
	 * the string search is being done with all text "flipped".
	 * 
	 * @param lev - the maximum Levenshtein distance for a substring match
	 * @return an ArrayList<Integer> containing the positions where a
	 * valid substring match can start
	 */
	public List<Integer> wuManber(int lev) {
		List<Integer> locatedPositions = new ArrayList<Integer>();
		long[] bitArray = generateBitArray(lev);

		for (int i = haystack.length() - 1; i >= 0; i--) {
			long[] old = bitArray.clone();
			bitArray[0] = (old[0] << 1) | alphabetMasks.get(haystack.charAt(i));
			if (lev > 0) {
				for (int k = 1; k <= lev; k++) {
					long ins = old[k - 1];
					long sub = ins << 1;
					long del = bitArray[k - 1] << 1;
					long match = (old[k] << 1) | alphabetMasks.get(haystack.charAt(i));
					bitArray[k] = ins & del & sub & match;
				}
			}
			
			if (0 == (bitArray[lev] & (1 << needle.length()))) {
				locatedPositions.add(i);
			}
		}
		
		Collections.sort(locatedPositions);
		return locatedPositions;
	}
}