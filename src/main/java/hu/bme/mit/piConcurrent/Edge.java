package hu.bme.mit.piConcurrent;

public class Edge {
    //Az elvárt eredmény
    public String assumption = null;
    //A kezdősor
    public int startLine;
    //A fv, amit az él reprezentál
    public String assumptionResultFunction;
    //VERIFIER_nondet fv (visszatérési) típusa
    public String type;
    //Allítás az adott élen (sokszor egy sor)
    public String stmt;
    //Használható él
    public boolean toUse = false;
    //Az adott él egy nondet fv-t reprezentál
    public boolean haveNondetfv = false;
    //Másik szál-e az előző élhez képest
    public boolean threadChanges = false;
    //Véget ér-e ezután már a fv, aminek ez az egyik (utolsó) sora
    public boolean isLast = false;
    //Szál azonosító
    public int thread;
    //létrehozott szál
    public int createdThread;

    /**
     * Paraméter nélüli konstruktor
     */
    public Edge(){
        assumption = "";
        startLine = 0;
        assumptionResultFunction = "fgv";
        type = "";
        thread = -1;
    }

    /**
     * Paraméterezett konstruktor
     * @param a - fordítás és futtatás
     * @param sl - Startline
     * @param arf - Assumptionresultfunction
     * @param t - Type
     * @param trd - Thread
     */
    public Edge(String a, int sl, String arf, String t, int trd){
        assumption = a;
        startLine = sl;
        assumptionResultFunction = arf;
        type = t;
        thread = trd;
    }

    /**
     * Kiiratja a jellemzőit egy élnek
     */
    public void Writer(){
        if(toUse)System.out.println("Assumption: " + assumption + "\tAssumptionresultfunction: " + assumptionResultFunction + "\tStartline: " + startLine + "\tThread: " + thread);
    }
}
