package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.println("___________ Testar Elpriser API _____________");

        //Frågar användaren om vilket område man vill hämta priser ifrån
        System.out.println("Vilket område skulle du vilja hämta priser ifrån? (--zone alla|SE1|SE2|SE3|SE4): ");

        //Validerar kommando/input från användaren i terminalen
        if (args.length < 2 || !args[0].equals("--zone")){
            System.out.println("Ej förväntat argument. Använd \n --zone alla|SE1|SE2|SE3|SE4");
            return;
        }

        // Sparar det andra argumentet av inmatningen i en sträng för att köra hämtning och display
        String valAvPrisKlass = args[1].toUpperCase();

        // Skapa en variabel för att hämta dagens priser
        LocalDate idag = LocalDate.now();


        // IF ---- Enhanced loop for att få fram alla områden
        // ELSE --- framtagning av pris för specifikt område
        if(valAvPrisKlass.equals("-- ZONE ALLA")){
            for(ElpriserAPI.Prisklass klass : ElpriserAPI.Prisklass.values()){
                List<ElpriserAPI.Elpris> allaDagensPriser = elpriserAPI.getPriser(idag, klass);

                if (allaDagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för idag i område: " + klass);
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
                List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(idag, valdKlass);

                if (dagensPriser.isEmpty()) {
                    System.out.println("Kunde inte hämta några priser för idag i område: " + valdKlass);
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
