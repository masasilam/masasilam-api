package com.naskah.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Konfigurasi thread pool untuk operasi async.
 *
 * Mengapa perlu thread pool terpisah untuk epub rebuild:
 *  - Spring Boot punya default SimpleAsyncTaskExecutor yang membuat
 *    thread baru untuk setiap @Async call (tidak pooled)
 *  - Epub rebuild adalah operasi berat (baca DB + serialize + upload)
 *  - Jika pakai thread pool default, bisa interferensi dengan request lain
 *  - Dengan thread pool dedicated, max 2 rebuild berjalan bersamaan
 *    sisanya masuk antrian (queueCapacity = 10)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool khusus untuk epub rebuild.
     *
     * Konfigurasi:
     *  - corePoolSize  = 1 : normalnya hanya 1 rebuild berjalan
     *  - maxPoolSize   = 2 : maksimal 2 paralel (lonjakan singkat)
     *  - queueCapacity = 10: antrian maksimal 10 buku pending rebuild
     *    Jika antrian penuh dan maxPool tercapai → RejectedExecutionException
     *    (log sebagai error, tidak crash server)
     *
     * Referensi di @Async: @Async("rebuildExecutor")
     */
    @Bean("rebuildExecutor")
    public Executor rebuildExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("epub-rebuild-");

        // Saat shutdown, tunggu thread yang sedang berjalan selesai
        // (jangan potong di tengah upload Cloudinary)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // tunggu max 60 detik

        executor.initialize();
        return executor;
    }

    @Bean(name = "socialTaskExecutor")
    public Executor socialTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("social-async-");
        executor.setRejectedExecutionHandler(
                (r, e) -> {} // silently discard if queue full — notifications are non-critical
        );
        executor.initialize();
        return executor;
    }
}