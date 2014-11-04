import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

@SuppressWarnings("deprecation")
public class Main {
	static Map<Map <String, String>, Integer> frequencia_atributo;
	
	static Map<String, Integer> freq_x;
	static Map<String, ArrayList<String>> context_id;

	static List<String> binary_att;
	
	
	/** Para funcionamento desse algoritmo, precisamos manter as pastas
	 * no seguinte formato:
	 * 	- "./projeto/context/"
	 * 		=> Dentro desta pasta, devem contem todos os corpus de contexto, com o sufixo -context.xml
	 * 		=> Ex: Stars-context.xml, GRE3D-context.xml, ...
	 * 
	 * 	- "./projeto/trials/"
	 * 		=> Nesta pasta, devem conter subpastas para seu respectivo contexto.
	 * 		=> Ex: Se estivermos tratando do contexto "Stars-context.xml", precisamos
	 * 				de uma pasta Stars no diretorio "./projeto/trials/Stars/",
	 * 				contendo todos os arquivos .xml trial.
	 * 
	 * Este algoritmo pode ser adaptado para tratar apenas de um contexto. Mas no momento,
	 * ele esta processando todos os arquivos context de uma vez.
	 * 
	 * Os arquivos .arff gerados sao gravados dentro da pasta "output" de seu respectivo contexto.
	 * Para cada contexto, sao gerados 2 arquivos:
	 * 	- Um contem cerca de 80% dos casos para treinamento, gravado no arquivo nome_do_contexto.arff.
	 * 	- Outro contem cerca de 20% dos casos para teste. Este arquivo sera processado novamente,
	 * 		a ideia principal deste arquivo, eh informar a qual trial pertence o trial escolhido para
	 * 		teste que sera realizado posteriormente.
	 * 
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void main (String args[]) throws Exception{
		String pasta = "projeto/contexts/";
		
		File dir = new File(pasta);
		File[] folder_context = dir.listFiles();

		for (int i = 0; i< folder_context.length; i++) if (folder_context[i].isFile()){
			String arquivo = folder_context[i].getName().replace("-context.xml", "");

			Instances instancias = new Instances(arquivo, new ArrayList<Attribute>(), 0);
			binary_att = new ArrayList<String>();
			binary_att.add("0"); binary_att.add("1");

			// String attribute - Context ID
			instancias.insertAttributeAt(new Attribute("context_id", (FastVector) null), instancias.numAttributes());

			// Processa poder discriminatorio
			build_frequences (instancias, dir.getPath()+ "/", arquivo + "-context.xml");
			int nro_discr = instancias.numAttributes();

			// String attribute - SEQ
			instancias.insertAttributeAt(new Attribute("seq", (FastVector) null), instancias.numAttributes());
			
			// Gera atributos use_
			Enumeration<Attribute> e = instancias.enumerateAttributes();
			e.nextElement(); // pula context_id
			
			for (int j = 1; j<nro_discr; j++){
				instancias.insertAttributeAt(
						new Attribute("use_" + e.nextElement().name().replace("discr_", ""), binary_att),
						instancias.numAttributes());
			}

			// Processamento dos arquivos trial
			String trial_path = "projeto/trials/" + arquivo;
			File dir2 = new File (trial_path);
			File[] files_trial = dir2.listFiles();
			
			ArrayList<String> trial_name = new ArrayList<String>();
			for (int j = 0; j<files_trial.length; j++)
				trial_name.add(files_trial[j].getName());
			
			// Embaralha arquivos trial, e seleciona 20% para teste,
			// enquanto os outros 80% sao utilizados para criar o .arff de treinamento.
			Collections.shuffle(trial_name);
			int qtde = (int) (trial_name.size()*0.2f);

			HashSet<String> trial_test = new HashSet<String>();
			for (int j = 0; j<qtde; j++) trial_test.add(trial_name.get(j));
			
			instancias.insertAttributeAt(new Attribute("trial_name", (FastVector) null), instancias.numAttributes());
			
			for (int j = 0; j<files_trial.length; j++)
				trials (instancias, dir2.getPath() + "/", files_trial[j].getName(), nro_discr);
			
			Instances testSet = new Instances(instancias, 0);
			
			for (int j = instancias.size()-1; j>=0; j--){
				String tmp = instancias.get(j).stringValue(instancias.attribute("trial_name"));
				if(trial_test.contains(tmp)){
					testSet.add(instancias.get(j));
					instancias.remove(j);
				}
			}
			
			Remove remove = new Remove();
			int idx[] = new int[1];
			idx[0] = instancias.attribute("trial_name").index();
			
			// Remove "trial_name" das intancias de treinamento
			remove.setAttributeIndicesArray(idx);
			remove.setInputFormat(instancias);
			
			instancias = Filter.useFilter(instancias, remove);
			instancias.setRelationName(arquivo);
			
			System.out.println("OK " + arquivo);
			
			// Os arquivos de teste so precisam dos atributos:
			// - trial_name: utilizado para gerar a expressao esperada.
			// - context_id: utilizada tanto para a expressao esperada
			//				 quanto para a expressao gerada pelo classificador.
			ArrayList<Integer> idx_tmp = new ArrayList<Integer>();
			for (int j = 0; j<testSet.numAttributes(); j++) {
				String att_name = testSet.attribute(j).name();
				if(att_name.startsWith("use_") || att_name.startsWith("discr_") || att_name.startsWith("seq"))
					idx_tmp.add(j);
			}
			
			idx = new int[idx_tmp.size()];
			for (int j = 0; j < idx_tmp.size(); j++) idx[j] = idx_tmp.get(j);
			
			remove.setAttributeIndicesArray(idx);
			remove.setInputFormat(testSet);
			
			testSet = Filter.useFilter(testSet, remove);
			testSet.setRelationName("test_" + arquivo);

			Utils.write(instancias.toString(), "projeto/output/"+arquivo, arquivo+".arff");
			Utils.write(testSet.toString(), "projeto/output/"+arquivo, "test_" + arquivo+".arff");
			
		}
	}
	
	/************************************************************/
	/************************************************************/
	
	/** Constroi frequencias de todos pares (atributo-valor) presente em um arquivo de contexto.
	 *  Essa parte eh essencial para contabilizar posteriormente o poder discriminario de um par. 
	 * 
	 * @param instancias 	Instancias utilizadas para incluir todos os atributos
	 * 						presentes em um arquivo de contexto.
	 * @param dir			Diretorio do contexto que esta sendo processado.
	 * @param arquivo		Qual arquivo de contexto esta sendo processado.
	 */
	public static void build_frequences (Instances instancias, String dir, String arquivo){
		try {
			File fxml = new File (dir + arquivo);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fxml);
			
			doc.getDocumentElement().normalize();
			
			// Pega todos os CONTEXT
			NodeList nlist = doc.getElementsByTagName("CONTEXT");
			context_id = new HashMap<String, ArrayList<String>>();
			
			for (int i = 0; i<nlist.getLength(); i++){
				frequencia_atributo = new HashMap<Map <String, String>, Integer>();
				freq_x = new HashMap<String, Integer>();
				
				// Acesso a um CONTEXT
				Node context_node = nlist.item(i);
				if (context_node.getNodeType() == Node.ELEMENT_NODE){
					Element context_element = (Element) context_node;
					
					// Pega ATTRIBUTE-SET de um CONTEXT
					NodeList attributes_list = context_element.getChildNodes();
					for (int j = 0; j<attributes_list.getLength(); j++){
						Node attribute_set_node = attributes_list.item(j);
						
						if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
							Element att_set_element = (Element) attribute_set_node;
							
							// Acesso ao ATTRIBUTE de um ATTRIBUTE-SET
							NodeList attribute = att_set_element.getChildNodes();
							for (int k = 0; k<attribute.getLength(); k++){
								Node attribute_node = attribute.item(k);
								if (attribute_node.getNodeType() == Node.ELEMENT_NODE){
									Element attribute_element = (Element) attribute_node;
									
									/** Frequencia do par (chave, valor) */
									String chave = attribute_element.getAttribute("NAME");
									String valor = attribute_element.getAttribute("VALUE");

									Map<String, String> tmp = new HashMap<String, String>();
									tmp.put(chave, valor);

									int count = frequencia_atributo.containsKey(tmp) ? frequencia_atributo.get(tmp) : 0; 
									frequencia_atributo.put(tmp, count + 1);
									
									/** Frequencia da chave */
									count = freq_x.containsKey(chave) ? freq_x.get(chave) : 0;
									freq_x.put(chave, count + 1);
								}
							}
						}
					}
					poder(instancias, context_element);
				}
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**	Este metodo faz a montagem dos atributos booleanos. Para cara trial, este metodo encontra
	 * qual contexto esta sendo descrito por humano, e insere 1 se aquele atributo foi utilizado,
	 * 0 caso contrario.
	 * 
	 * @param instancias	Informar o Objeto de conjuntos de intancias, este arquivo sera
	 * 						gravado posteriormente em um arquivo .arff.
	 * @param dir			Diretorio onde estao todos os arquivos trial de contexto.
	 * @param arquivo		Qual arquivo trial esta sendo processado.
	 * @param nro_disc		Informar a qual indice da instancia pertence o atributo booleano.
	 */
	public static void trials (Instances instancias, String dir, String arquivo, int nro_disc){
		try {
			File fxml = new File (dir + arquivo);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fxml);
			
			doc.getDocumentElement().normalize();
			
			// Pega todos os CONTEXT de um trial
			NodeList nlist = doc.getElementsByTagName("CONTEXT");
			
			for (int i = 0; i<nlist.getLength(); i++){
				ArrayList<String> resposta = new ArrayList<String>();
				for (int j = 0; j<nro_disc; j++) resposta.add("0");
				
				// Acesso a um CONTEXT
				Node context_node = nlist.item(i);
				if (context_node.getNodeType() == Node.ELEMENT_NODE){
					Element context_element = (Element) context_node;
					
					String id = context_element.getAttribute("ID");
					String seq = context_element.getAttribute("SEQ");
					
					resposta.set(0, seq);
					
					// Pega ATTRIBUTE-SET de um CONTEXT
					NodeList attributes_list = context_element.getChildNodes();
					for (int j = 0; j<attributes_list.getLength(); j++){
						Node attribute_set_node = attributes_list.item(j);
						
						if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
							Element att_set_element = (Element) attribute_set_node;
							
							// Acesso ao ATTRIBUTE de um ATTRIBUTE-SET
							NodeList attribute = att_set_element.getChildNodes();
							for (int k = 0; k<attribute.getLength(); k++){
								Node attribute_node = attribute.item(k);
								if (attribute_node.getNodeType() == Node.ELEMENT_NODE){
									Element attribute_element = (Element) attribute_node;

									String chave = attribute_element.getAttribute("NAME");
									
									if(instancias.attribute("discr_" + chave) != null){
										int index = instancias.attribute("discr_" + chave).index();
										resposta.set(index, "1");
									}
								}
							}
						}
					}

					Instance inst = new DenseInstance(instancias.numAttributes());
					inst.setDataset(instancias);
					
					ArrayList<String> descr = context_id.get(id);
					inst.setValue(0, descr.get(0));

					for (int j = 1; j<descr.size(); j++) inst.setValue(j, Double.valueOf(descr.get(j)));

					for (int j = 0; j<resposta.size(); j++) {
						if(instancias.attribute(j).type() == Attribute.NOMINAL) inst.setValue(nro_disc + j, resposta.get(j));
						else if(instancias.attribute(j).type() == Attribute.NUMERIC) inst.setValue(nro_disc + j, Double.valueOf(resposta.get(j)));
						else if(instancias.attribute(j).type() == Attribute.STRING) inst.setValue(nro_disc + j, resposta.get(j));
					}

					inst.setValue(inst.numAttributes()-1, arquivo);
					instancias.add(inst);

				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}

	/**	Este metodo calcula o poder discriminatorio de um dado par (atributo-valor).
	 * 
	 * @param s			Uma lista de string para gravar a resposta (forma uma tupla).
	 * @param chave		Atributo do par (atributo-valor).
	 * @param valor		Valor do par (atributo-valor).
	 * @param pos		Indice do objeto instancia que esta sendo inserido.
	 */
	private static void calcula_poder (ArrayList<String> s, String chave, String valor, int pos){
		Map <String, String> objeto = new HashMap<String, String>();
		objeto.put(chave, valor);
		
		int total = freq_x.get(chave) - frequencia_atributo.get(objeto);
		s.set(pos, String.valueOf(total)); 
	}
	
	/**	Este metodo eh utilizado para contruir todas as tupla contendo todas as informacoes
	 * 	do poder discriminatorio e atributos booleanos.
	 * 
	 * @param instancias	Informar o Objeto de conjuntos de intancias, este arquivo sera
	 * 						gravado posteriormente em um arquivo .arff.
	 * @param context_node	Informar qual context_node do arquivo xml esta sendo processado.
	 */
	private static void poder(Instances instancias, Node context_node) {
		ArrayList<String> resposta = new ArrayList<String>();
		for(int i = 0; i<instancias.numAttributes(); i++) resposta.add("0");
		
		if (context_node.getNodeType() == Node.ELEMENT_NODE){
			Element context_element = (Element) context_node;
			String main_target = context_element.getAttribute("MAIN-TARGET");
			resposta.set(0, context_element.getAttribute("ID"));
			
			// Pega ATTRIBUTE-SET de um CONTEXT
			NodeList attributes_list = context_element.getChildNodes();
			for (int j = 0; j<attributes_list.getLength(); j++){
				Node attribute_set_node = attributes_list.item(j);
				
				if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
					Element att_set_element = (Element) attribute_set_node;
					
					if (att_set_element.getAttribute("ID").equals(main_target)){
						// Acesso ao ATTRIBUTE de um ATTRIBUTE-SET apenas se for um target
						NodeList attribute = att_set_element.getChildNodes();
						for (int k = 0; k<attribute.getLength(); k++){
							Node attribute_node = attribute.item(k);
							if (attribute_node.getNodeType() == Node.ELEMENT_NODE){
								Element attribute_element = (Element) attribute_node;
								
								String chave = attribute_element.getAttribute("NAME");
								String valor = attribute_element.getAttribute("VALUE");
	
								int index = 0;
								
								if(instancias.attribute("discr_" + chave) != null){
									index = instancias.attribute("discr_" + chave).index();
								} else {
									index = instancias.numAttributes();
									instancias.insertAttributeAt(new Attribute("discr_" + chave), instancias.numAttributes());
									resposta.add("0");
								}
								
								calcula_poder(resposta, chave, valor, index);
							}
						}
						
						break;
					}
				}
			}
		}

		context_id.put(resposta.get(0), resposta);
	}
	
}
