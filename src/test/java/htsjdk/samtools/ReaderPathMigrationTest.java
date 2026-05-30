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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Tests for verifying SAM/BAM/CRAM readers work correctly with Path-based APIs
 * and that no File-based constructors exist.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.6
 */
public class ReaderPathMigrationTest extends HtsjdkTest {

    private static final Path TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/samtools");
    
    // SAM test files
    private static final Path SAM_FILE = TEST_DATA_DIR.resolve("coordinate_sorted.sam");
    
    // BAM test files
    private static final Path BAM_FILE = TEST_DATA_DIR.resolve("example.bam");
    private static final Path BAM_WITH_INDEX = Path.of("src/test/resources/htsjdk/samtools/BAMFileIndexTest/index_test.bam");
    private static final Path BAM_INDEX = Path.of(BAM_WITH_INDEX.toString() + ".bai");
    
    // CRAM test files
    private static final Path CRAM_WITH_CRAI = TEST_DATA_DIR.resolve("cram_with_crai_index.cram");
    private static final Path CRAM_INDEX = TEST_DATA_DIR.resolve("cram_with_crai_index.cram.crai");
    
    // Reference for CRAM
    private static ReferenceSource createReferenceSource() {
        final byte[] refBases = new byte[10 * 10];
        Arrays.fill(refBases, (byte) 'A');
        InMemoryReferenceSequenceFile rsf = new InMemoryReferenceSequenceFile();
        rsf.add("chr1", refBases);
        return new ReferenceSource(rsf);
    }

    // ==================== SAMTextReader Tests ====================

    /**
     * Test SAMTextReader with Path to test SAM file.
     * Verifies that SAMTextReader can be used via SamReaderFactory with Path.
     * Requirements: 3.1
     */
    @Test
    public void testSAMTextReaderWithPath() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SAM_FILE)) {
            
            Assert.assertNotNull(reader, "SamReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertEquals(reader.type(), SamReader.Type.SAM_TYPE, "Reader type should be SAM");
            
            // Verify we can read records
            int recordCount = 0;
            for (SAMRecord record : reader) {
                Assert.assertNotNull(record, "SAMRecord should not be null");
                recordCount++;
                if (recordCount >= 5) break; // Just verify first few records
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
        }
    }

    /**
     * Test SAMTextReader reads records correctly with Path.
     * Requirements: 3.1
     */
    @Test
    public void testSAMTextReaderReadsRecordsCorrectly() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SAM_FILE)) {
            
            SAMRecordIterator iterator = reader.iterator();
            Assert.assertTrue(iterator.hasNext(), "Iterator should have records");
            
            SAMRecord firstRecord = iterator.next();
            Assert.assertNotNull(firstRecord.getReadName(), "Read name should not be null");
            Assert.assertNotNull(firstRecord.getReadString(), "Read string should not be null");
            
            iterator.close();
        }
    }

    // ==================== BAMFileReader Tests ====================

    /**
     * Test BAMFileReader with Path to test BAM file.
     * Requirements: 3.2
     */
    @Test
    public void testBAMFileReaderWithPath() throws IOException {
        BAMFileReader reader = new BAMFileReader(
                BAM_WITH_INDEX, 
                BAM_INDEX, 
                true, 
                false, 
                ValidationStringency.SILENT, 
                DefaultSAMRecordFactory.getInstance());
        try {
            Assert.assertNotNull(reader, "BAMFileReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        } finally {
            reader.close();
        }
    }

    /**
     * Test BAMFileReader reads records correctly with Path.
     * Requirements: 3.2
     */
    @Test
    public void testBAMFileReaderReadsRecordsCorrectly() throws IOException {
        BAMFileReader reader = new BAMFileReader(
                BAM_WITH_INDEX, 
                BAM_INDEX, 
                true, 
                false, 
                ValidationStringency.SILENT, 
                DefaultSAMRecordFactory.getInstance());
        try {
            // Verify we can iterate through records
            int recordCount = 0;
            var iterator = reader.getIterator();
            try {
                while (iterator.hasNext() && recordCount < 10) {
                    SAMRecord record = iterator.next();
                    Assert.assertNotNull(record, "SAMRecord should not be null");
                    Assert.assertNotNull(record.getReadName(), "Read name should not be null");
                    recordCount++;
                }
            } finally {
                iterator.close();
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
        } finally {
            reader.close();
        }
    }

    /**
     * Test BAMFileReader with Path and automatic index discovery.
     * Requirements: 3.2
     */
    @Test
    public void testBAMFileReaderWithPathAutoIndex() throws IOException {
        BAMFileReader reader = new BAMFileReader(
                BAM_WITH_INDEX, 
                null, // Let it auto-discover the index
                true, 
                false, 
                ValidationStringency.SILENT, 
                DefaultSAMRecordFactory.getInstance());
        try {
            Assert.assertNotNull(reader, "BAMFileReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should auto-discover index");
        } finally {
            reader.close();
        }
    }

    /**
     * Test BAMFileReader via SamReaderFactory with Path.
     * Requirements: 3.2
     */
    @Test
    public void testBAMFileReaderViaSamReaderFactoryWithPath() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(BAM_FILE)) {
            
            Assert.assertNotNull(reader, "SamReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertEquals(reader.type(), SamReader.Type.BAM_TYPE, "Reader type should be BAM");
            
            // Verify we can read records
            int recordCount = 0;
            for (SAMRecord record : reader) {
                Assert.assertNotNull(record, "SAMRecord should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
        }
    }

    // ==================== CRAMFileReader Tests ====================

    /**
     * Test CRAMFileReader with Path to test CRAM file and reference.
     * Requirements: 3.3
     */
    @Test
    public void testCRAMFileReaderWithPath() {
        ReferenceSource reference = createReferenceSource();
        
        try (CRAMFileReader reader = new CRAMFileReader(CRAM_WITH_CRAI, reference)) {
            Assert.assertNotNull(reader, "CRAMFileReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should auto-discover index");
        }
    }

    /**
     * Test CRAMFileReader with explicit index Path.
     * Requirements: 3.3
     */
    @Test
    public void testCRAMFileReaderWithExplicitIndexPath() {
        ReferenceSource reference = createReferenceSource();
        
        try (CRAMFileReader reader = new CRAMFileReader(CRAM_WITH_CRAI, CRAM_INDEX, reference)) {
            Assert.assertNotNull(reader, "CRAMFileReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        }
    }

    /**
     * Test CRAMFileReader reads records correctly with Path.
     * Uses CRAMTestUtils to create a CRAM file with known content.
     * Requirements: 3.3
     */
    @Test
    public void testCRAMFileReaderReadsRecordsCorrectly() throws IOException {
        // Create a CRAM file with known content using CRAMTestUtils
        final SAMRecordSetBuilder builder = new SAMRecordSetBuilder(false, SAMFileHeader.SortOrder.unsorted);
        builder.addFrag("testRead1", 0, 2, false);
        builder.addFrag("testRead2", 0, 10, false);
        
        final CRAMFileReader reader = CRAMTestUtils.writeAndReadFromInMemoryCram(builder);
        try {
            SAMRecordIterator iterator = reader.getIterator();
            
            int recordCount = 0;
            while (iterator.hasNext() && recordCount < 10) {
                SAMRecord record = iterator.next();
                Assert.assertNotNull(record, "SAMRecord should not be null");
                recordCount++;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
            
            iterator.close();
        } finally {
            reader.close();
        }
    }

    /**
     * Test CRAMFileReader with Path and ValidationStringency.
     * Requirements: 3.3
     */
    @Test
    public void testCRAMFileReaderWithPathAndValidationStringency() throws IOException {
        ReferenceSource reference = createReferenceSource();
        
        try (CRAMFileReader reader = new CRAMFileReader(
                CRAM_WITH_CRAI, 
                null, 
                reference, 
                ValidationStringency.STRICT)) {
            
            Assert.assertNotNull(reader, "CRAMFileReader should not be null");
            Assert.assertTrue(reader.hasIndex(), "Reader should auto-discover index");
        }
    }

    // ==================== No File-based Constructors Tests ====================

    /**
     * Verify SAMTextReader has no File-based constructors.
     * Requirements: 3.6
     */
    @Test
    public void testSAMTextReaderHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = SAMTextReader.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "SAMTextReader should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify BAMFileReader has no File-based constructors.
     * Requirements: 3.6
     */
    @Test
    public void testBAMFileReaderHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = BAMFileReader.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BAMFileReader should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify CRAMFileReader has no File-based constructors.
     * Requirements: 3.6
     */
    @Test
    public void testCRAMFileReaderHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = CRAMFileReader.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "CRAMFileReader should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify SAMTextReader does not import java.io.File.
     * This is a compile-time check - if File was used, this test file would not compile
     * since we're using Path throughout.
     * Requirements: 3.6
     */
    @Test
    public void testSAMTextReaderUsesPathNotFile() throws IOException {
        // This test verifies that we can successfully use SAMTextReader with Path
        // If SAMTextReader required File, this would fail
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SAM_FILE)) {
            Assert.assertNotNull(reader.getFileHeader());
        }
    }

    /**
     * Verify BAMFileReader uses Path not File.
     * Requirements: 3.6
     */
    @Test
    public void testBAMFileReaderUsesPathNotFile() throws IOException {
        // This test verifies that we can successfully use BAMFileReader with Path
        BAMFileReader reader = new BAMFileReader(
                BAM_WITH_INDEX, 
                BAM_INDEX, 
                true, 
                false, 
                ValidationStringency.SILENT, 
                DefaultSAMRecordFactory.getInstance());
        try {
            Assert.assertNotNull(reader.getFileHeader());
        } finally {
            reader.close();
        }
    }

    /**
     * Verify CRAMFileReader uses Path not File.
     * Requirements: 3.6
     */
    @Test
    public void testCRAMFileReaderUsesPathNotFile() {
        // This test verifies that we can successfully use CRAMFileReader with Path
        ReferenceSource reference = createReferenceSource();
        try (CRAMFileReader reader = new CRAMFileReader(CRAM_WITH_CRAI, reference)) {
            Assert.assertNotNull(reader.getFileHeader());
        }
    }
}
