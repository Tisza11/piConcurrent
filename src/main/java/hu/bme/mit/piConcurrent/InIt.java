package hu.bme.mit.piConcurrent;

import java.util.Collections;
import java.util.Comparator;

public class InIt {
    /**
     * Header fileba a nondet fv-ek megvalósítására írt kód összerakása
     *Ez alatt találhatóak a benne felhasznált fv-ek, amik azért lettek kirendezve, hogy átláthatóbb legyen az Init()
     */
    public static void Init(){
        if(Main.edges.size() == 0) return;
        int diff = 0;
        for(int i = 0; i < Main.edges.size(); i++){
            if(Main.edges.get(i).haveNondetfv) diff++;
        }
        if(diff == 0) return;
        diff = 0;
        SortByFunction();
        Main.headerbe.append(Main.edgesForInit.get(0).type + " tomb" + diff + "[" + (int) Main.edgesForInit.stream().filter(c -> Main.edgesForInit.get(0).type.equals(c.type)).count() + "] = {");
        for(int i = 0; i < Main.edgesForInit.size() - 1; i++){
            Main.headerbe.append(Main.edgesForInit.get(i).assumption);
            if(!Main.edgesForInit.get(i).assumptionResultFunction.equals(Main.edgesForInit.get(i + 1).assumptionResultFunction)) {
                Main.headerbe.append("};\nint idx" + diff + " = 0;\n" + Main.edgesForInit.get(i).type + " " + Main.edgesForInit.get(i).assumptionResultFunction + "(){\n\treturn tomb" + diff + "[idx" + diff + "++];\n}\n");
                diff++;
                String seged = Main.edgesForInit.get(i + 1).type;
                Main.headerbe.append(Main.edgesForInit.get(i + 1).type + " tomb" + diff + "[" + (int) Main.edgesForInit.stream().filter(c -> seged.equals(c.type)).count() + "] = {");
            }else{
                Main.headerbe.append(", ");
            }
        }
        Main.headerbe.append(Main.edgesForInit.get(Main.edgesForInit.size() - 1).assumption + "};\nint idx" + diff + " = 0;\n" + Main.edgesForInit.get(Main.edgesForInit.size() - 1).type + " " + Main.edgesForInit.get(Main.edgesForInit.size() - 1).assumptionResultFunction + "(){\n\treturn tomb" + diff + "[idx" + diff + "++];\n}\n");
    }

    /**
     * Ez a fv másolja át az elemeket az elrendezendő ArrayList-be, majd rendezi azt fv nevek alapján
     * Azért lett kiszervezve, hogy az Init() átláthatóbb legyen
     */

    public static void SortByFunction(){
        for(int i = 0; i < Main.edges.size(); i++){
            Edge newEdge = new Edge();
            newEdge = Main.edges.get(i);
            Main.edgesForInit.add(newEdge);
        }
        Collections.sort(Main.edgesForInit,new Comparator<>() {
            @Override
            public int compare(Edge e1, Edge e2) {
                return e1.assumptionResultFunction.compareTo(e2.assumptionResultFunction);
            }
        });
    }
}
