package com.ksh;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvDeduplicator {

    public static void main(String[] args) {
        try {
            // Load configuration
            Map<String, Object> config = loadYamlConfig("C:/work/eclipse/eclipse-jee-oxygen/eclipse-workspace/CsvDeduplicator/src/config/config.yml");
            String csvFile = (String) config.get("csvFile");
            String outputFile = (String) config.get("outputFile");
            String delimiter = (String) config.get("DelimiterCharacter"); // Get DelimiterCharacter
            char delimiterChar = delimiter.charAt(0); // Convert delimiter to char
            int headerRowNumber = (int) config.get("HeaderRowNumber"); // Get HeaderRowNumber
            int linesToSkip = headerRowNumber - 1; // Calculate lines to skip (zero-based index)
            String deduplicateMode = (String) config.get("DeduplicateMode");
            
            @SuppressWarnings("unchecked")
			List<String> groupingColumns = (List<String>) config.get("groupingColumns");
            List<Integer> groupingColumnsNumbers = (List<Integer>) config.get("groupingColumnsNumber"); // Get column indices

            // Skip the first 10 lines and start reading from the 11th line 
            CSVReader reader = new CSVReader(new FileReader(csvFile), delimiterChar, '"', linesToSkip);
            @SuppressWarnings("unchecked")
			List<String[]> rows = reader.readAll();
            reader.close();
            List<String[]> deduplicatedRows=null;
            // Deduplicate records
            if (deduplicateMode.compareToIgnoreCase("groupingColumns")== 0) {
            	deduplicatedRows = deduplicateName(rows, groupingColumns);	
            }else {
                deduplicatedRows = deduplicate(rows, groupingColumnsNumbers);
            }
            
            printDeduplicatedRows(deduplicatedRows);
            // Write output CSV
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile));
            writer.writeAll(deduplicatedRows);
            writer.close();
            //System.out.println("Deduplication completed. Output written to " + deduplicatedRows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void printDeduplicatedRows(List<String[]> deduplicatedRows) {
        for (String[] row : deduplicatedRows) {
            System.out.println(Arrays.toString(row));
        }
    }
    private static Map<String, Object> loadYamlConfig(String fileName) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(new File(fileName));
        return yaml.load(inputStream);
    }

    private static List<String[]> deduplicateName(List<String[]> rows, List<String> groupingColumns) {
        if (rows.isEmpty()) {
            return rows;
        }

        // Get header row and determine indices of grouping columns
        String[] header = rows.get(0);
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            columnIndexMap.put(header[i], i);
        }

        List<Integer> groupingIndices = groupingColumns.stream()
                .map(columnIndexMap::get)
                .collect(Collectors.toList());

        // Deduplicate based on grouping columns
        Set<String> seenKeys = new HashSet<>();
        List<String[]> deduplicatedRows = new ArrayList<>();
        deduplicatedRows.add(header); // Add header to the result

        for (int i = 1; i < rows.size(); i++) { // Skip header row
            String[] row = rows.get(i);
            String key = groupingIndices.stream()
                    .map(index -> row[index])
                    .collect(Collectors.joining("|"));
            if (seenKeys.add(key)) {
                deduplicatedRows.add(row);
            }
        }

        return deduplicatedRows;
    }
    
    private static List<String[]> deduplicate(List<String[]> rows, List<Integer> groupingColumns) {
        if (rows.isEmpty()) {
            return rows;
        }

        // Deduplicate based on grouping columns (indices)
        Set<String> seenKeys = new HashSet<>();
        List<String[]> deduplicatedRows = new ArrayList<>();
        deduplicatedRows.add(rows.get(0)); // Add header row

        for (int i = 1; i < rows.size(); i++) { // Skip header row
            String[] row = rows.get(i);
            String key = groupingColumns.stream()
                    .map(index -> row[index])
                    .collect(Collectors.joining("|"));
            if (seenKeys.add(key)) {
                deduplicatedRows.add(row);
            }
        }

        return deduplicatedRows;
    }    
}
