package degubi.utils;

import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.zip.*;

public final class IOUtils {
    private static final ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static<T> T parseJsonObject(byte[] data, Class<T> type) {
        try {
            return json.readValue(data, type);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unable to parse json data...\n" + new String(data));
        }
    }

    public static boolean doFilesMismatch(String file1, String file2) {
        try {
            return Files.mismatch(Path.of(file1), Path.of(file2)) != -1;
        } catch (IOException e) {
            return true;
        }
    }

    public static byte[] readAllBytesFromFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteDirectory(Path workDir) {
        try(var toDelete = Files.walk(workDir)) {
            toDelete.sorted(Comparator.reverseOrder())
                    .forEach(k -> {
                        try {
                            Files.delete(k);
                        } catch (IOException e) {}
                    });
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static boolean runProcessWithTimeout(Process process) {
        try {
            var completed = process.waitFor(5, TimeUnit.SECONDS);
            if(!completed) {
                process.destroy();
            }

            return completed;
        } catch (InterruptedException e) {
            throw new IllegalStateException("WTF Happened here?");
        }
    }

    public static String getProcessOutputFrom(InputStream stream) {
        try {
            return new String(stream.readAllBytes()).strip();
        } catch (IOException e) {
            throw new IllegalStateException("WTF Happened here?");
        }
    }

    public static void copyFile(Path from, Path to) {
        try {
            Files.copy(from, to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Boolean, List<String>> getAllZipEntryNamesTruncated(byte[] zipBytes) {
        try(var input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            return Stream.generate(() -> getNextEntry(input))
                         .takeWhile(Objects::nonNull)
                         .collect(Collectors.partitioningBy(k -> k.isDirectory(), Collectors.mapping(k -> k.getName().substring(17), Collectors.toList())));

        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public static byte[] getZipEntry(byte[] zipBytes, String entryName) {
        try(var asd = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            return Stream.generate(() -> getNextEntry(asd))
                         .filter(k -> k.getName().endsWith(entryName))
                         .findFirst()
                         .map(k -> {
                             try {
                                 return asd.readNBytes((int) k.getSize());
                             } catch (IOException e1) {
                                 e1.printStackTrace();
                                 return null;
                             }
                         })
                         .orElseThrow();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void extractZipEntry(Path destinationFolder, String entryName, byte[] zipBytes) {
        var entryData = IOUtils.getZipEntry(zipBytes, entryName);

        try {
            Files.write(Path.of(destinationFolder + "/" + entryName.substring(entryName.lastIndexOf('/') + 1)), entryData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ZipEntry getNextEntry(ZipInputStream stream) {
        try {
            return stream.getNextEntry();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private IOUtils() {}
}