/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpserver.javahttpwebserver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

// The tutorial can be found just here on the SSaurel's Blog : 
// https://www.ssaurel.com/blog/create-a-simple-http-web-server-in-java
// Each Client Connection will be managed in a dedicated Thread
public class JavaHTTPServer implements Runnable{ 
    static final File F=new File("");
    static final String PATH=F.getAbsolutePath() + "/../";
    static final File WEB_ROOT = new File(PATH+"/src/main/java/httpserver/risorseWeb");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final String FILE_MOVED = "301.html";
    // port to listen connection
    static final int PORT = 3000;
    // verbose mode
    static final boolean verbose = true;
    // Client Connection via Socket Class
    private Socket clientSocket;
    public JavaHTTPServer(Socket c) {
	clientSocket = c;
    }
    public static void main(String[] args) {
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
            // we listen until user halts server execution
            while (true) {
                JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
                if (verbose) {
                    System.out.println("Connecton opened. (" + new Date() + ")");
                }
                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    @Override
    public void run() {
	// we manage our particular client connection
	BufferedReader inDalClient = null;
        PrintWriter outAlClient = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;
	try {
            // we read characters from the client via input stream on the socket
            inDalClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // we get character output stream to client (for headers)
            outAlClient = new PrintWriter(clientSocket.getOutputStream());
            // get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(clientSocket.getOutputStream());
            // get first line of the request from the client
            String input = inDalClient.readLine();
            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            fileRequested = parse.nextToken().toLowerCase();
            // we support only GET and HEAD methods, we check
            if (!method.equals("GET")  &&  !method.equals("HEAD")) {
                if (verbose) {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }
		// we return the not supported file to the client
		File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
		int fileLength = (int) file.length();
                String contentMimeType = "text/html";
		//read content to return to client
		byte[] fileData = readFileData(file, fileLength);
		// we send HTTP Headers with data to client
		outAlClient.println("HTTP/1.1 501 Not Implemented");
                outAlClient.println("Server: Java HTTP Server from SSaurel : 1.0");
                outAlClient.println("Date: " + new Date());
                outAlClient.println("Content-type: " + contentMimeType);
                outAlClient.println("Content-length: " + fileLength);
		outAlClient.println(); // blank line between headers and content, very important !
		outAlClient.flush(); // flush character output stream buffer
		// file
		dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            } 
            else {
		// GET or HEAD method
		if (fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }
		File file;
                if(fileRequested.equals("/punti-vendita.xml")){
                    file=fromJSONToXML();
                }
                else if(fileRequested.equals("/db/xml")||fileRequested.equals("/db/json")){
                    Elenco el=retriveElenco();
                    if(fileRequested.endsWith("xml")){
                        file=classToXML(el);
                        fileRequested+="elenco.xml";
                    }
                    else{
                        file=classToJSON(el);
                        fileRequested+="elenco.json";
                    }
                }
                else{
                    file = new File(WEB_ROOT, fileRequested);
                }
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);
		if (method.equals("GET")) { // GET method so we return content
                    byte[] fileData = readFileData(file, fileLength);
                    // send HTTP Headers
                    outAlClient.println("HTTP/1.1 200 OK");
                    outAlClient.println("Server: Java HTTP Server from SSaurel : 1.0");
                    outAlClient.println("Date: " + new Date());
                    outAlClient.println("Content-type: " + content);
                    outAlClient.println("Content-length: " + fileLength);
                    outAlClient.println(); // blank line between headers and content, very important !
                    outAlClient.flush();  // flush character output stream buffer
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
		}
                if (verbose) {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
		}
            }
	}
        catch (FileNotFoundException fnfe) {
            try {
                String[] percorso=fileRequested.split("/");
                int numPercorsi=percorso.length;
                String oggetto=percorso[numPercorsi-1];
                if(oggetto.lastIndexOf(".")==-1){
                    directoryWithoutSlash(outAlClient,dataOut,fileRequested+"/");
                }
                else{
                    fileNotFound(outAlClient, dataOut, fileRequested);
                }
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }
        }
        catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Errore nel recupero dei dati dal DB. "+ex.getLocalizedMessage());
        }
        finally {
            try {
                inDalClient.close();
                outAlClient.close();
                dataOut.close();
		clientSocket.close(); // we close socket connection
            } catch (IOException e) {
                System.err.println("Error closing stream : " + e.getMessage());
            } 
            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }
    }
    
    private File fromJSONToXML() throws FileNotFoundException, IOException{
        File fJSON=new File(PATH+"/src/main/java/httpserver/risorseGenerali/puntiVendita.json");
        String fileString=readFile(fJSON);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        ListaRisultati pv = objectMapper.readValue(fileString, ListaRisultati.class);
        XmlMapper xmlMapper = new XmlMapper();
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        xmlMapper.writeValue(byteArray, pv); 
        String arrayXML=byteArray.toString();
        System.out.println(arrayXML);
        File fXML=new File(WEB_ROOT+"/punti-vendita.xml");
        if(fXML.exists()){
            fXML.delete();
        }
        fXML.createNewFile();
        FileWriter fw=new FileWriter(fXML);
        fw.write(arrayXML);
        fw.close();
        return fXML;
    }
    /*
    
    */
    private String readFile(File f) throws FileNotFoundException, IOException{
        String ret="";
        FileReader fr=new FileReader(f);
        BufferedReader br=new BufferedReader(fr);
        for(;;){
            String appo=br.readLine();
            if(appo==null){
                break;
            }
            ret+=appo;
        }
        return ret;
    }
    
    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        }
        finally {
            if (fileIn != null){
                fileIn.close();
            }
        }
        return fileData;
    }
    
    // return supported MIME Types
    private String getContentType(String fileRequested) {
        String ext=fileRequested.substring(fileRequested.lastIndexOf(".")+1);
        switch(ext){
            case "htm":
            case "html":
                return "text/html";
            case "jpg":
                return "image/jpg";
            case "xml":
                return "text/xml";
            case "json":
                return "application/json";
            default:
                return "text/plain";
        }
    }
	
    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);
        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server from SSaurel : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
	out.println(); // blank line between headers and content, very important !
	out.flush(); // flush character output stream buffer
	dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }
        
    private void directoryWithoutSlash(PrintWriter out, OutputStream dataOut,String directoryRequested) throws IOException{
        File file = new File(WEB_ROOT, FILE_MOVED);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);
        out.println("HTTP/1.1 301 Moved Permanently");
        out.println("Server: Java HTTP Server from SSaurel : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        //probabile posizione della cartella
        out.println("Location: "+directoryRequested);
        out.println();
        out.flush();
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        if (verbose) {
            System.out.println("Directory " + directoryRequested + " hint sended");
        }
    }
    
    private Elenco retriveElenco() throws ClassNotFoundException, SQLException{
        ArrayList<Nominativo> nomi=new ArrayList<>();
        ResultSet res=new InterrogazioneDataBase().eseguiQuery("select nome, cognome from persone;");
        while (res.next()) {
            nomi.add(new Nominativo(res.getString(1), res.getString(2)));
        }
        return new Elenco(nomi);
    }
    /*
    
    */
    private File classToXML(Elenco el) throws IOException{
        XmlMapper xmlMapper = new XmlMapper();
        File fXML=new File(WEB_ROOT+"/elenco.xml");
        if(!fXML.exists()){
            fXML.createNewFile();
        }
        xmlMapper.writeValue(fXML, el);
        return fXML;
    }
    /*
    
    */
    private File classToJSON(Elenco el) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        File fJSON=new File(WEB_ROOT+"/elenco.json");
        if(!fJSON.exists()){
            fJSON.createNewFile();
        }
        objectMapper.writeValue(fJSON, el);
        return fJSON;
    }
}