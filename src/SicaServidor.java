import java.io.*;
import java.net.*;

public class SicaServidor {
    private static final int PORTA = 5000;
    private static final String DIRETORIO_SERVIDOR = "servidor_arquivos";

    public static void main(String[] args) {
        // Cria o diretório do servidor caso não exista
        File diretorio = new File(DIRETORIO_SERVIDOR);
        if (!diretorio.exists()) {
            diretorio.mkdir();
        }

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor SICA iniciado na porta " + PORTA + ". Aguardando conexões...");

            while (true) {
                // Aguarda e aceita a conexão de um cliente
                Socket socketCliente = serverSocket.accept();
                System.out.println("Cliente conectado: " + socketCliente.getInetAddress().getHostAddress());

                // Cria as streams de entrada e saída de dados
                DataInputStream dis = new DataInputStream(socketCliente.getInputStream());
                DataOutputStream dos = new DataOutputStream(socketCliente.getOutputStream());

                boolean conectado = true;
                
                // Loop de comunicação com o cliente conectado
                while (conectado) {
                    try {
                        String comando = dis.readUTF(); // Lê o comando enviado pelo cliente

                        switch (comando) {
                            case "LISTAR":
                                listarArquivos(dos);
                                break;
                            case "ENVIAR":
                                receberArquivo(dis, dos);
                                break;
                            case "BAIXAR":
                                enviarArquivo(dis, dos);
                                break;
                            case "SAIR":
                                conectado = false;
                                System.out.println("Cliente desconectado.");
                                break;
                            default:
                                System.out.println("Comando desconhecido.");
                        }
                    } catch (EOFException e) {
                        // Trata desconexão abrupta do cliente
                        conectado = false;
                        System.out.println("Cliente encerrou a conexão.");
                    }
                }
                socketCliente.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para listar arquivos do diretório e enviar os nomes ao cliente
    private static void listarArquivos(DataOutputStream dos) throws IOException {
        File pasta = new File(DIRETORIO_SERVIDOR);
        File[] arquivos = pasta.listFiles();

        if (arquivos != null && arquivos.length > 0) {
            dos.writeInt(arquivos.length); // Envia a quantidade de arquivos
            for (File arquivo : arquivos) {
                if (arquivo.isFile()) {
                    dos.writeUTF(arquivo.getName()); // Envia o nome de cada arquivo
                }
            }
        } else {
            dos.writeInt(0); // Informa que a pasta está vazia
        }
    }

    // Método para receber um arquivo enviado pelo cliente (Upload)
    private static void receberArquivo(DataInputStream dis, DataOutputStream dos) throws IOException {
        String nomeArquivo = dis.readUTF();
        long tamanhoArquivo = dis.readLong();
        
        File arquivo = new File(DIRETORIO_SERVIDOR + File.separator + nomeArquivo);
        
        try (FileOutputStream fos = new FileOutputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int lidos;
            long totalLido = 0;
            
            // Lê os bytes da rede e escreve no arquivo em disco
            while (totalLido < tamanhoArquivo && (lidos = dis.read(buffer, 0, (int)Math.min(buffer.length, tamanhoArquivo - totalLido))) != -1) {
                fos.write(buffer, 0, lidos);
                totalLido += lidos;
            }
            dos.writeUTF("SUCESSO: Arquivo recebido pelo servidor.");
        } catch (Exception e) {
            dos.writeUTF("ERRO: Falha ao salvar arquivo no servidor.");
        }
    }

    // Método para enviar um arquivo solicitado pelo cliente (Download)
    private static void enviarArquivo(DataInputStream dis, DataOutputStream dos) throws IOException {
        String nomeArquivo = dis.readUTF();
        File arquivo = new File(DIRETORIO_SERVIDOR + File.separator + nomeArquivo);

        if (arquivo.exists() && arquivo.isFile()) {
            dos.writeUTF("OK"); // Confirma que o arquivo existe
            dos.writeLong(arquivo.length()); // Envia o tamanho do arquivo
            
            try (FileInputStream fis = new FileInputStream(arquivo)) {
                byte[] buffer = new byte[4096];
                int lidos;
                // Lê o arquivo do disco e envia os bytes pela rede
                while ((lidos = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, lidos);
                }
            }
        } else {
            dos.writeUTF("ERRO"); // Informa que o arquivo não existe
        }
    }
}