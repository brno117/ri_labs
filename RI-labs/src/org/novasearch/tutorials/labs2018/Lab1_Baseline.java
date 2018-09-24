package org.novasearch.tutorials.labs2018;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Lab1_Baseline {

	String indexPath = "./index";
	String docPath = "./eval/Answers.csv";

	boolean create = true;

	private IndexWriter idx;

	public static void main(String[] args) {

		Analyzer analyzer = new StandardAnalyzer();
		Similarity similarity = new ClassicSimilarity();

		Lab1_Baseline baseline = new Lab1_Baseline();

		// Create a new index
		baseline.openIndex(analyzer, similarity);
		
		//Indexa os documentos 
		baseline.indexDocuments();
		
		//fecha o index
		baseline.close();
		
		
		//Para nao teres que estar sempre a indexar os documentos cada vez que corres a aplicacao
		//comentas as 3 linhas de codigo acima deixando so o search abaixo

		// Search the index
		baseline.indexSearch(analyzer, similarity);
	}

	public void openIndex(Analyzer analyzer, Similarity similarity) {
		try {
			// ====================================================
			// Configure the index to be created/opened
			//
			// IndexWriterConfig has many options to be set if needed.
			//
			// Example: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setSimilarity(similarity);
			if (create) {
				// Create a new index, removing any
				// previously indexed documents:
				iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			}

			// ====================================================
			// Open/create the index in the specified location
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			idx = new IndexWriter(dir, iwc);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void indexDocuments() {
		if (idx == null)
			return;

		// ====================================================
		// Parse the Answers data
		
		
		//FileReader e o objecto que te permite ler o ficheiro
		//BufferedReader e o objecto/destino de cada leitura feita ao ficheiro
		try (BufferedReader br = new BufferedReader(new FileReader(docPath))) {
			
			//StringBuilder e o objecto que te permite ir construindo novo texto
			StringBuilder sb = new StringBuilder();
			
			//Cada br.readLine le uma linha do ficheiro e coloca na variavel "line"
			String line = br.readLine(); // The first line is dummy
			line = br.readLine();

			// ====================================================
			// Read documents
			
			//Efetua um ciclo enquanto o br conseguir ler linhas ao ficheiro (Enquanto o ficheiro tiver conteudo)
			while (line != null) {
				int i = line.length();

				
				//Dentro do ficheiro ha varios documentos, as proximas linhas procuram pelo caracter
				//de separacao de cada documento, se encontrar fazem a indexacao (chamam a funcao
				//indexDoc que eu ainda nao sei o que faz exatamente) se nao encontrarem continuam a 
				//ler linhas do ficheiro ate atingir o fim do documento
				
				// Search for the end of document delimiter
				if (i != 0)
					sb.append(line);
				sb.append(System.lineSeparator());
				if (((i >= 2) && (line.charAt(i - 1) == '"') && (line.charAt(i - 2) != '"'))
						|| ((i == 1) && (line.charAt(i - 1) == '"'))) {
					// Index the document
					indexDoc(sb.toString());

					// Start a new document
					sb = new StringBuilder();
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void indexDoc(String rawDocument) {

		Document doc = new Document();

		// ====================================================
		// Each document is organized as:
		// Id,OwnerUserId,CreationDate,ParentId,Score,Body
		Integer AnswerId = 0;
		try {

			// Extract field Id
			Integer start = 0;
			Integer end = rawDocument.indexOf(',');
			String aux = rawDocument.substring(start, end);
			AnswerId = Integer.decode(aux);

			// Index _and_ store the AnswerId field
			doc.add(new IntPoint("AnswerId", AnswerId));
			doc.add(new StoredField("AnswerId", AnswerId));

			// Extract field OwnerUserId
			start = end + 1;
			end = rawDocument.indexOf(',', start);
			aux = rawDocument.substring(start, end);
			Integer OwnerUserId = Integer.decode(aux);
			doc.add(new IntPoint("OwnerUserId", OwnerUserId));

			// Extract field CreationDate
			try {
				start = end + 1;
				end = rawDocument.indexOf(',', start);
				aux = rawDocument.substring(start, end);
				Date creationDate;
				creationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(aux);
				doc.add(new LongPoint("CreationDate", creationDate.getTime()));
			} catch (ParseException e1) {
				System.out.println("Error parsing date for document " + AnswerId);
			}
			
			//Teste Gits

			// Extract field ParentId
			start = end + 1;
			end = rawDocument.indexOf(',', start);
			aux = rawDocument.substring(start, end);
			Integer ParentId = Integer.decode(aux);
			doc.add(new IntPoint("ParentId", ParentId));

			// Extract field Score
			start = end + 1;
			end = rawDocument.indexOf(',', start);
			aux = rawDocument.substring(start, end);
			Integer Score = Integer.decode(aux);
			doc.add(new IntPoint("Score", Score));

			// Extract field Body
			String body = rawDocument.substring(end + 1);
			doc.add(new TextField("Body", body, Field.Store.YES));

		// ====================================================
		// Add the document to the index
			if (idx.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
				System.out.println("adding " + AnswerId);
				idx.addDocument(doc);
			} else {
				idx.updateDocument(new Term("AnswerId", AnswerId.toString()), doc);
			}
		} catch (IOException e) {
			System.out.println("Error adding document " + AnswerId);
		} catch (Exception e) {
		System.out.println("Error parsing document " + AnswerId);
		}
	}

	// ====================================================
	// Comment and refactor this method yourself
	//
	//Este e o metodo que faz a pesquisa
	public void indexSearch(Analyzer analyzer, Similarity similarity) {

		IndexReader reader = null;
		try {
			
			//Abre o ficheiro onde fizemos a indexacao
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			
			//Passa o ficheiro como parametro deste objeto searcher que eu nao sei o que faz ainda
			IndexSearcher searcher = new IndexSearcher(reader);
			
			//Este searcher tem uma propriedade "similarity" que e definida usando o objeto
			//recebido em parametro
			searcher.setSimilarity(similarity);

			//BufferedReader e o objecto onde vai ser armazenado e processado o ficheiro index que criamos no metodo
			//anterior
			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

			//Objeto que vai processar o que escrevemos na consola
			//(pesquisa pelo conteudo nos ficheiros
			QueryParser parser = new QueryParser("Body", analyzer);
			
			//Este ciclo itera infinitamente para poderes escrever na consola quando quiseres
			//Basicamente fica a espera de input
			while (true) {
				System.out.println("Enter query: ");

				
				String line = in.readLine();

				if (line == null || line.length() == -1) {
					break;
				}

				line = line.trim();
				if (line.length() == 0) {
					break;
				}

				//Faz o processamento da linha lida devolvendo um objeto "query"
				Query query;
				try {
					query = parser.parse(line);
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					System.out.println("Error parsing query string.");
					continue;
				}

				//Faz uma pesquisa usando a "query" criada e devolve um objeto "TopDocs"
				TopDocs results = searcher.search(query, 100);
				
				//Cria um array com as pontuacoes de cada documento obtidas apartir do "TopDocs"
				ScoreDoc[] hits = results.scoreDocs;

				//Numero de documentos encontrados com a pesquisa efetuada
				int numTotalHits = (int)results.totalHits;
				System.out.println(numTotalHits + " total matching documents");

				//Efetua um ciclo que por cada um dos documentos encontrados faz:
				//Obtem o objeto "Document" apartir do Searcher criado acima
				//Apartir do documento obtem a frase onde fez match com as palavras que procuravamos
				//Obtemt o "Id" da resposta tambem apartir do "Document" gerado
				//Escreve esta informacao na consola (a partir do system.out.println)
				
				for (int j = 0; j < hits.length; j++) {
					Document doc = searcher.doc(hits[j].doc);
					String answer = doc.get("Body");
					Integer AnswerId = doc.getField("AnswerId").numericValue().intValue();
					System.out.println("------------------------------------------");
					System.out.println("AnswerId: " + AnswerId);
					System.out.println("Answer: " + answer);
					System.out.println();
				}

				if (line.equals("")) {
					break;
				}
			}
			reader.close();
			
			//Excecoes para se o programa rebentar a abrir os ficheiros guardados na maquina 
			
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			idx.close();
		} catch (IOException e) {
			System.out.println("Error closing the index.");
		}
	}

}
