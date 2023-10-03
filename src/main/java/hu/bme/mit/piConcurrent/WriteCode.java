package hu.bme.mit.piConcurrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteCode {
    /**
     * C és header fileok megírása
     * Ez alatt találhatóak a benne felhasznált fv-ek, amik azért lettek kirendezve, hogy átláthatóbb legyen az WriteCode()
     */
    public static void Write_Code(){
        SearchThreadChanges();
        int j = SearchFirstJoin();
        /*int LastLineInMain = */FindLastLines();
        int eleje = TopOfCode();
        //sorok és hozzájuk tartozó részek összeállítása
        for(int i = eleje; i < Main.sorok.size(); i++){
            if(j == (i + 1) && j > 0)Main.kod.append("    pthread_mutex_unlock(&mutex);\n");
            SetTheEndOfFunctions(i);
            Main.kod.append(Main.sorok.get(i));   /*<----------sor hozzáadás---------<<<<<*/
            Main.kod.append("\n");
            SetAfterCrate(i);
            SetTheChange(i);
            SetTheStartOfFunctions(i);
        }
        CleareFolder();
        WriteOut();
    }

    /**
     * Beállítja a threadChanged flaget ott, ahol van szálváltás (az új threadId-val rendelkező szálra)
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     */
    public static void SearchThreadChanges(){
        for(int i = 0; i < Main.edges.size(); i++){
            if(Main.edges.get(i).thread != -1){
                Main.szalelek.add(Main.edges.get(i));
            }
        }
        //ha adott élre történik az ugrás
        for (int i = 0; i < Main.szalelek.size() - 1; i++) {
            if(Main.szalelek.get(i).thread != Main.szalelek.get(i + 1).thread){
                Main.szalelek.get(i).threadChanges = true;
            }
        }
    }

    /**
     * A kód elejére írt importok, globális változók, amik előre adottak
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     */
    public static int TopOfCode(){
        int eleje = 0;
        for(int i = 0; i < Main.edges.size(); i++){
            if(eleje == 0) eleje = Main.edges.get(i).startLine;
            if(Main.edges.get(i).startLine < eleje && Main.edges.get(i).startLine != 0) eleje = Main.edges.get(i).startLine;
        }
        Main.kod.append("/*\n");
        for(int i = 0; i < eleje; i++){
            Main.kod.append(Main.sorok.get(i));   /*<----------sor hozzáadás---------<<<<<*/
            Main.kod.append("\n");
        }
        Main.kod.append("*/\n");
        Main.kod.append(
                /*"#include <assert.h>\n" +
                "#include <stdbool.h>\n" +
                "#include <stdio.h>\n" +
                "#include <pthread.h>\n" +
                "#include \"nondetfvek.h\"\n" +*/



                "#include <assert.h>\n" +
                "#include \"nondetfvek.h\"\n" +
                "#include <pthread.h>\n" +
                "extern void abort(void);\n" +
                "extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));\n" +
                "void reach_error() {_assert(\"0\", \"valahol\", 3); }\n" +
                "void __VERIFIER_assert(int cond) {\n" +
                "    if (!(cond)) {\n" +
                "        ERROR: {reach_error();abort();}\n" +
                "    }\n" +
                "    return;\n" +
                "}\n" +



                "\npthread_mutex_t mutex;\n" +
                        "pthread_cond_t cond;\n" +
                        "int flag;\n");
        //thread ID-kat tárolják
        for(int i = 0; i < Main.idk.size();i++){
            Main.kod.append("int t");
            Main.kod.append(Main.idk.get(i));
            Main.kod.append(" = -1;\n");
        }
        return eleje;
    }

    /**
     * Megkeresi a (main-ben a) join első előfordulását --> (ez még lehet, hogy nem les így jó, ha két join között szükség lesz a mutexre)
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     * @return - megkeresi az ez első join-t a mainben
     */
    public static int SearchFirstJoin(){
        Pattern pattern;
        Matcher matcher;
        int start = 1;
        int end = 0;
        int sor = 0;
        //első join elé mutex unlock, hogy működjön (talán csak main-ben lesz ilyen, de nincs kizárva máshol sem, bár itt nem lesz még lekezelve)
//        pattern = Pattern.compile("main");
//        matcher = pattern.matcher(sorok.get(j));
//        while(!matcher.find() && j < sorok.size()){
//            j++;
//            matcher = pattern.matcher(sorok.get(j));
//        }
        for(int i = 0; i < Main.funok.size(); i++){
            if(Main.funok.get(i).name.equals("main")){
                start = Main.funok.get(i).startLine;
                end = Main.funok.get(i).endLine;
            }
        }
        pattern = Pattern.compile(".*pthread_join.*");
        matcher = pattern.matcher(Main.sorok.get(start - 1));
        while(start < end){
            if(matcher.find()){
                sor = start;
                return sor;
            }
            start++;
            matcher = pattern.matcher(Main.sorok.get(start - 1));
        }
        return sor;
    }

    /**
     * Megtalálja minden thread utolsó futó (witnessben feltüntetett) sorát, majd azon beállítja a megfelelő flag-et
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     * @return - visszatér annak a sornak a számával, ami utoljára fut le a main-en belül
     */
    public static int FindLastLines(){
        //utolsó részek felderítése egyes fv-ekből, mainnél majd kicsit másképp
        int k = 0;/* = edges.size() - 1*;*/   //ha a lenti while ciklus lenne a for helyett
        int lastOfMain = 0;
        int idx = 0;
        for(int i = 0; i < Main.funok.size(); i++){
            if(Main.funok.get(i).name.equals("main")) idx = i;
        }
        for (int i = 0; i < Main.idk.size(); i++) {
            //a for ciklusnál optimálisabbnak tűnik, de nincs még kipróbálva
//            while (k >= 0 && !(edges.get(k).Thread == idk.get(i))){
//                k--;
//            }
            for(int l = 0; l < Main.edges.size(); l++){
                if(Main.edges.get(l).thread == Main.idk.get(i)){
                    if(Main.idk.get(i) == 0){
                        if(Main.edges.get(l).startLine > Main.funok.get(idx).startLine && Main.edges.get(l).startLine < Main.funok.get(idx).endLine)
                            k = l;
                    }
                    else k = l;
                }
            }
            Main.lastLines.put(k, Main.edges.get(k).startLine);
            //k = edges.size(); //while-nál
        }
        return lastOfMain;
    }

    /**
     * A fv-ek utolsó futó sora elé/után hozzáadja a mutex, condition variable és várakozás miatti szükséges sorokat
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     * @param i - i-edik sorhoz kapja majd meg az indexet
     */
    public static void SetTheEndOfFunctions(int i){
        int idx = 0;
        int idxM = 0;
        //Megvizsgálja, hogy a lastLines Map-nek(k-dik él, StartLine), van-e olyan értéke (StartLine), ami i, tehát az i-dik sor szerepel-e az utolsó sorok között (i+1 a main miatt kell)
        if(Main.lastLines.containsValue(i) || Main.lastLines.containsValue(i + 1)){
            //végig megy a Map összes elemén, hogy kiderítse, hogy ahhoz az utolsó sorhoz milyen él index tartozik
            for (Map.Entry mapElement: Main.lastLines.entrySet()) {
                if((int)mapElement.getValue() == i){
                    //Ezen az élen van az utolsó sora egy fv-nek
                    idx = (int)mapElement.getKey();
                }
                if((int)mapElement.getValue() == (i + 1)){
                    //Ezen az élen van az utolsó sora egy fv-nek
                    idxM = (int)mapElement.getKey();
                }
            }
            //más, ha a main vége...
            if(Main.edges.get(idxM).thread == 0 && Main.lastLines.containsValue(i + 1)){
                Main.kod.append("    pthread_cond_destroy(&cond);\n" +
                        "    pthread_mutex_destroy(&mutex);\n");
            }
            //...és más, ha egyéb fv utolsó sora
            if(Main.edges.get(idx).thread != 0 && Main.lastLines.containsValue(i)){
                Main.kod.append("    flag = t" + Main.edges.get(idx + 1).thread + ";\n" +
                        "    pthread_cond_broadcast(&cond);\n" +
                        "    pthread_mutex_unlock(&mutex);\n");
            }
        }
    }

    /**
     * A pthread_create() sorok után a létrejött thread ID-ja kerü eltárolásra
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     * @param i - i-edik sorhoz kapja majd meg az indexet
     */
    public static void SetAfterCrate(int i){
        if(Main.sorok.get(i).contains("pthread_create")){
            for(int a = 0; a < Main.edges.size(); a++){
                int idx = Main.sorok.get(i).indexOf("&");
                if(Main.edges.get(a).startLine == (i + 1)){
                    Main.kod.append("    t" + Main.edges.get(a).createdThread + " = " + Main.sorok.get(i).charAt(idx + 1) + ";\n");
                }
            }
        }
    }

    /**
     * Ha a paraméterként kapott indexű sor után szál váltás történik (amivel még nem ér véget a szál), akkor a szálváltás menetét írja hozzá
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     * @param idx - i-edik sorhoz kapja majd meg az indexet
     */
    public static void SetTheChange(int idx){
        for (Map.Entry mapElement: Main.lastLines.entrySet()) {
            if((int)mapElement.getValue() == idx + 1){
                //Ha utolsó él, nem sima váltás következik
                return;
            }
        }
        for (int i = 0; i < Main.szalelek.size(); i++){
            if(Main.szalelek.get(i).startLine == (idx + 1) && Main.szalelek.get(i).threadChanges){
                Main.kod.append(
                        "    flag = t" + Main.szalelek.get(i + 1).thread +";\n" +
                                "    pthread_cond_broadcast(&cond);\n");
                if(Main.szalelek.get(i).thread == 0){
                    Main.kod.append("    while (flag != t0) {\n");
                }
                else{
                    Main.kod.append("    while (flag != maga) {\n");
                }
                Main.kod.append(
                        "        pthread_cond_wait(&cond, &mutex);\n" +
                                "    }\n");
            }
        }
    }

    /**
     * A fv-ek fejléce után hozzáadja a mutex, condition variable és várakozás miatti szükséges sorokat
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     * @param i - i-edik sort vizsgálja
     */
    public static void SetTheStartOfFunctions(int i){
        Pattern pattern;
        Matcher matcher;
        //valami szóköz csillag valami nyitó zárójel - regex-re illeszkedő után (remélhetőleg fv) elején fv eleji beállítási dolgok
        pattern = Pattern.compile(".*\\*.*\\(.*\\).*\\{.*");
        matcher = pattern.matcher(Main.sorok.get(i));
        if(matcher.find() && !Main.sorok.get(i).contains("main")){
            Main.kod.append("    pthread_mutex_lock(&mutex);\n" +
                    "    int maga = pthread_self();\n" +
                    "    while (flag != maga) {\n" +
                    "        pthread_cond_wait(&cond, &mutex);\n" +
                    "    }\n");
        }
        else{
            //main eleji beállítások
            pattern = Pattern.compile("int main\\(.*");
            matcher = pattern.matcher(Main.sorok.get(i));
            if(matcher.find()){
                Main.kod.append("    pthread_cond_init(&cond, 0);\n" +
                        "    pthread_mutex_init(&mutex, 0);\n" +
                        "    pthread_mutex_lock(&mutex);\n" +
                        "    t0 = pthread_self();\n");
            }
        }
    }

    /**
     * Ha van bármi a mappában, kitörli
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     */
    public static void CleareFolder(){
        File tFolder = new File(Main.targetFolder);
        try{
            if(!tFolder.exists())
                new File(Main.targetFolder).mkdir();
        }catch (Exception e){

        }
        String[] files = tFolder.list();
        if(files == null) return;   //???????????????????????????????????????????????????
        if(files.length > 0){
            final File[] fileok = tFolder.listFiles();
            for (File f: fileok) f.delete();
        }
    }

    /**
     * A header és c fileba beleírja a szükséges dolgokat
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     */
    public static void WriteOut(){
        //c file kiírása
        try {
            FileWriter myWriter = new FileWriter(Main.targetFolder + "/main.c");
            myWriter.write(Main.kod.toString());
            myWriter.close();
        } catch (IOException e) {
            System.out.println("Hiba van a kiiratassal.");
            e.printStackTrace();
        }
        //header file kiiratása
        try {
            FileWriter myWriter = new FileWriter(Main.targetFolder + "/nondetfvek.h");
            myWriter.write("#ifndef NONDETFVEK_H_INCLUDED\n" +
                    "#define NONDETFVEK_H_INCLUDED\n");
            myWriter.write(Main.headerbe.toString());
            myWriter.write("#endif");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("Hiba van a kiiratassal.");
            e.printStackTrace();
        }
    }
}
