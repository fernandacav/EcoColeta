import java.io.Serializable;
import java.util.List;

// A classe precisa implementar Serializable para ser enviada via Sockets
class PontoColeta implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nome;
    private String endereco;
    private List<String> materiaisAceitos;

    public PontoColeta(String nome, String endereco, List<String> materiaisAceitos) {
        this.nome = nome;
        this.endereco = endereco;
        this.materiaisAceitos = materiaisAceitos;
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public List<String> getMateriaisAceitos() { return materiaisAceitos; }

    @Override
    public String toString() {
        return "Nome: " + nome + ", Endereço: " + endereco + ", Aceita: " + String.join(", ", materiaisAceitos);
    }
    
    // Método para checar se aceita um material
    public boolean aceitaMaterial(String material) {
        return materiaisAceitos.contains(material);
    }
}