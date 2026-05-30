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
package htsjdk.samtools.fastq;

import htsjdk.HtsjdkTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.nio.file.Path;

/**
 * Tests for verifying FastqReader works correctly with Path-based APIs
 * and that no File-based constructors exist.
 * 
 * Requirements: 3.5, 3.6
 */
public class FastqReaderPathMigrationTest extends HtsjdkTest {

    private static final Path TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/samtools/util/QualityEncodingDetectorTest");
    
    // FASTQ test files
    private static final Path FASTQ_FILE = TEST_DATA_DIR.resolve("solexa_full_range_as_solexa.fastq");
    private static final Path FASTQ_FILE_2 = TEST_DATA_DIR.resolve("5k-30BB2AAXX.3.aligned.sam.fastq");

    // Sample FASTQ content for testing with BufferedReader
    private static final String SAMPLE_FASTQ_CONTENT = 
        "@SEQ_ID\n" +
        "GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT\n" +
        "+\n" +
        "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65\n" +
        "@SEQ_ID2\n" +
        "ACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGT\n" +
        "+\n" +
        "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII\n";

    // ==================== FastqReader Tests with Path ====================

    /**
     * Test FastqReader with Path to test FASTQ file.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderWithPath() {
        try (FastqReader reader = new FastqReader(FASTQ_FILE)) {
            Assert.assertNotNull(reader, "FastqReader should not be null");
            Assert.assertEquals(reader.getPath(), FASTQ_FILE, "Path should match");
        }
    }

    /**
     * Test FastqReader with Path and skipBlankLines option.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderWithPathAndSkipBlankLines() {
        try (FastqReader reader = new FastqReader(FASTQ_FILE, true)) {
            Assert.assertNotNull(reader, "FastqReader should not be null");
            Assert.assertEquals(reader.getPath(), FASTQ_FILE, "Path should match");
        }
    }

    /**
     * Test FastqReader with Path and BufferedReader.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderWithPathAndBufferedReader() {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(SAMPLE_FASTQ_CONTENT));
        Path testPath = Path.of("test.fastq");
        
        try (FastqReader reader = new FastqReader(testPath, bufferedReader)) {
            Assert.assertNotNull(reader, "FastqReader should not be null");
            Assert.assertEquals(reader.getPath(), testPath, "Path should match");
        }
    }

    /**
     * Test FastqReader with Path, BufferedReader, and skipBlankLines.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderWithPathBufferedReaderAndSkipBlankLines() {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(SAMPLE_FASTQ_CONTENT));
        Path testPath = Path.of("test.fastq");
        
        try (FastqReader reader = new FastqReader(testPath, bufferedReader, false)) {
            Assert.assertNotNull(reader, "FastqReader should not be null");
            Assert.assertEquals(reader.getPath(), testPath, "Path should match");
        }
    }

    /**
     * Test FastqReader reads records correctly with Path.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderReadsRecordsCorrectly() {
        try (FastqReader reader = new FastqReader(FASTQ_FILE)) {
            Assert.assertTrue(reader.hasNext(), "Reader should have records");
            
            int recordCount = 0;
            while (reader.hasNext() && recordCount < 10) {
                FastqRecord record = reader.next();
                Assert.assertNotNull(record, "FastqRecord should not be null");
                Assert.assertNotNull(record.getReadName(), "Read name should not be null");
                Assert.assertNotNull(record.getReadString(), "Read string should not be null");
                Assert.assertNotNull(record.getBaseQualityString(), "Quality string should not be null");
                recordCount++;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
        }
    }

    /**
     * Test FastqReader reads records correctly from in-memory content.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderReadsRecordsFromBufferedReader() {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(SAMPLE_FASTQ_CONTENT));
        Path testPath = Path.of("test.fastq");
        
        try (FastqReader reader = new FastqReader(testPath, bufferedReader)) {
            Assert.assertTrue(reader.hasNext(), "Reader should have records");
            
            // Read first record
            FastqRecord record1 = reader.next();
            Assert.assertNotNull(record1, "First record should not be null");
            Assert.assertEquals(record1.getReadName(), "SEQ_ID", "First read name should match");
            Assert.assertEquals(record1.getReadString(), 
                "GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT",
                "First read sequence should match");
            
            // Read second record
            Assert.assertTrue(reader.hasNext(), "Reader should have second record");
            FastqRecord record2 = reader.next();
            Assert.assertNotNull(record2, "Second record should not be null");
            Assert.assertEquals(record2.getReadName(), "SEQ_ID2", "Second read name should match");
            
            // No more records
            Assert.assertFalse(reader.hasNext(), "Reader should have no more records");
        }
    }

    /**
     * Test FastqReader getLineNumber method.
     * Note: The constructor reads the first record, so line number starts at 5 (after 4 lines of first record).
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderGetLineNumber() {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(SAMPLE_FASTQ_CONTENT));
        Path testPath = Path.of("test.fastq");
        
        try (FastqReader reader = new FastqReader(testPath, bufferedReader)) {
            // Constructor reads first record, so line number is already at 5
            Assert.assertEquals(reader.getLineNumber(), 5, "Line number should be 5 after constructor reads first record");
            
            reader.next(); // Return first record and read second record (4 more lines)
            Assert.assertEquals(reader.getLineNumber(), 9, "Line number should be 9 after reading second record");
        }
    }

    /**
     * Test FastqReader iterator functionality.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderIterator() {
        try (FastqReader reader = new FastqReader(FASTQ_FILE)) {
            int recordCount = 0;
            for (FastqRecord record : reader) {
                Assert.assertNotNull(record, "FastqRecord should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to iterate through records");
        }
    }

    /**
     * Test FastqReader toString method.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderToString() {
        try (FastqReader reader = new FastqReader(FASTQ_FILE)) {
            String str = reader.toString();
            Assert.assertNotNull(str, "toString should not return null");
            Assert.assertTrue(str.contains("FastqReader"), "toString should contain class name");
            Assert.assertTrue(str.contains(FASTQ_FILE.toString()), "toString should contain path");
        }
    }

    /**
     * Test FastqReader with different FASTQ file.
     * Requirements: 3.5
     */
    @Test
    public void testFastqReaderWithDifferentFile() {
        try (FastqReader reader = new FastqReader(FASTQ_FILE_2)) {
            Assert.assertNotNull(reader, "FastqReader should not be null");
            Assert.assertTrue(reader.hasNext(), "Reader should have records");
            
            FastqRecord record = reader.next();
            Assert.assertNotNull(record, "FastqRecord should not be null");
            Assert.assertNotNull(record.getReadName(), "Read name should not be null");
        }
    }

    // ==================== No File-based Constructors Tests ====================

    /**
     * Verify FastqReader has no File-based constructors.
     * Requirements: 3.6
     */
    @Test
    public void testFastqReaderHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = FastqReader.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "FastqReader should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify FastqReader uses Path not File.
     * Requirements: 3.6
     */
    @Test
    public void testFastqReaderUsesPathNotFile() {
        // This test verifies that we can successfully use FastqReader with Path
        // If FastqReader required File, this would fail
        try (FastqReader reader = new FastqReader(FASTQ_FILE)) {
            Assert.assertNotNull(reader.getPath());
        }
    }
}
