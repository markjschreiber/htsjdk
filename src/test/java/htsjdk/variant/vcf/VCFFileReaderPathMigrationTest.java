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
package htsjdk.variant.vcf;

import htsjdk.HtsjdkTest;
import htsjdk.variant.variantcontext.VariantContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;

/**
 * Tests for verifying VCFFileReader works correctly with Path-based APIs
 * and that no File-based constructors exist.
 * 
 * Requirements: 3.4, 3.6
 */
public class VCFFileReaderPathMigrationTest extends HtsjdkTest {

    private static final Path TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/variant");
    
    // VCF test files
    private static final Path VCF_FILE = TEST_DATA_DIR.resolve("VCF4HeaderTest.vcf");
    private static final Path VCF_WITH_INDEX = TEST_DATA_DIR.resolve("test1.vcf");
    private static final Path VCF_INDEX = TEST_DATA_DIR.resolve("test1.vcf.idx");
    private static final Path VCF_BGZIP = TEST_DATA_DIR.resolve("test.vcf.bgz");
    private static final Path VCF_BGZIP_INDEX = TEST_DATA_DIR.resolve("test.vcf.bgz.tbi");
    private static final Path BCF_FILE = TEST_DATA_DIR.resolve("serialization_test.bcf");
    private static final Path BCF_INDEX = TEST_DATA_DIR.resolve("serialization_test.bcf.idx");

    // ==================== VCFFileReader Tests with Path ====================

    /**
     * Test VCFFileReader with Path to test VCF file.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderWithPath() {
        try (VCFFileReader reader = new VCFFileReader(VCF_FILE, false)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
        }
    }

    /**
     * Test VCFFileReader with Path and index requirement.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderWithPathAndIndex() {
        try (VCFFileReader reader = new VCFFileReader(VCF_WITH_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with index");
        }
    }

    /**
     * Test VCFFileReader with explicit index Path.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderWithExplicitIndexPath() {
        try (VCFFileReader reader = new VCFFileReader(VCF_WITH_INDEX, VCF_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with explicit index");
        }
    }

    /**
     * Test VCFFileReader reads records correctly with Path.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderReadsRecordsCorrectly() {
        try (VCFFileReader reader = new VCFFileReader(VCF_FILE, false)) {
            int recordCount = 0;
            for (VariantContext vc : reader) {
                Assert.assertNotNull(vc, "VariantContext should not be null");
                Assert.assertNotNull(vc.getContig(), "Contig should not be null");
                Assert.assertTrue(vc.getStart() > 0, "Start position should be positive");
                recordCount++;
                if (recordCount >= 5) break; // Just verify first few records
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
        }
    }

    /**
     * Test VCFFileReader with bgzipped VCF file and tabix index.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderWithBgzipAndTabix() {
        try (VCFFileReader reader = new VCFFileReader(VCF_BGZIP, VCF_BGZIP_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with tabix index");
            
            // Verify we can read records
            int recordCount = 0;
            for (VariantContext vc : reader) {
                Assert.assertNotNull(vc, "VariantContext should not be null");
                recordCount++;
                if (recordCount >= 5) break;
            }
            Assert.assertTrue(recordCount > 0, "Should be able to read at least one record");
        }
    }

    /**
     * Test VCFFileReader with BCF file.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderWithBCF() {
        try (VCFFileReader reader = new VCFFileReader(BCF_FILE, BCF_INDEX, true)) {
            Assert.assertNotNull(reader, "VCFFileReader should not be null");
            Assert.assertNotNull(reader.getFileHeader(), "File header should not be null");
            Assert.assertTrue(reader.isQueryable(), "Reader should be queryable with BCF index");
        }
    }

    /**
     * Test VCFFileReader.getSequenceDictionary static method with Path.
     * Uses a VCF file that has contig lines in the header.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderGetSequenceDictionaryWithPath() {
        // Use a VCF file that has contig lines (dbsnp file has them)
        Path vcfWithContigs = TEST_DATA_DIR.resolve("dbsnp_135.b37.1000.vcf");
        var dict = VCFFileReader.getSequenceDictionary(vcfWithContigs);
        Assert.assertNotNull(dict, "Sequence dictionary should not be null");
        Assert.assertTrue(dict.size() > 0, "Sequence dictionary should have entries");
    }

    /**
     * Test VCFFileReader.isBCF static method with Path.
     * Requirements: 3.4
     */
    @Test
    public void testVCFFileReaderIsBCFWithPath() {
        Assert.assertTrue(VCFFileReader.isBCF(BCF_FILE), "BCF file should be detected as BCF");
        Assert.assertFalse(VCFFileReader.isBCF(VCF_FILE), "VCF file should not be detected as BCF");
    }

    // ==================== No File-based Constructors Tests ====================

    /**
     * Verify VCFFileReader has no File-based constructors.
     * Requirements: 3.6
     */
    @Test
    public void testVCFFileReaderHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = VCFFileReader.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "VCFFileReader should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify VCFFileReader uses Path not File.
     * Requirements: 3.6
     */
    @Test
    public void testVCFFileReaderUsesPathNotFile() {
        // This test verifies that we can successfully use VCFFileReader with Path
        // If VCFFileReader required File, this would fail
        try (VCFFileReader reader = new VCFFileReader(VCF_FILE, false)) {
            Assert.assertNotNull(reader.getFileHeader());
        }
    }
}
