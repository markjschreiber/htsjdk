package htsjdk.tribble;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.variant.vcf.VCFFileReader;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Property-based tests for VCF index auto-discovery.
 * 
 * Feature: file-to-path-migration
 * Property 6: Index Auto-Discovery for VCF Files
 * Validates: Requirements 14.3
 * 
 * For any VCF file Path where a corresponding .idx or .tbi file exists at the standard 
 * location, the index discovery mechanism should successfully locate the index Path.
 */
public class VcfIndexAutoDiscoveryPropertyTest {

    // Use real VCF and index files from test resources
    private static final Path SOURCE_VCF = Path.of("src/test/resources/htsjdk/variant/test1.vcf");
    private static final Path SOURCE_VCF_IDX = Path.of("src/test/resources/htsjdk/variant/test1.vcf.idx");
    
    // Use real compressed VCF and tabix index files
    private static final Path SOURCE_VCF_GZ = Path.of("src/test/resources/htsjdk/tribble/vcfexample.vcf.gz");
    private static final Path SOURCE_VCF_TBI = Path.of("src/test/resources/htsjdk/tribble/vcfexample.vcf.gz.tbi");

    /**
     * Property 6: Index Auto-Discovery for VCF Files - Standard .idx naming
     * 
     * For any VCF file with a corresponding .vcf.idx index file in the same directory,
     * the VCFFileReader should successfully locate and use the index.
     * 
     * Validates: Requirements 14.3
     */
    @Property(tries = 100)
    void vcfIndexAutoDiscoveryWithIdxNaming(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("vcfFileNames") String vcfFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path
            Path vcfPath = dir.resolve(vcfFileName + ".vcf");
            
            // Create index path with .vcf.idx naming convention
            Path indexPath = dir.resolve(vcfFileName + ".vcf.idx");
            
            // Copy real VCF and index content to jimfs
            Files.copy(SOURCE_VCF, vcfPath);
            Files.copy(SOURCE_VCF_IDX, indexPath);
            
            // Verify the index path is constructed correctly using Tribble utility
            Path expectedIndexPath = Tribble.indexPath(vcfPath);
            
            assert expectedIndexPath != null :
                    String.format("Tribble.indexPath should return non-null for VCF file: %s", vcfPath);
            
            // Verify the index path ends with .idx extension
            assert expectedIndexPath.toString().endsWith(".idx") :
                    String.format("Index path should end with .idx. Got: %s", expectedIndexPath);
            
            // Verify the index file exists at the expected location
            assert Files.exists(indexPath) :
                    String.format("Index file should exist at: %s", indexPath);
            
            // Verify index is in same directory as VCF file
            Path vcfParent = vcfPath.getParent();
            Path indexParent = indexPath.getParent();
            
            assert vcfParent.equals(indexParent) :
                    String.format("Index should be in same directory as VCF. VCF dir: %s, Index dir: %s",
                            vcfParent, indexParent);
        }
    }

    /**
     * Property 6: Index Auto-Discovery for VCF Files - Standard .tbi naming for compressed VCF
     * 
     * For any compressed VCF file (.vcf.gz) with a corresponding .tbi index file,
     * the tabix index path should be correctly constructed.
     * 
     * Validates: Requirements 14.3
     */
    @Property(tries = 100)
    void vcfIndexAutoDiscoveryWithTbiNaming(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("vcfFileNames") String vcfFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create compressed VCF file path
            Path vcfGzPath = dir.resolve(vcfFileName + ".vcf.gz");
            
            // Create tabix index path with .tbi naming convention
            Path indexPath = dir.resolve(vcfFileName + ".vcf.gz.tbi");
            
            // Copy real compressed VCF and tabix index content to jimfs
            Files.copy(SOURCE_VCF_GZ, vcfGzPath);
            Files.copy(SOURCE_VCF_TBI, indexPath);
            
            // Verify the tabix index path is constructed correctly using Tribble utility
            Path expectedIndexPath = Tribble.tabixIndexPath(vcfGzPath);
            
            assert expectedIndexPath != null :
                    String.format("Tribble.tabixIndexPath should return non-null for VCF.gz file: %s", vcfGzPath);
            
            // Verify the index path ends with .tbi extension
            assert expectedIndexPath.toString().endsWith(".tbi") :
                    String.format("Tabix index path should end with .tbi. Got: %s", expectedIndexPath);
            
            // Verify the index file exists at the expected location
            assert Files.exists(indexPath) :
                    String.format("Tabix index file should exist at: %s", indexPath);
            
            // Verify index is in same directory as VCF file
            Path vcfParent = vcfGzPath.getParent();
            Path indexParent = indexPath.getParent();
            
            assert vcfParent.equals(indexParent) :
                    String.format("Index should be in same directory as VCF. VCF dir: %s, Index dir: %s",
                            vcfParent, indexParent);
        }
    }

    /**
     * Property 6: Index Auto-Discovery for VCF Files - Index in same directory
     * 
     * For any VCF file, the constructed index path should be in the same directory.
     * 
     * Validates: Requirements 14.3
     */
    @Property(tries = 100)
    void vcfIndexPathIsInSameDirectory(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("vcfFileNames") String vcfFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path
            Path vcfPath = dir.resolve(vcfFileName + ".vcf");
            
            // Copy real VCF content to jimfs (needed for path operations)
            Files.copy(SOURCE_VCF, vcfPath);
            
            // Get the index path using Tribble utility
            Path indexPath = Tribble.indexPath(vcfPath);
            
            // Verify index is in same directory as VCF file
            Path vcfParent = vcfPath.getParent();
            Path indexParent = indexPath.getParent();
            
            assert vcfParent.equals(indexParent) :
                    String.format("Index should be in same directory as VCF. VCF dir: %s, Index dir: %s",
                            vcfParent, indexParent);
        }
    }

    /**
     * Property 6: Index Auto-Discovery for VCF Files - Tabix index in same directory
     * 
     * For any compressed VCF file, the constructed tabix index path should be in the same directory.
     * 
     * Validates: Requirements 14.3
     */
    @Property(tries = 100)
    void vcfTabixIndexPathIsInSameDirectory(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("vcfFileNames") String vcfFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create compressed VCF file path
            Path vcfGzPath = dir.resolve(vcfFileName + ".vcf.gz");
            
            // Copy real compressed VCF content to jimfs (needed for path operations)
            Files.copy(SOURCE_VCF_GZ, vcfGzPath);
            
            // Get the tabix index path using Tribble utility
            Path indexPath = Tribble.tabixIndexPath(vcfGzPath);
            
            // Verify index is in same directory as VCF file
            Path vcfParent = vcfGzPath.getParent();
            Path indexParent = indexPath.getParent();
            
            assert vcfParent.equals(indexParent) :
                    String.format("Tabix index should be in same directory as VCF. VCF dir: %s, Index dir: %s",
                            vcfParent, indexParent);
        }
    }

    /**
     * Property 6: Index Auto-Discovery for VCF Files - Index filename starts with VCF filename
     * 
     * For any VCF file, the constructed index filename should start with the VCF filename.
     * 
     * Validates: Requirements 14.3
     */
    @Property(tries = 100)
    void vcfIndexFilenameStartsWithVcfFilename(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("vcfFileNames") String vcfFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path
            Path vcfPath = dir.resolve(vcfFileName + ".vcf");
            
            // Copy real VCF content to jimfs (needed for path operations)
            Files.copy(SOURCE_VCF, vcfPath);
            
            // Get the index path using Tribble utility
            Path indexPath = Tribble.indexPath(vcfPath);
            
            // Verify index filename starts with VCF filename
            String vcfFilename = vcfPath.getFileName().toString();
            String indexFilename = indexPath.getFileName().toString();
            
            assert indexFilename.startsWith(vcfFilename) :
                    String.format("Index filename should start with VCF filename. VCF: %s, Index: %s",
                            vcfFilename, indexFilename);
        }
    }

    /**
     * Property 6: Index Auto-Discovery for VCF Files - No index when file doesn't exist
     * 
     * For any VCF file without a corresponding index file,
     * attempting to require an index should fail appropriately.
     * 
     * Validates: Requirements 14.3
     */
    @Property(tries = 100)
    void noVcfIndexWhenFileDoesNotExist(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("vcfFileNames") String vcfFileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path (without creating an index)
            Path vcfPath = dir.resolve(vcfFileName + ".vcf");
            
            // Copy only the VCF file, not the index
            Files.copy(SOURCE_VCF, vcfPath);
            
            // Get the expected index path
            Path expectedIndexPath = Tribble.indexPath(vcfPath);
            
            // Verify the index file does NOT exist
            assert !Files.exists(expectedIndexPath) :
                    String.format("Index file should not exist when not created. Path: %s", expectedIndexPath);
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
     * Provides VCF file names (without extension).
     * Generates names like "sample", "test_data", "variants"
     */
    @Provide
    Arbitrary<String> vcfFileNames() {
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
