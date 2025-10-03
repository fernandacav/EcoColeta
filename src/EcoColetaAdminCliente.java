import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class EcoColetaAdminCliente {
    private static final String HOST = "localhost";
    private static final int PORTA = 12345;
    private static Scanner scanner = new Scanner(System.in);
    private static boolean autenticado = false;

    // --- Funções de Ajuda ---
    private static PontoColeta solicitarDadosPonto() {
        System.out.print("Nome do Ponto: ");
        String nome = scanner.nextLine();
        System.out.print("Endereço: ");
        String endereco = scanner.nextLine();
        System.out.print("Materiais aceitos (separados por vírgula, Ex: Papel, Vidro, Metal): ");
        List<String> materiais = Arrays.asList(scanner.nextLine().split(","))
                .stream().map(String::trim).collect(Collectors.toList());
        return new PontoColeta(nome, endereco, materiais);
    }

    private static void listarPontos() {
        try (Socket socket = new Socket(HOST, PORTA);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // 1. Envia o comando de listar
            oos.writeObject("ADMIN_LISTAR");

            // 2. Envia as credenciais para autenticar
            if (!autenticado)
                return;

            // 3. Recebe a lista de pontos
            @SuppressWarnings("unchecked")
            List<PontoColeta> resultados = (List<PontoColeta>) ois.readObject();

            System.out.println("\n--- LISTA DE PONTOS DE COLETA CADASTRADOS ---");
            if (resultados.isEmpty()) {
                System.out.println("Nenhum ponto cadastrado.");
            } else {
                resultados.forEach(System.out::println);
            }
            System.out.println("----------------------------------------------");

        } catch (ConnectException e) {
            System.err.println("Erro: Servidor não está ativo.");
        } catch (Exception e) {
            System.err.println("Erro de comunicação ou dados: " + e.getMessage());
        }
    }

    // Rotina de Autenticação (RF-G01)
    private static boolean autenticar(ObjectOutputStream oos, ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        System.out.print("Usuário Admin: ");
        String user = scanner.nextLine();
        System.out.print("Senha Admin: ");
        String pass = scanner.nextLine();

        oos.writeObject("ADMIN_LOGIN");

        oos.writeObject(user);
        oos.writeObject(pass);
        oos.flush();

        String respostaAuth = (String) ois.readObject();
        if (respostaAuth.equals("SUCESSO_AUTENTICACAO")) {
            System.out.println("\nLogin ADMINISTRADOR bem-sucedido.");
            autenticado = true;
            return autenticado;
        } else {
            System.err.println("ERRO DE AUTENTICAÇÃO. Acesso negado.");
            return false;
        }
    }

    // --- Rotina de Cadastro (RF-A01) ---
    private static void cadastrarPonto() {
        try (Socket socket = new Socket(HOST, PORTA);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            System.out.println("\n--- OPÇÃO: CADASTRAR NOVO PONTO ---");

            // 1. Envia o comando
            oos.writeObject("ADMIN_CADASTRAR");

            if (!autenticado)
                return;

            // 3. Solicita e envia os dados do novo ponto
            PontoColeta novoPonto = solicitarDadosPonto();
            oos.writeObject(novoPonto);
            oos.flush();

            // 4. Recebe o feedback (RF-05)
            String feedback = (String) ois.readObject();
            System.out.println("\n[FEEDBACK DO SERVIDOR]: " + feedback);

        } catch (Exception e) {
            System.err.println("Erro durante o cadastro: " + e.getMessage());
        }
    }

    // --- Rotina de Remoção (RF-A03) ---
    private static void removerPonto() {
        listarPontos(); // Lista para o admin ver o ID que precisa

        System.out.print("\nDigite o ID do ponto de coleta que deseja REMOVER: ");

        try (Socket socket = new Socket(HOST, PORTA);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // 1. Envia o comando
            oos.writeObject("ADMIN_REMOVER");

            // 2. Tenta autenticar (RF-G01)
            if (!autenticado)
                return;

            // 3. Envia o ID
            long id = scanner.nextLong();
            scanner.nextLine(); // Consome o \n
            oos.writeObject(id);
            oos.flush();

            // 4. Recebe o feedback (RF-05)
            String feedback = (String) ois.readObject();
            System.out.println("\n[FEEDBACK DO SERVIDOR]: " + feedback);

        } catch (InputMismatchException e) {
            System.err.println("Erro: O ID deve ser um número inteiro.");
            scanner.nextLine(); // Limpa o buffer
        } catch (Exception e) {
            System.err.println("Erro durante a remoção: " + e.getMessage());
        }
    }

    // --- Menu Principal do Administrador ---
    public static void main(String[] args) {

        System.out.println("--- Sistema EcoColeta - Login Administrador ---");

        try (Socket socket = new Socket(HOST, PORTA);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            autenticar(oos, ois);
        } catch (Exception e) {
            System.err.println("Erro de comunicação ou dados: " + e.getMessage());
            return;
        }

        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n==================================");
            System.out.println("   EcoColeta - Painel Administrador");
            System.out.println("==================================");
            System.out.println("1. Listar todos os Pontos de Coleta");
            System.out.println("2. Cadastrar Novo Ponto (RF-A01)");
            System.out.println("3. Remover Ponto Existente (RF-A03)");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            try {
                opcao = scanner.nextInt();
                scanner.nextLine(); // Consumir nova linha

                switch (opcao) {
                    case 1:
                        listarPontos();
                        break;
                    case 2:
                        cadastrarPonto();
                        break;
                    case 3:
                        removerPonto();
                        break;
                    case 0:
                        System.out.println("Saindo do painel.");
                        break;
                    default:
                        System.out.println("Opção inválida.");
                }
            } catch (InputMismatchException e) {
                System.err.println("Entrada inválida. Digite um número.");
                scanner.nextLine(); // Limpar buffer
                opcao = -1;
            }
        }
    }
}