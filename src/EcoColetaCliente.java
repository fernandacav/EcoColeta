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
                // O ObjectOutputStream DEVE ser inicializado antes do ObjectInputStream
                // para evitar Deadlocks/Problemas de Cabeçalho na serialização.
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            // Forçar a escrita do cabeçalho do stream imediatamente após a criação
            oos.flush();

            // O ObjectInputStream deve ser inicializado DEPOIS do ObjectOutputStream
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            System.out.println("Conectado ao servidor. Enviando requisição...");

            // NOVO PASSO: Envia o comando esperado pelo Servidor
            oos.writeObject("BUSCAR");

            // Passo Antigo: Envia o dado (materialBusca)
            oos.writeObject(materialBusca);
            oos.flush(); // Garante que AMBOS os objetos sejam enviados

            // O restante do código de leitura e exibição...
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
            // ...
        } catch (IOException | ClassNotFoundException e) {
            // Mantenha essa linha para debug, ela ajudará a ver o erro real se não for
            // "null"
            System.err.println("Erro de comunicação: " + e.getMessage());
        }
    }
}