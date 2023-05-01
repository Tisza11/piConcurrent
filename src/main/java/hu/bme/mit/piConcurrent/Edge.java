package hu.bme.mit.piConcurrent;

public class Edge {
    //Az elvárt eredmény
    public String Assumption = null;
    //A kezdősor
    public int Startline;
    //A fv, amit az él reprezentál
    public String Assumptionresultfunction;
    //VERIFIER_nondet fv (visszatérési) típusa
    public String Type;
    //Allítás az adott élen (sokszor egy sor)
    public String Stmt;
    //Használható él
    public boolean toUse = false;
    //Az adott él egy nondet fv-t reprezentál
    public boolean haveNondetfv = false;
    //Másik szál-e az előző élhez képest
    public boolean threadChanged = false;
    //Véget ér-e ezután már a fv, aminek ez az egyik (utolsó) sora
    public boolean isLast = false;
    //Szál azonosító
    public int Thread;

    /**
     * Paraméter nélüli konstruktor
     */
    public Edge(){
        Assumption = "";
        Startline = 0;
        Assumptionresultfunction = "fgv";
        Type = "";
        Thread = -1;
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
        Assumption = a;
        Startline = sl;
        Assumptionresultfunction = arf;
        Type = t;
        Thread = trd;
    }

    /**
     * Kiiratja a jellemzőit egy élnek
     */
    public void Writer(){
        if(toUse)System.out.println("Assumption: " + Assumption + "\tAssumptionresultfunction: " + Assumptionresultfunction + "\tStartline: " + Startline + "\tThread: " + Thread);
    }
}
