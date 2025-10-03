import java.io.*;
import java.net.*;
import java.util.*;

public class EcoColetaServer {
    private static final int PORTA = 12345;
    // Torna a lista thread-safe para operações de escrita/leitura concorrentes
    // Embora List.of() crie a lista inicial, um ArrayList é necessário para adicionar/remover
    private static List<PontoColeta> pontos = new ArrayList<>(); 
    
    // Credenciais simples para o administrador (RF-G01)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "12345";

    public static void main(String[] args) {
        // Inicializa dados de exemplo
        pontos.add(new PontoColeta("Ecoponto Central", "Rua A, 100", Arrays.asList("Papel", "Plástico", "Vidro")));
        pontos.add(new PontoColeta("Coleta Bairro Sul", "Av. Principal, 500", Arrays.asList("Plástico", "Metal")));
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor EcoColeta iniciado na porta " + PORTA + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, pontos)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lógica de Autenticação
    public static boolean autenticar(String user, String pass) {
        return ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass);
    }

    // Lógica de Busca (Cidadão)
    public static List<PontoColeta> buscarPontos(String material) {
        List<PontoColeta> encontrados = new ArrayList<>();
        String materialBusca = material.trim().toLowerCase();
        
        synchronized (pontos) { // Sincroniza o acesso à lista para leitura
            for (PontoColeta ponto : pontos) {
                boolean aceita = ponto.getMateriaisAceitos().stream()
                                      .anyMatch(m -> m.equalsIgnoreCase(materialBusca));
                if (aceita) {
                    encontrados.add(ponto);
                }
            }
        }
        return encontrados;
    }
    
    // Lógica de Cadastro (Administrador - RF-A01)
    public static boolean cadastrarPonto(PontoColeta novoPonto) {
        synchronized (pontos) { // Sincroniza o acesso à lista para escrita
            return pontos.add(novoPonto);
        }
    }
    
    // Lógica de Remoção (Administrador - RF-A03)
    public static boolean removerPonto(long id) {
        synchronized (pontos) { // Sincroniza o acesso à lista para escrita
            // Remove o ponto cujo ID corresponde ao ID fornecido
            return pontos.removeIf(ponto -> ponto.getId() == id);
        }
    }
    
    // Lógica para listar todos (útil para o admin)
    public static List<PontoColeta> listarTodosPontos() {
        synchronized (pontos) {
            // Retorna uma cópia da lista para segurança, ou a própria lista para simplicidade
            return new ArrayList<>(pontos);
        }
    }
}

// Thread para lidar com cada conexão de cliente (Alterada para Comandos)
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private List<PontoColeta> pontos;

    public ClientHandler(Socket socket, List<PontoColeta> pontos) {
        this.clientSocket = socket;
        this.pontos = pontos;
    }

    @Override
    public void run() {
        String status = "FALHA";
        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {

            // Lê o primeiro objeto que é sempre o COMANDO
            String comando = (String) ois.readObject();
            System.out.println("Requisição recebida: " + comando);
            
            // --- CIDADÃO: BUSCAR ---
            if (comando.equals("BUSCAR")) { 
                String materialBusca = (String) ois.readObject();
                List<PontoColeta> resultados = EcoColetaServer.buscarPontos(materialBusca);
                oos.writeObject(resultados);
                status = "SUCESSO_BUSCA";
            } 
            
            // --- ADMINISTRADOR: LOGIN E GESTÃO ---
            else if (comando.startsWith("ADMIN_")) {
                // 1. Tenta autenticar (RF-G01)
                String user = (String) ois.readObject();
                String pass = (String) ois.readObject();
                
                if (!EcoColetaServer.autenticar(user, pass)) {
                    oos.writeObject("ERRO_AUTENTICACAO");
                    System.out.println("Tentativa de login ADMIN falhou.");
                    return; 
                }
                oos.writeObject("SUCESSO_AUTENTICACAO");
                
                // --- ADMIN: CADASTRAR (RF-A01) ---
                if (comando.equals("ADMIN_CADASTRAR")) {
                    PontoColeta novoPonto = (PontoColeta) ois.readObject();
                    if (EcoColetaServer.cadastrarPonto(novoPonto)) {
                        oos.writeObject("Ponto '" + novoPonto.getNome() + "' cadastrado com sucesso! ID: " + novoPonto.getId());
                        status = "SUCESSO_CADASTRO";
                    } else {
                        oos.writeObject("ERRO: Falha ao cadastrar ponto.");
                    }
                } 
                
                // --- ADMIN: REMOVER (RF-A03) ---
                else if (comando.equals("ADMIN_REMOVER")) {
                    Long idParaRemover = (Long) ois.readObject();
                    if (EcoColetaServer.removerPonto(idParaRemover)) {
                        oos.writeObject("Ponto com ID " + idParaRemover + " removido com sucesso.");
                        status = "SUCESSO_REMOCAO";
                    } else {
                        oos.writeObject("ERRO: Nenhum ponto encontrado com ID " + idParaRemover + ".");
                    }
                } 
                
                // --- ADMIN: LISTAR TODOS ---
                else if (comando.equals("ADMIN_LISTAR")) {
                    oos.writeObject(EcoColetaServer.listarTodosPontos());
                    status = "SUCESSO_LISTAGEM";
                }
            }

            oos.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            System.out.println("Cliente desconectado com status: " + status);
            try {
                clientSocket.close();
            } catch (IOException e) {
                // ...
            }
        }
    }
}