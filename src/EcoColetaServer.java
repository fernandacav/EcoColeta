import java.io.*;
import java.net.*;
import java.util.*;

public class EcoColetaServer {
    private static final int PORTA = 12345;
    // Torna a lista thread-safe para operações de escrita/leitura concorrentes
    // Embora List.of() crie a lista inicial, um ArrayList é necessário para
    // adicionar/remover
    private static List<PontoColeta> pontos = new ArrayList<>();

    // Credenciais simples para o administrador (RF-G01)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "12345";
    private static boolean autenticado = false;

    public static void main(String[] args) {

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

    public static boolean getAutenticado() {
        return autenticado;
    }

    // Lógica de Autenticação
    public static boolean autenticar(String user, String pass) {
        autenticado = ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass);
        return autenticado;
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
            // Retorna uma cópia da lista para segurança
            return new ArrayList<>(pontos);
        }
    }
}

// Thread para lidar com cada conexão de cliente (Alterada para Comandos)
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private List<PontoColeta> pontos;
    private boolean autenticado = false;

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

            if (comando.equals("ADMIN_LOGIN")) {
                String user = (String) ois.readObject();
                String pass = (String) ois.readObject();
                if (EcoColetaServer.autenticar(user, pass)) {
                    oos.writeObject("SUCESSO_AUTENTICACAO");
                    autenticado = true;
                    System.out.println("Administrador autenticado com sucesso.");
                } else {
                    oos.writeObject("FALHA_AUTENTICACAO");
                    System.out.println("Falha na autenticação do administrador.");
                }
                oos.flush();
                return; // Após autenticação, espera novo comando
            }

            // --- ADMINISTRADOR: LOGIN E GESTÃO ---
            if (comando.startsWith("ADMIN_") && !EcoColetaServer.getAutenticado()) {
                oos.writeObject("ERRO_AUTENTICACAO");
                System.out.println("Cliente tentou comando administrativo sem autenticar.");
                return;
            }

            // --- ADMIN: CADASTRAR (RF-A01) ---
            if (comando.equals("ADMIN_CADASTRAR")) {
                PontoColeta novoPonto = (PontoColeta) ois.readObject();
                if (EcoColetaServer.cadastrarPonto(novoPonto)) {
                    oos.writeObject(
                            "Ponto '" + novoPonto.getNome() + "' cadastrado com sucesso! ID: " + novoPonto.getId());
                    status = "SUCESSO_CADASTRO";
                } else {
                    oos.writeObject("ERRO: Falha ao cadastrar ponto.");
                }
            }

            // --- ADMIN: REMOVER (RF-A03) ---
            if (comando.equals("ADMIN_REMOVER")) {
                Long idParaRemover = (Long) ois.readObject();
                if (EcoColetaServer.removerPonto(idParaRemover)) {
                    oos.writeObject("Ponto com ID " + idParaRemover + " removido com sucesso.");
                    status = "SUCESSO_REMOCAO";
                } else {
                    oos.writeObject("ERRO: Nenhum ponto encontrado com ID " + idParaRemover + ".");
                }
            }

            // --- ADMIN: LISTAR TODOS ---
            if (comando.equals("ADMIN_LISTAR")) {
                oos.writeObject(EcoColetaServer.listarTodosPontos());
                status = "SUCESSO_LISTAGEM";
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