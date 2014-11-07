EACH-GRE
========
Corpus extraction tool for Referring Expression Generation. It was developed using the following specifications:

 1. Windows 7 x64
 2. Eclipse SDK Kepler Service Release 2
 3. Java JDK 1.8 x64
 4. Weka 3.7.11



Requisitos
========
* Para utilizar os códigos-fonte no eclipse, é necessário adicionar a biblioteca "weka.jar", presente na pasta: "C:\Program Files\Weka-3-7".


* Para configurar o Eclipse:
 1. Selecione o projeto e clique com o botão direito.
 2. Vá em "Properties / Propriedades"
 3. Clique em "Java Build Path", em seguida clique na aba "Libraries / Biblioteca".
 4. Clique em "Add External JAR / Adicionar JAR Externo".
 5. Selecione o arquivo "weka.jar", na pasta citada anteriormente e confirme as ações.

 

 
Como Utilizar
========
Para funcionamento correto, siga os seguintes passos de utilização:

 1. **Main.java**
  *	Cálculo de poder discriminatório e geração de ***.arff*** compatível com o weka.
  *	Separa **20%** para teste, e outros **80%** para treinamento.

 2. **TesteModelo.java**
  * Para os arquivos de teste e treinamento gerados, cria ***n*** novos **.arff**. Sendo ***n*** o número de atributos binários (**"use_"**).

 3. **GeraExpressao.java**
  * Compõe uma *string* utilizando os classificadores.

 4. **Estatisticas.java**
  * Recebe como entrada dois arquivos de texto gerados no passo anterior, onde cada linha representa uma expressão:
        1. O arquivo com todas as expressões esperadas, ie., geradas por humanos em experimento.
        2. O arquivo com expressões geradas por máquina, utilizando os classificadores do tipo árvore de decisão (J48), gerados no passo 2.
  * É calculado o coeficiente **Dice** para cada expressão.
  * No final, é apresentada a média dos coeficientes.



Detalhes importantes
========
####  Main.java
 * Para funcionamento desse algoritmo, precisamos manter as pastas no seguinte formato:
  1. "./projeto/context/":
    * Dentro desta pasta, devem contem todos os corpus de contexto, com o sufixo "-context.xml"
    * Ex: *Stars-context.xml, GRE3D-context.xml, ..., etc.*
  2. "./projeto/trials/":
    * Nesta pasta, devem conter subpastas para seu respectivo contexto.
    * Ex: *Se estivermos tratando do contexto "Stars-context.xml", precisamos de uma pasta Stars no diretório "./projeto/trials/Stars/", contendo todos os arquivos **.xml** de trial*.

 * Este algoritmo pode ser adaptado para tratar apenas de um contexto. Mas no momento,ele esta processando todos os arquivos context de uma vez.

 * Os arquivos ***.arff*** gerados são gravados dentro da pasta **output** de seu respectivo contexto.

 * Para cada contexto, são gerados 2 arquivos:
  1. Um contem cerca de 80% dos casos para treinamento, gravado no arquivo *nome_do_contexto**.arff***.
  2. O outro contém cerca de 20% dos casos para teste. Este arquivo será processado novamente, a ideia principal deste arquivo, é informar a qual trial pertence o trial escolhido para teste, que será realizado posteriormente.
 
 
####  TesteModelo.java
 * Geração dos arquivos de ***.arff*** de treino teste.
 * Geração dos classificadores para cada uma das classes. 

 * Este método se encarrega de criar um (ou vários) arquivo(s) ***.arff*** de entrada para cada atributo do ***.arff*** original.

 * No caso do ***.arff*** de treinamento, vários novos arquivos são gerados, para cada instancia de treinamento, é feita uma combinação com seu respectivo atributo *booleano* (**use_**).
 
 
####  Estatisticas.java
 * Abre um arquivo representando um conjunto A.
 * Abre outro arquivo representando um conjunto B.

 *  Esses arquivos devem ter o formato: ***[chave_1=valor_1, ..., chave_n=valor_n]***
 * Com isso, é feito o cálculo do coeficiente Dice:
  * Dice (A, B) = (2*|A inter B|)/(|A| + |B|)
  * |x| representa o tamanho do conjunto x
 
 
 
