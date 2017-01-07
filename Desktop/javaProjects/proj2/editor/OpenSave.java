package editor;
import javafx.scene.Group;
import javafx.scene.text.Text;

import java.io.*;

import java.util.ArrayList;
/**
 * Created by rohansuresh on 3/6/16.
 */
public class OpenSave {
    public String inputFileName;
    public ArrayList<Holder> arr;
    public File inputFile;
    public FileReader fRead;
    public BufferedReader bRead;
    public Group root;

    public OpenSave(ArrayList<Holder> a, String in, Group rt) {
        arr = a;
        inputFileName = in;
        root = rt;
    }

    public void open() {
        try {
            inputFile = new File(inputFileName);
            fRead = new FileReader(inputFile);
            bRead = new BufferedReader(fRead);
            int ln = 0;
            int tracker;
            while ((tracker = bRead.read()) != -1) {
                char readChar = (char)tracker;
                String str = Character.toString(readChar);
                if(str.equals("\r") || str.equals("\n")){
                    ln++;
                    arr.add(new Holder());
                } else {
                    Text textChar = new Text(str);
                    arr.get(ln).add((textChar));
                    root.getChildren().add(textChar);
                }
            }
            bRead.close();
        }catch (FileNotFoundException notFound) {

        } catch (IOException ioe) {
            System.out.println("Error occurred when reading file, exception: " + ioe);
        }
    }

    public void save() {
        try {
            if(!inputFile.exists()) {
                System.out.println("Unable to save; file with name " + inputFileName + " does not exist");
                inputFile.createNewFile();
            }
            FileWriter write = new FileWriter(inputFileName);
            for (int i = 0; i < arr.size(); i++) {
                Node nd = arr.get(i).sentinel.next;
                while(nd != arr.get(i).sentinel) {
                    write.write(nd.item.getText().charAt(0));
                    nd = nd.next;
                }
            }
            System.out.println("Saved file: " + inputFileName);
            write.close();
        } catch(FileNotFoundException notFound) {
        } catch(IOException ioe) {
            System.out.println("Error occurred when saving, exception " + ioe);
        }
    }

}