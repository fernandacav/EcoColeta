import java.io.*;
import java.net.*;
import java.util.*;

public class EcoColetaCliente {
    private static final String HOST = "localhost";
    private static final int PORTA = 12345;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- Sistema EcoColeta - Busca de Pontos ---");
        System.out.print("Qual tipo de resíduo você deseja descartar (Ex: Plástico, Vidro, Óleo de Cozinha)? ");
        String materialBusca = scanner.nextLine();
        
        // Remove a necessidade do scanner após a leitura da busca
        scanner.close(); 

        try (Socket socket = new Socket(HOST, PORTA);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            System.out.println("Conectado ao servidor. Enviando requisição...");
            
            // Envia a requisição de busca
            oos.writeObject(materialBusca);
            oos.flush();

            // Recebe a lista de pontos de coleta
            // É necessário fazer um cast e ignorar o warning para manter a simplicidade
            @SuppressWarnings("unchecked")
            List<PontoColeta> resultados = (List<PontoColeta>) ois.readObject();
            
            System.out.println("\n--- Resultados da Busca para '" + materialBusca + "' ---");

            if (resultados.isEmpty()) {
                System.out.println("Nenhum ponto de coleta encontrado que aceite '" + materialBusca + "'.");
            } else {
                for (int i = 0; i < resultados.size(); i++) {
                    System.out.println("Ponto " + (i + 1) + ": " + resultados.get(i).toString());
                }
            }

        } catch (ConnectException e) {
            System.err.println("Erro: Não foi possível conectar ao servidor. Verifique se o EcoColetaServer está em execução.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro de comunicação: " + e.getMessage());
        }
    }
}