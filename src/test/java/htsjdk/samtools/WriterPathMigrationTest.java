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
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.FileExtensions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for verifying SAM/BAM/CRAM writers work correctly with Path-based APIs
 * and that no File-based constructors exist.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.7
 */
public class WriterPathMigrationTest extends HtsjdkTest {

    // ==================== Helper Methods ====================

    /**
     * Create a SAMFileHeader for testing.
     */
    private SAMFileHeader createSAMHeader(SAMFileHeader.SortOrder sortOrder) {
        final SAMFileHeader header = new SAMFileHeader();
        header.setSortOrder(sortOrder);
        header.addSequence(new SAMSequenceRecord("chr1", 10000));
        SAMReadGroupRecord readGroupRecord = new SAMReadGroupRecord("1");
        readGroupRecord.setSample("testSample");
        header.addReadGroup(readGroupRecord);
        return header;
    }

    /**
     * Create a ReferenceSource for CRAM testing.
     */
    private ReferenceSource createReferenceSource() {
        final byte[] refBases = new byte[10000];
        Arrays.fill(refBases, (byte) 'A');
        InMemoryReferenceSequenceFile rsf = new InMemoryReferenceSequenceFile();
        rsf.add("chr1", refBases);
        return new ReferenceSource(rsf);
    }

    /**
     * Create test SAMRecords.
     */
    private List<SAMRecord> createTestRecords(SAMFileHeader header, int count) {
        final List<SAMRecord> records = new ArrayList<>(count);
        final SAMRecordSetBuilder builder = new SAMRecordSetBuilder(true, header.getSortOrder());
        
        int posInRef = 1;
        for (int i = 0; i < count / 2; i++) {
            builder.addPair(Integer.toString(i), 0, posInRef += 1, posInRef += 3);
        }
        records.addAll(builder.getRecords());
        records.sort(new SAMRecordCoordinateComparator());
        
        return records;
    }

    // ==================== SAMTextWriter Tests ====================

    /**
     * Test SAMTextWriter writing to Path.
     * Requirements: 4.1
     */
    @Test
    public void testSAMTextWriterWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.SAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write using SAMTextWriter via factory
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(header, true, outputPath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify file was created
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output file should not be empty");
    }

    /**
     * Test SAMTextWriter written files can be read back correctly.
     * Requirements: 4.1
     */
    @Test
    public void testSAMTextWriterRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.SAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> originalRecords = createTestRecords(header, 10);
        
        // Write records
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(header, true, outputPath)) {
            for (SAMRecord record : originalRecords) {
                writer.addAlignment(record);
            }
        }
        
        // Read back and verify
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(outputPath)) {
            
            Assert.assertEquals(reader.type(), SamReader.Type.SAM_TYPE, "Reader type should be SAM");
            
            int recordCount = 0;
            try (CloseableIterator<SAMRecord> iterator = reader.iterator()) {
                while (iterator.hasNext()) {
                    SAMRecord readRecord = iterator.next();
                    Assert.assertNotNull(readRecord, "Read record should not be null");
                    Assert.assertNotNull(readRecord.getReadName(), "Read name should not be null");
                    recordCount++;
                }
            }
            Assert.assertEquals(recordCount, originalRecords.size(), "Should read back same number of records");
        }
    }

    /**
     * Test SAMTextWriter direct constructor with Path.
     * Requirements: 4.1
     */
    @Test
    public void testSAMTextWriterDirectConstructorWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.SAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        
        // Use direct constructor
        try (SAMTextWriter writer = new SAMTextWriter(outputPath)) {
            writer.setHeader(header);
            
            SAMRecord record = new SAMRecord(header);
            record.setReadName("testRead");
            record.setReferenceName("chr1");
            record.setAlignmentStart(100);
            record.setReadBases("ACGT".getBytes());
            record.setBaseQualities(new byte[]{30, 30, 30, 30});
            record.setCigarString("4M");
            record.setReadUnmappedFlag(false);
            
            writer.addAlignment(record);
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output file should not be empty");
    }

    // ==================== BAMFileWriter Tests ====================

    /**
     * Test BAMFileWriter writing to Path.
     * Requirements: 4.2
     */
    @Test
    public void testBAMFileWriterWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.BAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write using BAMFileWriter via factory
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, outputPath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify file was created
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output file should not be empty");
    }

    /**
     * Test BAMFileWriter written files can be read back correctly.
     * Requirements: 4.2
     */
    @Test
    public void testBAMFileWriterRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.BAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> originalRecords = createTestRecords(header, 10);
        
        // Write records
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, outputPath)) {
            for (SAMRecord record : originalRecords) {
                writer.addAlignment(record);
            }
        }
        
        // Read back and verify
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(outputPath)) {
            
            Assert.assertEquals(reader.type(), SamReader.Type.BAM_TYPE, "Reader type should be BAM");
            
            int recordCount = 0;
            try (CloseableIterator<SAMRecord> iterator = reader.iterator()) {
                while (iterator.hasNext()) {
                    SAMRecord readRecord = iterator.next();
                    Assert.assertNotNull(readRecord, "Read record should not be null");
                    Assert.assertNotNull(readRecord.getReadName(), "Read name should not be null");
                    recordCount++;
                }
            }
            Assert.assertEquals(recordCount, originalRecords.size(), "Should read back same number of records");
        }
    }

    /**
     * Test BAMFileWriter with compression level.
     * Requirements: 4.2
     */
    @Test
    public void testBAMFileWriterWithCompressionLevel() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.BAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write with specific compression level
        try (SAMFileWriter writer = new SAMFileWriterFactory()
                .setCompressionLevel(5)
                .makeBAMWriter(header, true, outputPath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output file should not be empty");
    }

    // ==================== CRAMFileWriter Tests ====================

    /**
     * Test CRAMFileWriter writing to Path.
     * Requirements: 4.3
     */
    @Test
    public void testCRAMFileWriterWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.CRAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource referenceSource = createReferenceSource();
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write using CRAMFileWriter via factory with stream
        try (CRAMFileWriter writer = new CRAMFileWriter(
                Files.newOutputStream(outputPath), 
                referenceSource, 
                header, 
                outputPath.toString())) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify file was created
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output file should not be empty");
    }

    /**
     * Test CRAMFileWriter written files can be read back correctly.
     * Requirements: 4.3
     */
    @Test
    public void testCRAMFileWriterRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.CRAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource referenceSource = createReferenceSource();
        final List<SAMRecord> originalRecords = createTestRecords(header, 10);
        
        // Write records
        try (CRAMFileWriter writer = new CRAMFileWriter(
                Files.newOutputStream(outputPath), 
                referenceSource, 
                header, 
                outputPath.toString())) {
            for (SAMRecord record : originalRecords) {
                writer.addAlignment(record);
            }
        }
        
        // Read back and verify
        try (CRAMFileReader reader = new CRAMFileReader(null, 
                Files.newInputStream(outputPath), 
                referenceSource)) {
            
            int recordCount = 0;
            SAMRecordIterator iterator = reader.getIterator();
            while (iterator.hasNext()) {
                SAMRecord readRecord = iterator.next();
                Assert.assertNotNull(readRecord, "Read record should not be null");
                Assert.assertNotNull(readRecord.getReadName(), "Read name should not be null");
                recordCount++;
            }
            Assert.assertEquals(recordCount, originalRecords.size(), "Should read back same number of records");
        }
    }

    /**
     * Test CRAMFileWriter with index stream.
     * Requirements: 4.3
     */
    @Test
    public void testCRAMFileWriterWithIndexStream() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.CRAM);
        final Path indexPath = Path.of(outputPath.toString() + FileExtensions.BAI_INDEX);
        outputPath.toFile().deleteOnExit();
        indexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource referenceSource = createReferenceSource();
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write with index stream
        try (CRAMFileWriter writer = new CRAMFileWriter(
                Files.newOutputStream(outputPath),
                Files.newOutputStream(indexPath),
                referenceSource, 
                header, 
                outputPath.toString())) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.exists(indexPath), "Index file should exist");
        Assert.assertTrue(Files.size(indexPath) > 0, "Index file should not be empty");
    }

    // ==================== No File-based Constructors Tests ====================

    /**
     * Verify SAMTextWriter has no File-based constructors.
     * Requirements: 4.7
     */
    @Test
    public void testSAMTextWriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = SAMTextWriter.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "SAMTextWriter should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify BAMFileWriter has no File-based constructors.
     * Requirements: 4.7
     */
    @Test
    public void testBAMFileWriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = BAMFileWriter.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BAMFileWriter should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify CRAMFileWriter has no File-based constructors.
     * Requirements: 4.7
     */
    @Test
    public void testCRAMFileWriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = CRAMFileWriter.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "CRAMFileWriter should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify SAMTextWriter uses Path not File.
     * Requirements: 4.7
     */
    @Test
    public void testSAMTextWriterUsesPathNotFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.SAM);
        outputPath.toFile().deleteOnExit();
        
        // This test verifies that we can successfully use SAMTextWriter with Path
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(header, true, outputPath)) {
            Assert.assertNotNull(writer);
        }
    }

    /**
     * Verify BAMFileWriter uses Path not File.
     * Requirements: 4.7
     */
    @Test
    public void testBAMFileWriterUsesPathNotFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.BAM);
        outputPath.toFile().deleteOnExit();
        
        // This test verifies that we can successfully use BAMFileWriter with Path
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, outputPath)) {
            Assert.assertNotNull(writer);
        }
    }

    /**
     * Verify CRAMFileWriter uses Path not File.
     * Requirements: 4.7
     */
    @Test
    public void testCRAMFileWriterUsesPathNotFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.CRAM);
        outputPath.toFile().deleteOnExit();
        
        // This test verifies that we can successfully use CRAMFileWriter with Path
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource referenceSource = createReferenceSource();
        try (CRAMFileWriter writer = new CRAMFileWriter(
                Files.newOutputStream(outputPath), 
                referenceSource, 
                header, 
                outputPath.toString())) {
            Assert.assertNotNull(writer);
        }
    }

    // ==================== Additional Writer Tests ====================

    /**
     * Test makeSAMOrBAMWriter with SAM extension.
     * Requirements: 4.1
     */
    @Test
    public void testMakeSAMOrBAMWriterWithSAMExtension() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.SAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(header, true, outputPath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify it's a SAM file
        try (SamReader reader = SamReaderFactory.makeDefault().open(outputPath)) {
            Assert.assertEquals(reader.type(), SamReader.Type.SAM_TYPE);
        }
    }

    /**
     * Test makeSAMOrBAMWriter with BAM extension.
     * Requirements: 4.2
     */
    @Test
    public void testMakeSAMOrBAMWriterWithBAMExtension() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.BAM);
        outputPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(header, true, outputPath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify it's a BAM file
        try (SamReader reader = SamReaderFactory.makeDefault().open(outputPath)) {
            Assert.assertEquals(reader.type(), SamReader.Type.BAM_TYPE);
        }
    }

    /**
     * Test makeWriter with CRAM extension and reference Path.
     * Requirements: 4.3
     */
    @Test
    public void testMakeWriterWithCRAMExtension() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.CRAM);
        outputPath.toFile().deleteOnExit();
        
        // Create a temporary reference file
        final Path referencePath = Files.createTempFile("ref.", ".fasta");
        referencePath.toFile().deleteOnExit();
        
        // Write a simple FASTA reference
        StringBuilder fastaContent = new StringBuilder();
        fastaContent.append(">chr1\n");
        byte[] refBases = new byte[10000];
        Arrays.fill(refBases, (byte) 'A');
        fastaContent.append(new String(refBases)).append("\n");
        Files.writeString(referencePath, fastaContent.toString());
        
        // Create index for the reference
        final Path indexPath = Path.of(referencePath.toString() + ".fai");
        indexPath.toFile().deleteOnExit();
        Files.writeString(indexPath, "chr1\t10000\t6\t10000\t10001\n");
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        try (SAMFileWriter writer = new SAMFileWriterFactory().makeWriter(header, true, outputPath, referencePath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output file should not be empty");
    }
}
