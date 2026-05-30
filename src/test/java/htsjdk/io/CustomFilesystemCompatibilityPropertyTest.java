package htsjdk.io;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import htsjdk.samtools.*;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.tribble.Tribble;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Property-based tests for NIO SPI compatibility with custom filesystems.
 * 
 * Feature: file-to-path-migration
 * Property 8: NIO SPI Compatibility with Custom Filesystems
 * Validates: Requirements 9.3, 10.4, 14.6
 * 
 * For any custom filesystem implementation that provides a valid NIO SPI provider,
 * HTSJDK should be able to operate on Paths from that filesystem without modification.
 * This includes index discovery, reader operations, and writer operations.
 */
public class CustomFilesystemCompatibilityPropertyTest {

    // Source test files from test resources
    private static final Path SOURCE_BAM = Path.of("src/test/resources/htsjdk/samtools/serialization_test.bam");
    private static final Path SOURCE_BAI = Path.of("src/test/resources/htsjdk/samtools/serialization_test.bam.bai");
    private static final Path SOURCE_SAM = Path.of("src/test/resources/htsjdk/samtools/coordinate_sorted.sam");
    
    // CRAM files with reference
    private static final Path SOURCE_CRAM = Path.of("src/test/resources/htsjdk/samtools/cram_with_crai_index.cram");
    private static final Path SOURCE_CRAI = Path.of("src/test/resources/htsjdk/samtools/cram_with_crai_index.cram.crai");
    private static final Path SOURCE_CRAM_REF = Path.of("src/test/resources/htsjdk/samtools/hg19mini.fasta");
    
    // VCF files
    private static final Path SOURCE_VCF = Path.of("src/test/resources/htsjdk/variant/test1.vcf");
    private static final Path SOURCE_VCF_IDX = Path.of("src/test/resources/htsjdk/variant/test1.vcf.idx");

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - BAM Index Discovery
     * 
     * For any BAM file with index in a custom filesystem (jimfs), the index discovery
     * mechanism should successfully locate the index using Path-based operations.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void bamIndexDiscoveryWorksInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in custom filesystem
            Path bamPath = dir.resolve(fileName + ".bam");
            Path baiPath = dir.resolve(fileName + ".bam.bai");
            
            // Copy real BAM and BAI content to custom filesystem
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, baiPath);
            
            // Verify index discovery works in custom filesystem
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered in custom filesystem for BAM file: %s", bamPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist in custom filesystem: %s", discoveredIndex);
            
            // Verify the discovered index is in the same filesystem
            assert discoveredIndex.getFileSystem().equals(fs) :
                    String.format("Discovered index should be in the same custom filesystem");
            
            // Verify the discovered index is in the same directory
            assert bamPath.getParent().equals(discoveredIndex.getParent()) :
                    String.format("Index should be in same directory as BAM. BAM dir: %s, Index dir: %s",
                            bamPath.getParent(), discoveredIndex.getParent());
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - CRAM Index Discovery
     * 
     * For any CRAM file with index in a custom filesystem (jimfs), the index discovery
     * mechanism should successfully locate the index using Path-based operations.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void cramIndexDiscoveryWorksInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path in custom filesystem
            Path cramPath = dir.resolve(fileName + ".cram");
            Path craiPath = dir.resolve(fileName + ".cram.crai");
            
            // Copy real CRAM and CRAI content to custom filesystem
            Files.copy(SOURCE_CRAM, cramPath);
            Files.copy(SOURCE_CRAI, craiPath);
            
            // Verify index discovery works in custom filesystem
            Path discoveredIndex = SamFiles.findIndex(cramPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered in custom filesystem for CRAM file: %s", cramPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist in custom filesystem: %s", discoveredIndex);
            
            // Verify the discovered index is in the same filesystem
            assert discoveredIndex.getFileSystem().equals(fs) :
                    String.format("Discovered index should be in the same custom filesystem");
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - VCF Index Path Construction
     * 
     * For any VCF file in a custom filesystem (jimfs), the Tribble index path
     * construction should work correctly using Path-based operations.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void vcfIndexPathConstructionWorksInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path in custom filesystem
            Path vcfPath = dir.resolve(fileName + ".vcf");
            
            // Copy real VCF content to custom filesystem
            Files.copy(SOURCE_VCF, vcfPath);
            
            // Verify index path construction works in custom filesystem
            Path indexPath = Tribble.indexPath(vcfPath);
            
            assert indexPath != null :
                    String.format("Index path should be constructed for VCF file: %s", vcfPath);
            
            // Verify the index path is in the same filesystem
            assert indexPath.getFileSystem().equals(fs) :
                    String.format("Index path should be in the same custom filesystem");
            
            // Verify the index path is in the same directory
            assert vcfPath.getParent().equals(indexPath.getParent()) :
                    String.format("Index should be in same directory as VCF. VCF dir: %s, Index dir: %s",
                            vcfPath.getParent(), indexPath.getParent());
            
            // Verify the index path has correct extension
            assert indexPath.toString().endsWith(".idx") :
                    String.format("Index path should end with .idx. Got: %s", indexPath);
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - BAM Read Operations
     * 
     * For any BAM file in a custom filesystem (jimfs), HTSJDK should be able to
     * successfully open and read records using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void bamReadOperationsWorkInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in custom filesystem
            Path bamPath = dir.resolve(fileName + ".bam");
            Path baiPath = dir.resolve(fileName + ".bam.bai");
            
            // Copy real BAM and BAI content to custom filesystem
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, baiPath);
            
            // Open and read using HTSJDK in custom filesystem
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(bamPath)) {
                
                // Verify reader is not null
                assert reader != null :
                        String.format("SamReader should not be null for BAM file in custom filesystem: %s", bamPath);
                
                // Verify we can read records
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                    assert record != null :
                            String.format("SAMRecord should not be null");
                }
                
                // Verify we read at least some records
                assert recordCount > 0 :
                        String.format("Should read at least one record from BAM file in custom filesystem: %s", bamPath);
            }
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - BAM Write Operations
     * 
     * For any BAM file written to a custom filesystem (jimfs), HTSJDK should be able to
     * successfully write records and then read them back.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void bamWriteOperationsWorkInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // First, read records from source BAM
            List<SAMRecord> originalRecords = new ArrayList<>();
            SAMFileHeader header;
            try (SamReader sourceReader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(SOURCE_BAM)) {
                header = sourceReader.getFileHeader();
                for (SAMRecord record : sourceReader) {
                    originalRecords.add(record);
                }
            }
            
            // Write to custom filesystem
            Path outputBamPath = dir.resolve(fileName + ".bam");
            try (SAMFileWriter writer = new SAMFileWriterFactory()
                    .makeBAMWriter(header, true, outputBamPath)) {
                for (SAMRecord record : originalRecords) {
                    writer.addAlignment(record);
                }
            }
            
            // Verify file was written in custom filesystem
            assert Files.exists(outputBamPath) :
                    String.format("BAM file should exist after writing in custom filesystem: %s", outputBamPath);
            
            // Verify file is in the custom filesystem
            assert outputBamPath.getFileSystem().equals(fs) :
                    String.format("Written BAM file should be in the custom filesystem");
            
            // Read back and verify
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(outputBamPath)) {
                
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                }
                
                // Verify we read the same number of records
                assert recordCount == originalRecords.size() :
                        String.format("Should read same number of records in custom filesystem. Expected: %d, Got: %d",
                                originalRecords.size(), recordCount);
            }
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - SAM Read/Write Operations
     * 
     * For any SAM file in a custom filesystem (jimfs), HTSJDK should be able to
     * successfully read and write records using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void samReadWriteOperationsWorkInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create SAM file path in custom filesystem
            Path samPath = dir.resolve(fileName + ".sam");
            
            // Copy real SAM content to custom filesystem
            Files.copy(SOURCE_SAM, samPath);
            
            // Read from custom filesystem
            List<SAMRecord> records = new ArrayList<>();
            SAMFileHeader header;
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(samPath)) {
                
                header = reader.getFileHeader();
                for (SAMRecord record : reader) {
                    records.add(record);
                }
            }
            
            assert records.size() > 0 :
                    String.format("Should read at least one record from SAM file in custom filesystem: %s", samPath);
            
            // Write to a new file in custom filesystem
            Path outputSamPath = dir.resolve(fileName + "_output.sam");
            try (SAMFileWriter writer = new SAMFileWriterFactory()
                    .makeSAMWriter(header, true, outputSamPath)) {
                for (SAMRecord record : records) {
                    writer.addAlignment(record);
                }
            }
            
            // Verify file was written
            assert Files.exists(outputSamPath) :
                    String.format("SAM file should exist after writing in custom filesystem: %s", outputSamPath);
            
            // Read back and verify count
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(outputSamPath)) {
                
                int readBackCount = 0;
                for (SAMRecord record : reader) {
                    readBackCount++;
                }
                
                assert readBackCount == records.size() :
                        String.format("Should read same number of records. Expected: %d, Got: %d",
                                records.size(), readBackCount);
            }
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - VCF Read Operations
     * 
     * For any VCF file in a custom filesystem (jimfs), HTSJDK should be able to
     * successfully open and read variants using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void vcfReadOperationsWorkInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path in custom filesystem
            Path vcfPath = dir.resolve(fileName + ".vcf");
            Path idxPath = Tribble.indexPath(vcfPath);
            
            // Copy real VCF and index content to custom filesystem
            Files.copy(SOURCE_VCF, vcfPath);
            Files.copy(SOURCE_VCF_IDX, idxPath);
            
            // Open and read using HTSJDK in custom filesystem
            try (VCFFileReader reader = new VCFFileReader(vcfPath, false)) {
                
                // Verify reader is not null
                assert reader != null :
                        String.format("VCFFileReader should not be null for VCF file in custom filesystem: %s", vcfPath);
                
                // Verify we can get header
                VCFHeader header = reader.getFileHeader();
                assert header != null :
                        String.format("VCF header should not be null");
                
                // Verify we can read variants
                int variantCount = 0;
                for (VariantContext vc : reader) {
                    variantCount++;
                    assert vc != null :
                            String.format("VariantContext should not be null");
                }
                
                // Verify we read at least some variants
                assert variantCount > 0 :
                        String.format("Should read at least one variant from VCF file in custom filesystem: %s", vcfPath);
            }
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - CRAM Read Operations
     * 
     * For any CRAM file in a custom filesystem (jimfs), HTSJDK should be able to
     * successfully open and read records using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void cramReadOperationsWorkInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path in custom filesystem
            Path cramPath = dir.resolve(fileName + ".cram");
            Path craiPath = dir.resolve(fileName + ".cram.crai");
            
            // Copy real CRAM and CRAI content to custom filesystem
            Files.copy(SOURCE_CRAM, cramPath);
            Files.copy(SOURCE_CRAI, craiPath);
            
            // Open and read using HTSJDK with reference from local filesystem
            // (Reference stays on local filesystem as it's typically large)
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .referenceSource(new ReferenceSource(SOURCE_CRAM_REF))
                    .validationStringency(ValidationStringency.SILENT)
                    .open(cramPath)) {
                
                // Verify reader is not null
                assert reader != null :
                        String.format("SamReader should not be null for CRAM file in custom filesystem: %s", cramPath);
                
                // Verify we can read records
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                    assert record != null :
                            String.format("SAMRecord should not be null");
                }
                
                // Verify we read at least some records
                assert recordCount > 0 :
                        String.format("Should read at least one record from CRAM file in custom filesystem: %s", cramPath);
            }
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - File Operations
     * 
     * For any file in a custom filesystem (jimfs), basic file operations
     * should work correctly with HTSJDK utilities.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void fileOperationsWorkInCustomFilesystem(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in custom filesystem
            Path bamPath = dir.resolve(fileName + ".bam");
            
            // Copy real BAM content to custom filesystem
            Files.copy(SOURCE_BAM, bamPath);
            
            // Verify file operations work in custom filesystem
            assert Files.exists(bamPath) :
                    String.format("File should exist in custom filesystem: %s", bamPath);
            
            assert Files.isReadable(bamPath) :
                    String.format("File should be readable in custom filesystem: %s", bamPath);
            
            assert Files.size(bamPath) > 0 :
                    String.format("File should have non-zero size in custom filesystem: %s", bamPath);
            
            // Verify directory operations work in custom filesystem
            assert Files.isDirectory(dir) :
                    String.format("Directory should exist in custom filesystem: %s", dir);
            
            // Verify file type detection works in custom filesystem
            assert SamFiles.isBAMFile(bamPath) :
                    String.format("SamFiles.isBAMFile should return true for BAM file: %s", bamPath);
        }
    }

    /**
     * Property 8: NIO SPI Compatibility with Custom Filesystems - Cross-Filesystem Index Discovery
     * 
     * For any BAM file in a custom filesystem, index discovery should work
     * even when the index is in a different location within the same filesystem.
     * 
     * Validates: Requirements 9.3, 10.4, 14.6
     */
    @Property(tries = 100)
    void indexDiscoveryWorksWithDifferentNamingConventions(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure in custom filesystem
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in custom filesystem
            Path bamPath = dir.resolve(fileName + ".bam");
            
            // Create index with .bai naming convention (file.bai instead of file.bam.bai)
            Path baiPath = dir.resolve(fileName + ".bai");
            
            // Copy real BAM and BAI content to custom filesystem
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, baiPath);
            
            // Verify index discovery works with .bai naming convention
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered with .bai naming in custom filesystem: %s", bamPath);
            
            assert discoveredIndex.equals(baiPath) :
                    String.format("Discovered index should match expected. Expected: %s, Found: %s",
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
     * Provides file names (without extension).
     * Generates names like "sample", "test_data", "reads"
     */
    @Provide
    Arbitrary<String> fileNames() {
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
