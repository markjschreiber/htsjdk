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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for verifying BasicFastqWriter and FastqWriterFactory work correctly with Path-based APIs
 * and that no File-based constructors exist.
 * 
 * Requirements: 4.6, 4.7
 */
public class FastqWriterPathMigrationTest extends HtsjdkTest {

    // ==================== Helper Methods ====================

    /**
     * Create a test FastqRecord.
     */
    private FastqRecord createFastqRecord(String name, String bases, String quality) {
        return new FastqRecord(name, bases, null, quality);
    }

    /**
     * Create multiple test FastqRecords.
     */
    private List<FastqRecord> createTestRecords(int count) {
        List<FastqRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(createFastqRecord(
                "read" + i,
                "ACGTACGTACGT",
                "IIIIIIIIIIII"
            ));
        }
        return records;
    }

    // ==================== BasicFastqWriter Tests ====================

    /**
     * Test BasicFastqWriter writing to Path.
     * Requirements: 4.6
     */
    @Test
    public void testBasicFastqWriterWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        final List<FastqRecord> records = createTestRecords(5);
        
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath)) {
            for (FastqRecord record : records) {
                writer.write(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output FASTQ file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output FASTQ file should not be empty");
    }

    /**
     * Test BasicFastqWriter written files can be read back correctly.
     * Requirements: 4.6
     */
    @Test
    public void testBasicFastqWriterRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        final List<FastqRecord> originalRecords = createTestRecords(5);
        
        // Write records
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath)) {
            for (FastqRecord record : originalRecords) {
                writer.write(record);
            }
        }
        
        // Read back and verify
        try (FastqReader reader = new FastqReader(outputPath)) {
            int recordCount = 0;
            for (FastqRecord record : reader) {
                Assert.assertNotNull(record, "FastqRecord should not be null");
                Assert.assertNotNull(record.getReadName(), "Read name should not be null");
                Assert.assertEquals(record.getReadName(), originalRecords.get(recordCount).getReadName(),
                    "Read name should match");
                Assert.assertEquals(record.getReadString(), originalRecords.get(recordCount).getReadString(),
                    "Read bases should match");
                Assert.assertEquals(record.getBaseQualityString(), originalRecords.get(recordCount).getBaseQualityString(),
                    "Quality string should match");
                recordCount++;
            }
            Assert.assertEquals(recordCount, originalRecords.size(), "Should read back same number of records");
        }
    }

    /**
     * Test BasicFastqWriter with MD5 creation.
     * Requirements: 4.6
     */
    @Test
    public void testBasicFastqWriterWithMd5() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        final Path md5Path = Path.of(outputPath.toString() + ".md5");
        md5Path.toFile().deleteOnExit();
        
        final List<FastqRecord> records = createTestRecords(3);
        
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath, true)) {
            for (FastqRecord record : records) {
                writer.write(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output FASTQ file should exist");
        Assert.assertTrue(Files.exists(md5Path), "MD5 file should exist");
        Assert.assertTrue(Files.size(md5Path) > 0, "MD5 file should not be empty");
    }

    // ==================== FastqWriterFactory Tests ====================

    /**
     * Test FastqWriterFactory creating writer with Path.
     * Requirements: 4.6
     */
    @Test
    public void testFastqWriterFactoryWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        final FastqWriterFactory factory = new FastqWriterFactory();
        final List<FastqRecord> records = createTestRecords(3);
        
        try (FastqWriter writer = factory.newWriter(outputPath)) {
            for (FastqRecord record : records) {
                writer.write(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output FASTQ file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output FASTQ file should not be empty");
    }

    /**
     * Test FastqWriterFactory with MD5 creation.
     * Requirements: 4.6
     */
    @Test
    public void testFastqWriterFactoryWithMd5() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        final Path md5Path = Path.of(outputPath.toString() + ".md5");
        md5Path.toFile().deleteOnExit();
        
        final FastqWriterFactory factory = new FastqWriterFactory();
        factory.setCreateMd5(true);
        
        final List<FastqRecord> records = createTestRecords(3);
        
        try (FastqWriter writer = factory.newWriter(outputPath)) {
            for (FastqRecord record : records) {
                writer.write(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output FASTQ file should exist");
        Assert.assertTrue(Files.exists(md5Path), "MD5 file should exist");
    }

    /**
     * Test FastqWriterFactory round trip.
     * Requirements: 4.6
     */
    @Test
    public void testFastqWriterFactoryRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        final FastqWriterFactory factory = new FastqWriterFactory();
        final List<FastqRecord> originalRecords = createTestRecords(5);
        
        // Write records
        try (FastqWriter writer = factory.newWriter(outputPath)) {
            for (FastqRecord record : originalRecords) {
                writer.write(record);
            }
        }
        
        // Read back and verify
        try (FastqReader reader = new FastqReader(outputPath)) {
            int recordCount = 0;
            for (FastqRecord record : reader) {
                Assert.assertEquals(record.getReadName(), originalRecords.get(recordCount).getReadName(),
                    "Read name should match");
                recordCount++;
            }
            Assert.assertEquals(recordCount, originalRecords.size(), "Should read back same number of records");
        }
    }

    // ==================== No File-based Constructors Tests ====================

    /**
     * Verify BasicFastqWriter has no File-based constructors.
     * Requirements: 4.7
     */
    @Test
    public void testBasicFastqWriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = BasicFastqWriter.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BasicFastqWriter should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify FastqWriterFactory has no File-based methods.
     * Requirements: 4.7
     */
    @Test
    public void testFastqWriterFactoryHasNoFileBasedMethods() {
        Method[] methods = FastqWriterFactory.class.getMethods();
        
        for (Method method : methods) {
            Class<?>[] paramTypes = method.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "FastqWriterFactory should not have File-based method: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                "FastqWriterFactory should not return File: " + method.getName());
        }
    }

    /**
     * Verify BasicFastqWriter uses Path not File.
     * Requirements: 4.7
     */
    @Test
    public void testBasicFastqWriterUsesPathNotFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        // This test verifies that we can successfully use BasicFastqWriter with Path
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath)) {
            Assert.assertNotNull(writer, "Writer should not be null");
            writer.write(createFastqRecord("test", "ACGT", "IIII"));
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
    }

    // ==================== Additional Writer Tests ====================

    /**
     * Test writing empty FASTQ file.
     * Requirements: 4.6
     */
    @Test
    public void testWriteEmptyFastqFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath)) {
            // Write nothing
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output FASTQ file should exist");
        Assert.assertEquals(Files.size(outputPath), 0, "Empty FASTQ file should have zero size");
    }

    /**
     * Test writing FASTQ with special characters in read name.
     * Requirements: 4.6
     */
    @Test
    public void testWriteFastqWithSpecialCharacters() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        final String readName = "read:1:2:3:4#ACGT/1";
        final FastqRecord record = createFastqRecord(readName, "ACGTACGT", "IIIIIIII");
        
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath)) {
            writer.write(record);
        }
        
        // Read back and verify
        try (FastqReader reader = new FastqReader(outputPath)) {
            FastqRecord readRecord = reader.iterator().next();
            Assert.assertEquals(readRecord.getReadName(), readName, "Read name with special characters should be preserved");
        }
    }

    /**
     * Test writing multiple FASTQ records preserves order.
     * Requirements: 4.6
     */
    @Test
    public void testWriteMultipleFastqRecordsPreservesOrder() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".fastq");
        outputPath.toFile().deleteOnExit();
        
        final List<FastqRecord> originalRecords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            originalRecords.add(createFastqRecord("read_" + i, "ACGT", "IIII"));
        }
        
        // Write records
        try (BasicFastqWriter writer = new BasicFastqWriter(outputPath)) {
            for (FastqRecord record : originalRecords) {
                writer.write(record);
            }
        }
        
        // Read back and verify order
        try (FastqReader reader = new FastqReader(outputPath)) {
            int index = 0;
            for (FastqRecord record : reader) {
                Assert.assertEquals(record.getReadName(), originalRecords.get(index).getReadName(),
                    "Record order should be preserved at index " + index);
                index++;
            }
            Assert.assertEquals(index, originalRecords.size(), "Should read all records");
        }
    }
}
