package org.gitlucaslima.dtos;

public record NomeRecord(String nome) {

    @Override
    public String toString() {
        return "NomeRecord{" +
                "nome='" + nome + '\'' +
                '}';
    }
}
