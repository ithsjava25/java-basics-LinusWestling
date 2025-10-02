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

        // Variabler / objekt som behövs i main
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ElpriserAPI.Prisklass valdKlass;
        LocalDate datum;

        if (argMap.containsKey("help")) {
            skrivUtHjälp();
            return;
        }

        // Kollar ifall input innehåller zon som man vill kolla efter
        if (!argMap.containsKey("zone")) {
            System.out.println("Du måste ange zone");
            skrivUtHjälp();
            return;
        } else {
            try {
                valdKlass = ElpriserAPI.Prisklass.valueOf(argMap.get("zone").toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Ogiltig zon: " + argMap.get("zone").toUpperCase());
                skrivUtHjälp();
                return;
            }
        }

        // Hantera datum input
        if (argMap.containsKey("date")) {
            try {
                datum = LocalDate.parse(argMap.get("date"));
            } catch (DateTimeParseException e) {
                System.out.println("ogiltigt datum, se guide nedan");
                skrivUtHjälp();
                return;
            }
        } else {
            datum = LocalDate.now();
        }

        // Hämta priser för båda dagarna och lägga till i samma arrayList om det finns data i dom
        List<ElpriserAPI.Elpris> sammanslagnaPriser = new ArrayList<>();
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdKlass);
        List<ElpriserAPI.Elpris> morgonDagensPriser = elpriserAPI.getPriser(datum.plusDays(1), valdKlass);
        if (dagensPriser != null && morgonDagensPriser != null) {
            sammanslagnaPriser.addAll(dagensPriser);
            sammanslagnaPriser.addAll(morgonDagensPriser);
        }

        // Kalla på rätt utskriftsmetod beroende på ifall terminalen innehåller charging, sorted eller inget av dom.
        if (sammanslagnaPriser.isEmpty() || dagensPriser.isEmpty()) {
            System.out.println("inga priser tillgängliga i område: " + valdKlass);
        } else if (argMap.containsKey("charging".toLowerCase())) {
            int antalTimmar = Integer.parseInt(argMap.get("charging").replace("h", ""));
            optimaltLaddningsFönster(valdKlass, sammanslagnaPriser, antalTimmar);
            return;
        } else if (argMap.containsKey("sorted")){
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
        if (priser.isEmpty()) {
            System.out.println("Inga elpriser tillgängliga...");
            return;
        }

        // Sortering och utskrift
        System.out.printf("\nSorterade elpriser för %s (%d st värden):\n", valdKlass, priser.size());
        priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());
        priser.stream().limit(maxAntal).forEach(pris -> {
            LocalTime startTid = pris.timeStart().toLocalTime();
            LocalTime slutTid = startTid.plusHours(1);
            System.out.println(startTid.format(tidFormatter()) + "-" +
                    slutTid.format(tidFormatter()) + " " +
                    formateraPrisIÖre(pris.sekPerKWh()) + " öre");
        });
    }
    public static void skrivUtPriser(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, int maxAntal) {

        if (priser.isEmpty()) {
            System.out.println("Inga elpriser tillgängliga...");
            return;
        }

        // Beräkning av medelpris, lägstapris och högstapris
        double meanPrice = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(0);
        ElpriserAPI.Elpris lowestPrice = priser.stream().min(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh)).orElse(null);
        ElpriserAPI.Elpris highestPrice = priser.stream().max(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh)).orElse(null);

        // Utskrifter
        System.out.printf("\nElpriser för %s (%d st värden):", valdKlass, priser.size());
        System.out.println("Medelpris: " + formateraPrisIÖre(meanPrice) + " öre");
        System.out.println("Lägsta pris: " + formateraPrisIÖre(lowestPrice.sekPerKWh()) +
                " öre kl. " + lowestPrice.timeStart().toLocalTime().format(tidFormatter()) +
                "-" + lowestPrice.timeStart().toLocalTime().plusHours(1).format(tidFormatter()));
        System.out.println("Högsta pris: " + formateraPrisIÖre(highestPrice.sekPerKWh()) +
                " öre kl. " + highestPrice.timeStart().toLocalTime().format(tidFormatter()) +
                "-" + highestPrice.timeStart().toLocalTime().plusHours(1).format(tidFormatter()) + "\n");

        // Hantering av priser varje kvart
        if (priser.size() == 96) {
            for (int hour = 0; hour < 24; hour++) {
                double summaTimpris = 0;
                for (int quarter = 0; quarter < 4; quarter++) {
                    int index = hour * 4 + quarter;
                    summaTimpris += priser.get(index).sekPerKWh();
                }
                summaTimpris = summaTimpris / 4;
                System.out.println("Medelpriset mellan " +
                        LocalTime.of((hour), 0).format(tidFormatter()) + "-" +
                        LocalTime.of(hour, 0).plusHours(1).format(tidFormatter()) + " är " +
                        formateraPrisIÖre(summaTimpris) + " öre");
            }
        }
        // Ifall det INTE är 96 datapunkter
        else {
            // Skriv ut datapunkterna upp till det högsta antal som efterfrågas i anropet av metoden
            priser.stream().limit(maxAntal).forEach(pris -> {
                LocalTime startTid = pris.timeStart().toLocalTime();
                LocalTime slutTid = startTid.plusHours(1);
                System.out.println(startTid.format(tidFormatter()) + "-" +
                        slutTid.format(tidFormatter()) + " " +
                        formateraPrisIÖre(pris.sekPerKWh()) + " öre");
            });
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
            System.out.println(">>>> Kör optimaltLaddningsFönster för " + valdKlass + " <<<<");
            System.out.println("\nLägsta pris för att ladda inom: " + valdKlass + " (" + antalTimmar + "h):");
            for (int i = bästaStartIndex; i < bästaStartIndex + antalTimmar; i++) {
                ElpriserAPI.Elpris pris = priser.get(i);
                System.out.println("Klockan: " +
                        pris.timeStart().toLocalTime() + ", Pris: " +
                        formateraPrisIÖre(pris.sekPerKWh()) + " öre/kWh");
            }
            LocalTime påBörjaLaddning = priser.get(bästaStartIndex).timeStart().toLocalTime();
            System.out.println("\nPåbörja laddning kl " + påBörjaLaddning);

            System.out.println("Totalt pris för " + antalTimmar + "h: " +
                    formateraPrisIÖre(lägstaPris) + " öre. Medelpris för fönster: " +
                    formateraPrisIÖre(lägstaPris / antalTimmar) + " öre");
        } else {
            System.out.println("Något gick fel när jag försökte hitta laddningsfönster för " + valdKlass);
        }
    }
    public static String formateraPrisIÖre(double sekPerKWh) {
        NumberFormat formatKommatecken = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        formatKommatecken.setMinimumFractionDigits(2);
        formatKommatecken.setMaximumFractionDigits(2);
        double örePerKWh = sekPerKWh * 100;
        return formatKommatecken.format(örePerKWh);
    }
    public static DateTimeFormatter tidFormatter() {
        return DateTimeFormatter.ofPattern("HH");
    }
}