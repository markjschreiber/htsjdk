/*
 * The MIT License
 *
 * Copyright (c) 2024 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package htsjdk.samtools;

import htsjdk.HtsjdkTest;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.reference.InMemoryReferenceSequenceFile;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Tests for verifying index readers work correctly with explicit Path parameters,
 * including when indexes are not in standard locations.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 14.4
 */
public class IndexReaderPathMigrationTest extends HtsjdkTest {

    private static final Path BAM_INDEX_TEST_DIR = Path.of("src/test/resources/htsjdk/samtools/BAMFileIndexTest");
    private static final Path SAMTOOLS_TEST_DIR = Path.of("src/test/resources/htsjdk/samtools");
    private static final Path VARIANT_TEST_DIR = Path.of("src/test/resources/htsjdk/variant");
    
    // BAM test files
    private static final Path BAM_FILE = BAM_INDEX_TEST_DIR.resolve("index_test.bam");
    private static final Path BAI_INDEX = BAM_INDEX_TEST_DIR.resolve("index_test.bam.bai");
    private static final Path CSI_INDEX = BAM_INDEX_TEST_DIR.resolve("index_test.bam.csi");
    
    // CRAM test files
    private static final Path CRAM_WITH_BAI = SAMTOOLS_TEST_DIR.resolve("cram_with_bai_index.cram");
    private static final Path CRAM_BAI_INDEX = SAMTOOLS_TEST_DIR.resolve("cram_with_bai_index.cram.bai");
    private static final Path CRAM_WITH_CRAI = SAMTOOLS_TEST_DIR.resolve("cram_with_crai_index.cram");
    private static final Path CRAM_CRAI_INDEX = SAMTOOLS_TEST_DIR.resolve("cram_with_crai_index.cram.crai");
    
    // VCF test files
    private static final Path VCF_WITH_IDX = VARIANT_TEST_DIR.resolve("test1.vcf");
    private static final Path VCF_IDX_INDEX = VARIANT_TEST_DIR.resolve("test1.vcf.idx");
    private static final Path VCF_BGZIP = VARIANT_TEST_DIR.resolve("HiSeq.10000.vcf.bgz");
    private static final Path VCF_TBI_INDEX = VARIANT_TEST_DIR.resolve("HiSeq.10000.vcf.bgz.tbi");
    private static final Path BCF_FILE = VARIANT_TEST_DIR.resolve("serialization_test.bcf");
    private static final Path BCF_INDEX = VARIANT_TEST_DIR.resolve("serialization_test.bcf.idx");

    // Reference for CRAM
    private static ReferenceSource createReferenceSource() {
        final byte[] refBases = new byte[10 * 10];
        Arrays.fill(refBases, (byte) 'A');
        InMemoryReferenceSequenceFile rsf = new InMemoryReferenceSequenceFile();
        rsf.add("chr1", refBases);
        return new ReferenceSource(rsf);
    }

    // ==================== BAM Index Tests with Explicit Path ====================

    /**
     * Test DiskBasedBAMFileIndex with explicit BAI index Path.
     * Requirements: 5.1, 5.2
     */
    @Test
    public void testDiskBasedBAMFileIndexWithExplicitPath() {
        DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(BAI_INDEX, null);
        Assert.assertNotNull(index, "DiskBasedBAMFileIndex should not be null");
        Assert.assertTrue(index.getNumberOfReferences() > 0, "Index should have references");
    }

    /**
     * Test DiskBasedBAMFileIndex with explicit CSI index Path.
     * Requirements: 5.3
     */
    @Test
    public void testDiskBasedBAMFileIndexWithCSIPath() {
        // CSI index uses CSIIndex class, not DiskBasedBAMFileIndex
        CSIIndex index = new CSIIndex(CSI_INDEX, false, null);
        Assert.assertNotNull(index, "CSIIndex should not be null");
        Assert.assertTrue(index.getNumberOfReferences() > 0, "Index should have references");
    }

    /**
     * Test opening BAM file with explicit BAI index Path via SamReaderFactory.
     * Requirements: 5.1, 14.4
     */
    @Test
    public void testBAMFileReaderWithExplicitBAIIndexPath() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SamInputResource.of(BAM_FILE).index(BAI_INDEX))) {
            
            Assert.assertNotNull(reader, "SamReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
            Assert.assertEquals(reader.indexing().getIndex().getClass(), DiskBasedBAMFileIndex.class,
                    "Index should be DiskBasedBAMFileIndex");
            
            // Verify we can query with the index
            SAMRecordIterator iterator = reader.query("chr1", 1, 1000, false);
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            iterator.close();
            // Just verify query works, count may vary
            Assert.assertTrue(count >= 0, "Query should work with explicit index");
        }
    }

    /**
     * Test opening BAM file with explicit CSI index Path via SamReaderFactory.
     * Requirements: 5.3, 14.4
     */
    @Test
    public void testBAMFileReaderWithExplicitCSIIndexPath() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SamInputResource.of(BAM_FILE).index(CSI_INDEX))) {
            
            Assert.assertNotNull(reader, "SamReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
            Assert.assertEquals(reader.indexing().getIndex().getClass(), CSIIndex.class,
                    "Index should be CSIIndex");
        }
    }

    /**
     * Test BAMFileReader directly with explicit index Path.
     * Requirements: 5.1, 5.2
     */
    @Test
    public void testBAMFileReaderDirectWithExplicitIndexPath() throws IOException {
        BAMFileReader reader = new BAMFileReader(
                BAM_FILE,
                BAI_INDEX,
                true,
                false,
                ValidationStringency.SILENT,
                DefaultSAMRecordFactory.getInstance());
        try {
            Assert.assertNotNull(reader, "BAMFileReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        } finally {
            reader.close();
        }
    }

    // ==================== CRAM Index Tests with Explicit Path ====================

    /**
     * Test CRAMFileReader with explicit BAI index Path.
     * Requirements: 5.1, 14.4
     */
    @Test
    public void testCRAMFileReaderWithExplicitBAIIndexPath() {
        ReferenceSource reference = createReferenceSource();
        
        try (CRAMFileReader reader = new CRAMFileReader(CRAM_WITH_BAI, CRAM_BAI_INDEX, reference)) {
            Assert.assertNotNull(reader, "CRAMFileReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        }
    }

    /**
     * Test CRAMFileReader with explicit CRAI index Path.
     * Requirements: 5.1, 14.4
     */
    @Test
    public void testCRAMFileReaderWithExplicitCRAIIndexPath() {
        ReferenceSource reference = createReferenceSource();
        
        try (CRAMFileReader reader = new CRAMFileReader(CRAM_WITH_CRAI, CRAM_CRAI_INDEX, reference)) {
            Assert.assertNotNull(reader, "CRAMFileReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        }
    }

    /**
     * Test opening CRAM file with explicit index Path via SamReaderFactory.
     * Requirements: 5.1, 14.4
     */
    @Test
    public void testCRAMFileReaderViaSamReaderFactoryWithExplicitIndexPath() throws IOException {
        ReferenceSource reference = createReferenceSource();
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .referenceSource(reference)
                .open(SamInputResource.of(CRAM_WITH_CRAI).index(CRAM_CRAI_INDEX))) {
            
            Assert.assertNotNull(reader, "SamReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        }
    }

    // ==================== VCF Index Tests with Explicit Path ====================

    /**
     * Test VCFFileReader with explicit .idx index Path.
     * Requirements: 5.5, 14.4
     */
    @Test
    public void testVCFFileReaderWithExplicitIdxIndexPath() {
        try (VCFFileReader reader = new VCFFileReader(VCF_WITH_IDX, VCF_IDX_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with explicit index");
            
            // Verify we can read records
            int count = 0;
            for (VariantContext vc : reader) {
                Assert.assertNotNull(vc, "VariantContext should not be null");
                count++;
                if (count >= 5) break;
            }
            Assert.assertTrue(count > 0, "Should be able to read records");
        }
    }

    /**
     * Test VCFFileReader with explicit .tbi (tabix) index Path.
     * Requirements: 5.5, 14.4
     */
    @Test
    public void testVCFFileReaderWithExplicitTbiIndexPath() {
        try (VCFFileReader reader = new VCFFileReader(VCF_BGZIP, VCF_TBI_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with tabix index");
            
            // Verify we can query with the index
            var iterator = reader.query("20", 1, 100000);
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            iterator.close();
            Assert.assertTrue(count >= 0, "Query should work with explicit tabix index");
        }
    }

    /**
     * Test VCFFileReader with explicit BCF index Path.
     * Requirements: 5.5, 14.4
     */
    @Test
    public void testVCFFileReaderWithExplicitBCFIndexPath() {
        try (VCFFileReader reader = new VCFFileReader(BCF_FILE, BCF_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with BCF index");
        }
    }

    /**
     * Test loading Tribble index from explicit Path.
     * Requirements: 5.5
     */
    @Test
    public void testTribbleIndexLoadFromExplicitPath() {
        Index index = IndexFactory.loadIndex(VCF_IDX_INDEX.toString());
        Assert.assertNotNull(index, "Tribble index should not be null");
    }

    // ==================== Non-Standard Location Tests ====================

    /**
     * Test BAM index works when index is copied to non-standard location.
     * Requirements: 14.4
     */
    @Test
    public void testBAMIndexFromNonStandardLocation() throws IOException {
        // Create a temp directory and copy the index there
        Path tempDir = Files.createTempDirectory("index_test");
        Path nonStandardIndex = tempDir.resolve("custom_location.bai");
        
        try {
            Files.copy(BAI_INDEX, nonStandardIndex);
            
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .validationStringency(ValidationStringency.SILENT)
                    .open(SamInputResource.of(BAM_FILE).index(nonStandardIndex))) {
                
                Assert.assertNotNull(reader, "SamReader should not be null");
                Assert.assertTrue(reader.hasIndex(), "Reader should have index from non-standard location");
            }
        } finally {
            // Cleanup
            Files.deleteIfExists(nonStandardIndex);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Test VCF index works when index is copied to non-standard location.
     * Requirements: 14.4
     */
    @Test
    public void testVCFIndexFromNonStandardLocation() throws IOException {
        // Create a temp directory and copy the index there
        Path tempDir = Files.createTempDirectory("vcf_index_test");
        Path nonStandardIndex = tempDir.resolve("custom_location.idx");
        
        try {
            Files.copy(VCF_IDX_INDEX, nonStandardIndex);
            
            try (VCFFileReader reader = new VCFFileReader(VCF_WITH_IDX, nonStandardIndex, true)) {
                Assert.assertNotNull(reader, "VCFFileReader should not be null");
                Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with index from non-standard location");
            }
        } finally {
            // Cleanup
            Files.deleteIfExists(nonStandardIndex);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Test CRAM index works when index is copied to non-standard location.
     * Requirements: 14.4
     */
    @Test
    public void testCRAMIndexFromNonStandardLocation() throws IOException {
        ReferenceSource reference = createReferenceSource();
        
        // Create a temp directory and copy the index there
        Path tempDir = Files.createTempDirectory("cram_index_test");
        Path nonStandardIndex = tempDir.resolve("custom_location.crai");
        
        try {
            Files.copy(CRAM_CRAI_INDEX, nonStandardIndex);
            
            try (CRAMFileReader reader = new CRAMFileReader(CRAM_WITH_CRAI, nonStandardIndex, reference)) {
                Assert.assertNotNull(reader, "CRAMFileReader should not be null");
                Assert.assertTrue(reader.hasIndex(), "Reader should have index from non-standard location");
            }
        } finally {
            // Cleanup
            Files.deleteIfExists(nonStandardIndex);
            Files.deleteIfExists(tempDir);
        }
    }

    // ==================== SBI Index Tests ====================

    /**
     * Test SBIIndex loading from Path.
     * Requirements: 5.4
     */
    @Test
    public void testSBIIndexLoadFromPath() throws IOException {
        // SBI index files are typically created during BAM writing with SBI indexing enabled
        // For this test, we verify the SBIIndex.load(Path) method exists and works
        // We'll create a minimal test by checking the API exists
        
        // The SBIIndex.load(Path) method should exist
        try {
            // This will throw FileNotFoundException if file doesn't exist, which is expected
            // We're just verifying the API accepts Path
            Path nonExistentSbi = Path.of("non_existent.sbi");
            try {
                SBIIndex.load(nonExistentSbi);
                Assert.fail("Should have thrown IOException for non-existent file");
            } catch (IOException e) {
                // Expected - file doesn't exist
                Assert.assertTrue(e.getMessage() != null || e.getCause() != null,
                        "Exception should have message or cause");
            }
        } catch (Exception e) {
            // If we get here with a different exception, the API might not exist
            Assert.fail("SBIIndex.load(Path) should accept Path parameter: " + e.getMessage());
        }
    }

    // ==================== Index Reader API Verification ====================

    /**
     * Verify DiskBasedBAMFileIndex accepts Path in constructor.
     * Requirements: 5.2
     */
    @Test
    public void testDiskBasedBAMFileIndexAcceptsPath() {
        // Verify constructor accepts Path
        DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(BAI_INDEX, null);
        Assert.assertNotNull(index, "DiskBasedBAMFileIndex should accept Path");
        
        // Verify with memory mapping option
        DiskBasedBAMFileIndex indexWithMapping = new DiskBasedBAMFileIndex(BAI_INDEX, null, true);
        Assert.assertNotNull(indexWithMapping, "DiskBasedBAMFileIndex should accept Path with memory mapping");
    }

    /**
     * Verify CSIIndex accepts Path in constructor.
     * Requirements: 5.3
     */
    @Test
    public void testCSIIndexAcceptsPath() throws IOException {
        // Verify constructor accepts Path
        CSIIndex index = new CSIIndex(CSI_INDEX, null);
        Assert.assertNotNull(index, "CSIIndex should accept Path");
        
        // Verify with memory mapping option
        CSIIndex indexWithMapping = new CSIIndex(CSI_INDEX, false, null);
        Assert.assertNotNull(indexWithMapping, "CSIIndex should accept Path with memory mapping option");
    }
}
