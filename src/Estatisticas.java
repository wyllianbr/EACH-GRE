import java.io.*;
import java.util.*;


public class Estatisticas {
	
	/** Abre um arquivo representando um conjunto A.
	 * E outro arquivo representando um conjunto B.
	 * Esses arquivos devem ter o formato [chave_1=valor_1, ..., chave_n=valor_n]
	 * Com isso, eh feito o calculo do coeficiente Dice:
	 * 
	 * Dice = (2*|A inter B|)/(|A| + |B|)
	 * |x| representa o comprimento (tamanho do conjunto x)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String context = "Stars";
		String filename_classifier = String.format("projeto/output/%s/expressaoClassificador.txt", context);
		String filename_expected = String.format("projeto/output/%s/expressaoEsperada.txt", context);
		
		ArrayList<String> A = open(filename_classifier);
		ArrayList<String> B = open(filename_expected);

		double ans = 0;
		for (int i = 0; i<A.size(); i++){
			String x[] = A.get(i).replaceAll("\\[|\\]", "").split(", ");
			String y[] = B.get(i).replaceAll("\\[|\\]", "").split(", ");

			ArrayList<String> inter = intersection(x, y);
			int uni = union(x, y);
	
			double tmp = (double) (2.0f*inter.size()) / uni;
			System.out.printf("[Expressao %3d] = %.5f\n", i+1, tmp);
			ans += tmp;
		}

		System.out.println("=========================");
		System.out.println("Media: " + ans / A.size());
	}
	
	/************************************************************/
	/************************************************************/

	/**Retorna |x| + |y|, necessario no coeficiente Dice.
	 * 
	 * @param x Conjunto x
	 * @param y Conjunto y
	 * @return (Tamanho de x) + (tamanho de y)
	 */
    public static int union(String x[], String y[]) {
        return x.length + y.length;
    }

    /**Faz a intersecao de dois conjuntos.
     * 
     * @param x representa um conjunto x
     * @param y representa um conjunto y
     * @return z = conjunto de x interseccao y
     */
    public static ArrayList<String> intersection(String x[], String y[]) {
        ArrayList<String> list1 = new ArrayList<String>(Arrays.asList(x));
        ArrayList<String> list2 = new ArrayList<String>(Arrays.asList(y));
        
        ArrayList<String> list = new ArrayList<String>();

        for (String t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
    
    /**	Abre um arquivo de texto, e faz leitura linha a linha.
     * Retorna essas linhas de String no formato de um ArrayList
     * 
     * @param String com o diretorio/nome do arquivo a ser aberto
     * @return ArrayList de String com cada linha do arquivo aberto
     */
	@SuppressWarnings("resource")
	public static ArrayList<String> open(String arq){
		ArrayList<String> ans = new ArrayList<String>();
		try {
			File file = new File(arq);
			BufferedReader reader = null;
		    reader = new BufferedReader(new FileReader(file));
		    String text = null;

		    while ((text = reader.readLine()) != null) {
		        ans.add(text.replaceAll("\\(|\\)", ""));
		    }
		} catch (FileNotFoundException err) {
			System.err.println("Could not open file: " + arq);
			System.err.println(err);
		} catch (IOException err) {
			System.err.println("Could not read file: " + arq);
			System.err.println(err);
		}
		
		return ans;
	}
	


}
