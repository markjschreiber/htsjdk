package htsjdk.samtools;

import java.nio.file.Path;

import htsjdk.HtsjdkTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Test valid combinations of bam/cram vs bai/crai files.
 * Created by vadim on 10/08/2015.
 */
public class SamFilesTest extends HtsjdkTest {
    private static final String TEST_DATA = "src/test/resources/htsjdk/samtools/BAMFileIndexTest/";
    private static final Path BAM_FILE = Path.of(TEST_DATA + "index_test.bam");

    @DataProvider(name = "FindIndexParams")
    public static Object[][] paramsFindIndexForSuffixes() {
        return new Object[][]{
                // no index available sanity checks:
                {".tmp", null, null},
                {".bam", null, null},
                {".cram", null, null},

                // legit cases for BAM files:
                {".bam", ".bai", ".bai"},
                {".bam", ".bam.bai", ".bam.bai"},

                // legit cases for CRAM files:
                {".cram", ".cram.bai", ".cram.bai"},
                {".cram", ".cram.crai", ".cram.crai"},

                // special prohibited cases:
                {".bam", ".crai", null},
                {".tmp", ".crai", null},
        };
    }

    @Test(dataProvider = "FindIndexParams")
    public void testFindIndexForSuffixes(final String dataFileSuffix, final String indexFileSuffix, final String expectIndexSuffix) throws IOException {
        final Path dataFile = Files.createTempFile("test", dataFileSuffix);
        try {
            Assert.assertNull(SamFiles.findIndex(dataFile));

            Path indexFile = null;
            if (indexFileSuffix != null) {
                final String dataFileStr = dataFile.toString();
                final String indexFileStr = dataFileStr.replaceFirst("\\.\\S+$", indexFileSuffix);
                indexFile = Path.of(indexFileStr);
                Files.createFile(indexFile);
            }

            final Path foundIndexPath = SamFiles.findIndex(dataFile);
            if (expectIndexSuffix == null) {
                Assert.assertNull(foundIndexPath);
            } else {
                Assert.assertNotNull(foundIndexPath);
                Assert.assertTrue(foundIndexPath.getFileName().toString().endsWith(expectIndexSuffix));
            }
            
            // Clean up index file if created
            if (indexFile != null) {
                Files.deleteIfExists(indexFile);
            }
        } finally {
            Files.deleteIfExists(dataFile);
        }
    }

    @DataProvider(name = "filesAndIndicies")
    public Object[][] getFilesAndIndicies() throws IOException {

        final Path REAL_INDEX_FILE = Path.of(BAM_FILE + ".bai"); //test regular file
        final Path SYMLINKED_BAM_WITH_SYMLINKED_INDEX = Path.of(TEST_DATA, "symlink_with_index.bam");

        return new Object[][]{
                {BAM_FILE, REAL_INDEX_FILE},
                {SYMLINKED_BAM_WITH_SYMLINKED_INDEX, Path.of(SYMLINKED_BAM_WITH_SYMLINKED_INDEX + ".bai")},
                {Path.of(TEST_DATA, "symlink_without_linked_index.bam"), REAL_INDEX_FILE.toRealPath()},
                {Path.of(TEST_DATA, "FileThatDoesntExist"), null}
        };
    }

    @Test(dataProvider ="filesAndIndicies")
    public void testIndexSymlinking(Path bam, Path expected_index) {
        Assert.assertEquals(SamFiles.findIndex(bam), expected_index);
    }
}
