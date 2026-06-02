package htsjdk.samtools;

import htsjdk.samtools.seekablestream.SeekablePathStream;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Property-based test for BAM index local path preservation behavior.
 *
 * Feature: bam-index-stream-fallback
 * Property 2: Preservation - Local Path Buffer Selection and Query Results Unchanged
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4
 *
 * These tests verify that local path behavior is preserved:
 * - Memory-mapped mode (MemoryMappedFileBuffer) and non-memory-mapped mode (RandomAccessFileBuffer)
 *   produce identical query results
 * - SeekableStream-based construction (IndexStreamBuffer) produces identical results
 * - Local path construction always succeeds
 *
 * IMPORTANT: These tests must PASS on UNFIXED code. They capture baseline behavior
 * that any fix must preserve.
 */
public class BamIndexLocalPathPreservationPropertyTest {

    private static final Path LOCAL_BAI_PATH = Path.of("src/test/resources/htsjdk/samtools/BAMFileIndexTest/index_test.bam.bai");

    /**
     * Number of references in the test BAI file.
     * Determined by opening the index and calling getNumberOfReferences().
     * We use a known upper bound and let the test discover valid references.
     */
    private static final int MAX_REFERENCE_INDEX = 45;

    /**
     * Property: For all valid reference indices and query regions, getSpanOverlapping()
     * returns the same result in memory-mapped mode as in non-memory-mapped mode.
     *
     * This verifies that both buffer implementations (MemoryMappedFileBuffer and
     * RandomAccessFileBuffer) parse the BAI file identically for local paths.
     *
     * Validates: Requirements 3.1, 3.2, 3.4
     */
    @Property(tries = 100)
    void spanOverlappingIdenticalAcrossBufferModes(
            @ForAll @IntRange(min = 0, max = 45) int referenceIndex,
            @ForAll @IntRange(min = 1, max = 500000) int start,
            @ForAll @IntRange(min = 1, max = 500000) int endOffset) {

        int end = start + endOffset; // Ensure end >= start

        // Memory-mapped mode (default constructor) - uses MemoryMappedFileBuffer
        BAMFileSpan memoryMappedResult;
        try (CachingBAMFileIndex mmIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null)) {
            memoryMappedResult = mmIndex.getSpanOverlapping(referenceIndex, start, end);
        }

        // Non-memory-mapped mode - uses RandomAccessFileBuffer
        BAMFileSpan randomAccessResult;
        try (CachingBAMFileIndex raIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null, false)) {
            randomAccessResult = raIndex.getSpanOverlapping(referenceIndex, start, end);
        }

        // Results must be identical regardless of buffer mode
        assertSpansEqual(memoryMappedResult, randomAccessResult,
                String.format("ref=%d, start=%d, end=%d", referenceIndex, start, end));
    }

    /**
     * Property: For all valid reference indices and query regions, getSpanOverlapping()
     * returns the same result via SeekableStream (IndexStreamBuffer) as via memory-mapped mode.
     *
     * This verifies that stream-based construction produces the same results as local file access.
     *
     * Validates: Requirements 3.3, 3.4
     */
    @Property(tries = 100)
    void spanOverlappingIdenticalForStreamVsMemoryMapped(
            @ForAll @IntRange(min = 0, max = 45) int referenceIndex,
            @ForAll @IntRange(min = 1, max = 500000) int start,
            @ForAll @IntRange(min = 1, max = 500000) int endOffset) throws IOException {

        int end = start + endOffset;

        // Memory-mapped mode
        BAMFileSpan memoryMappedResult;
        try (CachingBAMFileIndex mmIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null)) {
            memoryMappedResult = mmIndex.getSpanOverlapping(referenceIndex, start, end);
        }

        // Stream-based mode (uses IndexStreamBuffer)
        BAMFileSpan streamResult;
        try (SeekablePathStream stream = new SeekablePathStream(LOCAL_BAI_PATH);
             CachingBAMFileIndex streamIndex = new CachingBAMFileIndex(stream, null)) {
            streamResult = streamIndex.getSpanOverlapping(referenceIndex, start, end);
        }

        assertSpansEqual(memoryMappedResult, streamResult,
                String.format("ref=%d, start=%d, end=%d (stream vs mmap)", referenceIndex, start, end));
    }

    /**
     * Property: For all valid reference indices, getMetaData() returns the same result
     * regardless of buffer mode (memory-mapped vs non-memory-mapped).
     *
     * Validates: Requirements 3.1, 3.2, 3.4
     */
    @Property(tries = 50)
    void metaDataIdenticalAcrossBufferModes(
            @ForAll @IntRange(min = 0, max = 45) int referenceIndex) {

        // Memory-mapped mode
        BAMIndexMetaData mmMetaData;
        try (CachingBAMFileIndex mmIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null)) {
            mmMetaData = mmIndex.getMetaData(referenceIndex);
        }

        // Non-memory-mapped mode
        BAMIndexMetaData raMetaData;
        try (CachingBAMFileIndex raIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null, false)) {
            raMetaData = raIndex.getMetaData(referenceIndex);
        }

        assertMetaDataEqual(mmMetaData, raMetaData,
                String.format("ref=%d", referenceIndex));
    }

    /**
     * Property: getStartOfLastLinearBin() returns the same value for both memory-mapped
     * and non-memory-mapped modes.
     *
     * Validates: Requirements 3.1, 3.2, 3.4
     */
    @Property(tries = 1)
    void startOfLastLinearBinIdenticalAcrossBufferModes() {
        long mmResult;
        try (CachingBAMFileIndex mmIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null)) {
            mmResult = mmIndex.getStartOfLastLinearBin();
        }

        long raResult;
        try (CachingBAMFileIndex raIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null, false)) {
            raResult = raIndex.getStartOfLastLinearBin();
        }

        assert mmResult == raResult :
                String.format("getStartOfLastLinearBin() differs: memory-mapped=%d, random-access=%d",
                        mmResult, raResult);
    }

    /**
     * Property: Local path construction always succeeds without exception for both
     * memory-mapped and non-memory-mapped modes.
     *
     * Validates: Requirements 3.1, 3.2
     */
    @Property(tries = 10)
    void localPathConstructionAlwaysSucceeds() {
        // Memory-mapped mode
        try (CachingBAMFileIndex mmIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null)) {
            int numRefs = mmIndex.getNumberOfReferences();
            assert numRefs >= 0 : "Memory-mapped index should report valid number of references";
        }

        // Non-memory-mapped mode
        try (CachingBAMFileIndex raIndex = new CachingBAMFileIndex(LOCAL_BAI_PATH, null, false)) {
            int numRefs = raIndex.getNumberOfReferences();
            assert numRefs >= 0 : "Random-access index should report valid number of references";
        }
    }

    /**
     * Helper: Compare two BAMFileSpan results for equality via their chunk lists.
     */
    private void assertSpansEqual(BAMFileSpan span1, BAMFileSpan span2, String context) {
        if (span1 == null && span2 == null) {
            return; // Both null is fine - means no data for this region
        }
        assert (span1 == null) == (span2 == null) :
                String.format("Span nullity mismatch for %s: one is null, other is not", context);

        List<Chunk> chunks1 = span1.getChunks();
        List<Chunk> chunks2 = span2.getChunks();

        assert chunks1.size() == chunks2.size() :
                String.format("Chunk count differs for %s: %d vs %d", context, chunks1.size(), chunks2.size());

        for (int i = 0; i < chunks1.size(); i++) {
            assert chunks1.get(i).equals(chunks2.get(i)) :
                    String.format("Chunk %d differs for %s: %s vs %s",
                            i, context, chunks1.get(i), chunks2.get(i));
        }
    }

    /**
     * Helper: Compare two BAMIndexMetaData results for equality.
     */
    private void assertMetaDataEqual(BAMIndexMetaData meta1, BAMIndexMetaData meta2, String context) {
        if (meta1 == null && meta2 == null) {
            return;
        }
        assert (meta1 == null) == (meta2 == null) :
                String.format("MetaData nullity mismatch for %s: one is null, other is not", context);

        assert meta1.getAlignedRecordCount() == meta2.getAlignedRecordCount() :
                String.format("AlignedRecordCount differs for %s: %d vs %d",
                        context, meta1.getAlignedRecordCount(), meta2.getAlignedRecordCount());

        assert meta1.getUnalignedRecordCount() == meta2.getUnalignedRecordCount() :
                String.format("UnalignedRecordCount differs for %s: %d vs %d",
                        context, meta1.getUnalignedRecordCount(), meta2.getUnalignedRecordCount());

        assert meta1.getFirstOffset() == meta2.getFirstOffset() :
                String.format("FirstOffset differs for %s: %d vs %d",
                        context, meta1.getFirstOffset(), meta2.getFirstOffset());

        assert meta1.getLastOffset() == meta2.getLastOffset() :
                String.format("LastOffset differs for %s: %d vs %d",
                        context, meta1.getLastOffset(), meta2.getLastOffset());
    }
}
