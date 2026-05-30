package htsjdk.samtools;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Property-based tests for BAM index auto-discovery.
 * 
 * Feature: file-to-path-migration
 * Property 4: Index Auto-Discovery for BAM Files
 * Validates: Requirements 14.1
 * 
 * For any BAM file Path where a corresponding .bai file exists at the standard 
 * location (same name + .bai extension), the index discovery mechanism should 
 * successfully locate the index Path.
 */
public class BamIndexAutoDiscoveryPropertyTest {

    // Use a real BAM file from test resources for content
    private static final Path SOURCE_BAM = Path.of("src/test/resources/htsjdk/samtools/serialization_test.bam");
    private static final Path SOURCE_BAI = Path.of("src/test/resources/htsjdk/samtools/serialization_test.bam.bai");

    /**
     * Property 4: Index Auto-Discovery for BAM Files - Standard .bam.bai naming
     * 
     * For any BAM file with a corresponding .bam.bai index file in the same directory,
     * SamFiles.findIndex() should successfully locate the index.
     * 
     * Validates: Requirements 14.1
     */
    @Property(tries = 100)
    void bamIndexAutoDiscoveryWithBamBaiNaming(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("bamFileNames") String bamFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path
            Path bamPath = dir.resolve(bamFileName + ".bam");
            
            // Create index path with .bam.bai naming convention
            Path indexPath = dir.resolve(bamFileName + ".bam.bai");
            
            // Copy real BAM and BAI content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, indexPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for BAM file: %s", bamPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist: %s", discoveredIndex);
            
            assert discoveredIndex.equals(indexPath) :
                    String.format("Discovered index should match expected. Expected: %s, Found: %s",
                            indexPath, discoveredIndex);
        }
    }

    /**
     * Property 4: Index Auto-Discovery for BAM Files - Standard .bai naming
     * 
     * For any BAM file with a corresponding .bai index file (without .bam extension)
     * in the same directory, SamFiles.findIndex() should successfully locate the index.
     * 
     * Validates: Requirements 14.1
     */
    @Property(tries = 100)
    void bamIndexAutoDiscoveryWithBaiNaming(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("bamFileNames") String bamFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path
            Path bamPath = dir.resolve(bamFileName + ".bam");
            
            // Create index path with .bai naming convention (file.bai instead of file.bam.bai)
            Path indexPath = dir.resolve(bamFileName + ".bai");
            
            // Copy real BAM and BAI content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, indexPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for BAM file: %s", bamPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist: %s", discoveredIndex);
            
            assert discoveredIndex.equals(indexPath) :
                    String.format("Discovered index should match expected. Expected: %s, Found: %s",
                            indexPath, discoveredIndex);
        }
    }

    /**
     * Property 4: Index Auto-Discovery for BAM Files - Index in same directory
     * 
     * For any discovered BAM index, it should be in the same directory as the BAM file.
     * 
     * Validates: Requirements 14.1
     */
    @Property(tries = 100)
    void discoveredIndexIsInSameDirectory(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("bamFileNames") String bamFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path
            Path bamPath = dir.resolve(bamFileName + ".bam");
            
            // Create index path
            Path indexPath = dir.resolve(bamFileName + ".bam.bai");
            
            // Copy real BAM and BAI content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, indexPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for BAM file: %s", bamPath);
            
            // Verify index is in same directory as BAM file
            Path bamParent = bamPath.getParent();
            Path indexParent = discoveredIndex.getParent();
            
            assert bamParent.equals(indexParent) :
                    String.format("Index should be in same directory as BAM. BAM dir: %s, Index dir: %s",
                            bamParent, indexParent);
        }
    }

    /**
     * Property 4: Index Auto-Discovery for BAM Files - No index returns null
     * 
     * For any BAM file without a corresponding index file,
     * SamFiles.findIndex() should return null.
     * 
     * Validates: Requirements 14.1
     */
    @Property(tries = 100)
    void noIndexReturnsNull(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("bamFileNames") String bamFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path (without creating an index)
            Path bamPath = dir.resolve(bamFileName + ".bam");
            
            // Copy only the BAM file, not the index
            Files.copy(SOURCE_BAM, bamPath);
            
            // Verify no index is discovered
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex == null :
                    String.format("No index should be discovered when index file doesn't exist. " +
                            "BAM: %s, Found index: %s", bamPath, discoveredIndex);
        }
    }

    /**
     * Property 4: Index Auto-Discovery for BAM Files - Prefers .bai over .bam.bai
     * 
     * When both .bai and .bam.bai index files exist, the discovery mechanism
     * should prefer the .bai naming convention (file.bai over file.bam.bai).
     * 
     * Validates: Requirements 14.1
     */
    @Property(tries = 100)
    void prefersBaiOverBamBai(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("bamFileNames") String bamFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path
            Path bamPath = dir.resolve(bamFileName + ".bam");
            
            // Create both index naming conventions
            Path baiPath = dir.resolve(bamFileName + ".bai");
            Path bamBaiPath = dir.resolve(bamFileName + ".bam.bai");
            
            // Copy real BAM and BAI content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, baiPath);
            Files.copy(SOURCE_BAI, bamBaiPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for BAM file: %s", bamPath);
            
            // The .bai naming should be preferred
            assert discoveredIndex.equals(baiPath) :
                    String.format("Should prefer .bai over .bam.bai. Expected: %s, Found: %s",
                            baiPath, discoveredIndex);
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

    /**
     * Provides BAM file names (without extension).
     * Generates names like "sample", "test_data", "reads"
     */
    @Provide
    Arbitrary<String> bamFileNames() {
        // Use alphanumeric characters and underscores for valid file names
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                           'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                           'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                           'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                           '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_')
                .ofMinLength(1)
                .ofMaxLength(20);
    }
}
