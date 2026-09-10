import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SicaCliente {
    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PORTA = 5000;
    private static final String DIRETORIO_CLIENTE = "cliente_arquivos";

    public static void main(String[] args) {
        // Cria a pasta de downloads do cliente
        File diretorio = new File(DIRETORIO_CLIENTE);
        if (!diretorio.exists()) {
            diretorio.mkdir();
        }

        try (Socket socket = new Socket(IP_SERVIDOR, PORTA);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado ao Servidor SICA!");
            boolean executando = true;

            // Loop do menu principal
            while (executando) {
                System.out.println("\n--- MENU SICA ---");
                System.out.println("1. Listar arquivos no servidor");
                System.out.println("2. Enviar arquivo para o servidor (Upload)");
                System.out.println("3. Baixar arquivo do servidor (Download)");
                System.out.println("4. Sair");
                System.out.print("Escolha uma opção: ");
                
                String opcao = scanner.nextLine();

                switch (opcao) {
                    case "1":
                        dos.writeUTF("LISTAR");
                        receberListaArquivos(dis);
                        break;
                    case "2":
                        System.out.print("Digite o caminho completo do arquivo para envio: ");
                        String caminho = scanner.nextLine();
                        enviarArquivoParaServidor(caminho, dos, dis);
                        break;
                    case "3":
                        System.out.print("Digite o nome do arquivo que deseja baixar: ");
                        String nomeDownload = scanner.nextLine();
                        baixarArquivoDoServidor(nomeDownload, dos, dis);
                        break;
                    case "4":
                        dos.writeUTF("SAIR");
                        executando = false;
                        System.out.println("Encerrando cliente...");
                        break;
                    default:
                        System.out.println("Opção inválida!");
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao conectar no servidor: " + e.getMessage());
        }
    }

    // Processa a lista de arquivos recebida do servidor
    private static void receberListaArquivos(DataInputStream dis) throws IOException {
        int qtd = dis.readInt();
        if (qtd == 0) {
            System.out.println("O servidor não possui arquivos.");
        } else {
            System.out.println("Arquivos disponíveis no servidor:");
            for (int i = 0; i < qtd; i++) {
                System.out.println("- " + dis.readUTF());
            }
        }
    }

    // Gerencia a leitura de um arquivo local e o envio de seus bytes pela rede
    private static void enviarArquivoParaServidor(String caminho, DataOutputStream dos, DataInputStream dis) throws IOException {
        File arquivo = new File(caminho);
        if (arquivo.exists() && arquivo.isFile()) {
            dos.writeUTF("ENVIAR");
            dos.writeUTF(arquivo.getName());
            dos.writeLong(arquivo.length()); // Envia o tamanho para controle do servidor

            try (FileInputStream fis = new FileInputStream(arquivo)) {
                byte[] buffer = new byte[4096];
                int lidos;
                while ((lidos = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, lidos);
                }
            }
            // Aguarda a confirmação de sucesso do servidor
            System.out.println(dis.readUTF());
        } else {
            System.out.println("Erro: Arquivo local não encontrado.");
        }
    }

    // Gerencia a requisição e a recepção de bytes de um arquivo do servidor
    private static void baixarArquivoDoServidor(String nomeArquivo, DataOutputStream dos, DataInputStream dis) throws IOException {
        dos.writeUTF("BAIXAR");
        dos.writeUTF(nomeArquivo);

        String status = dis.readUTF(); // Verifica se o arquivo existe lá
        if (status.equals("OK")) {
            long tamanhoArquivo = dis.readLong();
            File arquivoDestino = new File(DIRETORIO_CLIENTE + File.separator + nomeArquivo);

            try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
                byte[] buffer = new byte[4096];
                int lidos;
                long totalLido = 0;

                while (totalLido < tamanhoArquivo && (lidos = dis.read(buffer, 0, (int)Math.min(buffer.length, tamanhoArquivo - totalLido))) != -1) {
                    fos.write(buffer, 0, lidos);
                    totalLido += lidos;
                }
                System.out.println("SUCESSO: Arquivo baixado na pasta '" + DIRETORIO_CLIENTE + "'.");
            }
        } else {
            System.out.println("ERRO: Arquivo não existe no servidor.");
        }
    }
}