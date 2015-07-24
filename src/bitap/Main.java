package bitap;

import guttmanlab.core.util.CommandLineParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		
		CommandLineParser p = new CommandLineParser();
		p.addStringArg("--barcode", "File containing barcodes to search", true);
		p.addStringArg("--reads", "FASTQ file of reads to search", true);
		p.addStringArg("--out", "Output text file", true);
		p.addStringArg("--nomatches", "Output file for reads with no barcode matches", true);
		p.addIntArg("--distance", "Maximum edit (Levenshtein) distance", true);
		p.parse(args);

		String BARCODE_FILE = p.getStringArg("--barcode");
		String READS_FILE = p.getStringArg("--reads");
		String OUT_FILE = p.getStringArg("--out");
		String NOMATCHES_FILE = p.getStringArg("--nomatches");
		int lev = p.getIntArg("--distance");
		
		Character[] alphabet = {'A', 'C', 'T', 'G', 'N'};
		Map<String, String> barcodes = new HashMap<String, String>();
		BufferedReader barcodeReader = null;

		try {
		    File file = new File(BARCODE_FILE);
		    barcodeReader = new BufferedReader(new FileReader(file));

		    String barcodeLine;
		    while ((barcodeLine = barcodeReader.readLine()) != null) {
		        String[] barcodePieces = barcodeLine.split("\\t");
		        String barcodeName = barcodePieces[0];
		        String barcodeSeq = barcodePieces[1];
		        barcodes.put(barcodeName, barcodeSeq);
		    }
		} finally {
		    if (barcodeReader != null) {barcodeReader.close();}
		}

		BufferedReader readsReader = null;
		BufferedWriter outWriter = null;
		BufferedWriter noMatchesWriter = null;

		try {
			File file = new File(READS_FILE);
			readsReader = new BufferedReader(new FileReader(file));
			outWriter = new BufferedWriter(new FileWriter(new File(OUT_FILE)));
			noMatchesWriter = new BufferedWriter(new FileWriter(new File(NOMATCHES_FILE)));
			
			String read;
			int line_num = 1; // For modulo operator to read every 4th line (see FASTQ format)
			while ((read = readsReader.readLine()) != null) {
				boolean hasBarcode = false;
				if ((line_num + 2) % 4 == 0) {
					for (Map.Entry<String, String> barcode : barcodes.entrySet()) {
						String barcodeName = barcode.getKey();
						String barcodeSeq = barcode.getValue();
						if (barcodeSeq != null) {
							Bitap b = new Bitap(barcodeSeq, read, alphabet);
							List<Integer> startSites = b.wuManber(lev);
							if (!startSites.isEmpty()) {
								hasBarcode = true;
								outWriter.write(read + "\t" + barcodeName + "\t" + barcodeSeq + "\t" + startSites + "\n");
							}
						}
					}
				}
				if (!hasBarcode) {
					noMatchesWriter.write(read + "\n");
				}
				line_num++;
			}
		} finally {
			if (readsReader != null) {readsReader.close();}
			if (outWriter != null) {outWriter.close();}
			if (noMatchesWriter != null) {noMatchesWriter.close();}
		}
		
		System.out.println("Program finished: " + (System.currentTimeMillis() - startTime));
	}
}