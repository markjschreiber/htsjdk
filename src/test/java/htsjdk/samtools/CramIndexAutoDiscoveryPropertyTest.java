package htsjdk.samtools;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Property-based tests for CRAM index auto-discovery.
 * 
 * Feature: file-to-path-migration
 * Property 5: Index Auto-Discovery for CRAM Files
 * Validates: Requirements 14.2
 * 
 * For any CRAM file Path where a corresponding .crai file exists at the standard 
 * location (same name + .crai extension), the index discovery mechanism should 
 * successfully locate the index Path.
 */
public class CramIndexAutoDiscoveryPropertyTest {

    // Use real CRAM and CRAI files from test resources
    private static final Path SOURCE_CRAM = Path.of("src/test/resources/htsjdk/samtools/cram_with_crai_index.cram");
    private static final Path SOURCE_CRAI = Path.of("src/test/resources/htsjdk/samtools/cram_with_crai_index.cram.crai");

    /**
     * Property 5: Index Auto-Discovery for CRAM Files - Standard .cram.crai naming
     * 
     * For any CRAM file with a corresponding .cram.crai index file in the same directory,
     * SamFiles.findIndex() should successfully locate the index.
     * 
     * Validates: Requirements 14.2
     */
    @Property(tries = 100)
    void cramIndexAutoDiscoveryWithCramCraiNaming(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("cramFileNames") String cramFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path
            Path cramPath = dir.resolve(cramFileName + ".cram");
            
            // Create index path with .cram.crai naming convention
            Path indexPath = dir.resolve(cramFileName + ".cram.crai");
            
            // Copy real CRAM and CRAI content to jimfs
            Files.copy(SOURCE_CRAM, cramPath);
            Files.copy(SOURCE_CRAI, indexPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(cramPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for CRAM file: %s", cramPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist: %s", discoveredIndex);
            
            assert discoveredIndex.equals(indexPath) :
                    String.format("Discovered index should match expected. Expected: %s, Found: %s",
                            indexPath, discoveredIndex);
        }
    }

    /**
     * Property 5: Index Auto-Discovery for CRAM Files - Standard .crai naming
     * 
     * For any CRAM file with a corresponding .crai index file (without .cram extension)
     * in the same directory, SamFiles.findIndex() should successfully locate the index.
     * 
     * Validates: Requirements 14.2
     */
    @Property(tries = 100)
    void cramIndexAutoDiscoveryWithCraiNaming(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("cramFileNames") String cramFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path
            Path cramPath = dir.resolve(cramFileName + ".cram");
            
            // Create index path with .crai naming convention (file.crai instead of file.cram.crai)
            Path indexPath = dir.resolve(cramFileName + ".crai");
            
            // Copy real CRAM and CRAI content to jimfs
            Files.copy(SOURCE_CRAM, cramPath);
            Files.copy(SOURCE_CRAI, indexPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(cramPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for CRAM file: %s", cramPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist: %s", discoveredIndex);
            
            assert discoveredIndex.equals(indexPath) :
                    String.format("Discovered index should match expected. Expected: %s, Found: %s",
                            indexPath, discoveredIndex);
        }
    }

    /**
     * Property 5: Index Auto-Discovery for CRAM Files - Index in same directory
     * 
     * For any discovered CRAM index, it should be in the same directory as the CRAM file.
     * 
     * Validates: Requirements 14.2
     */
    @Property(tries = 100)
    void discoveredCramIndexIsInSameDirectory(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("cramFileNames") String cramFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path
            Path cramPath = dir.resolve(cramFileName + ".cram");
            
            // Create index path
            Path indexPath = dir.resolve(cramFileName + ".cram.crai");
            
            // Copy real CRAM and CRAI content to jimfs
            Files.copy(SOURCE_CRAM, cramPath);
            Files.copy(SOURCE_CRAI, indexPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(cramPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for CRAM file: %s", cramPath);
            
            // Verify index is in same directory as CRAM file
            Path cramParent = cramPath.getParent();
            Path indexParent = discoveredIndex.getParent();
            
            assert cramParent.equals(indexParent) :
                    String.format("Index should be in same directory as CRAM. CRAM dir: %s, Index dir: %s",
                            cramParent, indexParent);
        }
    }

    /**
     * Property 5: Index Auto-Discovery for CRAM Files - No index returns null
     * 
     * For any CRAM file without a corresponding index file,
     * SamFiles.findIndex() should return null.
     * 
     * Validates: Requirements 14.2
     */
    @Property(tries = 100)
    void noCramIndexReturnsNull(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("cramFileNames") String cramFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path (without creating an index)
            Path cramPath = dir.resolve(cramFileName + ".cram");
            
            // Copy only the CRAM file, not the index
            Files.copy(SOURCE_CRAM, cramPath);
            
            // Verify no index is discovered
            Path discoveredIndex = SamFiles.findIndex(cramPath);
            
            assert discoveredIndex == null :
                    String.format("No index should be discovered when index file doesn't exist. " +
                            "CRAM: %s, Found index: %s", cramPath, discoveredIndex);
        }
    }

    /**
     * Property 5: Index Auto-Discovery for CRAM Files - Prefers .crai over .cram.crai
     * 
     * When both .crai and .cram.crai index files exist, the discovery mechanism
     * should prefer the .crai naming convention (file.crai over file.cram.crai).
     * 
     * Validates: Requirements 14.2
     */
    @Property(tries = 100)
    void prefersCraiOverCramCrai(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("cramFileNames") String cramFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path
            Path cramPath = dir.resolve(cramFileName + ".cram");
            
            // Create both index naming conventions
            Path craiPath = dir.resolve(cramFileName + ".crai");
            Path cramCraiPath = dir.resolve(cramFileName + ".cram.crai");
            
            // Copy real CRAM and CRAI content to jimfs
            Files.copy(SOURCE_CRAM, cramPath);
            Files.copy(SOURCE_CRAI, craiPath);
            Files.copy(SOURCE_CRAI, cramCraiPath);
            
            // Verify the index is discovered
            Path discoveredIndex = SamFiles.findIndex(cramPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for CRAM file: %s", cramPath);
            
            // The .crai naming should be preferred
            assert discoveredIndex.equals(craiPath) :
                    String.format("Should prefer .crai over .cram.crai. Expected: %s, Found: %s",
                            craiPath, discoveredIndex);
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
     * Provides CRAM file names (without extension).
     * Generates names like "sample", "test_data", "reads"
     */
    @Provide
    Arbitrary<String> cramFileNames() {
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
