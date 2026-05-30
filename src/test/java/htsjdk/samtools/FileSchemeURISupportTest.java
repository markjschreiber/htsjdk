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
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Tests for verifying file:// URI support across all file formats.
 * This test class validates that files can be opened and read correctly
 * using file:// URIs.
 * 
 * Requirements: 9.1
 */
public class FileSchemeURISupportTest extends HtsjdkTest {

    private static final Path TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/samtools");
    private static final Path VARIANT_TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/variant");
    private static final Path FASTQ_TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/samtools/util/QualityEncodingDetectorTest");
    
    // BAM test files
    private static final Path BAM_FILE = TEST_DATA_DIR.resolve("example.bam");
    private static final Path BAM_WITH_INDEX = Path.of("src/test/resources/htsjdk/samtools/BAMFileIndexTest/index_test.bam");
    
    // CRAM test files
    private static final Path CRAM_WITH_CRAI = TEST_DATA_DIR.resolve("cram_with_crai_index.cram");
    private static final Path CRAM_REFERENCE = TEST_DATA_DIR.resolve("hg19mini.fasta");
    
    // VCF test files
    private static final Path VCF_FILE = VARIANT_TEST_DATA_DIR.resolve("ex2.vcf");
    private static final Path VCF_WITH_INDEX = VARIANT_TEST_DATA_DIR.resolve("test1.vcf");
    
    // FASTQ test files
    private static final Path FASTQ_FILE = FASTQ_TEST_DATA_DIR.resolve("solexa_full_range_as_solexa.fastq");

    /**
     * Helper method to create a file:// URI from a Path.
     */
    private static URI createFileUri(Path path) {
        return URI.create("file://" + path.toAbsolutePath().toString());
    }

    // ==================== BAM file:// URI Tests ====================

    /**
     * Test opening BAM file with file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testOpenBAMFileWithFileSchemeURI() throws IOException {
        final URI bamUri = createFileUri(BAM_FILE);
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(bamUri)) {
            
            Assert.assertNotNull(reader, "SamReader should not be null for file:// URI");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertEquals(reader.type(), SamReader.Type.BAM_TYPE, "Reader type should be BAM");
        }
    }

    /**
     * Test reading BAM records via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testReadBAMRecordsViaFileSchemeURI() throws IOException {
        final URI bamUri = createFileUri(BAM_FILE);
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(bamUri)) {
            
            int recordCount = 0;
            for (SAMRecord record : reader) {
                Assert.assertNotNull(record, "SAMRecord should not be null");
                Assert.assertNotNull(record.getReadName(), "Read name should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record via file:// URI");
        }
    }

    /**
     * Test opening indexed BAM file with file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testOpenIndexedBAMFileWithFileSchemeURI() throws IOException {
        final URI bamUri = createFileUri(BAM_WITH_INDEX);
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(bamUri)) {
            
            Assert.assertNotNull(reader, "SamReader should not be null for file:// URI");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index when opened via file:// URI");
        }
    }

    /**
     * Test getting file header via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testGetFileHeaderViaFileSchemeURI() throws IOException {
        final URI bamUri = createFileUri(BAM_FILE);
        
        final SAMFileHeader header = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .getFileHeader(bamUri);
        
        Assert.assertNotNull(header, "File header should not be null for file:// URI");
    }

    // ==================== CRAM file:// URI Tests ====================

    /**
     * Test opening CRAM file with file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testOpenCRAMFileWithFileSchemeURI() throws IOException {
        final URI cramUri = createFileUri(CRAM_WITH_CRAI);
        final ReferenceSource reference = new ReferenceSource(CRAM_REFERENCE);
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .referenceSource(reference)
                .open(cramUri)) {
            
            Assert.assertNotNull(reader, "SamReader should not be null for file:// URI");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertEquals(reader.type(), SamReader.Type.CRAM_TYPE, "Reader type should be CRAM");
        }
    }

    /**
     * Test reading CRAM records via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testReadCRAMRecordsViaFileSchemeURI() throws IOException {
        final URI cramUri = createFileUri(CRAM_WITH_CRAI);
        final ReferenceSource reference = new ReferenceSource(CRAM_REFERENCE);
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .referenceSource(reference)
                .open(cramUri)) {
            
            int recordCount = 0;
            for (SAMRecord record : reader) {
                Assert.assertNotNull(record, "SAMRecord should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record via file:// URI");
        }
    }

    /**
     * Test CRAM index auto-discovery via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testCRAMIndexAutoDiscoveryViaFileSchemeURI() throws IOException {
        final URI cramUri = createFileUri(CRAM_WITH_CRAI);
        final ReferenceSource reference = new ReferenceSource(CRAM_REFERENCE);
        
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .referenceSource(reference)
                .open(cramUri)) {
            
            Assert.assertTrue(reader.hasIndex(), "Reader should auto-discover index via file:// URI");
        }
    }

    // ==================== VCF file:// URI Tests ====================

    /**
     * Test opening VCF file with file:// URI via Path.of(URI).
     * Note: VCFFileReader doesn't have a direct URI constructor, so we use Path.of(URI).
     * Requirements: 9.1
     */
    @Test
    public void testOpenVCFFileWithFileSchemeURI() {
        final URI vcfUri = createFileUri(VCF_FILE);
        final Path vcfPath = Path.of(vcfUri);
        
        try (VCFFileReader reader = new VCFFileReader(vcfPath, false)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null for file:// URI");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
        }
    }

    /**
     * Test reading VCF records via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testReadVCFRecordsViaFileSchemeURI() {
        final URI vcfUri = createFileUri(VCF_FILE);
        final Path vcfPath = Path.of(vcfUri);
        
        try (VCFFileReader reader = new VCFFileReader(vcfPath, false)) {
            int recordCount = 0;
            for (VariantContext vc : reader) {
                Assert.assertNotNull(vc, "VariantContext should not be null");
                Assert.assertNotNull(vc.getContig(), "Contig should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record via file:// URI");
        }
    }

    /**
     * Test opening indexed VCF file with file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testOpenIndexedVCFFileWithFileSchemeURI() {
        final URI vcfUri = createFileUri(VCF_WITH_INDEX);
        final Path vcfPath = Path.of(vcfUri);
        
        try (VCFFileReader reader = new VCFFileReader(vcfPath, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null for file:// URI");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
        }
    }

    // ==================== FASTQ file:// URI Tests ====================

    /**
     * Test opening FASTQ file with file:// URI via Path.of(URI).
     * Note: FastqReader doesn't have a direct URI constructor, so we use Path.of(URI).
     * Requirements: 9.1
     */
    @Test
    public void testOpenFASTQFileWithFileSchemeURI() {
        final URI fastqUri = createFileUri(FASTQ_FILE);
        final Path fastqPath = Path.of(fastqUri);
        
        try (FastqReader reader = new FastqReader(fastqPath)) {
            Assert.assertNotNull(reader, "FastqReader should not be null for file:// URI");
            Assert.assertEquals(reader.getPath(), fastqPath, "Path should match");
        }
    }

    /**
     * Test reading FASTQ records via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testReadFASTQRecordsViaFileSchemeURI() {
        final URI fastqUri = createFileUri(FASTQ_FILE);
        final Path fastqPath = Path.of(fastqUri);
        
        try (FastqReader reader = new FastqReader(fastqPath)) {
            Assert.assertTrue(reader.hasNext(), "Reader should have records");
            
            int recordCount = 0;
            while (reader.hasNext() && recordCount < 5) {
                FastqRecord record = reader.next();
                Assert.assertNotNull(record, "FastqRecord should not be null");
                Assert.assertNotNull(record.getReadName(), "Read name should not be null");
                Assert.assertNotNull(record.getReadString(), "Read string should not be null");
                recordCount++;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record via file:// URI");
        }
    }

    /**
     * Test FASTQ reader iterator via file:// URI.
     * Requirements: 9.1
     */
    @Test
    public void testFASTQIteratorViaFileSchemeURI() {
        final URI fastqUri = createFileUri(FASTQ_FILE);
        final Path fastqPath = Path.of(fastqUri);
        
        try (FastqReader reader = new FastqReader(fastqPath)) {
            int recordCount = 0;
            for (FastqRecord record : reader) {
                Assert.assertNotNull(record, "FastqRecord should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to iterate through records via file:// URI");
        }
    }

    // ==================== URI Round-Trip Tests ====================

    /**
     * Test that Path.of(URI) preserves the file path correctly.
     * Requirements: 9.1
     */
    @Test
    public void testPathOfURIPreservesFilePath() {
        final URI bamUri = createFileUri(BAM_FILE);
        final Path pathFromUri = Path.of(bamUri);
        
        // The path from URI should resolve to the same file
        Assert.assertEquals(pathFromUri.toAbsolutePath(), BAM_FILE.toAbsolutePath(),
                "Path from URI should resolve to the same absolute path");
    }

    /**
     * Test that URI scheme is preserved in round-trip.
     * Requirements: 9.1
     */
    @Test
    public void testURISchemePreservedInRoundTrip() {
        final URI originalUri = createFileUri(BAM_FILE);
        final Path pathFromUri = Path.of(originalUri);
        final URI roundTripUri = pathFromUri.toUri();
        
        Assert.assertEquals(roundTripUri.getScheme(), "file", 
                "URI scheme should be 'file' after round-trip");
        Assert.assertEquals(roundTripUri.getPath(), originalUri.getPath(),
                "URI path should be preserved after round-trip");
    }
}
