package htsjdk.samtools.reference;

import htsjdk.HtsjdkTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;

/**
 * Unit tests for ReferenceSequenceFileFactory Path and URI methods.
 * Validates Requirements 2.3, 2.5 from the File to Path migration spec.
 */
public class ReferenceSequenceFileFactoryPathTest extends HtsjdkTest {

    private static final Path REFERENCE_PATH = Path.of("src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.fasta");
    private static final Path REFERENCE_PATH_BGZIP = Path.of("src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.fasta.gz");

    // ==================== Path-based tests ====================

    @Test
    public void testGetReferenceSequenceFileWithPath() {
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(REFERENCE_PATH);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof AbstractFastaSequenceFile, "Should return AbstractFastaSequenceFile");
    }

    @Test
    public void testGetReferenceSequenceFileWithPathTruncateNames() {
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(REFERENCE_PATH, true);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof IndexedFastaSequenceFile, "Should return IndexedFastaSequenceFile when indexed");
    }

    @Test
    public void testGetReferenceSequenceFileWithPathPreferIndexed() {
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(REFERENCE_PATH, true, true);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof IndexedFastaSequenceFile, "Should return IndexedFastaSequenceFile when preferIndexed=true");
    }

    @Test
    public void testGetReferenceSequenceFileWithPathNonIndexed() {
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(REFERENCE_PATH, true, false);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof FastaSequenceFile, "Should return FastaSequenceFile when preferIndexed=false");
    }

    @Test
    public void testGetReferenceSequenceFileWithBlockCompressedPath() {
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(REFERENCE_PATH_BGZIP, true);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof BlockCompressedIndexedFastaSequenceFile, 
                "Should return BlockCompressedIndexedFastaSequenceFile for bgzip file");
    }

    // ==================== URI-based tests ====================

    @Test
    public void testGetReferenceSequenceFileWithURI() throws IOException {
        final URI uri = REFERENCE_PATH.toUri();
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(uri);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof AbstractFastaSequenceFile, "Should return AbstractFastaSequenceFile");
    }

    @Test
    public void testGetReferenceSequenceFileWithURITruncateNames() throws IOException {
        final URI uri = REFERENCE_PATH.toUri();
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(uri, true);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof IndexedFastaSequenceFile, "Should return IndexedFastaSequenceFile when indexed");
    }

    @Test
    public void testGetReferenceSequenceFileWithURIPreferIndexed() throws IOException {
        final URI uri = REFERENCE_PATH.toUri();
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(uri, true, true);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof IndexedFastaSequenceFile, "Should return IndexedFastaSequenceFile when preferIndexed=true");
    }

    @Test
    public void testGetReferenceSequenceFileWithURINonIndexed() throws IOException {
        final URI uri = REFERENCE_PATH.toUri();
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(uri, true, false);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null");
        Assert.assertTrue(rsf instanceof FastaSequenceFile, "Should return FastaSequenceFile when preferIndexed=false");
    }

    @Test
    public void testGetReferenceSequenceFileWithFileSchemeURI() throws IOException {
        // Test with explicit file:// URI
        final URI uri = URI.create("file://" + REFERENCE_PATH.toAbsolutePath().toString());
        final ReferenceSequenceFile rsf = ReferenceSequenceFileFactory.getReferenceSequenceFile(uri);
        Assert.assertNotNull(rsf, "ReferenceSequenceFile should not be null for file:// URI");
    }

    // ==================== Error handling tests ====================

    @Test(expectedExceptions = IOException.class)
    public void testGetReferenceSequenceFileWithInvalidURI() throws IOException {
        // Test with a URI that has an unsupported scheme
        final URI invalidUri = URI.create("invalid-scheme://some/path.fasta");
        ReferenceSequenceFileFactory.getReferenceSequenceFile(invalidUri);
    }

    // ==================== Utility method tests ====================

    @Test
    public void testGetDefaultDictionaryForReferenceSequence() {
        final Path dictPath = ReferenceSequenceFileFactory.getDefaultDictionaryForReferenceSequence(REFERENCE_PATH);
        Assert.assertNotNull(dictPath, "Dictionary path should not be null");
        Assert.assertTrue(dictPath.toString().endsWith(".dict"), "Dictionary path should end with .dict");
    }

    @Test
    public void testGetFastaIndexFileName() {
        final Path indexPath = ReferenceSequenceFileFactory.getFastaIndexFileName(REFERENCE_PATH);
        Assert.assertNotNull(indexPath, "Index path should not be null");
        Assert.assertTrue(indexPath.toString().endsWith(".fai"), "Index path should end with .fai");
    }

    @Test
    public void testCanCreateIndexedFastaReader() {
        Assert.assertTrue(ReferenceSequenceFileFactory.canCreateIndexedFastaReader(REFERENCE_PATH),
                "Should be able to create indexed reader for indexed FASTA");
    }

    @DataProvider(name = "fastaExtensions")
    public Object[][] fastaExtensions() {
        return new Object[][] {
                { Path.of("test.fasta"), ".fasta" },
                { Path.of("test.fa"), ".fa" },
                { Path.of("test.fna"), ".fna" },
                { Path.of("test.fasta.gz"), ".fasta.gz" },
                { Path.of("test.fa.gz"), ".fa.gz" }
        };
    }

    @Test(dataProvider = "fastaExtensions")
    public void testGetFastaExtension(final Path path, final String expectedExtension) {
        final String extension = ReferenceSequenceFileFactory.getFastaExtension(path);
        Assert.assertEquals(extension, expectedExtension, "FASTA extension should match");
    }

    // ==================== No File-based methods verification ====================

    @Test
    public void testNoFileBasedPublicMethods() {
        final Method[] methods = ReferenceSequenceFileFactory.class.getMethods();
        for (final Method method : methods) {
            // Check parameter types
            for (final Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "ReferenceSequenceFileFactory should not have File parameters: " + method.getName());
            }
            // Check return type
            Assert.assertNotEquals(method.getReturnType(), File.class,
                    "ReferenceSequenceFileFactory should not return File: " + method.getName());
        }
    }
}
