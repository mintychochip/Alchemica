package org.aincraft.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public interface Extractor {

  InputStream getResourceStream(String filePath) throws FileNotFoundException;

  boolean copyResourceFile(Path resourcePath, Path outPath);

  final class ResourceExtractor implements Extractor {

    public InputStream getResourceStream(String filePath) throws FileNotFoundException {
      ClassLoader loader = ResourceExtractor.class.getClassLoader();
      InputStream resourceAsStream = loader.getResourceAsStream(filePath);
      if (resourceAsStream == null) {
        throw new FileNotFoundException(filePath);
      }
      return resourceAsStream;
    }

    public boolean copyResourceFile(Path resourcePath, Path outPath) {
      ClassLoader loader = ResourceExtractor.class.getClassLoader();
      try (InputStream resourceAsStream = loader.getResourceAsStream(resourcePath.toString())) {
        if (resourceAsStream == null) {
          throw new FileNotFoundException(
              "unable to locate resource at path: %s".formatted(resourcePath));
        }
        Path parentDir = outPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
          Files.createDirectories(parentDir);
        }

        if (Files.isDirectory(outPath)) {
          return false;
        }

        if (!Files.exists(outPath)) {
          Files.copy(resourceAsStream, outPath, StandardCopyOption.REPLACE_EXISTING);
          return true;
        }

        return true;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
