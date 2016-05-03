package web.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public final class WebServer {

	public static void main(String arvg[]) throws Exception {
		// Ajustar o número da porta.
		int port = 2020;

		// Estabelecer o socket de escuta.
		ServerSocket welcomeSocket = new ServerSocket(port);

		// Processar a requisição de serviço HTTP em um laço infinito.
		while (true) {
			// Escutar requisição de conexão TCP.
			Socket connectionSocket = welcomeSocket.accept();

			// Construir um objeto para processar a mensagem de requisição HTTP.
			HttpRequest request = new HttpRequest(connectionSocket);
			// Criar um novo thread para processar a requisição.
			Thread thread = new Thread(request);

			// Iniciar o thread.
			thread.start();

		}
	}
}

final class HttpRequest implements Runnable {

	final static String CRLF = "\r\n";
	Socket socket;

	// Construtor
	public HttpRequest(Socket socket) throws Exception {
		this.socket = socket;
	}

	// Implemente o método run() da interface Runnable.
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception {
		// obter uma referência para os trechos de entrada e saída do socket
		InputStream is = socket.getInputStream();
		DataOutputStream os = new DataOutputStream(socket.getOutputStream());

		// Ajustar os filtros do trecho de entrada
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		// Obter a linha de requisição da mensagem de requisição HTTP
		String requestLine = br.readLine();

		// Exibir a linha de requisição
		System.out.println();
		System.out.println(requestLine);

		// Obter e exibir as linhas de cabeçalho.
		String headerLine = null;
		while ((headerLine = br.readLine()).length() != 0) {
			System.out.println(headerLine);
		}
		
		os.close();
		br.close();
		socket.close();
		
		//Extrair o nome do arquivo a linha de requisição.
		StringTokenizer tokens = new StringTokenizer(requestLine);
		tokens.nextToken(); //pular o metodo, que deve ser "GET"
		String fileName = tokens.nextToken();
		
		//Acrescente um"." de modo que a requisição do arquivo esteja dentro do diretorios atual.
		fileName = "." + fileName;
		
		//Abrir o arquivo requisitado.
		FileInputStream fis = null;
		Boolean fileExists = true;
		try{
			fis = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			fileExists = false;
		}
		
		//Construir a mensagem de resposta
		String statusLine = null;
		String contentTypeLine = null;
		String entityBody = null;
		
		if(fileExists) {
			statusLine = "HTTP/1.0 200 ok" + CRLF;
			contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
		}
		else {
			statusLine = "HTTP/1.0 404 Not Found" + CRLF;
			contentTypeLine = "Content-type: " + "text/html" + CRLF;
			entityBody = "<HTML>" + "<HEAD><TITTLE>Not Found</TITTLE></HEAD>" + 
					 "<BODY>Not Found</BODY></HTML>";
		}
		
		//Enviar a linha de status.
		os.writeBytes(statusLine);
		
		//Enviar a linha de tipo de conteudo.
		os.writeBytes(contentTypeLine);
		
		//Enviar uma linha em branco para indicar o fim das linhas de cabeçalho.
		os.writeBytes(CRLF);
		
		//Enviar o corpo da entidade.
		if(fileExists) {
			sendBytes(fis, os);
			os.writeBytes(statusLine);
			fis.close();
		}
		else{
			os.writeBytes(statusLine);
			os.writeBytes(entityBody);
			os.writeBytes(contentTypeLine);
		}
	}
	private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
		
		//Construir um buffer de 1k para comportar os bytes no caminho para o sockte.
		byte[] buffer = new byte[1024];
		int bytes = 0;
		
		//Copiar o arquivo requisitado dentro da cadeia de saida do socket.
		while((bytes = fis.read(buffer)) != -1) {
			os.write(buffer, 0, bytes);
		}
	}
	
	private String contentType(String fileName) {
		if(fileName.endsWith(".htm") || fileName.endsWith(".html")){
			return "text/html";
		}
		if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")){
			return "image/jpeg";
		}
		if(fileName.endsWith(".gif")){
			return "image/gif";
		}
		return "application/octet-stream";
	}

}
