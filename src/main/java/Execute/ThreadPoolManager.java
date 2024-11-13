package Execute;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {

    private final ExecutorService threadPool;

    private int numThreads=5;

    // Construtor para inicializar o pool com o número de threads desejado
    public ThreadPoolManager() {
        this.threadPool = Executors.newFixedThreadPool(numThreads);
    }

    // Método para submeter uma tarefa de download ou pesquisa
    public void submitTask(Runnable task) {
        threadPool.submit(task);
    }

    // Método para encerrar a thread pool de forma controlada
    public void shutdown() {
        threadPool.shutdown();
        try {
            // Aguarda que as tarefas existentes terminem num limite de tempo
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); // Cancela as tarefas em execução
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool de threads não encerrou!");
            }
        } catch (InterruptedException ie) {
            //Para Garatnir que encerra vai tentar novamente
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
