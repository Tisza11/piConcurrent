package hu.bme.mit.piConcurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadXML {
    /**
     * beolvas egy graphml filet és el is tárolja
     * Ez alatt találhatóak a benne felhasznált fv-ek, amik azért lettek kirendezve, hogy átláthatóbb legyen az ReadXML()
     * @param gmlFile --> a witness file elérési útját meglapja
     * @param keres --> a C file neve, amit megkap és megkeres
     * @return --> összetartozik-e a C és a witness file
     */
    public static boolean Read_XML(String gmlFile, String keres) throws ParserConfigurationException, IOException, SAXException {
        boolean violation = true;
//        int idx = 0;
//        for (int i = 0; i < keres.length(); i++) {
//            if(keres.charAt(i) == '\\') idx = i;
//        }
        if(!CheckNameInGraphML(gmlFile, keres))return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(Main.sourceFolder + gmlFile);
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
        File myFile = new File(Main.sourceFolder + gmlFile);
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
            if(!Main.idk.contains(Integer.parseInt(data.getTextContent()))) Main.idk.add(Integer.parseInt(data.getTextContent()));
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
}
