package hu.bme.mit.piConcurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    //majd  @Parameter -nek ki kell szervezni, de amíg építem, addig marad így
    //C file neve
    public static String codeFile;

    //élek adatainak eltárolása beolvasás után ebben
    static ArrayList<Edge> edges = new ArrayList<>();

    //élek listájának fenntartása ahhoz, hogy rendezve legyenek fv szerint
    static ArrayList<Edge> edgesForInit = new ArrayList<>();

    //fv-ek tárolása (név, kezdő sor, utolsó sor)
    static ArrayList<Fun> funok = new ArrayList<>();

    //majd  @Parameter -nek ki kell szervezni, de amíg építem, addig marad így
    //witness file elérési útja
    public static String gmlFile;

    //ez a részlet fog bekerülni a C kódba a main elé, hogy futtatható legyen (nondet fv megvalósítások)
    public static StringBuilder headerbe = new StringBuilder();

    //thread ID-k
    static ArrayList<Integer> idk = new ArrayList<>();

    //ebbe lesz kiirvava a C kód
    public static StringBuilder kod = new StringBuilder();

    //edge-nek a k-dik idx-e, StartLine
    static Map<Integer, Integer> lastLines = new HashMap<>();

    //soronként tárolva a kódot
    public static ArrayList<String> sorok = new ArrayList<>();

    //ebben a mappában van a .i és *.graphml file
    public static String sourceFolder;

    //ebben vannak eltárolva azok az élek, amelyeken meg van adva threadID
    public static ArrayList<Edge> szalelek = new ArrayList<>();

//    //a jelenlegi szálat tárolja    //jelenleg nincs használva
//    public static int CurrentThread = 0;

    //majd  @Parameter -nek ki kell szervezni, de amíg építem, addig marad így
    //cél mappa elérési útja
    public static String targetFolder = "futasra";

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
//        sourceFolder = "C:\\egyetem masolat\\felev6\\Onlab\\c-probak\\";
        sourceFolder = "";
        gmlFile = "witnessAzonos.graphml";
        codeFile = "funok.i";
//        targetFolder = "C:\\egyetem masolat\\felev6\\Onlab\\futasra\\";
        if(!ReadXML(gmlFile, codeFile)) return;
        Init();
        ReadCode(/*codeFolder, */codeFile);
        ReadFuns();
        Checker();
        WriteCode();
        CompileCprog();
    }

    /**
     * beolvas egy graphml filet és el is tárolja
     * Ez alatt találhatóak a benne felhasznált fv-ek, amik azért lettek kirendezve, hogy átláthatóbb legyen az ReadXML()
     * @param gmlFile --> a witness file elérési útját meglapja
     * @param keres --> a C file neve, amit megkap és megkeres
     * @return --> összetartozik-e a C és a witness file
     */
    public static boolean ReadXML(String gmlFile, String keres) throws ParserConfigurationException, IOException, SAXException {
        boolean violation = true;
//        int idx = 0;
//        for (int i = 0; i < keres.length(); i++) {
//            if(keres.charAt(i) == '\\') idx = i;
//        }
        if(!CheckNameInGraphML(gmlFile, keres))return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(sourceFolder + gmlFile);
        violation = CheckWitnessType(doc);
        if(!violation)return false;
        NodeList edges = doc.getElementsByTagName("edge");
        //végig megyünk az éleken
        for(int i = 0; i < edges.getLength(); i++){
            Node e = edges.item(i);
            if(e.getNodeType() == Node.ELEMENT_NODE){
                Element edge = (Element) e;
                NodeList datas = edge.getChildNodes();
                Edge newEdge = new Edge();
                CheckEdges(datas, newEdge);
                //használható éleket felvesszük
                if(newEdge.toUse) Main.edges.add(newEdge);
            }
        }
        return true;
    }



    /**
     * Beolvassa a graphml filet és megnézi, hogy szerepel-e benne a c feladat file neve
     * Azért lett kiszervezve, hogy az ReadXML() átláthatóbb legyen
     */
    public static boolean CheckNameInGraphML(String gmlFile, String keres){
        boolean correct = false;
        File myFile = new File(sourceFolder + gmlFile);
        Scanner myReader = null;
        try {
            myReader = new Scanner(myFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        //vizsgálja, hogy jó-e a C file - witness file páros
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            if(data.contains(keres/*.substring(idx + 1)*/)){
                correct = true;
                break;
            }
        }
        if(!correct) return false;
        return true;
    }

    /**
     * Kideríti, hogy violation vagy correctness witness-ről van szó
     * Azért lett kiszervezve, hogy az ReadXML() átláthatóbb legyen
     * @param doc - A graphml file beolvasva Document típusként, ebben lehet keresni
     * @return - violation witness-e
     */
    public static boolean CheckWitnessType(Document doc) {
        boolean violation = true;
        //keressük, hogy correctness witness-e
        //data node-e
        NodeList dataAttributes = doc.getElementsByTagName("data");
        for(int i = 0; i < dataAttributes.getLength(); i++){
            Node data = dataAttributes.item(i);
            if (data.getNodeType() == Node.ELEMENT_NODE){
                Element d = (Element) data;
                if (d.getAttribute("key").equals("witness-type") && d.getTextContent().contains("correctness")){
                    //System.out.println("correctness");
                    violation = false;
                }
                if (d.getAttribute("key").equals("witness-type") && d.getTextContent().contains("violation")){
                    //System.out.println("violation");
                    violation = true;
                }
            }
//                dataAttributes.item(i).getAttributes();//megnézni, hogy correctnesswitness-e
        }
        return violation;
    }

    /**
     * Végig megyünk egy adott él adatain
     * Azért lett kiszervezve, hogy az ReadXML() átláthatóbb legyen
     * @param datas - Egy adott él adatai
     * @param newEdge - Az új él, ami fel lesz véve
     */
    public static void CheckEdges(NodeList datas, Edge newEdge){
        for(int j = 0; j < datas.getLength(); j++){
            Node d = datas.item(j);
            if(d.getNodeType() == Node.ELEMENT_NODE){
                Element data = (Element) d;
                FindAssumption(data, newEdge);
                FindThreadID(data, newEdge);
                //Pl.: for ciklus eleme-e és akkor figyelmen kívül hagyható-e (pl i < 5 vizsálata)
                if(data.getAttribute("key").equals("stmt")) {
                    newEdge.stmt = data.getTextContent();
                }
                FindFunType(data, newEdge);
                if(data.getAttribute("key").equals("startline")) {
                    newEdge.startLine = Integer.parseInt(data.getTextContent());
                    newEdge.toUse = true;
                }
            }
        }
    }

    /**
     * Ha egy élen van olyan adat, ami az elvárt eredményt tárolja (assumption), azt elmentjük az élre
     * Azért lett kiszervezve, hogy az ReadXML() átláthatóbb legyen
     * @param data - Egy él egy adata
     * @param newEdge - Az új él, ami fel lesz véve
     */
    public static void FindAssumption(Element data, Edge newEdge){
        if(data.getAttribute("key").equals("assumption")) {
            Pattern pattern = Pattern.compile("==");
            Matcher matcher = pattern.matcher(data.getTextContent());
            if(matcher.find()){
                int strtidx = matcher.end();
                newEdge.assumption = data.getTextContent().substring(strtidx);
            }
        }
    }

    /**
     * Ha egy élen van olyan adat, ami a threadID-t tárolja (akár a jelenlegit, akár az éppen létrehozottat), azt elmentjük az élre
     * Azért lett kiszervezve, hogy az ReadXML() átláthatóbb legyen
     * @param data - Egy él egy adata
     * @param newEdge - Az új él, ami fel lesz véve
     */
    public static void FindThreadID(Element data, Edge newEdge){
        if(data.getAttribute("key").equals("threadId")) {
            newEdge.thread = Integer.parseInt(data.getTextContent());
            newEdge.toUse = true;
            if(!idk.contains(Integer.parseInt(data.getTextContent()))) idk.add(Integer.parseInt(data.getTextContent()));
        }
        if(data.getAttribute("key").equals("createThread")) {
            newEdge.createdThread = Integer.parseInt(data.getTextContent());
        }
    }

    /**
     * Ha egy élen van olyan adat, ami egy __VERIFIER_nondet típusú fv-t tárolj, azt elmentjük az élre
     * Azért lett kiszervezve, hogy az ReadXML() átláthatóbb legyen
     * @param data - Egy él egy adata
     * @param newEdge - Az új él, ami fel lesz véve
     */
    public static void FindFunType(Element data, Edge newEdge){
        //Theta cSource-ban adja meg, Symbiotic assumption.resultfunction-ként adta meg, akár vagyként is lehetne
        //if(data.getAttribute("key").equals("assumption.resultfunction")) {
        if(data.getAttribute("key").equals("cSource")) {
            if(data.getTextContent().startsWith("__VERIFIER_nondet_")){
                Pattern pattern = Pattern.compile("__VERIFIER_nondet_" + "[a-z]+");
                Matcher matcher = pattern.matcher(data.getTextContent());
                matcher.find();
                int endidx = matcher.end();
                String funcname = data.getTextContent().substring(0, endidx);
                newEdge.type = funcname.substring(("__VERIFIER_nondet_").length());
                if(newEdge.type.startsWith("u")) newEdge.type = "unsigned " + newEdge.type.substring(1);
                newEdge.assumptionResultFunction = funcname;
                newEdge.haveNondetfv = true;
            }
        }
    }

    /**
     * Header fileba a nondet fv-ek megvalósítására írt kód összerakása
     *Ez alatt találhatóak a benne felhasznált fv-ek, amik azért lettek kirendezve, hogy átláthatóbb legyen az Init()
     */
    public static void Init(){
        if(edges.size() == 0) return;
        int diff = 0;
        for(int i = 0; i < edges.size(); i++){
            if(edges.get(i).haveNondetfv) diff++;
        }
        if(diff == 0) return;
        diff = 0;
        SortByFunction();
        headerbe.append(edgesForInit.get(0).type + " tomb" + diff + "[" + (int) edgesForInit.stream().filter(c -> edgesForInit.get(0).type.equals(c.type)).count() + "] = {");
        for(int i = 0; i < edgesForInit.size() - 1; i++){
            headerbe.append(edgesForInit.get(i).assumption);
            if(!edgesForInit.get(i).assumptionResultFunction.equals(edgesForInit.get(i + 1).assumptionResultFunction)) {
                headerbe.append("};\nint idx" + diff + " = 0;\n" + edgesForInit.get(i).type + " " + edgesForInit.get(i).assumptionResultFunction + "(){\n\treturn tomb" + diff + "[idx" + diff + "++];\n}\n");
                diff++;
                String seged = edgesForInit.get(i + 1).type;
                headerbe.append(edgesForInit.get(i + 1).type + " tomb" + diff + "[" + (int) edgesForInit.stream().filter(c -> seged.equals(c.type)).count() + "] = {");
            }else{
                headerbe.append(", ");
            }
        }
        headerbe.append(edgesForInit.get(edgesForInit.size() - 1).assumption + "};\nint idx" + diff + " = 0;\n" + edgesForInit.get(edgesForInit.size() - 1).type + " " + edgesForInit.get(edgesForInit.size() - 1).assumptionResultFunction + "(){\n\treturn tomb" + diff + "[idx" + diff + "++];\n}\n");
    }

    /**
     * Ez a fv másolja át az elemeket az erendezendő ArrayList-be, majd rendezi azt fv nevek alapján
     * Azért lett kiszervezve, hogy az Init() átláthatóbb legyen
     */

    public static void SortByFunction(){
        for(int i = 0; i < edges.size(); i++){
            Edge newEdge = new Edge();
            newEdge = edges.get(i);
            edgesForInit.add(newEdge);
        }
        Collections.sort(edgesForInit,new Comparator<>() {
            @Override
            public int compare(Edge e1, Edge e2) {
                return e1.assumptionResultFunction.compareTo(e2.assumptionResultFunction);
            }
        });
    }

    /**
     * beolvasásra kerül a c/i file tartalma
     * @param codeFile ez a code file kerül beolvasásra
     */
    public static void ReadCode(/*String codeFolder, */String codeFile){
        try {
            File myFile = new File(sourceFolder + codeFile);
            Scanner myReader = new Scanner(myFile);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                //akor kell, ha a saját windows-os gépemen akarom futtatni, library gondok miatt
//                int lastvmi = data.lastIndexOf('{');
//                int lastassertf = data.lastIndexOf("__assert_fail");
//                String seged1 = "";
//                if(lastvmi > -1 && lastassertf > -1){
//                    seged1  = data.substring(lastvmi, lastassertf + ("__assert_fail").length());
//                }
//                if(data.contains(seged1) && lastvmi > -1 && lastassertf > -1){
//                    String val = data.substring(0,lastvmi + 1) + "_assert" + data.substring(lastassertf + ("__assert_fail").length());
//                    data = val;
//
//                    int lastComa = data.lastIndexOf(',');
//                    int lastBracket = data.lastIndexOf(')');
//                    String val1 = data.substring(0,lastComa) + data.substring(lastBracket);
//                    data = val1;
//                    //System.out.println(data + "\n");
//                }
                sorok.add(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Hiba van a beolvasassal.");
            e.printStackTrace();
        }
    }

    /**
     * Megkeresi a használt fv-ek neveit, első és utolsó sorait
     */
    public static void ReadFuns() {
        Pattern pattern;
        Matcher matcher;
        int i = 1833;
        int counter = 1;
        //whitespace, valami, whitespace, csillag, whitespace, valami, whitespace, nyitó zárójel, valami, záró zárójel, whitespace, nyitó kapcsoszárójel - regex-re illeszkedő fv
//        pattern = Pattern.compile("\\s*.*\\s*\\*\\s*.*\\s*\\(.*\\)\\s*\\{\n");
        pattern = Pattern.compile(".*\\*.*\\(.*\\).*\\{.*");
        while (i < sorok.size()) {
            matcher = pattern.matcher(sorok.get(i));
            if (matcher.find() || sorok.get(i).contains("main")) {
                Fun fun = new Fun();
                if(sorok.get(i).contains("main")) fun.name = "main";
                else fun.name = sorok.get(i).substring(sorok.get(i).indexOf('*'), sorok.get(i).indexOf('('));
                if(fun.name.contains(" ")) fun.name = fun.name.trim();
                fun.startLine = i;
                while(counter > 0){
                    i++;
                    if(sorok.get(i).contains("{") || sorok.get(i).contains("}")){
                        for(int j = 0; j < sorok.get(i).length(); j++){
                            if(sorok.get(i).charAt(j) == '{') counter++;
                            if(sorok.get(i).charAt(j) == '}') counter--;
                        }
                    }
                }
                fun.endLine = i;
                funok.add(fun);
                counter = 1;
            }
            i++;
        }
    }

    /**
     * C file átnézése, ha a witnessben nem derült ki, hogy milyen fgvt kell hívni
     */
    public static void Checker(){
        for(int i = 0; i < edges.size(); i++){
            if(edges.get(i).assumptionResultFunction.equals("fgv")){
                int sor = edges.get(i).startLine;
                if(!sorok.get(i).contains("__VERIFIER_nondet_")) return;
                //2 sor lett hozzá adva (--> nem ehhez van hozzáadva elv)
                int typeEleje = sorok.get(sor + 1).lastIndexOf("__VERIFIER_nondet_");

                Pattern pattern = Pattern.compile("__VERIFIER_nondet_" + "[a-z]+");
                Matcher matcher = pattern.matcher(sorok.get(sor - 1));
                matcher.find();
                int endidx = matcher.end();
                int startidx = matcher.start();
                String funcname = sorok.get(sor + 1).substring(startidx, endidx);
                edges.get(i).type = funcname.substring(("__VERIFIER_nondet_").length());
                if(edges.get(i).type.startsWith("u")) edges.get(i).type = "unsigned " + edges.get(i).type.substring(1);
                edges.get(i).assumptionResultFunction = funcname;

            }
        }
    }

    /**
     * C és header fileok megírása
     * Ez alatt találhatóak a benne felhasznált fv-ek, amik azért lettek kirendezve, hogy átláthatóbb legyen az WriteCode()
     */
    public static void WriteCode(){
        SearchThreadChanges();
        int j = SearchFirstJoin();
        /*int LastLineInMain = */FindLastLines();
        int eleje = TopOfCode();
        //sorok és hozzájuk tartozó részek összeállítása
        for(int i = eleje; i < sorok.size(); i++){
            if(j == (i + 1) && j > 0)kod.append("    pthread_mutex_unlock(&mutex);\n");
            SetTheEndOfFunctions(i);
            kod.append(sorok.get(i));   /*<----------sor hozzáadás---------<<<<<*/
            kod.append("\n");
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
        for(int i = 0; i < edges.size(); i++){
            if(edges.get(i).thread != -1){
                szalelek.add(edges.get(i));
            }
        }
        //ha adott élre történik az ugrás
        for (int i = 0; i < szalelek.size() - 1; i++) {
            if(szalelek.get(i).thread != szalelek.get(i + 1).thread){
                szalelek.get(i).threadChanges = true;
            }
        }
    }

    /**
     * A kód elejére írt importok, globális változók, amik előre adottak
     * Azért lett kiszervezve, hogy a WriteCode() átláthatóbb legyen
     */
    public static int TopOfCode(){
        int eleje = 0;
        for(int i = 0; i < edges.size(); i++){
            if(eleje == 0) eleje = edges.get(i).startLine;
            if(edges.get(i).startLine < eleje && edges.get(i).startLine != 0) eleje = edges.get(i).startLine;
        }
        for(int i = 0; i < eleje; i++){
            kod.append(sorok.get(i));   /*<----------sor hozzáadás---------<<<<<*/
            kod.append("\n");
        }
        kod.append(
                /*"#include <assert.h>\n" +
                "#include <stdbool.h>\n" +
                "#include <stdio.h>\n" +
                "#include <pthread.h>\n" +
                "#include \"nondetfvek.h\"\n" +*/
                "\npthread_mutex_t mutex;\n" +
                "pthread_cond_t cond;\n" +
                "int flag;\n");
        //thread ID-kat tárolják
        for(int i = 0; i < idk.size();i++){
            kod.append("int t");
            kod.append(idk.get(i));
            kod.append(" = -1;\n");
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
        for(int i = 0; i < funok.size(); i++){
            if(funok.get(i).name.equals("main")){
                start = funok.get(i).startLine;
                end = funok.get(i).endLine;
            }
        }
        pattern = Pattern.compile(".*pthread_join.*");
        matcher = pattern.matcher(sorok.get(start - 1));
        while(start < end){
            if(matcher.find()){
                sor = start;
                return sor;
            }
            start++;
            matcher = pattern.matcher(sorok.get(start - 1));
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
        for(int i = 0; i < funok.size(); i++){
            if(funok.get(i).name.equals("main")) idx = i;
        }
        for (int i = 0; i < idk.size(); i++) {
            //a for ciklusnál optimálisabbnak tűnik, de nincs még kipróbálva
//            while (k >= 0 && !(edges.get(k).Thread == idk.get(i))){
//                k--;
//            }
            for(int l = 0; l < edges.size(); l++){
                if(edges.get(l).thread == idk.get(i)){
                    if(idk.get(i) == 0){
                        if(edges.get(l).startLine > funok.get(idx).startLine && edges.get(l).startLine < funok.get(idx).endLine)
                            k = l;
                    }
                    else k = l;
                }
            }
            lastLines.put(k, edges.get(k).startLine);
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
        if(lastLines.containsValue(i) || lastLines.containsValue(i + 1)){
            //végig megy a Map összes elemén, hogy kiderítse, hogy ahhoz az utolsó sorhoz milyen él index tartozik
            for (Map.Entry mapElement: lastLines.entrySet()) {
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
            if(edges.get(idxM).thread == 0 && lastLines.containsValue(i + 1)){
                kod.append("    pthread_cond_destroy(&cond);\n" +
                        "    pthread_mutex_destroy(&mutex);\n");
            }
            //...és más, ha egyéb fv utolsó sora
            if(edges.get(idx).thread != 0 && lastLines.containsValue(i)){
                kod.append("    flag = t" + edges.get(idx + 1).thread + ";\n" +
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
        if(sorok.get(i).contains("pthread_create")){
            for(int a = 0; a < edges.size(); a++){
                int idx = sorok.get(i).indexOf("&");
                if(edges.get(a).startLine == (i + 1)){
                    kod.append("    t" + edges.get(a).createdThread + " = " + sorok.get(i).charAt(idx + 1) + ";\n");
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
        for (Map.Entry mapElement: lastLines.entrySet()) {
            if((int)mapElement.getValue() == idx + 1){
                //Ha utolsó él, nem sima váltás következik
                return;
            }
        }
        for (int i = 0; i < szalelek.size(); i++){
            if(szalelek.get(i).startLine == (idx + 1) && szalelek.get(i).threadChanges){
                kod.append(
                        "    flag = t" + szalelek.get(i + 1).thread +";\n" +
                        "    pthread_cond_broadcast(&cond);\n");
                if(szalelek.get(i).thread == 0){
                    kod.append("    while (flag != t0) {\n");
                }
                else{
                    kod.append("    while (flag != maga) {\n");
                }
                kod.append(
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
        matcher = pattern.matcher(sorok.get(i));
        if(matcher.find() && !sorok.get(i).contains("main")){
            kod.append("    pthread_mutex_lock(&mutex);\n" +
                    "    int maga = pthread_self();\n" +
                    "    while (flag != maga) {\n" +
                    "        pthread_cond_wait(&cond, &mutex);\n" +
                    "    }\n");
        }
        else{
            //main eleji beállítások
            pattern = Pattern.compile("int main\\(.*");
            matcher = pattern.matcher(sorok.get(i));
            if(matcher.find()){
                kod.append("    pthread_cond_init(&cond, 0);\n" +
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
        File tFolder = new File(targetFolder);
        try{
            if(!tFolder.exists())
                new File(targetFolder).mkdir();
        }catch (Exception e){

        }
        String[] files = tFolder.list();
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
            FileWriter myWriter = new FileWriter(targetFolder + "/main.c");
            myWriter.write(kod.toString());
            myWriter.close();
        } catch (IOException e) {
            System.out.println("Hiba van a kiiratassal.");
            e.printStackTrace();
        }
        //header file kiiratása
        try {
            FileWriter myWriter = new FileWriter(targetFolder + "/nondetfvek.h");
            myWriter.write("#ifndef NONDETFVEK_H_INCLUDED\n" +
                    "#define NONDETFVEK_H_INCLUDED\n");
            myWriter.write(headerbe.toString());
            myWriter.write("#endif");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("Hiba van a kiiratassal.");
            e.printStackTrace();
        }
    }

    /**
     * fordítás és futtatás
     */
    public static void CompileCprog() throws IOException {
        ClearAndSetFolder();
        //linuxra
//        ProcessBuilder builder = new ProcessBuilder(
//                "sh", "-c", "cd " + targetFolder + " && gcc main.c -o main && ./main");
        ProcessBuilder builder;
        builder = new ProcessBuilder(
        "gcc", "-pthread", targetFolder +"/main.c ", "-o", targetFolder + "/main", "&&", targetFolder + "/main");
//        builder.directory(new File("/futasra/"));
        //windowsra
//        Process compile = new ProcessBuilder(
//                "cmd", "/C", "gcc" + "\"" + targetFolder + "-o", targetFolder + "main.exe", targetFolder + "main.c").start();
//        ProcessBuilder builder = new ProcessBuilder(
//                "cd", targetFolder, "&&", "./main.exe");
        builder.redirectErrorStream(true);
        Process p = null;
        try {
            p = builder.start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);  //itt hal meg
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            try {
                line = r.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) { break; }
            System.out.println(line);
        }
    }

    /**
     * Törli a mappa tartalmát és az elérési útvonalat módosítja
     * Azért lett kiszervezve, hogy a CompileProg() átláthatóbb legyen
     */
    public static void ClearAndSetFolder(){
        File dir = new File(targetFolder);

        String[] files = dir.list();
        if(files.length > 2){
            final File[] fileok = dir.listFiles();
            for (File f: fileok){
                if((!f.getName().equals("main.c")) && (!f.getName().equals("nondetfvek.h")));
                    f.delete();
            }
        }

        String newTargetFolder ="";
        for (int i = 0; i < targetFolder.length(); i++) {
            if(targetFolder.charAt(i) == '\\'){
                newTargetFolder += "\\\\";
            }else{
                newTargetFolder += targetFolder.charAt(i);
            }
        }
    }
}
