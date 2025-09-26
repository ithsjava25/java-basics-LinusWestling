package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
            System.out.println("Du måste ange zone");
            skrivUtHjälp();
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

        // framtagning av pris
        if (dagensPriser.isEmpty()) {
            System.out.println("inga priser för: " + datum + " i område: " + valdKlass);
        } else {
            skrivUtPriser(valdKlass, dagensPriser, sorteraPriser, 20);
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
        System.out.println("Usage:");
        System.out.println("--zone SE1|SE2|SE3|SE4 (obligatorisk)");
        System.out.println("--date YYYY-MM-DD (valfri, standard är idag)");
        System.out.println("--sorted (valfri, sorterar priser fallande)");
        System.out.println("--charging 2h|4h|8h (valfri, visar optimalt laddningsfönster)");
        System.out.println("--help (visar denna hjälptext)");
        System.out.println("Example:");
        System.out.println("java -cp target/classes com.example.Main --zone SE3 --date 2025-09-04");
    }
    public static List<String> skrivUtPriser(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, boolean sorteraPriser, int maxAntal) {

        // Testet kräver att en lista av strängar returneras. Skapar en array list
        List<String> utskrifter = new ArrayList<>();
        // Skapar en objekt för att kunna formatera utskrift om tider längre ner
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH");

        // Avbryter metod och returnerar tom arraylist ifall det inte finns något i den
        if (priser == null || priser.isEmpty()) {
            utskrifter.add("Inga elpriser tillgängliga...");
            return utskrifter;
        }
        // Körs ifall jag vill ha priser sorterade
        else if (sorteraPriser) {
            priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());
            priser.stream().limit(maxAntal).forEach(pris -> {
                LocalTime startTid = pris.timeStart().toLocalTime();
                LocalTime slutTid = startTid.plusHours(1);
                String rad = String.format("Klockan: %s-%s, Pris: %.2f öre",
                        startTid.format(formatter), slutTid.format(formatter), pris.sekPerKWh() * 100);
                utskrifter.add(rad);
            });
            return utskrifter;
        }
        // Körs ifall det är en "vanlig utskrift"
        else {

            int lägstaPrisIndex = -1;
            int högstaPrisIndex = -1;

            // Loop för att räkna ut medelpriset
            double meanPrice = 0;
            for (int i = 0; i < priser.size(); i++) {
                meanPrice += priser.get(i).sekPerKWh();
            }
            double lowestPrice = Double.MAX_VALUE;
            double highestPrice = 0;
            for (int i = 0; i < priser.size(); i++) {
                if (priser.get(i).sekPerKWh() < lowestPrice) {
                    lowestPrice = priser.get(i).sekPerKWh();
                    lägstaPrisIndex = i;
                }

                if (priser.get(i).sekPerKWh() > highestPrice) {
                    highestPrice = priser.get(i).sekPerKWh();
                    högstaPrisIndex = i;
                }
            }

            // Konvertera tids-index till klockslag
            LocalTime högstaPrisKlockan = LocalTime.of(högstaPrisIndex, 0);
            LocalTime lägstaPrisKlockan = LocalTime.of(lägstaPrisIndex, 0);

            String klassText = (valdKlass != null) ? valdKlass.toString() : "Okänd klass";
            utskrifter.add(String.format("\nDagens elpriser för %s (%d st värden):", klassText, priser.size()));
            utskrifter.add(String.format("Medelpriset för dagen är: %.2f öre\n", (meanPrice / priser.size()) * 100));
            utskrifter.add(String.format("Lägsta priset är: %.2f öre kl. %s", lowestPrice * 100, lägstaPrisIndex));
            utskrifter.add(String.format("Högsta priset är: %.2f öre kl. %s", highestPrice * 100, högstaPrisIndex));
            // Skriv ut antal rader som efterfrågas i metoden
            priser.stream().limit(maxAntal).forEach(pris -> {
                LocalTime startTid = pris.timeStart().toLocalTime();
                LocalTime slutTid = startTid.plusHours(1);
                String rad = String.format("Klockan: %s-%s, Pris: %.2f öre",
                        startTid.format(formatter), slutTid.format(formatter), pris.sekPerKWh() * 100);
                utskrifter.add(rad);
            });
            if (priser.size() > maxAntal)
                utskrifter.add("...");

            utskrifter.forEach(System.out::println);
            return utskrifter;
        }
    }
    public static void optimaltLaddningsFönster(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, int antalTimmar) {

        //Deklarera variabler att spara pris och startindex i
        double lägstaPris = Double.MAX_VALUE;
        int bästaStartIndex = -1;

        // Loop för att iterera genom alla priser i listan för dagen och elområdet
        for (int i = 0; i <= priser.size() - antalTimmar; i++) {
            double nuvarandePris = 0;

            // Addera priser i sekvenser om hur många timmar man vill ladda och öka totalpriset
            for (int j = 0; j < antalTimmar; j++) {
                nuvarandePris += priser.get(i + j).sekPerKWh();
            }
            // Uppdatera priset ifall det är lägre
            if (nuvarandePris < lägstaPris) {
                lägstaPris = nuvarandePris;
                bästaStartIndex = i;
            }
        }
        // Skriv ut priserna
        if (bästaStartIndex >= 0) {
            System.out.println("\nLägsta pris för att ladda inom: " + valdKlass + " (" + antalTimmar + "h):");
            for (int i = bästaStartIndex; i < bästaStartIndex + antalTimmar; i++) {
                ElpriserAPI.Elpris pris = priser.get(i);
                System.out.printf("Klockan: %s, Pris: %.4f SEK/kWh\n",
                        pris.timeStart().toLocalTime(), pris.sekPerKWh());
            }
            LocalTime påBörjaLaddning = LocalTime.of(bästaStartIndex, 0);
            System.out.println("\nPåbörja laddning kl " + påBörjaLaddning);
            System.out.printf("Totalt pris för %dh: %.1f öre. Medelpris för fönster: %.2f öre\n", antalTimmar, (lägstaPris * 100), ((lägstaPris / antalTimmar) * 100));
        } else {
            System.out.println("Något gick fel när jag försökte hitta laddningsfönster för " + valdKlass);
        }

    }

}