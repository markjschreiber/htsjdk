package htsjdk.variant.variantcontext.writer;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.variant.VariantBaseTest;
import htsjdk.variant.vcf.VCFHeader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for VariantContextWriterBuilder Path and URI methods.
 * Validates Requirements 2.4, 2.5 from the File to Path migration spec.
 */
public class VariantContextWriterBuilderPathTest extends VariantBaseTest {

    private SAMSequenceDictionary dictionary;
    private Path tempVcfPath;
    private Path tempBcfPath;

    @BeforeClass
    public void setUp() throws IOException {
        dictionary = createArtificialSequenceDictionary();
        tempVcfPath = Files.createTempFile("test", ".vcf");
        tempVcfPath.toFile().deleteOnExit();
        tempBcfPath = Files.createTempFile("test", ".bcf");
        tempBcfPath.toFile().deleteOnExit();
    }

    // ==================== Path-based tests ====================

    @Test
    public void testSetOutputPathVCF() {
        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputPath(tempVcfPath);

        try (final VariantContextWriter writer = builder.build()) {
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof VCFWriter, "Should create VCFWriter for .vcf path");
        }
    }

    @Test
    public void testSetOutputPathBCF() {
        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputPath(tempBcfPath);

        try (final VariantContextWriter writer = builder.build()) {
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof BCF2Writer, "Should create BCF2Writer for .bcf path");
        }
    }

    @Test
    public void testSetOutputPathWithJimfs() throws IOException {
        try (final FileSystem fs = Jimfs.newFileSystem("test", Configuration.unix())) {
            final Path vcfPath = fs.getPath("/test.vcf");
            
            final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                    .setReferenceDictionary(dictionary)
                    .clearOptions()
                    .setOutputPath(vcfPath);

            try (final VariantContextWriter writer = builder.build()) {
                Assert.assertNotNull(writer, "Writer should not be null for jimfs path");
                Assert.assertTrue(writer instanceof VCFWriter, "Should create VCFWriter for jimfs .vcf path");
                
                // Write a header to verify it works
                writer.writeHeader(new VCFHeader());
            }
            
            // Verify file was created
            Assert.assertTrue(Files.exists(vcfPath), "VCF file should exist in jimfs");
        }
    }

    // ==================== URI-based tests ====================

    @Test
    public void testSetOutputURIVCF() throws IOException {
        final Path tempPath = Files.createTempFile("uri-test", ".vcf");
        tempPath.toFile().deleteOnExit();
        final URI uri = tempPath.toUri();

        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputURI(uri);

        try (final VariantContextWriter writer = builder.build()) {
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof VCFWriter, "Should create VCFWriter for .vcf URI");
        }
    }

    @Test
    public void testSetOutputURIBCF() throws IOException {
        final Path tempPath = Files.createTempFile("uri-test", ".bcf");
        tempPath.toFile().deleteOnExit();
        final URI uri = tempPath.toUri();

        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputURI(uri);

        try (final VariantContextWriter writer = builder.build()) {
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof BCF2Writer, "Should create BCF2Writer for .bcf URI");
        }
    }

    @Test
    public void testSetOutputURIWithFileScheme() throws IOException {
        final Path tempPath = Files.createTempFile("file-scheme-test", ".vcf");
        tempPath.toFile().deleteOnExit();
        // Create explicit file:// URI
        final URI uri = URI.create("file://" + tempPath.toAbsolutePath().toString());

        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputURI(uri);

        try (final VariantContextWriter writer = builder.build()) {
            Assert.assertNotNull(writer, "Writer should not be null for file:// URI");
            Assert.assertTrue(writer instanceof VCFWriter, "Should create VCFWriter for file:// URI");
        }
    }

    // ==================== Error handling tests ====================

    @Test(expectedExceptions = RuntimeIOException.class)
    public void testSetOutputURIWithInvalidScheme() {
        final URI invalidUri = URI.create("invalid-scheme://some/path.vcf");
        new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputURI(invalidUri);
    }

    // ==================== String-based convenience method tests ====================

    @Test
    public void testSetOutputFileString() throws IOException {
        final Path tempPath = Files.createTempFile("string-test", ".vcf");
        tempPath.toFile().deleteOnExit();

        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .setOutputFile(tempPath.toString());

        try (final VariantContextWriter writer = builder.build()) {
            Assert.assertNotNull(writer, "Writer should not be null");
            Assert.assertTrue(writer instanceof VCFWriter, "Should create VCFWriter for string path");
        }
    }

    // ==================== Output type determination tests ====================

    @Test
    public void testDetermineOutputTypeFromPathVCF() {
        final VariantContextWriterBuilder.OutputType type = 
                VariantContextWriterBuilder.determineOutputTypeFromFile(tempVcfPath);
        Assert.assertEquals(type, VariantContextWriterBuilder.OutputType.VCF, 
                "Should determine VCF type from .vcf path");
    }

    @Test
    public void testDetermineOutputTypeFromPathBCF() {
        final VariantContextWriterBuilder.OutputType type = 
                VariantContextWriterBuilder.determineOutputTypeFromFile(tempBcfPath);
        Assert.assertEquals(type, VariantContextWriterBuilder.OutputType.BCF, 
                "Should determine BCF type from .bcf path");
    }

    @Test
    public void testDetermineOutputTypeFromPathCompressedVCF() throws IOException {
        final Path compressedPath = Files.createTempFile("test", ".vcf.gz");
        compressedPath.toFile().deleteOnExit();
        
        final VariantContextWriterBuilder.OutputType type = 
                VariantContextWriterBuilder.determineOutputTypeFromFile(compressedPath);
        Assert.assertEquals(type, VariantContextWriterBuilder.OutputType.BLOCK_COMPRESSED_VCF, 
                "Should determine BLOCK_COMPRESSED_VCF type from .vcf.gz path");
    }

    // ==================== No File-based methods verification ====================

    @Test
    public void testNoFileBasedPublicMethods() {
        final Method[] methods = VariantContextWriterBuilder.class.getMethods();
        for (final Method method : methods) {
            // Check parameter types - File class should not be used
            for (final Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "VariantContextWriterBuilder should not have File parameters: " + method.getName());
            }
            // Check return type - File class should not be returned
            Assert.assertNotEquals(method.getReturnType(), File.class,
                    "VariantContextWriterBuilder should not return File: " + method.getName());
        }
    }

    // ==================== Integration tests ====================

    @Test
    public void testWriteAndReadVCFWithPath() throws IOException {
        final Path tempPath = Files.createTempFile("write-read-test", ".vcf");
        tempPath.toFile().deleteOnExit();

        // Write a VCF file
        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .clearOptions()
                .setOutputPath(tempPath);

        try (final VariantContextWriter writer = builder.build()) {
            writer.writeHeader(new VCFHeader());
        }

        // Verify file was created and has content
        Assert.assertTrue(Files.exists(tempPath), "VCF file should exist");
        Assert.assertTrue(Files.size(tempPath) > 0, "VCF file should have content");
    }

    @Test
    public void testWriteVCFWithURIAndVerify() throws IOException {
        final Path tempPath = Files.createTempFile("uri-write-test", ".vcf");
        tempPath.toFile().deleteOnExit();
        final URI uri = tempPath.toUri();

        // Write a VCF file using URI
        final VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
                .setReferenceDictionary(dictionary)
                .clearOptions()
                .setOutputURI(uri);

        try (final VariantContextWriter writer = builder.build()) {
            writer.writeHeader(new VCFHeader());
        }

        // Verify file was created and has content
        Assert.assertTrue(Files.exists(tempPath), "VCF file should exist after URI write");
        Assert.assertTrue(Files.size(tempPath) > 0, "VCF file should have content after URI write");
    }
}
