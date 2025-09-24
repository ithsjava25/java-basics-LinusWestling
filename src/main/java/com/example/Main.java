package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        System.out.println("___________ Testar Elpriser API _____________");
        Scanner scanner = new Scanner(System.in);
        ElpriserAPI elpriserAPI = new ElpriserAPI();


        //Frågar användaren om vilket område man vill hämta priser ifrån
        System.out.println("Vilket område skulle du vilja hämta priser ifrån? (alla|SE1|SE2|SE3|SE4): ");
        String valAvPrisKlass = scanner.nextLine().trim().toUpperCase();

        // Skapa en variabel för vilken dag man vill hämta priser
        LocalDate idag = LocalDate.now();
        LocalDate datum;
        System.out.println("Vill du hämta priser för idag eller imorgon? (idag/imorgon)");
        String vilkenDag = scanner.nextLine().trim().toUpperCase();

        // Kolla vilken dag användaren har valt
        if (vilkenDag.equals("IDAG")){
            datum = idag;
        }else if (vilkenDag.equals("IMORGON")) {
            datum = idag.plusDays(1);
        } else {
            System.out.println("Ogiltigt datum, default är dagens datum");
            datum = idag;
        }


        System.out.println("Påbörja laddning"); // Behövs vara med enligt testet????


        // IF ---- Enhanced loop for att få fram alla områden
        // ELSE --- framtagning av pris för specifikt område
        if(valAvPrisKlass.equals("ALLA")){
            for(ElpriserAPI.Prisklass klass : ElpriserAPI.Prisklass.values()){
                List<ElpriserAPI.Elpris> allaDagensPriser = elpriserAPI.getPriser(datum, klass);

                if (allaDagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för " + datum + " i område: " + klass);
                } else {
                    System.out.println("\nDagens elpriser för " + klass + " (" + allaDagensPriser.size() + " st värden):");
                    // Skriv bara ut de 3 första för att hålla utskriften kort
                    allaDagensPriser.stream().limit(3).forEach(pris ->
                            System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                                    pris.timeStart().toLocalTime(), pris.sekPerKWh()));
                    if (allaDagensPriser.size() > 3) System.out.println("...");
                }
            }
        } else {
            try{
                ElpriserAPI.Prisklass valdKlass = ElpriserAPI.Prisklass.valueOf(valAvPrisKlass);
                List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdKlass);

                if (dagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för " + datum + " i område: " + valdKlass);
                } else {
                    System.out.println("\nDagens elpriser för " + valdKlass + " (" + dagensPriser.size() + " st värden):");
                    // Skriv bara ut de 3 första för att hålla utskriften kort
                    dagensPriser.stream().limit(3).forEach(pris ->
                            System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                                    pris.timeStart().toLocalTime(), pris.sekPerKWh()));
                    if (dagensPriser.size() > 3) System.out.println("...");
                }
            } catch (IllegalArgumentException e){
                System.out.println("Ogiltigt område angivet, försök igen.");
            }
        }
    }
}
