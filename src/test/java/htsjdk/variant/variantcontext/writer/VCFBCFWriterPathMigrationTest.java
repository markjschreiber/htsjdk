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
package htsjdk.variant.variantcontext.writer;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.FileExtensions;
import htsjdk.variant.VariantBaseTest;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for verifying VCF and BCF writers work correctly with Path-based APIs
 * and that no File-based constructors exist.
 * 
 * Requirements: 4.4, 4.5, 4.7
 */
public class VCFBCFWriterPathMigrationTest extends VariantBaseTest {

    // ==================== Helper Methods ====================

    /**
     * Create a VCF header for testing.
     */
    private VCFHeader createVCFHeader() {
        final SAMSequenceDictionary sequenceDict = createArtificialSequenceDictionary();
        final Set<VCFHeaderLine> metaData = new HashSet<>();
        final Set<String> sampleNames = new HashSet<>();
        
        metaData.add(new VCFHeaderLine("source", "VCFBCFWriterPathMigrationTest"));
        sampleNames.add("sample1");
        sampleNames.add("sample2");
        
        final VCFHeader header = new VCFHeader(metaData, sampleNames);
        header.addMetaDataLine(new VCFInfoHeaderLine("DP", 1, VCFHeaderLineType.Integer, "Total Depth"));
        header.addMetaDataLine(new VCFFormatHeaderLine("GT", 1, VCFHeaderLineType.String, "Genotype"));
        header.addMetaDataLine(new VCFFormatHeaderLine("GQ", 1, VCFHeaderLineType.Integer, "Genotype Quality"));
        header.setSequenceDictionary(sequenceDict);
        
        return header;
    }

    /**
     * Create a test VariantContext.
     */
    private VariantContext createVariantContext(VCFHeader header, String contig, int position) {
        final List<Allele> alleles = new ArrayList<>();
        alleles.add(Allele.create("A", true));
        alleles.add(Allele.create("G", false));
        
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("DP", 50);
        
        final GenotypesContext genotypes = GenotypesContext.create(header.getGenotypeSamples().size());
        for (final String sampleName : header.getGenotypeSamples()) {
            final Genotype gt = new GenotypeBuilder(sampleName, alleles.subList(0, 2))
                    .GQ(30)
                    .phased(false)
                    .make();
            genotypes.add(gt);
        }
        
        return new VariantContextBuilder("test", contig, position, position, alleles)
                .genotypes(genotypes)
                .attributes(attributes)
                .make();
    }

    // ==================== VCFWriter Tests ====================

    /**
     * Test VCFWriter writing to Path.
     * Requirements: 4.4
     */
    @Test
    public void testVCFWriterWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.VCF);
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .clearOptions()
                .build()) {
            
            writer.writeHeader(header);
            writer.add(createVariantContext(header, "1", 100));
            writer.add(createVariantContext(header, "1", 200));
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output VCF file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output VCF file should not be empty");
    }

    /**
     * Test VCFWriter written files can be read back correctly.
     * Requirements: 4.4
     */
    @Test
    public void testVCFWriterRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.VCF);
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        final int expectedRecordCount = 5;
        
        // Write records
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .clearOptions()
                .build()) {
            
            writer.writeHeader(header);
            for (int i = 0; i < expectedRecordCount; i++) {
                writer.add(createVariantContext(header, "1", 100 + i * 100));
            }
        }
        
        // Read back and verify
        try (VCFFileReader reader = new VCFFileReader(outputPath, false)) {
            int recordCount = 0;
            for (VariantContext vc : reader) {
                Assert.assertNotNull(vc, "VariantContext should not be null");
                Assert.assertEquals(vc.getContig(), "1", "Contig should match");
                recordCount++;
            }
            Assert.assertEquals(recordCount, expectedRecordCount, "Should read back same number of records");
        }
    }

    /**
     * Test VCFWriter with compressed output.
     * Requirements: 4.4
     */
    @Test
    public void testVCFWriterCompressed() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.COMPRESSED_VCF);
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .clearOptions()
                .build()) {
            
            writer.writeHeader(header);
            writer.add(createVariantContext(header, "1", 100));
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output compressed VCF file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output compressed VCF file should not be empty");
        
        // Verify it can be read back
        try (VCFFileReader reader = new VCFFileReader(outputPath, false)) {
            int count = 0;
            for (@SuppressWarnings("unused") VariantContext vc : reader) {
                count++;
            }
            Assert.assertEquals(count, 1, "Should read back one record");
        }
    }

    // ==================== BCF2Writer Tests ====================

    /**
     * Test BCF2Writer writing to Path.
     * Requirements: 4.5
     */
    @Test
    public void testBCF2WriterWithPath() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".bcf");
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .unsetOption(Options.INDEX_ON_THE_FLY)
                .build()) {
            
            writer.writeHeader(header);
            writer.add(createVariantContext(header, "1", 100));
            writer.add(createVariantContext(header, "1", 200));
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output BCF file should exist");
        Assert.assertTrue(Files.size(outputPath) > 0, "Output BCF file should not be empty");
    }

    /**
     * Test BCF2Writer written files can be read back correctly.
     * Requirements: 4.5
     */
    @Test
    public void testBCF2WriterRoundTrip() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".bcf");
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        final int expectedRecordCount = 5;
        
        // Write records
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .unsetOption(Options.INDEX_ON_THE_FLY)
                .build()) {
            
            writer.writeHeader(header);
            for (int i = 0; i < expectedRecordCount; i++) {
                writer.add(createVariantContext(header, "1", 100 + i * 100));
            }
        }
        
        // Read back and verify
        try (VCFFileReader reader = new VCFFileReader(outputPath, false)) {
            int recordCount = 0;
            for (VariantContext vc : reader) {
                Assert.assertNotNull(vc, "VariantContext should not be null");
                Assert.assertEquals(vc.getContig(), "1", "Contig should match");
                recordCount++;
            }
            Assert.assertEquals(recordCount, expectedRecordCount, "Should read back same number of records");
        }
    }

    /**
     * Test BCF2Writer is created for .bcf extension.
     * Requirements: 4.5
     */
    @Test
    public void testBCF2WriterCreatedForBCFExtension() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".bcf");
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .unsetOption(Options.INDEX_ON_THE_FLY)
                .build()) {
            
            Assert.assertTrue(writer instanceof BCF2Writer, "Should create BCF2Writer for .bcf extension");
        }
    }

    // ==================== No File-based Constructors Tests ====================

    /**
     * Verify VCFWriter has no File-based constructors.
     * Requirements: 4.7
     */
    @Test
    public void testVCFWriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = VCFWriter.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "VCFWriter should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify BCF2Writer has no File-based constructors.
     * Requirements: 4.7
     */
    @Test
    public void testBCF2WriterHasNoFileBasedConstructors() {
        Constructor<?>[] constructors = BCF2Writer.class.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                Assert.assertNotEquals(paramType, File.class, 
                    "BCF2Writer should not have File-based constructor: " + constructor);
            }
        }
    }

    /**
     * Verify VCFWriter uses Path not File.
     * Requirements: 4.7
     */
    @Test
    public void testVCFWriterUsesPathNotFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.VCF);
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .clearOptions()
                .build()) {
            
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof VCFWriter, "Should create VCFWriter for .vcf path");
        }
    }

    /**
     * Verify BCF2Writer uses Path not File.
     * Requirements: 4.7
     */
    @Test
    public void testBCF2WriterUsesPathNotFile() throws IOException {
        final Path outputPath = Files.createTempFile("test.", ".bcf");
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .unsetOption(Options.INDEX_ON_THE_FLY)
                .build()) {
            
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof BCF2Writer, "Should create BCF2Writer for .bcf path");
        }
    }

    // ==================== Additional Writer Tests ====================

    /**
     * Test VCFWriter with index on the fly.
     * Requirements: 4.4
     */
    @Test
    public void testVCFWriterWithIndexOnTheFly() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.VCF);
        outputPath.toFile().deleteOnExit();
        final Path indexPath = Path.of(outputPath.toString() + FileExtensions.TRIBBLE_INDEX);
        indexPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .setOption(Options.INDEX_ON_THE_FLY)
                .build()) {
            
            writer.writeHeader(header);
            writer.add(createVariantContext(header, "1", 100));
            writer.add(createVariantContext(header, "1", 200));
        }
        
        Assert.assertTrue(Files.exists(outputPath), "Output VCF file should exist");
        Assert.assertTrue(Files.exists(indexPath), "Index file should exist");
    }

    /**
     * Test writing multiple variants and verifying order.
     * Requirements: 4.4, 4.5
     */
    @Test
    public void testWriteMultipleVariantsPreservesOrder() throws IOException {
        final Path outputPath = Files.createTempFile("test.", FileExtensions.VCF);
        outputPath.toFile().deleteOnExit();
        
        final VCFHeader header = createVCFHeader();
        final int[] positions = {100, 200, 300, 400, 500};
        
        // Write records
        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputPath(outputPath)
                .setReferenceDictionary(header.getSequenceDictionary())
                .clearOptions()
                .build()) {
            
            writer.writeHeader(header);
            for (int pos : positions) {
                writer.add(createVariantContext(header, "1", pos));
            }
        }
        
        // Read back and verify order
        try (VCFFileReader reader = new VCFFileReader(outputPath, false)) {
            int index = 0;
            for (VariantContext vc : reader) {
                Assert.assertEquals(vc.getStart(), positions[index], 
                    "Position should match at index " + index);
                index++;
            }
            Assert.assertEquals(index, positions.length, "Should read all records");
        }
    }
}
