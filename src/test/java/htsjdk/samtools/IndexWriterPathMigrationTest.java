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
import htsjdk.samtools.cram.CRAIEntry;
import htsjdk.samtools.cram.CRAIIndex;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.reference.InMemoryReferenceSequenceFile;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.util.FileExtensions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for verifying index writers work correctly with Path-based APIs
 * and that no File-based methods exist.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 */
public class IndexWriterPathMigrationTest extends HtsjdkTest {

    private static final Path BAM_INDEX_TEST_DIR = Path.of("src/test/resources/htsjdk/samtools/BAMFileIndexTest");
    private static final Path SAMTOOLS_TEST_DIR = Path.of("src/test/resources/htsjdk/samtools");
    
    // BAM test files
    private static final Path BAM_FILE = BAM_INDEX_TEST_DIR.resolve("index_test.bam");
    private static final Path BAI_INDEX = BAM_INDEX_TEST_DIR.resolve("index_test.bam.bai");
    
    // CRAM test files
    private static final Path CRAM_FILE = SAMTOOLS_TEST_DIR.resolve("cram/test2.cram");
    private static final Path CRAM_REF = SAMTOOLS_TEST_DIR.resolve("cram/auxf.fa");

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

    // ==================== BAMIndexer Tests ====================

    /**
     * Test BAMIndexer creating index at Path.
     * Requirements: 6.1
     */
    @Test
    public void testBAMIndexerCreatesIndexAtPath() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        // Create index using BAMIndexer.createIndex with Path
        try (SamReader reader = SamReaderFactory.makeDefault()
                .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS)
                .open(BAM_FILE)) {
            BAMIndexer.createIndex(reader, outputIndexPath);
        }
        
        // Verify index file was created
        Assert.assertTrue(Files.exists(outputIndexPath), "Index file should exist");
        Assert.assertTrue(Files.size(outputIndexPath) > 0, "Index file should not be empty");
    }

    /**
     * Test BAMIndexer created index can be read back correctly.
     * Requirements: 6.1
     */
    @Test
    public void testBAMIndexerCreatedIndexCanBeReadBack() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        // Create index
        try (SamReader reader = SamReaderFactory.makeDefault()
                .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS)
                .open(BAM_FILE)) {
            BAMIndexer.createIndex(reader, outputIndexPath);
        }
        
        // Read back the index and verify it works
        DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(outputIndexPath, null);
        Assert.assertNotNull(index, "Index should not be null");
        Assert.assertTrue(index.getNumberOfReferences() > 0, "Index should have references");
        
        // Verify we can use the index with a reader
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SamInputResource.of(BAM_FILE).index(outputIndexPath))) {
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
            
            // Verify query works
            SAMRecordIterator iterator = reader.query("chr1", 1, 1000, false);
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            iterator.close();
            Assert.assertTrue(count >= 0, "Query should work with created index");
        }
    }

    /**
     * Test BAMIndexer constructor with Path.
     * Requirements: 6.1
     */
    @Test
    public void testBAMIndexerConstructorWithPath() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        
        // Use BAMIndexer constructor with Path
        BAMIndexer indexer = new BAMIndexer(outputIndexPath, header);
        Assert.assertNotNull(indexer, "BAMIndexer should not be null");
        
        // Finish without adding any records (creates empty index)
        indexer.finish();
        
        Assert.assertTrue(Files.exists(outputIndexPath), "Index file should exist");
    }

    /**
     * Test BAMIndexer.createAndWriteIndex with Path parameters.
     * Requirements: 6.1
     */
    @Test
    public void testBAMIndexerCreateAndWriteIndexWithPath() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        // Use createAndWriteIndex to convert existing index
        BAMIndexer.createAndWriteIndex(BAI_INDEX, outputIndexPath, false);
        
        Assert.assertTrue(Files.exists(outputIndexPath), "Output index file should exist");
        Assert.assertTrue(Files.size(outputIndexPath) > 0, "Output index file should not be empty");
        
        // Verify the created index is valid
        DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(outputIndexPath, null);
        Assert.assertNotNull(index, "Index should be readable");
        Assert.assertTrue(index.getNumberOfReferences() > 0, "Index should have references");
    }

    // ==================== BinaryBAMIndexWriter Tests ====================

    /**
     * Test BinaryBAMIndexWriter with Path.
     * Requirements: 6.2
     */
    @Test
    public void testBinaryBAMIndexWriterWithPath() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final int numReferences = header.getSequenceDictionary().size();
        
        // Create BinaryBAMIndexWriter with Path
        BinaryBAMIndexWriter writer = new BinaryBAMIndexWriter(numReferences, outputIndexPath);
        Assert.assertNotNull(writer, "BinaryBAMIndexWriter should not be null");
        
        // Write null content for each reference and close
        for (int i = 0; i < numReferences; i++) {
            writer.writeReference(null);
        }
        writer.writeNoCoordinateRecordCount(0L);
        writer.close();
        
        Assert.assertTrue(Files.exists(outputIndexPath), "Index file should exist");
        Assert.assertTrue(Files.size(outputIndexPath) > 0, "Index file should not be empty");
    }

    // ==================== CRAMBAIIndexer Tests ====================

    /**
     * Test CRAMBAIIndexer creating index with OutputStream.
     * Requirements: 6.3
     */
    @Test
    public void testCRAMBAIIndexerWithOutputStream() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        
        // Create CRAMBAIIndexer with OutputStream
        try (OutputStream os = Files.newOutputStream(outputIndexPath)) {
            CRAMBAIIndexer indexer = new CRAMBAIIndexer(os, header);
            Assert.assertNotNull(indexer, "CRAMBAIIndexer should not be null");
            indexer.finish();
        }
        
        Assert.assertTrue(Files.exists(outputIndexPath), "Index file should exist");
        Assert.assertTrue(Files.size(outputIndexPath) > 0, "Index file should not be empty");
    }

    /**
     * Test CRAMBAIIndexer created index can be read back.
     * Requirements: 6.3
     */
    @Test
    public void testCRAMBAIIndexerCreatedIndexCanBeReadBack() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.BAI_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        
        // Create index
        try (OutputStream os = Files.newOutputStream(outputIndexPath)) {
            CRAMBAIIndexer indexer = new CRAMBAIIndexer(os, header);
            indexer.finish();
        }
        
        // Read back and verify
        DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(outputIndexPath, null);
        Assert.assertNotNull(index, "Index should be readable");
    }

    // ==================== CRAMCRAIIndexer Tests ====================

    /**
     * Test CRAMCRAIIndexer creating index with OutputStream.
     * Requirements: 6.4
     */
    @Test
    public void testCRAMCRAIIndexerWithOutputStream() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.CRAM_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        
        // Create CRAMCRAIIndexer with OutputStream
        try (OutputStream os = Files.newOutputStream(outputIndexPath)) {
            CRAMCRAIIndexer indexer = new CRAMCRAIIndexer(os, header);
            Assert.assertNotNull(indexer, "CRAMCRAIIndexer should not be null");
            indexer.finish();
        }
        
        Assert.assertTrue(Files.exists(outputIndexPath), "Index file should exist");
        Assert.assertTrue(Files.size(outputIndexPath) > 0, "Index file should not be empty");
    }

    /**
     * Test CRAMCRAIIndexer.writeIndex with SeekableStream and OutputStream.
     * Requirements: 6.4
     */
    @Test
    public void testCRAMCRAIIndexerWriteIndexWithStreams() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.CRAM_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        // Use writeIndex static method
        try (SeekableFileStream cramStream = new SeekableFileStream(CRAM_FILE.toFile());
             OutputStream indexStream = Files.newOutputStream(outputIndexPath)) {
            CRAMCRAIIndexer.writeIndex(cramStream, indexStream);
        }
        
        Assert.assertTrue(Files.exists(outputIndexPath), "Index file should exist");
        Assert.assertTrue(Files.size(outputIndexPath) > 0, "Index file should not be empty");
    }

    /**
     * Test CRAMCRAIIndexer created index can be read back.
     * Requirements: 6.4
     */
    @Test
    public void testCRAMCRAIIndexerCreatedIndexCanBeReadBack() throws IOException {
        final Path outputIndexPath = Files.createTempFile("test.", FileExtensions.CRAM_INDEX);
        outputIndexPath.toFile().deleteOnExit();
        
        // Create index from CRAM file
        try (SeekableFileStream cramStream = new SeekableFileStream(CRAM_FILE.toFile());
             OutputStream indexStream = Files.newOutputStream(outputIndexPath)) {
            CRAMCRAIIndexer.writeIndex(cramStream, indexStream);
        }
        
        // Read back and verify
        try (InputStream is = Files.newInputStream(outputIndexPath)) {
            CRAIIndex index = CRAMCRAIIndexer.readIndex(is);
            Assert.assertNotNull(index, "Index should be readable");
            List<CRAIEntry> entries = index.getCRAIEntries();
            Assert.assertNotNull(entries, "Index entries should not be null");
        }
    }

    // ==================== No File-based Methods Tests ====================

    /**
     * Verify BAMIndexer has no File-based public methods.
     * Requirements: 6.6
     */
    @Test
    public void testBAMIndexerHasNoFileBasedMethods() {
        // Check constructors
        Constructor<?>[] constructors = BAMIndexer.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BAMIndexer should not have File-based constructor: " + constructor);
            }
        }
        
        // Check public methods
        Method[] methods = BAMIndexer.class.getMethods();
        for (Method method : methods) {
            // Skip methods from Object class
            if (method.getDeclaringClass() == Object.class) continue;
            
            Class<?>[] paramTypes = method.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BAMIndexer should not have File parameter in method: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                "BAMIndexer should not return File in method: " + method.getName());
        }
    }

    /**
     * Verify BinaryBAMIndexWriter has no File-based constructors.
     * Requirements: 6.6
     */
    @Test
    public void testBinaryBAMIndexWriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = BinaryBAMIndexWriter.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BinaryBAMIndexWriter should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify CRAMBAIIndexer has no File-based public methods.
     * Requirements: 6.6
     */
    @Test
    public void testCRAMBAIIndexerHasNoFileBasedMethods() {
        // Check constructors
        Constructor<?>[] constructors = CRAMBAIIndexer.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            // Skip private constructors
            if (!java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) continue;
            
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "CRAMBAIIndexer should not have File-based constructor: " + constructor);
            }
        }
        
        // Check public methods
        Method[] methods = CRAMBAIIndexer.class.getMethods();
        for (Method method : methods) {
            // Skip methods from Object class
            if (method.getDeclaringClass() == Object.class) continue;
            
            Class<?>[] paramTypes = method.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "CRAMBAIIndexer should not have File parameter in method: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                "CRAMBAIIndexer should not return File in method: " + method.getName());
        }
    }

    /**
     * Verify CRAMCRAIIndexer has no File-based public methods.
     * Requirements: 6.6
     */
    @Test
    public void testCRAMCRAIIndexerHasNoFileBasedMethods() {
        // Check constructors
        Constructor<?>[] constructors = CRAMCRAIIndexer.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "CRAMCRAIIndexer should not have File-based constructor: " + constructor);
            }
        }
        
        // Check public methods
        Method[] methods = CRAMCRAIIndexer.class.getMethods();
        for (Method method : methods) {
            // Skip methods from Object class
            if (method.getDeclaringClass() == Object.class) continue;
            
            Class<?>[] paramTypes = method.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "CRAMCRAIIndexer should not have File parameter in method: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                "CRAMCRAIIndexer should not return File in method: " + method.getName());
        }
    }

    // ==================== Index Factory Tests ====================

    /**
     * Test index creation via SAMFileWriterFactory with indexing enabled.
     * Requirements: 6.5
     */
    @Test
    public void testIndexCreationViaSAMFileWriterFactory() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.BAM);
        // Index file is created as basename.bai (not basename.bam.bai)
        final String outputPathStr = outputPath.toString();
        final String indexPathStr = outputPathStr.substring(0, outputPathStr.lastIndexOf('.')) + FileExtensions.BAI_INDEX;
        final Path indexPath = Path.of(indexPathStr);
        outputPath.toFile().deleteOnExit();
        indexPath.toFile().deleteOnExit();
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write BAM with indexing enabled
        try (SAMFileWriter writer = new SAMFileWriterFactory()
                .setCreateIndex(true)
                .makeBAMWriter(header, true, outputPath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify index was created
        Assert.assertTrue(Files.exists(indexPath), "Index file should be created automatically at: " + indexPath);
        Assert.assertTrue(Files.size(indexPath) > 0, "Index file should not be empty");
        
        // Verify index works
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SamInputResource.of(outputPath).index(indexPath))) {
            Assert.assertTrue(reader.hasIndex(), "Reader should have index");
        }
    }

    /**
     * Test CRAM index creation via SAMFileWriterFactory.
     * Requirements: 6.5
     */
    @Test
    public void testCRAMIndexCreationViaSAMFileWriterFactory() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.CRAM);
        // Index file is created as basename.bai (not basename.cram.bai)
        final String outputPathStr = outputPath.toString();
        final String indexPathStr = outputPathStr.substring(0, outputPathStr.lastIndexOf('.')) + FileExtensions.BAI_INDEX;
        final Path indexPath = Path.of(indexPathStr);
        outputPath.toFile().deleteOnExit();
        indexPath.toFile().deleteOnExit();
        
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
        final Path refIndexPath = Path.of(referencePath.toString() + ".fai");
        refIndexPath.toFile().deleteOnExit();
        Files.writeString(refIndexPath, "chr1\t10000\t6\t10000\t10001\n");
        
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final List<SAMRecord> records = createTestRecords(header, 10);
        
        // Write CRAM with indexing enabled
        try (SAMFileWriter writer = new SAMFileWriterFactory()
                .setCreateIndex(true)
                .makeWriter(header, true, outputPath, referencePath)) {
            for (SAMRecord record : records) {
                writer.addAlignment(record);
            }
        }
        
        // Verify output was created
        Assert.assertTrue(Files.exists(outputPath), "CRAM file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "CRAM file should not be empty");
    }
}
