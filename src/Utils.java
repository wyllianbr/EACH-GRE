import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class Utils {
	/**Metodo para salvar um conjunto de instancias em um arquivo .arff
	 * 
	 * @param file Conjunto de instancias que deseja gravar no arquivo .arff
	 * @param foldername Endereço (pasta) onde deseja salvar o arquivo .arff
	 * @param name Nome do arquivo que deseja salvar.
	 */
	public static void saveArff (Instances file, String foldername, String name){

		try {
			ArffSaver saver = new ArffSaver();
			saver.setInstances(file);
			saver.setFile(new File("./"+ foldername +"/" + name + ".arff"));
			saver.writeBatch();
		} catch (IOException e) {
			System.err.println("Erro na gravacao do .arff");
			e.printStackTrace();
		}
		
	}
	
	/** Metodo para gerar um arquivo de texto, recebendo
	 * uma String como parametro.
	 * 
	 * @param text Conteudo que deseja salvar em arquivo.
	 * @param folder Endereço da pasta onde deseja salvar o arquivo de texto. 
	 * @param name Nome do arquivo que deseja salvar.
	 */
    public static void write (String text, String folder, String name) {
        try {
        	new File(folder).mkdirs();
        	File dir = new File(folder);
            File logFile = new File(dir, name);
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(text);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
