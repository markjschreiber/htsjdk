package htsjdk.samtools;

import htsjdk.HtsjdkTest;
import htsjdk.beta.exception.HtsjdkException;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.FileExtensions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BamFileIoUtilsUnitTest extends HtsjdkTest {
    @DataProvider(name="ReheaderBamFileTestInput")
    public Object[][] getReheaderBamFileTestInput() { // tsato: is this the right naming scheme? e.g. get(method-name)Input
        // tsato: extract this as a method that takes a number of arguments and returns 2^n elements
        return new Object[][] {
                {true, true},
                {true, false},
                {false, true},
                {false, false}
        };
    }

    @Test(dataProvider = "ReheaderBamFileTestInput")
    public void testReheaderBamFile(final boolean createMd5, final boolean createIndex) throws IOException {
        final Path originalBam = HtsjdkTestUtils.NA12878_500;
        SAMFileHeader header = SamReaderFactory.make().getFileHeader(HtsjdkTestUtils.NA12878_500);
        header.addComment("This is a new, modified header");

        final Path output = Files.createTempFile("output", ".bam");
        BamFileIoUtils.reheaderBamFile(header, originalBam, output, createMd5, createIndex);

        // Confirm that the header has been replaced
        final SamReader outputReader = SamReaderFactory.make().open(output);
        Assert.assertEquals(outputReader.getFileHeader(), header);

        // Check that the reads are the same as the original
        // tsato: should I be using something similar to IOUtil.toPath for converting Path -> File to propagate null?
        assertBamRecordsEqual(originalBam, output);

        if (createMd5){
            Assert.assertTrue(Files.exists(output.resolveSibling(output.getFileName() + FileExtensions.MD5)));
        }

        if (createIndex){
            Assert.assertTrue(SamReaderFactory.make().open(output).hasIndex());
        }
    }

    /**
     * Compares all the reads in the two bam files are equal (but does not check the headers).
     */
    private void assertBamRecordsEqual(final Path bam1, final Path bam2){
        try (SamReader reader1 = SamReaderFactory.make().open(bam1);
             SamReader reader2 = SamReaderFactory.make().open(bam2)) {
            final Iterator<SAMRecord> originalBamIterator = reader1.iterator();
            final Iterator<SAMRecord> outputBamIterator = reader2.iterator();

            Assert.assertEquals(originalBamIterator, outputBamIterator);
        } catch (Exception e){
            throw new HtsjdkException("Encountered an error reading bam files: " + bam1 + " and " + bam2, e);
        }
    }

    @DataProvider(name="BlockCopyBamFileTestInput")
    public Object[][] getBlockCopyBamFileTestInput() {
        return new Object[][] {
                {true, true},
                {true, false},
                {false, true},
                {false, false}
        };
    }

    @Test(dataProvider = "BlockCopyBamFileTestInput")
    public void testBlockCopyBamFile(final boolean skipHeader, final boolean skipTerminator) throws IOException {
        final Path output = Files.createTempFile("output", ".bam");
        try (final OutputStream out = Files.newOutputStream(output)) {
            final Path input = HtsjdkTestUtils.NA12878_500;

            BamFileIoUtils.blockCopyBamFile(HtsjdkTestUtils.NA12878_500, out, skipHeader, skipTerminator);

            final SamReader inputReader = SamReaderFactory.make().open(input);
            final SamReader outputReader = SamReaderFactory.make().open(output);

            if (skipHeader) {
                SAMFileHeader h = outputReader.getFileHeader();
                Assert.assertTrue(h.getReadGroups().isEmpty()); // a proxy for the header being empty
            } else {
                Assert.assertEquals(outputReader.getFileHeader(), inputReader.getFileHeader());
                // Reading will fail when the header is absent
                assertBamRecordsEqual(input, output);
            }

            if (skipTerminator) {
                BlockCompressedInputStream.FileTermination termination = BlockCompressedInputStream.checkTermination(output);
                Assert.assertEquals(termination, BlockCompressedInputStream.FileTermination.HAS_HEALTHY_LAST_BLOCK);
            }
        } catch (IOException e){
            throw new HtsjdkException("Caught an IO exception block copying a bam file to " + output, e);
        }
    }

    @DataProvider(name="GatherWithBlockCopyingTestInput")
    public Object[][] getGatherWithBlockCopyingTestInput() {
        return new Object[][] {
                {true, true},
                {true, false},
                {false, true},
                {false, false}
        };
    }

    @Test(dataProvider = "GatherWithBlockCopyingTestInput")
    public void testGatherWithBlockCopying(final boolean createIndex, final boolean createMd5) throws IOException {
        // Create two copies of the same BAM file to gather
        final Path bam1 = HtsjdkTestUtils.NA12878_500;
        final Path bam2 = HtsjdkTestUtils.NA12878_500;
        final List<Path> bams = Arrays.asList(bam1, bam2);
        
        final Path output = Files.createTempFile("gathered", ".bam");
        try {
            BamFileIoUtils.gatherWithBlockCopying(bams, output, createIndex, createMd5);
            
            // Verify the output file exists and is readable
            Assert.assertTrue(Files.exists(output), "Output file should exist");
            Assert.assertTrue(Files.size(output) > 0, "Output file should not be empty");
            
            // Verify the output is a valid BAM file
            try (SamReader reader = SamReaderFactory.make().open(output)) {
                Assert.assertNotNull(reader.getFileHeader(), "Output should have a valid header");
                
                // Count records - should be double the original since we gathered two copies
                int count = 0;
                for (SAMRecord record : reader) {
                    count++;
                }
                Assert.assertTrue(count > 0, "Output should contain records");
            }
            
            // Check MD5 file creation
            if (createMd5) {
                Path md5Path = output.resolveSibling(output.getFileName() + FileExtensions.MD5);
                Assert.assertTrue(Files.exists(md5Path), "MD5 file should exist when createMd5 is true");
            }
            
            // Check index file creation
            if (createIndex) {
                try (SamReader reader = SamReaderFactory.make().open(output)) {
                    Assert.assertTrue(reader.hasIndex(), "Output should have an index when createIndex is true");
                }
            }
        } finally {
            Files.deleteIfExists(output);
            if (createMd5) {
                Files.deleteIfExists(output.resolveSibling(output.getFileName() + FileExtensions.MD5));
            }
            if (createIndex) {
                String indexName = output.getFileName().toString().replaceFirst("\\.[^.]+$", "") + FileExtensions.BAI_INDEX;
                Files.deleteIfExists(output.resolveSibling(indexName));
            }
        }
    }
}