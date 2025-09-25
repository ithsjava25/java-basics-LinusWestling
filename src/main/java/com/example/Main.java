package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("___________ Testar Elpriser API _____________");
        Scanner scanner = new Scanner(System.in);
        ElpriserAPI elpriserAPI = new ElpriserAPI();


        //Frågar användaren om vilket område man vill hämta priser ifrån
        System.out.println("Vilket område skulle du vilja hämta priser ifrån? (alla|SE1|SE2|SE3|SE4): ");
        String valAvPrisKlass = scanner.nextLine().trim().toUpperCase();


        // Skapa en variabel för vilken dag man vill hämta priser
        LocalDate datum = LocalDate.now();
        System.out.println("Vill du hämta priser för idag eller imorgon? (idag/imorgon)");
        String vilkenDag = scanner.nextLine().trim().toUpperCase();
        

        // Default är dagens datum, kollar ifall användaren vill ha morgondagens och ändrar
        if (vilkenDag.equals("IMORGON"))
            datum = datum.plusDays(1);


        // Sortera listan utifrån pris istället för tid
        boolean sorteraPriser = false;
        System.out.println("Vill du sortera på pris? (y/n): ");
        String sorteraPris = scanner.nextLine().trim().toLowerCase();
        if (sorteraPris.equals("y"))
            sorteraPriser = true;


        // Kalla på metod för optimalt laddningsfönster
        if (!valAvPrisKlass.equals("ALLA") && !sorteraPriser){
            System.out.println("Vill du kolla efter optimalt laddningsfönster? (y/n): ");
            String kollaOptimaltLaddningsFönster = scanner.nextLine().trim().toUpperCase();
            
            if (kollaOptimaltLaddningsFönster.equals("Y")){
                System.out.println("Hur många timmar vill du ladda? ");
                int antalTimmar = scanner.nextInt();
                optimaltLaddningsFönster(ElpriserAPI.Prisklass.valueOf(valAvPrisKlass), datum, antalTimmar);
            }
        }


        System.out.println("Påbörja laddning"); // Behövs vara med enligt testet????

        // IF ---- Enhanced loop for att få fram alla områden
        if(valAvPrisKlass.equals("ALLA")){
            for(ElpriserAPI.Prisklass valdKlass : ElpriserAPI.Prisklass.values()){
                //List<ElpriserAPI.Elpris> allaDagensPriser = elpriserAPI.getPriser(datum, valdKlass);

                if (allaDagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för " + datum + " i område: " + valAvPrisKlass);
                } else {
                    skrivUtPriser(valdKlass, allaDagensPriser, sorteraPriser, 3);
                }
            }
        }
        // ELSE --- framtagning av pris för specifikt område
        else {
            try{
                ElpriserAPI.Prisklass valdKlass = ElpriserAPI.Prisklass.valueOf(valAvPrisKlass);
                List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdKlass);

                if (dagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för " + datum + " i område: " + valdKlass);
                } else {
                    skrivUtPriser(valdKlass, dagensPriser, sorteraPriser, 3);
                }
            } catch (IllegalArgumentException e){
                System.out.println("Ogiltigt område angivet, försök igen.");
            }
        }
    }
    public static void skrivUtPriser(ElpriserAPI.Prisklass valdKlass, List<ElpriserAPI.Elpris> priser, boolean sorteraPriser, int maxAntal){
        if (sorteraPriser)
            priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());

        System.out.println("\nDagens elpriser för " + valdKlass + " (" + priser.size() + " st värden):");
        // Skriv ut antal rader som efterfrågas i metoden
        priser.stream().limit(8).forEach(pris ->
                System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                        pris.timeStart().toLocalTime(), pris.sekPerKWh()));
        if (priser.size() > 8) System.out.println("...");
    }
    public static void optimaltLaddningsFönster(ElpriserAPI.Prisklass valdKlass, LocalDate priser, int antalTimmar){

        //Deklarera variabler att spara pris och startindex i
        double lägstaPris = Double.MAX_VALUE;
        int bästaStartIndex = -1;

        // Loop för att iterera genom alla priser i listan för dagen och elområdet
        for (int i = 0; i < priser.size(); i++){
            double nuvarandePris = 0;

            // Addera priser i sekvenser om hur många timmar man vill ladda
            for (int j = 0; j < antalTimmar; j++){
                nuvarandePris += priser.get(j).sekPerKWh();
            }
            // Uppdatera priset ifall det är lägre
            if (nuvarandePris < lägstaPris) {
                lägstaPris = nuvarandePris;
                bästaStartIndex = i;
            }
        }
        // Skriv ut priserna
        if (bästaStartIndex < 0){
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
