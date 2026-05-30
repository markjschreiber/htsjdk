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
package htsjdk.samtools.reference;

import htsjdk.HtsjdkTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for reference sequence classes with Path.
 * Validates Requirements 7.1, 7.2, 7.3, 7.4, 7.5 from the File to Path migration spec.
 */
public class ReferenceSequencePathMigrationTest extends HtsjdkTest {

    private static final Path TEST_DATA_DIR = Path.of("src/test/resources/htsjdk/samtools/reference");
    private static final Path FASTA_PATH = TEST_DATA_DIR.resolve("Homo_sapiens_assembly18.trimmed.fasta");
    private static final Path FASTA_INDEX_PATH = TEST_DATA_DIR.resolve("Homo_sapiens_assembly18.trimmed.fasta.fai");
    private static final Path FASTA_NO_INDEX_PATH = TEST_DATA_DIR.resolve("Homo_sapiens_assembly18.trimmed.noindex.fasta");

    // ==================== FastaSequenceFile Path Tests (Requirement 7.1) ====================

    @Test
    public void testFastaSequenceFileWithPath() throws IOException {
        try (final FastaSequenceFile fastaFile = new FastaSequenceFile(FASTA_NO_INDEX_PATH, true)) {
            Assert.assertNotNull(fastaFile, "FastaSequenceFile should not be null");
            
            // Read first sequence
            final ReferenceSequence seq = fastaFile.nextSequence();
            Assert.assertNotNull(seq, "First sequence should not be null");
            Assert.assertEquals(seq.getName(), "chrM", "First sequence name should be chrM");
        }
    }

    @Test
    public void testFastaSequenceFileReadSequences() throws IOException {
        try (final FastaSequenceFile fastaFile = new FastaSequenceFile(FASTA_NO_INDEX_PATH, true)) {
            // Read all sequences and verify
            ReferenceSequence seq;
            int count = 0;
            while ((seq = fastaFile.nextSequence()) != null) {
                Assert.assertNotNull(seq.getName(), "Sequence name should not be null");
                Assert.assertTrue(seq.getBases().length > 0, "Sequence should have bases");
                count++;
            }
            Assert.assertTrue(count > 0, "Should have read at least one sequence");
        }
    }

    @Test
    public void testFastaSequenceFileReset() throws IOException {
        try (final FastaSequenceFile fastaFile = new FastaSequenceFile(FASTA_NO_INDEX_PATH, true)) {
            // Read first sequence
            final ReferenceSequence seq1 = fastaFile.nextSequence();
            Assert.assertNotNull(seq1, "First sequence should not be null");
            
            // Reset and read again
            fastaFile.reset();
            final ReferenceSequence seq2 = fastaFile.nextSequence();
            Assert.assertNotNull(seq2, "Sequence after reset should not be null");
            Assert.assertEquals(seq2.getName(), seq1.getName(), "Sequence names should match after reset");
        }
    }


    // ==================== IndexedFastaSequenceFile Path Tests (Requirement 7.2) ====================

    @Test
    public void testIndexedFastaSequenceFileWithPath() throws IOException {
        try (final IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(FASTA_PATH)) {
            Assert.assertNotNull(indexedFasta, "IndexedFastaSequenceFile should not be null");
            Assert.assertTrue(indexedFasta.isIndexed(), "File should be indexed");
        }
    }

    @Test
    public void testIndexedFastaSequenceFileWithPathAndIndex() throws IOException {
        final FastaSequenceIndex index = new FastaSequenceIndex(FASTA_INDEX_PATH);
        try (final IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(FASTA_PATH, index)) {
            Assert.assertNotNull(indexedFasta, "IndexedFastaSequenceFile should not be null");
            Assert.assertTrue(indexedFasta.isIndexed(), "File should be indexed");
        }
    }

    @Test
    public void testIndexedFastaSequenceFileGetSequence() throws IOException {
        try (final IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(FASTA_PATH)) {
            // Get sequence by name
            final ReferenceSequence seq = indexedFasta.getSequence("chrM");
            Assert.assertNotNull(seq, "Sequence chrM should not be null");
            Assert.assertEquals(seq.getName(), "chrM", "Sequence name should be chrM");
            Assert.assertTrue(seq.getBases().length > 0, "Sequence should have bases");
        }
    }

    @Test
    public void testIndexedFastaSequenceFileGetSubsequence() throws IOException {
        try (final IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(FASTA_PATH)) {
            // Get subsequence
            final ReferenceSequence subseq = indexedFasta.getSubsequenceAt("chrM", 1, 100);
            Assert.assertNotNull(subseq, "Subsequence should not be null");
            Assert.assertEquals(subseq.getBases().length, 100, "Subsequence should have 100 bases");
        }
    }

    @Test
    public void testIndexedFastaSequenceFileSequenceDictionary() throws IOException {
        try (final IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(FASTA_PATH)) {
            Assert.assertNotNull(indexedFasta.getSequenceDictionary(), "Sequence dictionary should not be null");
            Assert.assertTrue(indexedFasta.getSequenceDictionary().size() > 0, "Dictionary should have sequences");
        }
    }


    // ==================== FastaSequenceIndex Path Tests (Requirement 7.3) ====================

    @Test
    public void testFastaSequenceIndexWithPath() {
        final FastaSequenceIndex index = new FastaSequenceIndex(FASTA_INDEX_PATH);
        Assert.assertNotNull(index, "FastaSequenceIndex should not be null");
        Assert.assertTrue(index.size() > 0, "Index should have entries");
    }

    @Test
    public void testFastaSequenceIndexHasEntry() {
        final FastaSequenceIndex index = new FastaSequenceIndex(FASTA_INDEX_PATH);
        Assert.assertTrue(index.hasIndexEntry("chrM"), "Index should have chrM entry");
    }

    @Test
    public void testFastaSequenceIndexGetEntry() {
        final FastaSequenceIndex index = new FastaSequenceIndex(FASTA_INDEX_PATH);
        final FastaSequenceIndexEntry entry = index.getIndexEntry("chrM");
        Assert.assertNotNull(entry, "Index entry should not be null");
        Assert.assertEquals(entry.getContig(), "chrM", "Contig name should be chrM");
        Assert.assertTrue(entry.getSize() > 0, "Entry should have size > 0");
    }

    @Test
    public void testFastaSequenceIndexIteration() {
        final FastaSequenceIndex index = new FastaSequenceIndex(FASTA_INDEX_PATH);
        int count = 0;
        for (final FastaSequenceIndexEntry entry : index) {
            Assert.assertNotNull(entry.getContig(), "Contig name should not be null");
            count++;
        }
        Assert.assertEquals(count, index.size(), "Iteration count should match size");
    }

    @Test
    public void testFastaSequenceIndexWrite() throws IOException {
        final FastaSequenceIndex index = new FastaSequenceIndex(FASTA_INDEX_PATH);
        final Path tempFile = Files.createTempFile("test_index", ".fai");
        tempFile.toFile().deleteOnExit();
        
        // Write index to temp file
        index.write(tempFile);
        
        // Read it back and verify
        final FastaSequenceIndex reloadedIndex = new FastaSequenceIndex(tempFile);
        Assert.assertEquals(reloadedIndex.size(), index.size(), "Reloaded index should have same size");
        Assert.assertEquals(reloadedIndex, index, "Reloaded index should equal original");
    }


    // ==================== ReferenceSequenceFileWalker Path Tests (Requirement 7.4) ====================

    @Test
    public void testReferenceSequenceFileWalkerWithPath() throws IOException {
        try (final ReferenceSequenceFileWalker walker = new ReferenceSequenceFileWalker(FASTA_PATH)) {
            Assert.assertNotNull(walker, "ReferenceSequenceFileWalker should not be null");
        }
    }

    @Test
    public void testReferenceSequenceFileWalkerGet() throws IOException {
        try (final ReferenceSequenceFileWalker walker = new ReferenceSequenceFileWalker(FASTA_PATH)) {
            // Get first sequence by index
            final ReferenceSequence seq = walker.get(0);
            Assert.assertNotNull(seq, "Sequence at index 0 should not be null");
            Assert.assertEquals(seq.getContigIndex(), 0, "Contig index should be 0");
        }
    }

    @Test
    public void testReferenceSequenceFileWalkerGetMultiple() throws IOException {
        try (final ReferenceSequenceFileWalker walker = new ReferenceSequenceFileWalker(FASTA_PATH)) {
            // Get sequences in order
            final ReferenceSequence seq0 = walker.get(0);
            Assert.assertNotNull(seq0, "Sequence at index 0 should not be null");
            
            final ReferenceSequence seq1 = walker.get(1);
            Assert.assertNotNull(seq1, "Sequence at index 1 should not be null");
            Assert.assertEquals(seq1.getContigIndex(), 1, "Contig index should be 1");
        }
    }

    @Test
    public void testReferenceSequenceFileWalkerSequenceDictionary() throws IOException {
        try (final ReferenceSequenceFileWalker walker = new ReferenceSequenceFileWalker(FASTA_PATH)) {
            Assert.assertNotNull(walker.getSequenceDictionary(), "Sequence dictionary should not be null");
        }
    }


    // ==================== No File-based methods verification (Requirement 7.5) ====================

    @Test
    public void testFastaSequenceFileNoFileBasedConstructors() {
        final Constructor<?>[] constructors = FastaSequenceFile.class.getConstructors();
        for (final Constructor<?> constructor : constructors) {
            for (final Class<?> paramType : constructor.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "FastaSequenceFile should not have File constructor parameters: " + constructor);
            }
        }
    }

    @Test
    public void testFastaSequenceFileNoFileBasedMethods() {
        final Method[] methods = FastaSequenceFile.class.getMethods();
        for (final Method method : methods) {
            for (final Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "FastaSequenceFile should not have File parameters: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                    "FastaSequenceFile should not return File: " + method.getName());
        }
    }

    @Test
    public void testIndexedFastaSequenceFileNoFileBasedConstructors() {
        final Constructor<?>[] constructors = IndexedFastaSequenceFile.class.getConstructors();
        for (final Constructor<?> constructor : constructors) {
            for (final Class<?> paramType : constructor.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "IndexedFastaSequenceFile should not have File constructor parameters: " + constructor);
            }
        }
    }

    @Test
    public void testIndexedFastaSequenceFileNoFileBasedMethods() {
        final Method[] methods = IndexedFastaSequenceFile.class.getMethods();
        for (final Method method : methods) {
            for (final Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "IndexedFastaSequenceFile should not have File parameters: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                    "IndexedFastaSequenceFile should not return File: " + method.getName());
        }
    }

    @Test
    public void testFastaSequenceIndexNoFileBasedConstructors() {
        final Constructor<?>[] constructors = FastaSequenceIndex.class.getConstructors();
        for (final Constructor<?> constructor : constructors) {
            for (final Class<?> paramType : constructor.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "FastaSequenceIndex should not have File constructor parameters: " + constructor);
            }
        }
    }

    @Test
    public void testFastaSequenceIndexNoFileBasedMethods() {
        final Method[] methods = FastaSequenceIndex.class.getMethods();
        for (final Method method : methods) {
            for (final Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "FastaSequenceIndex should not have File parameters: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                    "FastaSequenceIndex should not return File: " + method.getName());
        }
    }

    @Test
    public void testReferenceSequenceFileWalkerNoFileBasedConstructors() {
        final Constructor<?>[] constructors = ReferenceSequenceFileWalker.class.getConstructors();
        for (final Constructor<?> constructor : constructors) {
            for (final Class<?> paramType : constructor.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "ReferenceSequenceFileWalker should not have File constructor parameters: " + constructor);
            }
        }
    }

    @Test
    public void testReferenceSequenceFileWalkerNoFileBasedMethods() {
        final Method[] methods = ReferenceSequenceFileWalker.class.getMethods();
        for (final Method method : methods) {
            for (final Class<?> paramType : method.getParameterTypes()) {
                Assert.assertNotEquals(paramType, File.class,
                        "ReferenceSequenceFileWalker should not have File parameters: " + method.getName());
            }
            Assert.assertNotEquals(method.getReturnType(), File.class,
                    "ReferenceSequenceFileWalker should not return File: " + method.getName());
        }
    }
}
