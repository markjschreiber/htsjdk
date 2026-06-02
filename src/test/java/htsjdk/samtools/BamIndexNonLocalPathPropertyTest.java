package htsjdk.samtools;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Property-based test for BAM index non-local path handling.
 *
 * Feature: bam-index-stream-fallback
 * Property 1: Bug Condition - Non-Local Path Throws on BAM Index Open
 * Validates: Requirements 1.1, 1.2, 2.1, 2.2
 *
 * This test encodes the EXPECTED behavior after the fix: non-local paths
 * should open successfully via a stream-based buffer fallback.
 *
 * On UNFIXED code, these tests are expected to FAIL with IOException or
 * UnsupportedOperationException because MemoryMappedFileBuffer and
 * RandomAccessFileBuffer cannot operate on non-local (Jimfs) paths.
 */
public class BamIndexNonLocalPathPropertyTest {

    private static final Path SOURCE_BAI = Path.of("src/test/resources/htsjdk/samtools/BAMFileIndexTest/index_test.bam.bai");

    /**
     * Property 1: Bug Condition - CachingBAMFileIndex with memory-mapped mode (default)
     *
     * For any non-local path containing a valid .bai file, CachingBAMFileIndex(path, dictionary)
     * should open successfully and allow index queries.
     *
     * On unfixed code: FAILS because MemoryMappedFileBuffer cannot map a Jimfs path.
     *
     * Validates: Requirements 1.1, 2.1
     */
    @Property(tries = 10)
    void cachingBAMFileIndexMemoryMappedOpensOnNonLocalPath(
            @ForAll("directoryPaths") String directoryPath) throws IOException {

        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);

            Path jimfsBai = dir.resolve("index_test.bam.bai");
            Files.copy(SOURCE_BAI, jimfsBai);

            // Bug condition: path is on a non-default file system
            assert !jimfsBai.getFileSystem().equals(java.nio.file.FileSystems.getDefault()) :
                    "Test setup error: path should be on a non-local file system";

            // Expected behavior (post-fix): opens successfully
            CachingBAMFileIndex index = new CachingBAMFileIndex(jimfsBai, null);
            try {
                // Verify we can query the index (proves it opened correctly)
                int numRefs = index.getNumberOfReferences();
                assert numRefs >= 0 :
                        String.format("Index should report valid number of references, got: %d", numRefs);
            } finally {
                index.close();
            }
        }
    }

    /**
     * Property 1: Bug Condition - CachingBAMFileIndex with non-memory-mapped mode
     *
     * For any non-local path containing a valid .bai file,
     * CachingBAMFileIndex(path, dictionary, false) should open successfully.
     *
     * On unfixed code: FAILS because RandomAccessFileBuffer uses FileChannel.open()
     * which is not supported on Jimfs paths.
     *
     * Validates: Requirements 1.2, 2.2
     */
    @Property(tries = 10)
    void cachingBAMFileIndexNonMemoryMappedOpensOnNonLocalPath(
            @ForAll("directoryPaths") String directoryPath) throws IOException {

        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);

            Path jimfsBai = dir.resolve("index_test.bam.bai");
            Files.copy(SOURCE_BAI, jimfsBai);

            // Bug condition: path is on a non-default file system
            assert !jimfsBai.getFileSystem().equals(java.nio.file.FileSystems.getDefault()) :
                    "Test setup error: path should be on a non-local file system";

            // Expected behavior (post-fix): opens successfully even with useMemoryMapping=false
            CachingBAMFileIndex index = new CachingBAMFileIndex(jimfsBai, null, false);
            try {
                int numRefs = index.getNumberOfReferences();
                assert numRefs >= 0 :
                        String.format("Index should report valid number of references, got: %d", numRefs);
            } finally {
                index.close();
            }
        }
    }

    /**
     * Property 1: Bug Condition - DiskBasedBAMFileIndex on non-local path
     *
     * For any non-local path containing a valid .bai file,
     * DiskBasedBAMFileIndex(path, dictionary) should open successfully.
     *
     * On unfixed code: FAILS because the constructor delegates to
     * AbstractBAMFileIndex(path, dictionary) which uses MemoryMappedFileBuffer.
     *
     * Validates: Requirements 1.1, 2.1
     */
    @Property(tries = 10)
    void diskBasedBAMFileIndexOpensOnNonLocalPath(
            @ForAll("directoryPaths") String directoryPath) throws IOException {

        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);

            Path jimfsBai = dir.resolve("index_test.bam.bai");
            Files.copy(SOURCE_BAI, jimfsBai);

            // Bug condition: path is on a non-default file system
            assert !jimfsBai.getFileSystem().equals(java.nio.file.FileSystems.getDefault()) :
                    "Test setup error: path should be on a non-local file system";

            // Expected behavior (post-fix): opens successfully
            DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(jimfsBai, null);
            try {
                int numRefs = index.getNumberOfReferences();
                assert numRefs >= 0 :
                        String.format("Index should report valid number of references, got: %d", numRefs);
            } finally {
                index.close();
            }
        }
    }

    /**
     * Property 1: Bug Condition - IndexFileBufferFactory.getBuffer on non-local path
     *
     * For any non-local path containing a valid .bai file,
     * IndexFileBufferFactory.getBuffer(path, true) should return a valid buffer.
     *
     * On unfixed code: FAILS because the factory unconditionally creates
     * MemoryMappedFileBuffer or RandomAccessFileBuffer, both requiring local paths.
     *
     * Validates: Requirements 1.1, 1.2, 2.1, 2.2
     */
    @Property(tries = 10)
    void indexFileBufferFactoryOpensOnNonLocalPath(
            @ForAll("directoryPaths") String directoryPath) throws IOException {

        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);

            Path jimfsBai = dir.resolve("index_test.bam.bai");
            Files.copy(SOURCE_BAI, jimfsBai);

            // Bug condition: path is on a non-default file system
            assert !jimfsBai.getFileSystem().equals(java.nio.file.FileSystems.getDefault()) :
                    "Test setup error: path should be on a non-local file system";

            // Expected behavior (post-fix): factory returns a valid buffer
            IndexFileBuffer buffer = IndexFileBufferFactory.getBuffer(jimfsBai, true);
            try {
                // Verify we can read from the buffer (proves it opened correctly)
                long pos = buffer.position();
                assert pos >= 0 : "Buffer should have a valid position";
            } finally {
                buffer.close();
            }
        }
    }

    /**
     * Provides directory paths for testing.
     * Generates paths like /data, /data/subdir, /data/subdir/nested
     */
    @Provide
    Arbitrary<String> directoryPaths() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10)
                .list()
                .ofMinSize(1)
                .ofMaxSize(3)
                .map(dirs -> "/" + String.join("/", dirs));
    }
}
