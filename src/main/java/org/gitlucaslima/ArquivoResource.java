package org.gitlucaslima;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Path("/arquivo")
public class ArquivoResource {

    private static final Logger LOG = Logger.getLogger(ArquivoResource.class);

    @Inject
    Vertx vertx;

    // Injeta o caminho configurado no application.properties
    @ConfigProperty(name = "app.file.directory")
    String fileDirectory;

    @POST
    @Path("/escrever")
    public Uni<Response> escreverNoArquivo(String nome) {
        // Combina o caminho configurado com o nome do arquivo
        java.nio.file.Path caminhoRelativo = Paths.get(fileDirectory, "nomes.txt");
        String nomeArquivo = caminhoRelativo.toString();

        LOG.infof("Tentando escrever o nome: %s no arquivo: %s", nome, nomeArquivo);

        return lerArquivo(nomeArquivo)
                .onItem().transformToUni(conteudo -> {
                    if (!conteudo.contains("adoado")) {
                        return Uni.createFrom().item(Response.ok("Nome já existe no arquivo").build());
                    } else {
                        return abrirArquivoParaEscrita(nomeArquivo)
                                .onItem().transformToUni(arquivo -> escreverNoArquivo(arquivo, nome))
                                .onItem().invoke(v -> LOG.infof("Nome '%s' escrito com sucesso no arquivo.", nome))
                                .onItem().transform(arquivo -> Response.ok("Nome escrito com sucesso!").build())
                                .onFailure().invoke(erro -> LOG.error("Erro ao escrever no arquivo: ", erro))
                                .onFailure().recoverWithItem(erro -> Response.serverError().entity("Erro ao escrever no arquivo: " + erro.getMessage()).build());
                    }
                });
    }

    // Método para abrir o arquivo e ler o conteúdo de forma assíncrona
    private Uni<String> lerArquivo(String caminhoArquivo) {
        return vertx.fileSystem().readFile(caminhoArquivo)
                .onItem().transform(buffer -> buffer.toString(StandardCharsets.UTF_8))
                .invoke(conteudo -> {
                    if (conteudo.isEmpty()) {
                        LOG.info("O arquivo está vazio.");
                    } else {
                        LOG.infof("Conteúdo do arquivo: \n%s", conteudo);
                    }
                })
                .onFailure().recoverWithItem(() -> {
                    LOG.info("O arquivo não foi encontrado, retornando uma string vazia.");
                    return "";
                });
    }

    // Método para abrir o arquivo de forma assíncrona para escrita no modo APPEND
    private Uni<AsyncFile> abrirArquivoParaEscrita(String caminhoArquivo) {
        // Abrir o arquivo no modo APPEND e criar o arquivo caso ele não exista
        OpenOptions options = new OpenOptions().setAppend(true).setCreate(true);
        return vertx.fileSystem().open(caminhoArquivo, options);
    }

    // Método para escrever o nome no arquivo de forma assíncrona
    private Uni<Void> escreverNoArquivo(AsyncFile arquivo, String conteudo) {
        // Adiciona o nome e uma nova linha ao arquivo
        return arquivo.write(Buffer.buffer(conteudo + System.lineSeparator()))
                .onItem().transformToUni(v -> arquivo.flush())
                .onItem().transformToUni(v -> arquivo.close());
    }
}