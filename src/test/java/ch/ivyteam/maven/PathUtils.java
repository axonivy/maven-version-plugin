package ch.ivyteam.maven;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

class PathUtils {

  public static void copyFolder(Path source, Path target)
          throws IOException {

      CustomFileVisitor customFileVisitor = new CustomFileVisitor(source, target, StandardCopyOption.REPLACE_EXISTING);
      Files.walkFileTree(source, customFileVisitor);
  }

  private static class CustomFileVisitor extends SimpleFileVisitor<Path> {

    private Path source;
    private Path target;
    private CopyOption[] options;

    private CustomFileVisitor(Path source, Path target, CopyOption... options) {
        this.target = target;
        this.source = source;
        this.options = options;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
        Files.createDirectories(target.resolve(source.relativize(dir)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {
        Files.copy(file, target.resolve(source.relativize(file)), options);
        return FileVisitResult.CONTINUE;
    }
}
}
