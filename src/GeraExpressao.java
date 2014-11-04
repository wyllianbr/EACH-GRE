import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;


public class GeraExpressao {
	static Map<Map <String, String>, Integer> frequencia_atributo;
	
	static Map<String, Integer> freq_x;
	static Map<String, ArrayList<String>> context_id;
	
	public static void main (String args[]) throws Exception{
		// Informe qual contexto deseja gerar expressoes.
		String contexto = "Stars";
		
		// Necessario para gerar expressoes utilizando classificador
		DataSource source = new DataSource(String.format("projeto/output/%s/%s.arff", contexto, contexto));
		String arquivo_contexto = String.format("projeto/contexts/%s-context.xml", contexto);
		String pasta_model = String.format("projeto/output/%s/data/", contexto);
		
		// Abre o arquivo de teste para gerar expressoes esperadas
		DataSource testSource = new DataSource(String.format("projeto/output/%s/test_%s.arff", contexto, contexto));
		Instances testSet = new Instances(testSource.getDataSet());
		String pasta_trial = String.format("projeto/trials/%s/", contexto);
		
		ArrayList<ArrayList<String>> listaEsperada = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> listaClassificador = new ArrayList<ArrayList<String>>();
		
		for (int i = 0; i<testSet.size(); i++){
			String context_id = testSet.get(i).stringValue(0);
			String trial_name = testSet.get(i).stringValue(1);
			
			Instances nova_instancia = new Instances(source.getDataSet(), 0);
			listaClassificador.add(MontaExpressaoClassificador(context_id, nova_instancia, arquivo_contexto, pasta_model));
			listaEsperada.add(MontaExpressaoEsperada(pasta_trial, trial_name, context_id));
		}
		
		Utils.write(listToString(listaClassificador), String.format("projeto/output/%s/", contexto), "expressaoClassificador.txt");
		Utils.write(listToString(listaEsperada), String.format("projeto/output/%s/", contexto), "expressaoEsperada.txt");
	}

	/************************************************************/
	/************************************************************/
	
	public static String listToString (ArrayList<ArrayList<String>> lista){
		String resposta = "";
		for(int i = 0; i<lista.size(); i++) resposta += lista.get(i).toString() + '\n';
		return resposta;
	}
	
	/** Metodo apenas para exemplo de geracao de expressao de referencia:
	 * 		- Informamos um arquivo de contexto
	 * 		- Informamos a pasta onde estao os classificadores (.model)
	 * 		- Informamos qual contexto queremos descrever.
	 * 
	 * @throws Exception	Ocorre se nao for possivel abrir o arquivo .arff ou .xml
	 */
	public static void exemploExpressaoClassificador() throws Exception{
		DataSource source = new DataSource("projeto/output/Stars/Stars.arff");
		Instances nova_instancia = new Instances(source.getDataSet(), 0);
		
		String arquivo_contexto = "projeto/contexts/Stars-context.xml";
		String pasta_model = "projeto/output/Stars/data/";
		String context_id = "abs3";
		
		System.out.println(MontaExpressaoClassificador(context_id, nova_instancia, arquivo_contexto, pasta_model));
	}
	
	private static ArrayList<String> MontaExpressaoEsperada(String pasta_trial, String trial_name, String contexto_alvo) throws Exception {
		String arquivo = pasta_trial + trial_name;
		
		try {
			ArrayList<String> resposta = new ArrayList<String>();
			
			File fxml = new File (arquivo);
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
					
					if(context_element.getAttribute("ID").equals(contexto_alvo)){
						
						relacional = new HashSet<String>();
						
						// Pega ATTRIBUTE-SET de um CONTEXT
						NodeList attributes_list = context_element.getChildNodes();
						for (int j = 0; j<attributes_list.getLength(); j++){
							Node attribute_set_node = attributes_list.item(j);
							
							if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
								Element att_set_element = (Element) attribute_set_node;
								relacional.add(att_set_element.getAttribute("ID"));
								
								// Acesso ao ATTRIBUTE de um ATTRIBUTE-SET
								NodeList attribute = att_set_element.getChildNodes();
								for (int k = 0; k<attribute.getLength(); k++){
									Node attribute_node = attribute.item(k);
									if (attribute_node.getNodeType() == Node.ELEMENT_NODE){
										Element attribute_element = (Element) attribute_node;
										
										/** Frequencia do par (chave, valor) */
										String chave = attribute_element.getAttribute("NAME");
										String valor = attribute_element.getAttribute("VALUE");
	
										resposta.add(chave + "=" + valor);
									}
								}
								
							}
						}
					}
					
				}
			}
			
			return resposta;
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return null;
	}

	
	public static HashSet<String> relacional;
	public static HashSet<String> used_relational;
	
	/**Procura o valor de uma chave em um atribute-set, dentro de um context-id especifico.
	 * Para isso, é necessario informar um tipo Node referente ao contexto,
	 * e informar (por uma String), o "ATTRIBUTE-SET ID" e o "ATTRIBUTE NAME"
	 * 
	 * @param context_node Estrtutura Node de um "CONTEXT ID".
	 * @param ID String informando o ID do "ATTRIBUTE-SET".
	 * @param name String com o "ATTRIBUTE NAME", ou seja, a chave.
	 * @return String com o valor do "ATTRIBUTE NAME" desejado.
	 */
	static String getValor(Node context_node, String ID, String name){
		if(name.startsWith("discr_")) name = name.replace("discr_", "");
		
		if (context_node.getNodeType() == Node.ELEMENT_NODE){
			Element context_element = (Element) context_node;
			
			// Pega ATTRIBUTE-SET de um CONTEXT
			NodeList attributes_list = context_element.getChildNodes();
			for (int j = 0; j<attributes_list.getLength(); j++){
				Node attribute_set_node = attributes_list.item(j);
				
				if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
					Element att_set_element = (Element) attribute_set_node;
					
					if (att_set_element.getAttribute("ID").equals(ID)){
						
						// Acesso ao ATTRIBUTE de um ATTRIBUTE-SET apenas se for um target
						NodeList attribute = att_set_element.getChildNodes();
						for (int k = 0; k<attribute.getLength(); k++){
							Node attribute_node = attribute.item(k);
							if (attribute_node.getNodeType() == Node.ELEMENT_NODE){
								
								Element attribute_element = (Element) attribute_node;
								
								String chave = attribute_element.getAttribute("NAME");
								String valor = attribute_element.getAttribute("VALUE");
								

								if(chave.equals(name)) return valor;
							}
						}
						
					}
				}
			}
		}
		
		return "";
	}
	
	/**Converte Set de Integer para um vetor de inteiros. A estrutura
	 * Set faz com que não seja permitida a repetição de numeros.
	 * 
	 * @param integers Set de inteiros
	 * @return int[] Inteiros
	 */
	public static int[] convertIntegers(Set<Integer> integers){
	    int[] ret = new int[integers.size()];
	    int i = 0;
	    for(Integer aux : integers) ret[i++] = aux;
	    return ret;
	}
	
	/** Dada uma instancia, este metodo se encarrega de avaliar
	 * quais atributos devem ser usados. Ele abre o classificador
	 * especifico para cada atributo, e devolve uma lista com o nome
	 * dos atributos que devem aparecer para o usuario.
	 * 
	 * @param instancia Deve ser passado um conjunto de instancias,
	 * 					por restricoes do da biblioteca, mas o metodo
	 * 					considera apenas a primeira instancia (indice 0).
	 * @return Lista com o nome dos atributos que devem ser usados. 
	 * @throws Exception Falha se houver algum problema na remoção de atributos.
	 */
	static ArrayList<String> avalia(Instances instancia) throws Exception{
		ArrayList<String> resposta = new ArrayList<String>();
		
		for(int i = 0; i<nome_classificador.size(); i++){
			
			Set<Integer> apaga = new HashSet<Integer>();
			for (int j = 0; j<instancia.numAttributes(); j++)
				if(!instancia.attribute(j).name().startsWith("discr_"))
					apaga.add(j);
			
			apaga.remove(instancia.attribute("use_" + nome_classificador.get(i)).index());
			int indices[] = convertIntegers(apaga);
			
			Remove remove = new Remove();
			remove.setAttributeIndicesArray(indices);
			remove.setInputFormat(instancia);
			
			Instances tmp = Filter.useFilter(instancia, remove);
			tmp.setClassIndex(tmp.numAttributes()-1);

			if(TesteModelo.classify(classificadores.get(i), tmp, 0))
				resposta.add(nome_classificador.get(i));

		}
		
		// Faz ordenacao
		ArrayList<String> tmp = new ArrayList<String>();
		int n = resposta.size();
		boolean used[] = new boolean[n];
		
		// 1o) por type
		for (int i = 0; i<n; i++) if(resposta.get(i).equals("type")){
			used[i] = true;
			tmp.add(resposta.get(i));
			break;
		}
		
		// 2o) por atributo atomico
		for (int i = 0; i<n; i++) if(!used[i] && relacional.contains(resposta.get(i))){
			used[i] = true;
			tmp.add(resposta.get(i));
		}
		
		// 3o) por atributo relacional
		for (int i = 0; i<n; i++) if(!used[i]) tmp.add(resposta.get(i));
		
		return tmp;
	}
	
	private static ArrayList<J48> classificadores;
	private static ArrayList<String> nome_classificador;
	
	/** Metodo para indicar qual diretiva utilizar:
	 * - Objeto 1, não é adicionada nenhuma diretiva.
	 * - Objeto 2, landmark indicando segundo objeto.
	 * - Objeto 3 ou mais, second-landmark indica que esta
	 * 				referenciando o terceiro objeto.
	 * 
	 * @param x nivel da profundidade
	 * @return String com o tipo designado para o objeto.
	 */
	static String evaldeep(int x){
		if(x == 0) return "";
		else if(x == 1) return "landmark-";
		else return "second-landmark-";
	}
	
	/** Recursivamente gera uma expressão para um contexto desejado.
	 * Utiliza os classificadores J48.
	 * 
	 * @param instancias Utilizado para gerar nova instancia com todos os atributos necessarios.
	 * @param context_node recebe um no de um contexto (do .xml).
	 * @param ID	nome da ID alvo do contexto de interesse.
	 * @param deep	indica qual eh o objeto que esta se referindo.
	 * @return		retorna uma string referente a expressao gerada recursivamente.
	 * @throws Exception	Excessao ocorre apenas se houve algum proclema com o "Instances".
	 */
	static void gera (Instances instancias, Node context_node, String ID, int deep, ArrayList<String> resposta) throws Exception{
		if(used_relational.contains(ID)) return;
		if(relacional.contains(ID)) used_relational.add(ID);
		
		Instances tmp = new Instances(instancias, 0);
		poder(tmp, context_node, ID);
		
		ArrayList<String> usar = avalia(tmp);
		
		System.out.print(ID + " [");
		for(int i = 0; i<usar.size(); i++){
			if(i > 0) System.out.print(", ");
			String name = evaldeep(deep) + usar.get(i);
			String value =  getValor(context_node, ID, usar.get(i));

			resposta.add(name + "=" + value);
			System.out.print(name + "=" + value);
		}
		System.out.println("]");
		
		for(int i = 0; i<usar.size(); i++){
			String atual = getValor(context_node, ID, usar.get(i));

			if(relacional.contains(atual)){
				gera(instancias, context_node, atual, deep+1, resposta);
			}
		}
		
	}
	
	/** Este metodo faz a montagem, identificando quais atributos sao relacionais ou nao.
	 * Junto com ele, sao carregados os classificadores presentes na pasta especificada.
	 * Inicialmente, o alvo principal ("MAIN-TARGET") é identificado.
	 * Apos abrir (carregar na memoria) todos os classificadores, finalmente é montada a
	 * expressao para um contexto_alvo que foi indicado pelo usuario.
	 * 
	 * @param contexto_alvo	String com o nome do contexto que quer descrever.
	 * @param instancias	Conjunto de instancias (normalmente serve para pegar todos
	 * 						os atributos presentes em um .arff).
	 * @param arquivo_contexto	String com nome do arquivo de contexto de interesse.
	 * @param pasta_model	String com endereco para pasta onde estao os .model.
	 * @throws Exception	Excessao ocorre apenas se houve algum proclema com o "Instances".
	 */
	static ArrayList<String> MontaExpressaoClassificador (String contexto_alvo, Instances instancias, String arquivo_contexto, String pasta_model) throws Exception{		
		Node attributes_set = build_frequences(instancias, arquivo_contexto, contexto_alvo);
		String id_alvo = ((Element) attributes_set).getAttribute("MAIN-TARGET");
		
		used_relational = new HashSet<String>();
		
		// Carrega todos os classificadores que podem ser usados
		try {
			File pasta = new File (pasta_model);
			File[] files_list = pasta.listFiles();
			
			classificadores = new ArrayList<J48>();
			nome_classificador = new ArrayList<String>();
			
			// Pega todos os classificadores e um dos arquivos de teste da pasta
			for (int i = 0; i<files_list.length; i++){
				if (files_list[i].isFile()){
					String arq = files_list[i].getName();
					
					if (arq.endsWith(".model")){
						J48 cls = (J48) SerializationHelper.read(new FileInputStream(pasta+ "/" +arq));
						classificadores.add(cls);
						nome_classificador.add(arq.substring(arq.lastIndexOf("_")+1, arq.lastIndexOf(".")));
					}
				}
			}
		} catch (Exception e) {
			System.err.println("erro");
		}

		ArrayList<String> resposta = new ArrayList<String>();
		gera(instancias, attributes_set, id_alvo, 0, resposta);

		return resposta;
	}
	
	private static void calcula_poder (ArrayList<String> s, String chave, String valor, int pos){
		Map <String, String> objeto = new HashMap<String, String>();
		objeto.put(chave, valor);
		
		int total = freq_x.get(chave) - frequencia_atributo.get(objeto);
		s.set(pos, String.valueOf(total)); 
	}
	
	public static Node build_frequences (Instances instancias, String arquivo, String contexto_alvo){
		try {
			File fxml = new File (arquivo);
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
					
					if(context_element.getAttribute("ID").equals(contexto_alvo)){
						
						relacional = new HashSet<String>();
						
						// Pega ATTRIBUTE-SET de um CONTEXT
						NodeList attributes_list = context_element.getChildNodes();
						for (int j = 0; j<attributes_list.getLength(); j++){
							Node attribute_set_node = attributes_list.item(j);
							
							if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
								Element att_set_element = (Element) attribute_set_node;
								relacional.add(att_set_element.getAttribute("ID"));
								
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
						
						return context_node;
					}
					
				}
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	

	private static void poder(Instances instancias, Node context_node, String id_alvo) {
		ArrayList<String> resposta = new ArrayList<String>();
		for(int i = 0; i<instancias.numAttributes(); i++) resposta.add("0");
		
		if (context_node.getNodeType() == Node.ELEMENT_NODE){
			Element context_element = (Element) context_node;
			resposta.set(0, context_element.getAttribute("ID"));
			
			// Pega ATTRIBUTE-SET de um CONTEXT
			NodeList attributes_list = context_element.getChildNodes();
			for (int j = 0; j<attributes_list.getLength(); j++){
				Node attribute_set_node = attributes_list.item(j);
				
				if (attribute_set_node.getNodeType() == Node.ELEMENT_NODE){
					Element att_set_element = (Element) attribute_set_node;
					
					if (att_set_element.getAttribute("ID").equals(id_alvo)){
						
						// Acesso ao ATTRIBUTE de um ATTRIBUTE-SET apenas se for um target
						NodeList attribute = att_set_element.getChildNodes();
						for (int k = 0; k<attribute.getLength(); k++){
							Node attribute_node = attribute.item(k);
							if (attribute_node.getNodeType() == Node.ELEMENT_NODE){
								
								Element attribute_element = (Element) attribute_node;
								
								String chave = attribute_element.getAttribute("NAME");
								String valor = attribute_element.getAttribute("VALUE");
								
//								System.out.println(chave + " " + valor);
	
								int index = 0;
								
								if(instancias.attribute("discr_" + chave) != null){
									index = instancias.attribute("discr_" + chave).index();
								}
								
								calcula_poder(resposta, chave, valor, index);
							}
						}
						
						break;
					}
				}
			}
		}
		
		Instance nova_instancia = new DenseInstance(resposta.size());
		
		for(int i = 0; i<resposta.size(); i++) {
			Attribute att = instancias.attribute(i);
			if(att.type() == Attribute.STRING ||att.type() == Attribute.NOMINAL) nova_instancia.setValue(att, resposta.get(i));
			else nova_instancia.setValue(att, Double.parseDouble(resposta.get(i)));
		}
		
		instancias.add(nova_instancia);
		
	}
	
}
