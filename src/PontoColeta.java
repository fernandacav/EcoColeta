import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

class PontoColeta implements Serializable {
    private static final long serialVersionUID = 1L;
    // Contador estático para gerar IDs únicos (simples, sem persistência)
    private static final AtomicLong counter = new AtomicLong(0); 
    
    private final long id; // ID é final, só pode ser setado na criação
    private String nome;
    private String endereco;
    private List<String> materiaisAceitos;

    public PontoColeta(String nome, String endereco, List<String> materiaisAceitos) {
        this.id = counter.incrementAndGet(); // Gera o próximo ID
        this.nome = nome;
        this.endereco = endereco;
        this.materiaisAceitos = materiaisAceitos;
    }

    // Getters
    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public List<String> getMateriaisAceitos() { return materiaisAceitos; }

    // SETTERS (Essenciais para a função de ATUALIZAR, se fosse implementada)
    public void setNome(String nome) { this.nome = nome; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public void setMateriaisAceitos(List<String> materiaisAceitos) { this.materiaisAceitos = materiaisAceitos; }

    @Override
    public String toString() {
        return "ID: " + id + ", Nome: " + nome + ", Endereço: " + endereco + ", Aceita: " + String.join(", ", materiaisAceitos);
    }
}