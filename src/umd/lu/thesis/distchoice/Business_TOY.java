package umd.lu.thesis.distchoice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import umd.lu.thesis.common.ThesisProperties;
import umd.lu.thesis.helper.FileUtils;

/**
 *
 * @author Home
 */
public class Business_TOY {

    private final static int bufferSize = 10485760;

    private static ArrayList<String> T = new ArrayList<>();

    private static ArrayList<Integer> idArr = new ArrayList<>();

    private static HashMap<Integer, Double> msaEmpTable = new HashMap<>();

    private static HashMap<Integer, Double> msaHhTable = new HashMap<>();

    private static HashMap<Integer, Integer> msaIdTable = new HashMap<>();

    private static HashMap<Integer, Integer> idMsaTable = new HashMap<>();

    private static HashMap<String, Double> odAirFare = new HashMap<>();

    private static HashMap<String, Double> odAirTime = new HashMap<>();

    private static HashMap<String, Double> odCarTime = new HashMap<>();

    private static HashMap<String, Double> odDriveCost = new HashMap<>();

    private static HashMap<String, Double> odStopNight = new HashMap<>();

    private static HashMap<String, String> odAirFareByQuarter = new HashMap<>();

    private final static double lgsCoefTime = -0.0276;

    private final static int lgsWeight = 1;

    private final static double coeffL = -0.00588;

    private final static double coeffM = -0.00387;

    private final static double coeffH = -0.00126;

    private static Random rand = new Random();

    public static void main(String[] args) throws IOException {
        loadHashTables();

        initT();

        try (FileWriter fw = new FileWriter(ThesisProperties.getProperties("output.file.path"))) {
            BufferedWriter bw = new BufferedWriter(fw, bufferSize);

            // write header to output file
            bw.write(prepareHeader());

            // prepare reader to read input file
            BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("data.file.path"), bufferSize);

            // for each line in input file...
            String line = null;
            while ((line = br.readLine()) != null) {
                // skip header then process each line, write the processed line to 
                // output file.
                if(!line.startsWith("Person_Char_woIntel_HHPer")) {
                    bw.write(processLine(line));
                }
            }

            bw.flush();
        }
    }

    private static void loadHashTables() throws IOException {
        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("dictionary.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("MSA")) {
                    // split[0] - MSA
                    // split[1] - ID
                    // split[2] - ZoneID
                    // split[3] - Employ
                    // split[4] - HHs   
                    String[] split = line.split("\t");

                    // MSA-Empl
                    msaEmpTable.put(Integer.parseInt(split[0]), Double.parseDouble(split[3]));
                    // MSA-HHs
                    msaHhTable.put(Integer.parseInt(split[0]), Double.parseDouble(split[4]));
                    // MSA-ID
                    msaIdTable.put(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                    // ID-MSA
                    idMsaTable.put(Integer.parseInt(split[1]), Integer.parseInt(split[0]));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("airfare.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    odAirFare.put(split[0] + "-" + split[1], Double.parseDouble(split[2]));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("others.file.path"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("O")) {
                    String[] split = line.split("\t");
                    odAirTime.put(split[0] + "-" + split[1], Double.parseDouble(split[2]));
                    odCarTime.put(split[0] + "-" + split[1], Double.parseDouble(split[3]));
                    odDriveCost.put(split[0] + "-" + split[1], Double.parseDouble(split[4]));
                    odStopNight.put(split[0] + "-" + split[1], Double.parseDouble(split[5]));
                }
            }
        }

        try (BufferedReader br = FileUtils.openFileToRead(ThesisProperties.getProperties("air.fare.by.quarter"), bufferSize)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(!line.startsWith("Quarter")) {
                    String[] split = line.split("\t");
                    odAirFareByQuarter.put(split[1] + "-" + split[2] + "-" + split[0], split[3]);
                }
            }
        }
    }

    private static void initT() {
        T.add("11");
        T.add("12");
        T.add("22");
        T.add("23");
        T.add("33");
        T.add("34");
        T.add("44");
    }

    private static String prepareHeader() {
        String header = "Person_Char_woIntel_HHPer\tHouseholdId\tPersonid\tTripId\tO_MSA/NMSA\tD_MSA/NMSA\tINC\tLowInc\tMedInc\tHighInc\tTOY\n";
        return header;
    }

    /**
     * Every "line" will turn into 7 new lines.
     * 
     * @param in input line
     * @return 7 output lines (in a string)
     */
    private static String processLine(String in) {
        String sevenLines = "";
        for (String t : T) {
            // fixed columns
            sevenLines += writeFixedColumns(in);
            // T column
            sevenLines += t + "\t";
            // lgs column
            sevenLines += writeLogSumColumn(in, t) + "\t";
            // choice column
            sevenLines += writeChoiceColumn(in, t) + "\t";
            // endline
            sevenLines += "\n";
        }
        return sevenLines;
    }

    private static String writeFixedColumns(String in) {
        String fixed = "";
        // A - Person_Char_woIntel_HHPer
        fixed += getColumnValue(1, in) + "\t";
        // B - 	HouseholdId
        fixed += getColumnValue(2, in) + "\t";
        // C - 	Personid
        fixed += getColumnValue(3, in) + "\t";
        // D - 	TripId
        fixed += getColumnValue(4, in) + "\t";
        // L - O_MSA/NMSA
        fixed += getColumnValue(12, in) + "\t";
        // R - D_MSA/NMSA
        fixed += getColumnValue(18, in) + "\t";
        // BT - INC
        fixed += getColumnValue(72, in) + "\t";
        // BV - LowInc
        fixed += getColumnValue(74, in) + "\t";
        // BW - MedInc
        fixed += getColumnValue(75, in) + "\t";
        // BX - HighInc
        fixed += getColumnValue(76, in) + "\t";
        // DS - TOY
        fixed += getColumnValue(123, in) + "\t";

        return fixed;
    }

    /**
     * find the value of the specific column from the given line column starts 
     * from 1.
     * @param column
     * @param in
     * @return 
     */
    private static String getColumnValue(int column, String in) {
        String[] split = in.split("\t");
        return split[column - 1];
    }

    private static String writeLogSumColumn(String in, String t) {
        Double lgs = -1.0;
        String key = getODPair(in);

        Double uCar = calculateUCar(in, key);

        Double uAir = calculateUAir(in, key, t);

        if (Double.parseDouble(getColumnValue(32, in)) == 0.0) {
            lgs = uCar;
        }
        else if (uAir == -9999999.0) {
            lgs = -9999999.0;
        }
        else {
            lgs = java.lang.Math.log(java.lang.Math.exp(uAir) + java.lang.Math.exp(uCar));
        }
        return lgs.toString();
    }

    private static String writeChoiceColumn(String in, String t) {
        return getColumnValue(123, in).equals(t) ? "1" : "0";
    }

    /**
     * Given current input line, return O-D pair.
     * i.e. "ColumnL" - "ColumnR"
     * @param in
     * @return String "O - D"
     */
    private static String getODPair(String in) {
        Integer oMsa = msaIdTable.get(Integer.parseInt(getColumnValue(12, in)));
        Integer dMsa = msaIdTable.get(Integer.parseInt(getColumnValue(18, in)));

        return oMsa.toString() + "-" + dMsa.toString();
    }

    private static Double calculateUCar(String in, String key) {
        Double uCar = -1.0;

        // get unitLodgeCost for FORMULA-2
        Double unitLodgeCost;
        Double inc = Double.parseDouble(getColumnValue(72, in));
        if(inc.doubleValue() <= 30000.0) {
            unitLodgeCost = 70.0;
        }
        else if(inc.doubleValue() > 30000.0 && inc.doubleValue() <= 70000.0) {
            unitLodgeCost = 90.0;
        }
        else {
            unitLodgeCost = 110.0;
        }
        // get stopNight for FORMULA-2
        Double stopNight = odStopNight.get(key);
        // FORMULA-2: lodgeCost = unitLodgeCost * stopNight
        Double lodgeCost = unitLodgeCost * stopNight;

        // get tourCarCost for FORMULA-3
        Double driveCost = odDriveCost.get(key);
        Double tourCarCost = (driveCost / lgsWeight + lodgeCost) * 2.0;
        // get tourCarTime for FORMULA-3
        Double tourCarTime = odCarTime.get(key) * 2.0;
        // get lowInc, midInc, and highInc for FORMULA-3
        Double lowInc = Double.parseDouble(getColumnValue(74, in));
        Double midInc = Double.parseDouble(getColumnValue(75, in));
        Double highInc = Double.parseDouble(getColumnValue(76, in));
        // FORMULA-3: U_Car = CoeffL*LowInc*tourCarCost + CoeffM * MedInc *tourCarCost + CoeffH*HigInc*tourCarCost + CoeffTime * tourCarTime
        uCar = (coeffL * lowInc + coeffM * midInc + coeffH * highInc) * tourCarCost + lgsCoefTime * tourCarTime;

        return uCar;
    }

    private static Double calculateUAir(String in, String key, String t) {
        Double uAir = -1.0;

        Double totalAirCost = calculateTotalAirCost(key, t);
        if(totalAirCost != -1.0) {
            Double totalAirTime = odAirTime.get(key) * 2.0;
            // get lowInc, midInc, and highInc
            Double lowInc = Double.parseDouble(getColumnValue(74, in));
            Double midInc = Double.parseDouble(getColumnValue(75, in));
            Double highInc = Double.parseDouble(getColumnValue(76, in));

            // FORMULA-1: U_Air = CoeffL*LowInc*Tour_AirCost + CoeffM * MedInc * Tour_AirCost + CoeffH*HigInc*Tour_AirCost + Coeff_Time * Tour_AirTime
            uAir = (coeffL * lowInc + coeffM * midInc + coeffH * highInc) * totalAirCost + lgsCoefTime * totalAirTime;
        }
        else {
            uAir = -9999999.0;
        }
        return uAir;
    }

    private static Double calculateTotalAirCost(String key, String t) {
        Double inAirCost = -1.0;
        Double outAirCost = -1.0;

        inAirCost = calculateAirCost(key, String.valueOf(t.charAt(0)));
        outAirCost = calculateAirCost(key, String.valueOf(t.charAt(1)));

        if(inAirCost == -1.0 || outAirCost == -1.0) {
            return -1.0;
        }
        else {
            return inAirCost + outAirCost;
        }
    }

    private static Double calculateAirCost(String key, String quarter) {
        Double airFare = -1.0;
        String tmpKey = odAirFareByQuarter.get(key + "-" + quarter);
        if(tmpKey != null) {
            airFare = Double.parseDouble(tmpKey);
        }

        return airFare;
    }
}
