import java.io.*;
import java.net.*;
import java.util.*;

public class EcoColetaServer {
    private static final int PORTA = 12345;
    private static List<PontoColeta> pontos = new ArrayList<>();

    public static void main(String[] args) {
        // Inicializa dados de exemplo (sem persistência)
        pontos.add(new PontoColeta("Ecoponto Central", "Rua A, 100", Arrays.asList("Papel", "Plástico", "Vidro")));
        pontos.add(new PontoColeta("Coleta Bairro Sul", "Av. Principal, 500", Arrays.asList("Plástico", "Metal")));
        pontos.add(new PontoColeta("Ponto Óleo", "Rua C, 30", Arrays.asList("Óleo de Cozinha", "Metal")));

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor EcoColeta iniciado na porta " + PORTA + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lida com a lógica de busca (RF-C01)
    public static List<PontoColeta> buscarPontos(String material) {
        List<PontoColeta> encontrados = new ArrayList<>();
        // Normaliza a entrada para a busca ser case-insensitive
        String materialBusca = material.trim().toLowerCase();
        
        for (PontoColeta ponto : pontos) {
            // Checa se algum material aceito corresponde ao material buscado
            boolean aceita = ponto.getMateriaisAceitos().stream()
                                  .anyMatch(m -> m.equalsIgnoreCase(materialBusca));
            if (aceita) {
                encontrados.add(ponto);
            }
        }
        return encontrados;
    }
}

// Thread para lidar com cada conexão de cliente
class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {

            // Lê o tipo de resíduo que o cliente deseja descartar
            String materialBusca = (String) ois.readObject();
            System.out.println("Requisição: Busca por material '" + materialBusca + "'");

            // Executa a lógica de busca
            List<PontoColeta> resultados = EcoColetaServer.buscarPontos(materialBusca);
            
            // Envia a lista de pontos de coleta encontrados de volta ao cliente (RF-C02)
            oos.writeObject(resultados);
            oos.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}