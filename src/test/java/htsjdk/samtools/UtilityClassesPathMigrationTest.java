package htsjdk.samtools;

import htsjdk.HtsjdkTest;
import htsjdk.samtools.filter.JavascriptSamRecordFilter;
import htsjdk.samtools.filter.ReadNameFilter;
import htsjdk.tribble.Tribble;
import htsjdk.variant.variantcontext.filter.JavascriptVariantFilter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Tests for utility classes Path migration.
 * Validates that utility classes use Path-based APIs and have no File-based methods.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
public class UtilityClassesPathMigrationTest extends HtsjdkTest {

    // ==================== SamFiles Tests ====================

    @DataProvider(name = "fileTypeDetectionData")
    public Object[][] fileTypeDetectionData() {
        return new Object[][] {
            // BAM files
            { "test.bam", true, false, false },
            { "test.sam.bam", true, false, false },
            { ".bam", true, false, false },
            { "/path/to/test.bam", true, false, false },
            { "test..bam", true, false, false },
            
            // SAM files
            { "test.sam", false, true, false },
            { "test.bam.sam", false, true, false },
            { ".sam", false, true, false },
            { "/path/to/test.sam", false, true, false },
            
            // CRAM files
            { "test.cram", false, false, true },
            { "test.bam.cram", false, false, true },
            { ".cram", false, false, true },
            { "/path/to/test.cram", false, false, true },
            
            // Non-matching files
            { "test.txt", false, false, false },
            { "test.BAM", false, false, false },  // Case sensitive
            { "test.SAM", false, false, false },
            { "test.CRAM", false, false, false },
            { "testbam", false, false, false },
            { "testsam", false, false, false },
            { "testcram", false, false, false },
        };
    }

    @Test(dataProvider = "fileTypeDetectionData")
    public void testSamFilesFileTypeDetection(String filename, boolean isBAM, boolean isSAM, boolean isCRAM) {
        Path path = Path.of(filename);
        Assert.assertEquals(SamFiles.isBAMFile(path), isBAM, "isBAMFile for " + filename);
        Assert.assertEquals(SamFiles.isSAMFile(path), isSAM, "isSAMFile for " + filename);
        Assert.assertEquals(SamFiles.isCRAMFile(path), isCRAM, "isCRAMFile for " + filename);
    }

    @Test
    public void testSamFilesFileTypeDetectionWithNull() {
        Assert.assertFalse(SamFiles.isBAMFile(null), "isBAMFile should return false for null");
        Assert.assertFalse(SamFiles.isSAMFile(null), "isSAMFile should return false for null");
        Assert.assertFalse(SamFiles.isCRAMFile(null), "isCRAMFile should return false for null");
    }

    // ==================== Tribble Tests ====================

    @Test
    public void testTribbleIndexPathConstruction() {
        Path vcfPath = Path.of("/data/test.vcf");
        Path indexPath = Tribble.indexPath(vcfPath);
        
        Assert.assertNotNull(indexPath, "Index path should not be null");
        Assert.assertTrue(indexPath.toString().endsWith(".idx"), 
            "Index path should end with .idx: " + indexPath);
        Assert.assertTrue(indexPath.toString().contains("test.vcf"), 
            "Index path should contain original filename: " + indexPath);
    }

    @Test
    public void testTribbleTabixIndexPathConstruction() {
        Path vcfPath = Path.of("/data/test.vcf.gz");
        Path indexPath = Tribble.tabixIndexPath(vcfPath);
        
        Assert.assertNotNull(indexPath, "Tabix index path should not be null");
        Assert.assertTrue(indexPath.toString().endsWith(".tbi"), 
            "Tabix index path should end with .tbi: " + indexPath);
        Assert.assertTrue(indexPath.toString().contains("test.vcf.gz"), 
            "Tabix index path should contain original filename: " + indexPath);
    }

    @Test
    public void testTribbleIndexPathPreservesFilesystem() {
        // Test that index path is in the same filesystem as the data path
        Path dataPath = Path.of("/data/test.vcf");
        Path indexPath = Tribble.indexPath(dataPath);
        
        Assert.assertEquals(indexPath.getFileSystem(), dataPath.getFileSystem(),
            "Index path should be in the same filesystem as data path");
    }

    // ==================== No File-based Methods Tests ====================

    @Test
    public void testSamFilesHasNoFileBasedMethods() {
        assertNoFileBasedPublicMethods(SamFiles.class);
    }

    @Test
    public void testBamFileIoUtilsHasNoFileBasedMethods() {
        assertNoFileBasedPublicMethods(BamFileIoUtils.class);
    }

    @Test
    public void testTribbleHasNoFileBasedMethods() {
        assertNoFileBasedPublicMethods(Tribble.class);
    }

    @Test
    public void testJavascriptSamRecordFilterHasNoFileBasedConstructors() {
        assertNoFileBasedPublicMethods(JavascriptSamRecordFilter.class);
    }

    @Test
    public void testReadNameFilterHasNoFileBasedConstructors() {
        assertNoFileBasedPublicMethods(ReadNameFilter.class);
    }

    @Test
    public void testJavascriptVariantFilterHasNoFileBasedConstructors() {
        assertNoFileBasedPublicMethods(JavascriptVariantFilter.class);
    }

    /**
     * Asserts that a class has no public methods or constructors that accept or return java.io.File.
     */
    private void assertNoFileBasedPublicMethods(Class<?> clazz) {
        // Check methods
        for (Method method : clazz.getMethods()) {
            // Skip methods inherited from Object
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            
            // Check parameter types
            for (Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                    clazz.getSimpleName() + "." + method.getName() + " should not have File parameter");
            }
            
            // Check return type
            Assert.assertNotEquals(method.getReturnType(), File.class,
                clazz.getSimpleName() + "." + method.getName() + " should not return File");
        }
        
        // Check constructors
        Arrays.stream(clazz.getConstructors())
            .filter(c -> Modifier.isPublic(c.getModifiers()))
            .forEach(constructor -> {
                for (Class<?> paramType : constructor.getParameterTypes()) {
                    Assert.assertNotEquals(paramType, File.class,
                        clazz.getSimpleName() + " constructor should not have File parameter");
                }
            });
    }
}
