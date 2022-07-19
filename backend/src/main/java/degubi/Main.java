package degubi;

import degubi.utils.*;
import java.io.*;
import java.nio.file.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication
public /*non-final*/ class Main {
    public static final String TASK_CACHE_FOLDER = "taskCache";

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        var cacheFolder = Path.of(TASK_CACHE_FOLDER);

        if(Files.exists(cacheFolder)) {
            IOUtils.deleteDirectory(cacheFolder);
        }

        Files.createDirectory(cacheFolder);
        SpringApplication.run(Main.class, args);
    }
}