package com.example;

import com.example.api.ElpriserAPI;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        // Spara zonen i en sträng variable
        String valAvPrisKlass = argMap.get("zone").toUpperCase();

        // Skapar en variabel för vald zon, för att senare kunna hämta rätt priser
        ElpriserAPI.Prisklass valdKlass;
        try {
            valdKlass = ElpriserAPI.Prisklass.valueOf(valAvPrisKlass);
        } catch (IllegalArgumentException e){
            System.out.println("Ogiltig zon: " + valAvPrisKlass);
            skrivUtHjälp();
            return;
        }

        // Skapa en variabel för vilken dag man vill hämta priser
        LocalDate datum;
        if (argMap.containsKey("date")) {
            try {
                datum = LocalDate.parse(argMap.get("date"));
            } catch (DateTimeParseException e) {
                System.out.println("ogiltigt datum, se guide");
                skrivUtHjälp();
                return;
            }
        } else {
            datum = LocalDate.now();
        }
        // Hämta priser för båda dagarna.
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdKlass);
        List<ElpriserAPI.Elpris> morgonDagensPriser = elpriserAPI.getPriser(datum.plusDays(1), valdKlass);

        // Skapa en array list för att spara båda dagarnas elpriser. Behövs för att testet på charging hours ska bli godkänt
        List<ElpriserAPI.Elpris> sammanslagnaPriser = new ArrayList<>();
        if (dagensPriser != null)
            sammanslagnaPriser.addAll(dagensPriser);
        if (morgonDagensPriser != null)
            sammanslagnaPriser.addAll(morgonDagensPriser);

        // Kalla på metod för optimalt laddningsfönster
        if (argMap.containsKey("charging")) {
            int antalTimmar = Integer.parseInt(argMap.get("charging").replace("h", ""));
            optimaltLaddningsFönster(valdKlass, sammanslagnaPriser, antalTimmar);
            return;
        }

        // Kolla ifall användaren vill sortera priserna
        boolean sorteraPriser = argMap.containsKey("sorted");

        // Kalla på rätt metod för att skriva ut priser
        if (sammanslagnaPriser.isEmpty() || dagensPriser.isEmpty()) {
            System.out.println("inga priser för: " + datum + " i område: " + valdKlass);
        } else if (sorteraPriser){
            skrivUtSorteradePriser(valdKlass, sammanslagnaPriser, 20);
        } else {
            skrivUtPriser(valdKlass, dagensPriser, 100);
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
        System.out.println("java -cp target/classes com.example.Main --zone SE3 --date 2025-09-04 --sorted");
    }
    public static void skrivUtSorteradePriser(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, int maxAntal) {

        // Avbryter metod och returnerar tom arraylist ifall det inte finns något i den
        if (priser.isEmpty())
            System.out.println("Inga elpriser tillgängliga...");

        // Sortering och utskrift
        System.out.printf("\nSorterade elpriser för %s (%d st värden):\n", valdKlass, priser.size());
        priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());
        priser.stream().limit(maxAntal).forEach(pris -> {
            LocalTime startTid = pris.timeStart().toLocalTime();
            LocalTime slutTid = startTid.plusHours(1);
            System.out.println(startTid.format(tidFormatter()) + "-" +
                    slutTid.format(tidFormatter()) + " " +
                    formatKommatecken(pris.sekPerKWh() * 100) + " öre");
        });
    }
    public static void skrivUtPriser(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, int maxAntal) {

        if (priser.isEmpty())
            System.out.println("Inga elpriser tillgängliga...");

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
                högstaPrisIndex = i / 4;
            }
        }

        // Konvertera tids-index till klockslag
        LocalTime högstaPrisKlockan = LocalTime.of((högstaPrisIndex), 0);
        LocalTime lägstaPrisKlockan = LocalTime.of(lägstaPrisIndex, 0);

        System.out.printf("\nElpriser för %s (%d st värden):\n", valdKlass, priser.size());
        System.out.println("Medelpris: " + formatKommatecken((meanPrice / priser.size()) * 100) + " öre");
        System.out.println("Lägsta pris: " + formatKommatecken(lowestPrice * 100) +
                " öre kl. " + lägstaPrisKlockan.format(tidFormatter()) +
                "-" + lägstaPrisKlockan.plusHours(1).format(tidFormatter()));

        System.out.println("Högsta pris: " + formatKommatecken(highestPrice * 100) +
                " öre kl. " + högstaPrisKlockan.format(tidFormatter()) +
                "-" + högstaPrisKlockan.plusHours(1).format(tidFormatter()) + "\n");


        if (priser.size() == 96) {
            for (int hour = 0; hour < 24; hour++) {

                double summaTimpris = 0;

                for (int quarter = 0; quarter < 4; quarter++) {
                    int index = hour * 4 + quarter;
                    summaTimpris += priser.get(index).sekPerKWh();
                }
                summaTimpris = summaTimpris / 4;
                LocalTime startTid = LocalTime.of((hour), 0);
                LocalTime slutTid = startTid.plusHours(1);
                System.out.println("Medelpriset mellan " +
                        startTid.format(tidFormatter()) + "-" +
                        slutTid.format(tidFormatter()) + " är " +
                        formatKommatecken(summaTimpris * 100) + " öre");
            }
        } else {
            // Skriv ut antal rader som efterfrågas i metoden
            priser.stream().limit(maxAntal).forEach(pris -> {
                LocalTime startTid = pris.timeStart().toLocalTime();
                LocalTime slutTid = startTid.plusHours(1);
                System.out.println(startTid.format(tidFormatter()) + "-" +
                        slutTid.format(tidFormatter()) + " " +
                        formatKommatecken(pris.sekPerKWh() * 100) + " öre");
            });
            if (priser.size() > maxAntal)
                System.out.println("Det finns fler priser att visa ... ");
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
                System.out.println(">>>> Kör optimaltLaddningsFönster för " + valdKlass + " <<<<");
                ElpriserAPI.Elpris pris = priser.get(i);
                System.out.println("Klockan: " +
                        pris.timeStart().toLocalTime() + ", Pris: " +
                        formatKommatecken(pris.sekPerKWh() * 100) + " öre/kWh");
            }
            LocalTime påBörjaLaddning = LocalTime.of(bästaStartIndex, 0);
            System.out.println("\nPåbörja laddning kl " + påBörjaLaddning);

            System.out.println("Totalt pris för " + antalTimmar + "h: " +
                    formatKommatecken(lägstaPris * 100) + " öre. Medelpris för fönster: " +
                    formatKommatecken((lägstaPris / antalTimmar) * 100) + " öre");
        } else {
            System.out.println("Något gick fel när jag försökte hitta laddningsfönster för " + valdKlass);
        }
    }
    public static String formatKommatecken(double prisIÖre) {
        NumberFormat formatKommatecken = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        formatKommatecken.setMinimumFractionDigits(2);
        formatKommatecken.setMaximumFractionDigits(2);
        return formatKommatecken.format(prisIÖre);
    }
    public static DateTimeFormatter tidFormatter() {
        return DateTimeFormatter.ofPattern("HH");
    }
}