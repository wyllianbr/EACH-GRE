import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class TesteModelo {

	/** Geracao dos arquivos de .arff de treino teste.
	 *  Geracao dos classificadores para cada uma classe. 
	 * 
	 * @param args
	 * @throws Exception
	 */
	
	public static void main(String[] args) throws Exception {
		String nome_contexto = "Stars";
		
		String pasta = "projeto/output/" + nome_contexto + "/";
		String arquivo = nome_contexto + ".arff";
		
		// gera arquivos .arff de treino e teste para cada atributo
		buildClasses(pasta, arquivo); 
		
		// gera os arquivos .model
		processFolder(pasta + "data/");
	}
	
	/************************************************************/
	/************************************************************/
	
	/**Converte arrayList de Integer
	 * para um vetor de inteiros.
	 * 
	 * @param integers Lista de inteiros
	 * @return int[] Inteiros
	 */
	public static int[] convertIntegers(ArrayList<Integer> integers){
	    int[] ret = new int[integers.size()];
	    for (int i=0; i < ret.length; i++) ret[i] = integers.get(i).intValue();
	    return ret;
	}
	
	/** Este metodo se encarrega de criar um (ou vários) arquivo(s) .arff de entrada
	 * para cada atributo do .arff original.

	 * No caso do .arff de treinamento, vários novos arquivos são gerados,
	 * para cada instancia de treinamento, é feita uma combinação com seu respectivo
	 * atributo booleano (use_).
	 * 
	 * @param folder String com endereco de uma pasta
	 * @param file String com o nome do arquivo .arff, para dividi-lo em classes
	 */
	public static void buildClasses (String folder, String file){
		try {
			String filename = Paths.get(file).getFileName().toString();
			filename = filename.substring(0, filename.lastIndexOf('.'));

			DataSource source = new DataSource(folder + file);
			Instances instances_original = source.getDataSet();
			
			/** Constroi quais indices devem ser removidos */
			// sempre teremos que remover "context_id" e "seq"
			ArrayList<Integer> indicesRemover = new ArrayList<Integer>();
			indicesRemover.add(instances_original.attribute("context_id").index());
			indicesRemover.add(instances_original.attribute("seq").index());
			
			for(int i = 0; i<instances_original.numAttributes(); i++){
				if(instances_original.attribute(i).name().startsWith("use_")){
					indicesRemover.add(i);
				}
			}
			
			// revemos todos os use_, exceto um deles não é
			// removido da lista
			for(int i = 2; i<indicesRemover.size(); i++){
				ArrayList<Integer> tmpArr = new ArrayList<Integer>(indicesRemover);
				for(int j = 0; j<tmpArr.size(); j++){
					if(indicesRemover.get(i) == tmpArr.get(j)){
						tmpArr.remove(j);
						int indices[] = convertIntegers(tmpArr);

						Remove remove = new Remove();
						remove.setAttributeIndicesArray(indices);
						remove.setInputFormat(instances_original);
						
						Instances tmp = Filter.useFilter(instances_original, remove);
						Instances train = new Instances(tmp, 0);
	
						for (int k = instances_original.size() - 1; k >= 0; k--) train.add(tmp.remove(k));

						/** Salva arquivos gerados */
						int last = train.numAttributes() - 1;
						
						String train_name = "train_" + filename + "_" + train.attribute(last).name();
						train.setRelationName(train_name);
						
						Utils.saveArff(train, folder + "data", train_name);
						
						break;
					}
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** Este metodo cria um classificador (.model) para cada atributo binario.
	 * Para isso, deve ser passado um conjunto de instancias para treinamento.
	 * Os classificadores sao salvos na pasta especificada.
	 * 
	 * @param trainingSet Recebe um conjunto de instancias para treinamento.
	 * @param folder Especificação da pasta onde se encontram os arquivos .arff
	 * 				 gerados pelo metodo "BuildClasses".
	 */
	public static void buildModel (Instances trainingSet, String folder){
		try {
			trainingSet.setClassIndex(trainingSet.numAttributes() - 1);
			
			J48 model = new J48();
			model.buildClassifier(trainingSet);
			
			Evaluation eval = new Evaluation(trainingSet);
			eval.evaluateModel(model, trainingSet);
			
			System.out.println(eval.toSummaryString("Results\n======", false));
			
			String filename = trainingSet.relationName();
			filename = filename.substring(filename.indexOf('_')+1, filename.length());
			
			SerializationHelper.write(folder + filename + ".model", model);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/** Este metodo faz a predicao utilizando os classificadores do tipo
	 *  arvore de decisao (J48) para uma dada instancia. 
	 * 
	 * @param model		Informar qual .model deve executar.
	 * @param dataset	Informar o conjunto de dados a realizar os testes (Uma instancia).
	 */
	public static void testTrainedModel (String model, String dataset){
		try {
			DataSource source = new DataSource(dataset);
			Instances instances = source.getDataSet();
			
			J48 cls = (J48) SerializationHelper.read(new FileInputStream(model));
			instances.setClassIndex(instances.numAttributes()-1);
			
			Evaluation eval = new Evaluation(instances);
			eval.evaluateModel(cls, instances);
			
			System.out.println(eval.toSummaryString("Results\n======", false));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Nessa etapa, todos os arquivos de treinamento de uma pasta são abertos.
	 * Com cada um desses arquivos será gerado um classificador espefico para
	 * cada classe. 
	 * 
	 * @param folder Pasta onde se encontram os arquivos .arff de treinamento
	 * 				 para geracao dos classificadores (.model).
	 */
	public static void processFolder (String folder){
		try {
			File pasta = new File (folder);
			File[] files_list = pasta.listFiles();
			for (int i = 0; i<files_list.length; i++){
				if (files_list[i].isFile()){
					String arq = files_list[i].getName();
					if (arq.startsWith("train_")){
						DataSource source = new DataSource(files_list[i].toString());
						Instances instances = source.getDataSet();
						buildModel(instances, folder);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Erro no processamento da pasta para geracao dos modelos");
		}
	}
	
	/**Metodo para classificar uma instancia, utilizando um classificador J48.
	 * 
	 * @param classificador Recebe um classificador (.model).
	 * @param testSet Recebe um conjunto de instancias que se deseja classificar (Instances).
	 * @param indice Deve-se especificar qual instancia (o indice) do conjunto de instancias
	 * 				 que deseja avaliar a classificação.
	 * @return Booleano, onde true indica que o atributo deve ser usado. false, caso contrario.
	 */
	public static boolean classify (J48 classificador, Instances testSet, int indice){
		try {
			double a;
		
			a = classificador.classifyInstance(testSet.instance(indice));
			if(testSet.classAttribute().value((int) a).equals("1"))
				return true;
			else
				return false;
		} catch (Exception e) {
			System.err.println("Erro ao classificar instancia");
			e.printStackTrace();
		}

		return false;
	}
	
	
	/************************************************************/
	/***** Metodos abaixo: mantidos, mas nunca utilizados *******/
	/***** pois pertencem a uma versao mais antiga.        *******/
	/************************************************************/
	
	public static void expectedExpression(String folder){
		try {
			File pasta = new File (folder);
			File[] files_list = pasta.listFiles();
			
			String resposta[] = null;
			
			for (int i = 0; i<files_list.length; i++){
				if (files_list[i].isFile()){
					String arq = files_list[i].getName();
					if(arq.startsWith("test_")){
						DataSource source = new DataSource(folder + arq);
						Instances instances = source.getDataSet();
						instances.setClassIndex(instances.numAttributes()-1);
						
						if(resposta == null) {
							resposta = new String[instances.size()];
							for(int k = 0; k<resposta.length; k++) resposta[k] = "";
						}
						
						for(int j = 0; j<instances.size(); j++){
							String valor = String.valueOf((int) instances.get(j).classValue());
							if(valor.equals("1")) {
								resposta[j] += resposta[j].length() > 1 ? ',' : '{';
								resposta[j] += arq.substring(arq.lastIndexOf("_")+1, arq.lastIndexOf("."));
							}
						}
					}
				}
			}
			
			for(int i = 0; i<resposta.length; i++) {
				resposta[i] += "}";
				System.out.println(resposta[i]);
			}
			
		} catch (Exception e){
			System.err.println("Erro na geração das expressões esperadas.");
		}
	}
		
	public static void expression(String folder){
		try {
			File pasta = new File (folder);
			File[] files_list = pasta.listFiles();
			
			ArrayList<J48> classificadores = new ArrayList<J48>();
			ArrayList<String> nome_classificador = new ArrayList<String>();
			
			String test = "";
			
			/** Pega todos os classificadores e um dos arquivos de teste da pasta */
			for (int i = 0; i<files_list.length; i++){
				if (files_list[i].isFile()){
					String arq = files_list[i].getName();
					if (arq.endsWith(".model")){
						J48 cls = (J48) SerializationHelper.read(new FileInputStream(folder+arq));
						classificadores.add(cls);
						nome_classificador.add(arq);
					} else if (arq.startsWith("test_")){
						test = arq;
					}
				}
			}
			
			// Carrega o .arff do arquivo test
			DataSource source = new DataSource(folder + test);
			Instances instances = source.getDataSet();
			instances.setClassIndex(instances.numAttributes()-1);
			
			for (int i = 0; i<instances.size(); i++){
				String expressao = "";
				
				for (int j = 0; j<classificadores.size(); j++){
					if(classify(classificadores.get(j), instances, i)){
						String tmp = nome_classificador.get(j);
						tmp = tmp.substring(tmp.lastIndexOf("_")+1, tmp.lastIndexOf("."));
						expressao += expressao.length() > 1 ? ',' : '{';
						expressao += tmp;
					}
				}
				
				expressao += "}";
				System.out.println(expressao);
			}
			
		} catch (Exception e) {
			System.err.println("Erro no processamento da pasta para geracao dos modelos");
		}
	}

}
