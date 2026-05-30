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
 * Property-based tests for NIO SPI compatibility with Jimfs.
 * 
 * Feature: file-to-path-migration
 * Property 7: NIO SPI Compatibility with Jimfs
 * Validates: Requirements 9.3, 10.3
 * 
 * For any valid file created in a jimfs (in-memory) filesystem, HTSJDK readers 
 * and writers should be able to successfully open, read from, and write to that 
 * file using Path-based APIs.
 */
public class NioSpiJimfsCompatibilityPropertyTest {

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
     * Property 7: NIO SPI Compatibility with Jimfs - BAM Reading
     * 
     * For any BAM file copied to a jimfs filesystem, HTSJDK should be able to
     * successfully open and read records from the file using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void bamReadingWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in jimfs
            Path bamPath = dir.resolve(fileName + ".bam");
            Path baiPath = dir.resolve(fileName + ".bam.bai");
            
            // Copy real BAM and BAI content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, baiPath);
            
            // Open and read using HTSJDK
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(bamPath)) {
                
                // Verify reader is not null
                assert reader != null :
                        String.format("SamReader should not be null for BAM file: %s", bamPath);
                
                // Verify we can read records
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                    // Verify record is valid
                    assert record != null :
                            String.format("SAMRecord should not be null");
                }
                
                // Verify we read at least some records
                assert recordCount > 0 :
                        String.format("Should read at least one record from BAM file: %s", bamPath);
            }
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - BAM Writing
     * 
     * For any BAM file written to a jimfs filesystem, HTSJDK should be able to
     * successfully write records and then read them back.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void bamWritingWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
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
            
            // Write to jimfs
            Path outputBamPath = dir.resolve(fileName + ".bam");
            try (SAMFileWriter writer = new SAMFileWriterFactory()
                    .makeBAMWriter(header, true, outputBamPath)) {
                for (SAMRecord record : originalRecords) {
                    writer.addAlignment(record);
                }
            }
            
            // Verify file was written
            assert Files.exists(outputBamPath) :
                    String.format("BAM file should exist after writing: %s", outputBamPath);
            
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
                        String.format("Should read same number of records. Expected: %d, Got: %d",
                                originalRecords.size(), recordCount);
            }
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - SAM Reading
     * 
     * For any SAM file copied to a jimfs filesystem, HTSJDK should be able to
     * successfully open and read records from the file using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void samReadingWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create SAM file path in jimfs
            Path samPath = dir.resolve(fileName + ".sam");
            
            // Copy real SAM content to jimfs
            Files.copy(SOURCE_SAM, samPath);
            
            // Open and read using HTSJDK
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(samPath)) {
                
                // Verify reader is not null
                assert reader != null :
                        String.format("SamReader should not be null for SAM file: %s", samPath);
                
                // Verify we can read records
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                    assert record != null :
                            String.format("SAMRecord should not be null");
                }
                
                // Verify we read at least some records
                assert recordCount > 0 :
                        String.format("Should read at least one record from SAM file: %s", samPath);
            }
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - SAM Writing
     * 
     * For any SAM file written to a jimfs filesystem, HTSJDK should be able to
     * successfully write records and then read them back.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void samWritingWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // First, read records from source SAM
            List<SAMRecord> originalRecords = new ArrayList<>();
            SAMFileHeader header;
            try (SamReader sourceReader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(SOURCE_SAM)) {
                header = sourceReader.getFileHeader();
                for (SAMRecord record : sourceReader) {
                    originalRecords.add(record);
                }
            }
            
            // Write to jimfs
            Path outputSamPath = dir.resolve(fileName + ".sam");
            try (SAMFileWriter writer = new SAMFileWriterFactory()
                    .makeSAMWriter(header, true, outputSamPath)) {
                for (SAMRecord record : originalRecords) {
                    writer.addAlignment(record);
                }
            }
            
            // Verify file was written
            assert Files.exists(outputSamPath) :
                    String.format("SAM file should exist after writing: %s", outputSamPath);
            
            // Read back and verify
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(outputSamPath)) {
                
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                }
                
                // Verify we read the same number of records
                assert recordCount == originalRecords.size() :
                        String.format("Should read same number of records. Expected: %d, Got: %d",
                                originalRecords.size(), recordCount);
            }
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - VCF Reading
     * 
     * For any VCF file copied to a jimfs filesystem, HTSJDK should be able to
     * successfully open and read variants from the file using Path-based APIs.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void vcfReadingWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create VCF file path in jimfs
            Path vcfPath = dir.resolve(fileName + ".vcf");
            Path idxPath = Tribble.indexPath(vcfPath);
            
            // Copy real VCF and index content to jimfs
            Files.copy(SOURCE_VCF, vcfPath);
            Files.copy(SOURCE_VCF_IDX, idxPath);
            
            // Open and read using HTSJDK
            try (VCFFileReader reader = new VCFFileReader(vcfPath, false)) {
                
                // Verify reader is not null
                assert reader != null :
                        String.format("VCFFileReader should not be null for VCF file: %s", vcfPath);
                
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
                        String.format("Should read at least one variant from VCF file: %s", vcfPath);
            }
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - CRAM Reading with Reference
     * 
     * For any CRAM file copied to a jimfs filesystem with its reference,
     * HTSJDK should be able to successfully open and read records.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void cramReadingWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create CRAM file path in jimfs
            Path cramPath = dir.resolve(fileName + ".cram");
            Path craiPath = dir.resolve(fileName + ".cram.crai");
            
            // Copy real CRAM and CRAI content to jimfs
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
                        String.format("SamReader should not be null for CRAM file: %s", cramPath);
                
                // Verify we can read records
                int recordCount = 0;
                for (SAMRecord record : reader) {
                    recordCount++;
                    assert record != null :
                            String.format("SAMRecord should not be null");
                }
                
                // Verify we read at least some records
                assert recordCount > 0 :
                        String.format("Should read at least one record from CRAM file: %s", cramPath);
            }
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - Index Discovery
     * 
     * For any BAM file with index copied to jimfs, the index discovery
     * mechanism should work correctly.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void indexDiscoveryWorksOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in jimfs
            Path bamPath = dir.resolve(fileName + ".bam");
            Path baiPath = dir.resolve(fileName + ".bam.bai");
            
            // Copy real BAM and BAI content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            Files.copy(SOURCE_BAI, baiPath);
            
            // Verify index discovery works
            Path discoveredIndex = SamFiles.findIndex(bamPath);
            
            assert discoveredIndex != null :
                    String.format("Index should be discovered for BAM file: %s", bamPath);
            
            assert Files.exists(discoveredIndex) :
                    String.format("Discovered index should exist: %s", discoveredIndex);
            
            // Verify the discovered index is in the same directory
            assert bamPath.getParent().equals(discoveredIndex.getParent()) :
                    String.format("Index should be in same directory as BAM. BAM dir: %s, Index dir: %s",
                            bamPath.getParent(), discoveredIndex.getParent());
        }
    }

    /**
     * Property 7: NIO SPI Compatibility with Jimfs - File Operations
     * 
     * For any file in jimfs, basic file operations (exists, size, etc.)
     * should work correctly with HTSJDK utilities.
     * 
     * Validates: Requirements 9.3, 10.3
     */
    @Property(tries = 100)
    void fileOperationsWorkOnJimfs(
            @ForAll("directoryPaths") String directoryPath,
            @ForAll("fileNames") String fileName) throws IOException {
        
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            // Create directory structure
            Path dir = fs.getPath(directoryPath);
            Files.createDirectories(dir);
            
            // Create BAM file path in jimfs
            Path bamPath = dir.resolve(fileName + ".bam");
            
            // Copy real BAM content to jimfs
            Files.copy(SOURCE_BAM, bamPath);
            
            // Verify file operations work
            assert Files.exists(bamPath) :
                    String.format("File should exist: %s", bamPath);
            
            assert Files.isReadable(bamPath) :
                    String.format("File should be readable: %s", bamPath);
            
            assert Files.size(bamPath) > 0 :
                    String.format("File should have non-zero size: %s", bamPath);
            
            // Verify directory operations work
            assert Files.isDirectory(dir) :
                    String.format("Directory should exist: %s", dir);
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
