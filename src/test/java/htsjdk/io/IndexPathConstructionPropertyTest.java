package htsjdk.io;

import net.jqwik.api.*;

import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * Property-based tests for index path construction.
 * 
 * Feature: file-to-path-migration
 * Property 3: Index Path Construction
 * Validates: Requirements 14.5
 * 
 * For any data file Path, constructing the index Path by appending the appropriate 
 * extension should produce a Path in the same filesystem and parent directory as 
 * the data file.
 */
public class IndexPathConstructionPropertyTest {

    /**
     * Property 3: Index Path Construction - Same Filesystem
     * 
     * For any data file Path, constructing an index Path by appending an extension
     * should produce a Path in the same filesystem as the data file.
     * 
     * Validates: Requirements 14.5
     */
    @Property(tries = 100)
    void indexPathIsInSameFilesystem(
            @ForAll("dataFilePaths") Path dataPath,
            @ForAll("indexExtensions") String indexExtension) {
        
        // Construct index path by appending extension
        Path indexPath = Path.of(dataPath.toString() + indexExtension);
        
        // Verify same filesystem
        FileSystem dataFileSystem = dataPath.getFileSystem();
        FileSystem indexFileSystem = indexPath.getFileSystem();
        
        assert dataFileSystem.equals(indexFileSystem) :
                String.format("Index path should be in same filesystem. " +
                        "Data filesystem: %s, Index filesystem: %s",
                        dataFileSystem, indexFileSystem);
    }

    /**
     * Property 3: Index Path Construction - Same Parent Directory
     * 
     * For any data file Path, constructing an index Path by appending an extension
     * should produce a Path in the same parent directory as the data file.
     * 
     * Validates: Requirements 14.5
     */
    @Property(tries = 100)
    void indexPathIsInSameParentDirectory(
            @ForAll("dataFilePaths") Path dataPath,
            @ForAll("indexExtensions") String indexExtension) {
        
        // Construct index path by appending extension
        Path indexPath = Path.of(dataPath.toString() + indexExtension);
        
        // Verify same parent directory
        Path dataParent = dataPath.getParent();
        Path indexParent = indexPath.getParent();
        
        assert dataParent.equals(indexParent) :
                String.format("Index path should have same parent directory. " +
                        "Data parent: %s, Index parent: %s",
                        dataParent, indexParent);
    }

    /**
     * Property 3: Index Path Construction - Filename Prefix Preserved
     * 
     * For any data file Path, constructing an index Path by appending an extension
     * should produce a Path whose filename starts with the data file's filename.
     * 
     * Validates: Requirements 14.5
     */
    @Property(tries = 100)
    void indexPathFilenameStartsWithDataFilename(
            @ForAll("dataFilePaths") Path dataPath,
            @ForAll("indexExtensions") String indexExtension) {
        
        // Construct index path by appending extension
        Path indexPath = Path.of(dataPath.toString() + indexExtension);
        
        // Verify filename prefix is preserved
        String dataFileName = dataPath.getFileName().toString();
        String indexFileName = indexPath.getFileName().toString();
        
        assert indexFileName.startsWith(dataFileName) :
                String.format("Index filename should start with data filename. " +
                        "Data filename: %s, Index filename: %s",
                        dataFileName, indexFileName);
    }

    /**
     * Property 3: Index Path Construction - Extension Appended
     * 
     * For any data file Path, constructing an index Path by appending an extension
     * should produce a Path whose filename ends with the index extension.
     * 
     * Validates: Requirements 14.5
     */
    @Property(tries = 100)
    void indexPathFilenameEndsWithExtension(
            @ForAll("dataFilePaths") Path dataPath,
            @ForAll("indexExtensions") String indexExtension) {
        
        // Construct index path by appending extension
        Path indexPath = Path.of(dataPath.toString() + indexExtension);
        
        // Verify extension is appended
        String indexFileName = indexPath.getFileName().toString();
        
        assert indexFileName.endsWith(indexExtension) :
                String.format("Index filename should end with extension. " +
                        "Extension: %s, Index filename: %s",
                        indexExtension, indexFileName);
    }

    /**
     * Property 3: Index Path Construction - Using resolveSibling
     * 
     * For any data file Path, constructing an index Path using resolveSibling
     * should produce a Path in the same parent directory as the data file.
     * This tests the alternative construction method used in SamFiles.
     * 
     * Validates: Requirements 14.5
     */
    @Property(tries = 100)
    void indexPathUsingResolveSiblingIsInSameDirectory(
            @ForAll("dataFilePaths") Path dataPath,
            @ForAll("indexExtensions") String indexExtension) {
        
        // Construct index path using resolveSibling (as used in SamFiles)
        String indexFileName = dataPath.getFileName().toString() + indexExtension;
        Path indexPath = dataPath.resolveSibling(indexFileName);
        
        // Verify same parent directory
        Path dataParent = dataPath.getParent();
        Path indexParent = indexPath.getParent();
        
        assert dataParent.equals(indexParent) :
                String.format("Index path via resolveSibling should have same parent. " +
                        "Data parent: %s, Index parent: %s",
                        dataParent, indexParent);
        
        // Verify same filesystem
        assert dataPath.getFileSystem().equals(indexPath.getFileSystem()) :
                String.format("Index path via resolveSibling should be in same filesystem.");
    }

    /**
     * Property 3: Index Path Construction - Absolute Path Preserved
     * 
     * For any absolute data file Path, constructing an index Path should
     * also produce an absolute Path.
     * 
     * Validates: Requirements 14.5
     */
    @Property(tries = 100)
    void absoluteDataPathProducesAbsoluteIndexPath(
            @ForAll("absoluteDataFilePaths") Path dataPath,
            @ForAll("indexExtensions") String indexExtension) {
        
        // Construct index path by appending extension
        Path indexPath = Path.of(dataPath.toString() + indexExtension);
        
        // Verify absolute nature is preserved
        assert dataPath.isAbsolute() :
                "Test precondition: data path should be absolute";
        
        assert indexPath.isAbsolute() :
                String.format("Index path should be absolute when data path is absolute. " +
                        "Data path: %s, Index path: %s",
                        dataPath, indexPath);
    }

    /**
     * Provides data file paths for testing.
     * Generates paths like /data/dir/file.bam, /data/file.vcf, etc.
     */
    @Provide
    Arbitrary<Path> dataFilePaths() {
        return Combinators.combine(
                directoryNames().list().ofMinSize(1).ofMaxSize(3),
                fileBaseNames(),
                dataFileExtensions()
        ).as((dirs, baseName, ext) -> {
            StringBuilder pathStr = new StringBuilder("/");
            for (int i = 0; i < dirs.size(); i++) {
                pathStr.append(dirs.get(i));
                pathStr.append("/");
            }
            pathStr.append(baseName).append(ext);
            return Path.of(pathStr.toString());
        });
    }

    /**
     * Provides absolute data file paths for testing.
     * Same as dataFilePaths but explicitly absolute.
     */
    @Provide
    Arbitrary<Path> absoluteDataFilePaths() {
        return dataFilePaths().filter(Path::isAbsolute);
    }

    /**
     * Provides directory names for path construction.
     */
    @Provide
    Arbitrary<String> directoryNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
    }

    /**
     * Provides file base names (without extension).
     */
    @Provide
    Arbitrary<String> fileBaseNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(15);
    }

    /**
     * Provides data file extensions for genomic files.
     */
    @Provide
    Arbitrary<String> dataFileExtensions() {
        return Arbitraries.of(
                ".bam",
                ".sam",
                ".cram",
                ".vcf",
                ".vcf.gz",
                ".bcf",
                ".bed",
                ".gff"
        );
    }

    /**
     * Provides index file extensions.
     */
    @Provide
    Arbitrary<String> indexExtensions() {
        return Arbitraries.of(
                ".bai",      // BAM index
                ".csi",      // CSI index
                ".crai",     // CRAM index
                ".idx",      // Tribble index
                ".tbi",      // Tabix index
                ".sbi"       // SBI index
        );
    }
}
