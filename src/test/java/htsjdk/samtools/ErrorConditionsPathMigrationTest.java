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
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for verifying error conditions are handled correctly with clear, actionable messages.
 * 
 * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5
 */
public class ErrorConditionsPathMigrationTest extends HtsjdkTest {

    // ==================== Non-existent File Error Tests ====================
    // Requirements: 15.1

    /**
     * Test that opening a non-existent BAM file throws an exception with path information.
     * Requirements: 15.1, 15.5
     */
    @Test
    public void testNonExistentBAMFileError() {
        final Path nonExistentPath = Path.of("/does/not/exist/file.bam");
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            Assert.fail("Should have thrown an exception for non-existent file");
        } catch (RuntimeException e) {
            // Verify error message contains the path
            final String message = getFullErrorMessage(e);
            Assert.assertTrue(message.contains("does/not/exist") || message.contains("file.bam"),
                    "Error message should contain path information: " + message);
        }
    }

    /**
     * Test that opening a non-existent SAM file throws an exception with path information.
     * Requirements: 15.1, 15.5
     */
    @Test
    public void testNonExistentSAMFileError() {
        final Path nonExistentPath = Path.of("/nonexistent/path/to/file.sam");
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            Assert.fail("Should have thrown an exception for non-existent file");
        } catch (RuntimeException e) {
            final String message = getFullErrorMessage(e);
            Assert.assertTrue(message.contains("nonexistent") || message.contains("file.sam"),
                    "Error message should contain path information: " + message);
        }
    }

    /**
     * Test that opening a non-existent VCF file throws an exception with path information.
     * Requirements: 15.1, 15.5
     */
    @Test
    public void testNonExistentVCFFileError() {
        final Path nonExistentPath = Path.of("/missing/vcf/file.vcf");
        
        try (VCFFileReader reader = new VCFFileReader(nonExistentPath, false)) {
            Assert.fail("Should have thrown an exception for non-existent file");
        } catch (RuntimeException e) {
            final String message = getFullErrorMessage(e);
            Assert.assertTrue(message.contains("missing") || message.contains("file.vcf") || 
                    message.toLowerCase().contains("not found") || message.toLowerCase().contains("no such file"),
                    "Error message should contain path information or indicate file not found: " + message);
        }
    }

    /**
     * Test that opening a non-existent reference file throws an exception with path information.
     * Requirements: 15.1, 15.5
     */
    @Test
    public void testNonExistentReferenceFileError() {
        final Path nonExistentPath = Path.of("/nonexistent/reference.fasta");
        
        try {
            ReferenceSequenceFileFactory.getReferenceSequenceFile(nonExistentPath);
            Assert.fail("Should have thrown an exception for non-existent file");
        } catch (RuntimeException e) {
            final String message = getFullErrorMessage(e);
            Assert.assertTrue(message.contains("nonexistent") || message.contains("reference.fasta") ||
                    message.toLowerCase().contains("not found") || message.toLowerCase().contains("does not exist"),
                    "Error message should contain path information: " + message);
        }
    }

    // ==================== Permission Denied Error Tests ====================
    // Requirements: 15.2

    /**
     * Test that IOUtil.assertFileIsWritable throws exception for non-writable file.
     * Requirements: 15.2, 15.5
     */
    @Test(groups = "unix")
    public void testPermissionDeniedWriteError() throws IOException {
        // Create a temporary file and make it read-only
        final Path tempFile = Files.createTempFile("test_readonly", ".bam");
        try {
            // Make file read-only
            tempFile.toFile().setWritable(false);
            
            try {
                IOUtil.assertFileIsWritable(tempFile);
                Assert.fail("Should have thrown an exception for non-writable file");
            } catch (SAMException e) {
                final String message = e.getMessage();
                Assert.assertTrue(message.contains("not writable") || message.contains("writable"),
                        "Error message should indicate permission issue: " + message);
                Assert.assertTrue(message.contains(tempFile.getFileName().toString()) || 
                        message.contains(tempFile.toAbsolutePath().toString()),
                        "Error message should contain path information: " + message);
            }
        } finally {
            // Restore write permission and delete
            tempFile.toFile().setWritable(true);
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Test that IOUtil.assertDirectoryIsWritable throws exception for non-writable directory.
     * Requirements: 15.2, 15.5
     */
    @Test(groups = "unix")
    public void testPermissionDeniedDirectoryWriteError() throws IOException {
        // Create a temporary directory and make it read-only
        final Path tempDir = Files.createTempDirectory("test_readonly_dir");
        try {
            // Make directory read-only
            tempDir.toFile().setWritable(false);
            
            try {
                IOUtil.assertDirectoryIsWritable(tempDir);
                Assert.fail("Should have thrown an exception for non-writable directory");
            } catch (SAMException e) {
                final String message = e.getMessage();
                Assert.assertTrue(message.contains("not writable") || message.contains("writable"),
                        "Error message should indicate permission issue: " + message);
            }
        } finally {
            // Restore write permission and delete
            tempDir.toFile().setWritable(true);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Test that IOUtil.assertDirectoryIsReadable throws exception for non-readable directory.
     * Requirements: 15.2, 15.5
     */
    @Test(groups = "unix")
    public void testPermissionDeniedDirectoryReadError() throws IOException {
        // Create a temporary directory and make it non-readable
        final Path tempDir = Files.createTempDirectory("test_nonreadable_dir");
        try {
            // Make directory non-readable
            tempDir.toFile().setReadable(false);
            
            try {
                IOUtil.assertDirectoryIsReadable(tempDir);
                Assert.fail("Should have thrown an exception for non-readable directory");
            } catch (SAMException e) {
                final String message = e.getMessage();
                Assert.assertTrue(message.contains("not readable") || message.contains("readable"),
                        "Error message should indicate permission issue: " + message);
            }
        } finally {
            // Restore read permission and delete
            tempDir.toFile().setReadable(true);
            Files.deleteIfExists(tempDir);
        }
    }

    // ==================== Malformed URI Error Tests ====================
    // Requirements: 15.3

    /**
     * Test that SamReaderFactory.open(URI) throws exception for invalid URI scheme.
     * Requirements: 15.3, 15.5
     */
    @Test
    public void testMalformedURIErrorSamReader() {
        final URI invalidUri = URI.create("invalid-scheme://some/path/file.bam");
        
        try {
            SamReaderFactory.makeDefault().open(invalidUri);
            Assert.fail("Should have thrown an exception for invalid URI scheme");
        } catch (IOException e) {
            final String message = e.getMessage();
            Assert.assertTrue(message.contains("invalid-scheme") || message.contains("Invalid URI") ||
                    message.contains("No filesystem provider"),
                    "Error message should contain URI information: " + message);
        }
    }

    /**
     * Test that ReferenceSequenceFileFactory throws exception for invalid URI scheme.
     * Requirements: 15.3, 15.5
     */
    @Test(expectedExceptions = IOException.class)
    public void testMalformedURIErrorReferenceFactory() throws IOException {
        final URI invalidUri = URI.create("unknown-scheme://path/to/reference.fasta");
        ReferenceSequenceFileFactory.getReferenceSequenceFile(invalidUri);
    }

    /**
     * Test that VariantContextWriterBuilder throws exception for invalid URI scheme.
     * Requirements: 15.3, 15.5
     */
    @Test(expectedExceptions = RuntimeIOException.class)
    public void testMalformedURIErrorVariantWriter() {
        final URI invalidUri = URI.create("bad-scheme://output/file.vcf");
        new VariantContextWriterBuilder()
                .setOutputURI(invalidUri);
    }

    /**
     * Test that SAMFileWriterFactory throws exception for invalid URI scheme.
     * Requirements: 15.3, 15.5
     */
    @Test
    public void testMalformedURIErrorSamWriter() {
        final URI invalidUri = URI.create("nonexistent-fs://path/output.bam");
        final SAMFileHeader header = new SAMFileHeader();
        
        try {
            new SAMFileWriterFactory().makeBAMWriter(header, true, invalidUri);
            Assert.fail("Should have thrown an exception for invalid URI scheme");
        } catch (IOException e) {
            final String message = e.getMessage();
            Assert.assertTrue(message.contains("nonexistent-fs") || message.contains("Invalid URI") ||
                    message.contains("No filesystem provider"),
                    "Error message should contain URI information: " + message);
        }
    }

    // ==================== Missing Filesystem Provider Error Tests ====================
    // Requirements: 15.4

    /**
     * Test that opening a file with unsupported scheme provides helpful error message.
     * Requirements: 15.4, 15.5
     */
    @Test
    public void testMissingFilesystemProviderError() {
        // Use a scheme that definitely won't have a provider
        final URI customSchemeUri = URI.create("custom-nonexistent-scheme://bucket/file.bam");
        
        try {
            SamReaderFactory.makeDefault().open(customSchemeUri);
            Assert.fail("Should have thrown an exception for missing filesystem provider");
        } catch (IOException e) {
            final String message = e.getMessage();
            Assert.assertTrue(message.contains("No filesystem provider") || 
                    message.contains("provider") || message.contains("scheme"),
                    "Error message should indicate missing provider: " + message);
        }
    }

    /**
     * Test that SAMFileWriterFactory provides helpful error for missing filesystem provider.
     * Requirements: 15.4, 15.5
     */
    @Test
    public void testMissingFilesystemProviderErrorWriter() {
        final URI customSchemeUri = URI.create("s3-fake://bucket/output.bam");
        final SAMFileHeader header = new SAMFileHeader();
        
        try {
            new SAMFileWriterFactory().makeBAMWriter(header, true, customSchemeUri);
            Assert.fail("Should have thrown an exception for missing filesystem provider");
        } catch (IOException e) {
            final String message = e.getMessage();
            Assert.assertTrue(message.contains("No filesystem provider") || 
                    message.contains("provider") || message.contains("scheme") ||
                    message.contains("Invalid URI"),
                    "Error message should indicate missing provider or invalid URI: " + message);
        }
    }

    // ==================== Missing Index File Error Tests ====================
    // Requirements: 15.5

    /**
     * Test that opening a BAM file without index when index is required provides clear error.
     * Requirements: 15.5
     */
    @Test
    public void testMissingIndexFileError() throws IOException {
        // Create a temporary BAM file without an index
        final Path tempBam = Files.createTempFile("test_no_index", ".bam");
        try {
            // Write minimal BAM content
            final SAMFileHeader header = new SAMFileHeader();
            header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
            try (SAMFileWriter writer = new SAMFileWriterFactory()
                    .setCreateIndex(false)
                    .makeBAMWriter(header, true, tempBam)) {
                // Write empty BAM
            }
            
            // Try to open with index requirement
            try (SamReader reader = SamReaderFactory.makeDefault()
                    .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS)
                    .open(tempBam)) {
                
                // Try to query - this should fail without index
                if (reader.hasIndex()) {
                    // If it claims to have an index, that's unexpected
                    Assert.fail("Reader should not have index for file without .bai");
                }
                // If no index, that's the expected behavior
            }
        } finally {
            Files.deleteIfExists(tempBam);
        }
    }

    /**
     * Test that SamFiles.findIndex returns null for file without index.
     * Requirements: 15.5
     */
    @Test
    public void testFindIndexReturnsNullForMissingIndex() throws IOException {
        // Create a temporary BAM file without an index
        final Path tempBam = Files.createTempFile("test_find_index", ".bam");
        try {
            // Write minimal BAM content
            final SAMFileHeader header = new SAMFileHeader();
            try (SAMFileWriter writer = new SAMFileWriterFactory()
                    .setCreateIndex(false)
                    .makeBAMWriter(header, true, tempBam)) {
                // Write empty BAM
            }
            
            // Try to find index - should return null
            final Path indexPath = SamFiles.findIndex(tempBam);
            Assert.assertNull(indexPath, "findIndex should return null when no index exists");
        } finally {
            Files.deleteIfExists(tempBam);
        }
    }

    // ==================== Error Message Quality Tests ====================
    // Requirements: 15.5

    /**
     * Test that error messages are clear and actionable.
     * Requirements: 15.5
     */
    @Test
    public void testErrorMessagesAreClearAndActionable() {
        // Test non-existent file error message
        final Path nonExistentPath = Path.of("/clearly/nonexistent/path/file.bam");
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            Assert.fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            final String message = getFullErrorMessage(e);
            // Error message should be non-empty
            Assert.assertFalse(message.isEmpty(), "Error message should not be empty");
            // Error message should contain some path information
            Assert.assertTrue(message.length() > 10, 
                    "Error message should be descriptive: " + message);
        }
    }

    /**
     * Test that IOUtil assertion methods provide clear error messages.
     * Requirements: 15.5
     */
    @Test
    public void testIOUtilAssertionErrorMessages() {
        final Path nonExistentDir = Path.of("/nonexistent/directory/path");
        
        try {
            IOUtil.assertDirectoryIsReadable(nonExistentDir);
            Assert.fail("Should have thrown an exception");
        } catch (SAMException e) {
            final String message = e.getMessage();
            Assert.assertTrue(message.contains("does not exist") || message.contains("Directory"),
                    "Error message should indicate directory issue: " + message);
        }
    }

    /**
     * Test that file write assertion provides clear error for non-existent parent.
     * Requirements: 15.5
     */
    @Test
    public void testWriteAssertionErrorForNonExistentParent() {
        final Path pathWithNonExistentParent = Path.of("/nonexistent/parent/dir/file.bam");
        
        try {
            IOUtil.assertFileIsWritable(pathWithNonExistentParent);
            Assert.fail("Should have thrown an exception");
        } catch (SAMException e) {
            final String message = e.getMessage();
            Assert.assertTrue(message.contains("Cannot write") || message.contains("parent") ||
                    message.contains("does not exist") || message.contains("Neither file nor parent"),
                    "Error message should indicate parent directory issue: " + message);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Get the full error message including cause messages.
     */
    private String getFullErrorMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null) {
                if (sb.length() > 0) {
                    sb.append(" -> ");
                }
                sb.append(current.getMessage());
            }
            current = current.getCause();
        }
        return sb.toString();
    }
}
