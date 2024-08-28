/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.management.tests.acceptance.utils;

import com.ericsson.oss.management.tests.acceptance.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.tests.acceptance.exceptions.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.RESOURCES_DIR;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;

@Slf4j
public final class FileUtils {
    private FileUtils() {
    }

    public static Path copyFile(String sourcePath, String targetPath) {
        log.info("Will copy file {} to {}", sourcePath, targetPath);
        try {
            return Files.copy(Path.of(sourcePath), Path.of(targetPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(String.format("Something went wrong during copying file %s. Details: %s",
                    sourcePath, e.getMessage()), e);
            throw new InternalRuntimeException(String.format("Can't copy file from %s to %s", sourcePath, targetPath), e);
        }
    }

    public static Path createDirectory(String directoryName) {
        log.info(String.format("Creating directory with the given name %s", directoryName));
        try {
            var directory = Paths.get(RESOURCES_DIR).resolve(directoryName);
            return Files.createDirectory(directory);
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Failed to create directory with name %s. Details: %s",
                    directoryName, e.getMessage()));
        }
    }

    public static void deleteDirectory(final Path directory) {
        log.info("Will delete {}", directory);
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(reverseOrder()).forEach(FileUtils::deleteFile);
        } catch (IOException e) {
            throw new InternalRuntimeException("Failed to delete directory. Details: " + e.getMessage());
        }
    }

    public static FileSystemResource getFileResource(final String fileToLocate) {
        log.info("File to locate is {}", fileToLocate);
        var file = new File(fileToLocate);
        if (!file.exists()) {
            log.error("The file {} does not exist", file.getAbsolutePath());
            throw new IllegalArgumentException("Could not find this resource!");
        }
        if (!file.isFile()) {
            var classLoader = FileUtils.class.getClassLoader();
            file = new File(requireNonNull(classLoader.getResource(fileToLocate)).getFile());
        }

        return new FileSystemResource(file);
    }

    private static void deleteFile(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new InvalidFileException("Failed to delete file. Details: " + e.getMessage());
        }
    }
}

