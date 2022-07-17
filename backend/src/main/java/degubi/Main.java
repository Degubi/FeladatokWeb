package degubi;

import degubi.utils.*;
import java.io.*;
import java.nio.file.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication
public /*non-final*/ class Main {
    public static final int PROD_ENV = 1;
    public static final int DEV_ENV = 2;

    public static final int ENV = DEV_ENV;
    public static final String CACHE_FOLDER = "taskCache";

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        var cacheFolder = Path.of(CACHE_FOLDER);

        if(Files.exists(cacheFolder)) {
            IOUtils.deleteDirectory(cacheFolder);
        }

        Files.createDirectory(cacheFolder);
        SpringApplication.run(Main.class, args);
    }
}