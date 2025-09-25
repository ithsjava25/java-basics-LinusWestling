package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("___________ Testar Elpriser API _____________");
        Map<String, String> argMap = parseArgs(args);

        if (argMap.containsKey("help")) {
            skrivUtHjälp();
            return;
        }

        // SKapa API objekt
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        // Kollar ifall input innehåller zon som man vill kolla efter
        if (!argMap.containsKey("zone")) {
            System.out.println("Du måste ange --zone");
            return;
        }
        String valAvPrisKlass = argMap.get("zone").toUpperCase();

        // Skapa en variabel för vilken dag man vill hämta priser
        LocalDate datum = argMap.containsKey("date") ? LocalDate.parse(argMap.get("date")) : LocalDate.now();

        // Kolla ifall användaren vill sortera priserna
        boolean sorteraPriser = argMap.containsKey("sorted");

        // Skapar en variabel för vald zon, för att sedan hämta priserna för vald zon och aktuellt datum
        ElpriserAPI.Prisklass valdKlass = ElpriserAPI.Prisklass.valueOf(valAvPrisKlass);
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdKlass);

        // Kalla på metod för optimalt laddningsfönster
        if (argMap.containsKey("charging")) {
            int antalTimmar = Integer.parseInt(argMap.get("charging").replace("h", ""));
            optimaltLaddningsFönster(valdKlass, dagensPriser, antalTimmar);
            return;
        }

        System.out.println("Påbörja laddning"); // Behövs vara med enligt testet????


        /*
        // IF ---- Enhanced loop for att få fram alla områden
        if(valAvPrisKlass.equals("ALLA")){
            for(ElpriserAPI.Prisklass allaKlasser : ElpriserAPI.Prisklass.values()){
                if (dagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för " + datum + " i område: " + valAvPrisKlass);
                } else {
                    skrivUtPriser(allaKlasser, dagensPriser, sorteraPriser, 3);
                }
            }
        }

         */
        // framtagning av pris för specifikt område

        if (dagensPriser.isEmpty()) {
            System.out.println("Kunde inte hämta några priser för " + datum + " i område: " + valdKlass);
        } else {
            skrivUtPriser(valdKlass, dagensPriser, sorteraPriser, 3);
        }

    }
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2).toLowerCase();
                String value = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i] : "true";
                map.put(key, value);
            }
        }
        return map;
    }
    public static void skrivUtHjälp() {
        System.out.println("Användning:");
        System.out.println("--zone SE1|SE2|SE3|SE4 (obligatorisk)");
        System.out.println("--date YYYY-MM-DD (valfri, standard är idag)");
        System.out.println("--sorted (valfri, sorterar priser fallande)");
        System.out.println("--charging 2h|4h|8h (valfri, visar optimalt laddningsfönster)");
        System.out.println("--help (visar denna hjälptext)");
        System.out.println("Exempel:");
        System.out.println("java -cp target/classes com.example.Main --zone SE3 --date 2025-09-04");
    }
    public static void skrivUtPriser(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, boolean sorteraPriser, int maxAntal){
        if (sorteraPriser)
            priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());

        // Loop för att räkna ut medelpriset
        double meanPrice = 0;
        for (int i = 0; i < priser.size(); i++){
            meanPrice += priser.get(i).sekPerKWh();
        }

        System.out.println("\nDagens elpriser för " + valdKlass + " (" + priser.size() + " st värden):");
        System.out.println("Medelpriset för dagen är: " + (meanPrice/priser.size()));
        // Skriv ut antal rader som efterfrågas i metoden
        priser.stream().limit(maxAntal).forEach(pris ->
                System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                        pris.timeStart().toLocalTime(), pris.sekPerKWh()));
        if (priser.size() > maxAntal) System.out.println("...");
    }
    public static void optimaltLaddningsFönster(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, int antalTimmar){

        //Deklarera variabler att spara pris och startindex i
        double lägstaPris = Double.MAX_VALUE;
        int bästaStartIndex = -1;

        // Loop för att iterera genom alla priser i listan för dagen och elområdet
        for (int i = 0; i < priser.size(); i++){
            double nuvarandePris = 0;

            // Addera priser i sekvenser om hur många timmar man vill ladda
            for (int j = 0; j < antalTimmar; j++){
                nuvarandePris += priser.get(i + j).sekPerKWh();
            }
            // Uppdatera priset ifall det är lägre
            if (nuvarandePris < lägstaPris) {
                lägstaPris = nuvarandePris;
                bästaStartIndex = i;
            }
        }
        // Skriv ut priserna
        if (bästaStartIndex >= 0){
            System.out.println("\nOptimalt laddningsfönster för " + valdKlass + " (" + antalTimmar + "h):");
            for (int i = bästaStartIndex; i < bästaStartIndex + antalTimmar; i++){
                ElpriserAPI.Elpris pris = priser.get(i);
                System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                        pris.timeStart().toLocalTime(), pris.sekPerKWh());
            }
            System.out.printf("Totalt pris för %dh: %.4f SEK\n", antalTimmar, lägstaPris);
        } else {
            System.out.println("Något gick fel när jag försökte hitta laddningsfönster för " + valdKlass);
        }

    }
}
