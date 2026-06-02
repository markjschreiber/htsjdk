# BAM Index Stream Fallback Bugfix Design

## Overview

`AbstractBAMFileIndex` unconditionally memory-maps `.bai` files when opened via a `Path`. This fails for non-local paths (e.g., S3 via `s3-nio-spi`) because `MemoryMappedFileBuffer` and `RandomAccessFileBuffer` both require local file semantics. The fix detects non-local paths and falls back to an `IndexStreamBuffer` backed by a `SeekablePathStream`, mirroring the approach already used by `CSIIndex` and tabix indexes.

## Glossary

- **Bug_Condition (C)**: The BAM index path refers to a non-local file system (URI scheme != "file") — memory mapping and `FileChannel`-based random access are impossible
- **Property (P)**: When a non-local path is provided, the index is read successfully via a stream-based buffer (`IndexStreamBuffer` backed by `SeekablePathStream`)
- **Preservation**: Local-path BAM index reading continues to use `MemoryMappedFileBuffer` (when memory mapping is enabled) or `RandomAccessFileBuffer` (when disabled), with identical behavior
- **AbstractBAMFileIndex**: The base class in `htsjdk/samtools/AbstractBAMFileIndex.java` that provides BAM index file reading. Superclass of `DiskBasedBAMFileIndex`, `CachingBAMFileIndex`, and `CSIIndex`
- **IndexFileBuffer**: Interface for random-access reading of index files, implemented by `MemoryMappedFileBuffer`, `RandomAccessFileBuffer`, `IndexStreamBuffer`, and `CompressedIndexFileBuffer`
- **IndexFileBufferFactory**: Factory class that selects the appropriate `IndexFileBuffer` implementation based on path locality and compression
- **SeekablePathStream**: A `SeekableStream` implementation that wraps a `java.nio.file.Path` via `Files.newByteChannel()`, supporting both local and non-local NIO file system providers

## Bug Details

### Bug Condition

The bug manifests when a BAM index is opened via a `Path` whose URI scheme is not `"file"` (i.e., backed by a non-default NIO file system such as S3, GCS, or HDFS). The `AbstractBAMFileIndex(Path, SAMSequenceDictionary)` and `AbstractBAMFileIndex(Path, SAMSequenceDictionary, boolean)` constructors unconditionally instantiate `MemoryMappedFileBuffer` or `RandomAccessFileBuffer`, both of which require local file system semantics that non-local paths cannot provide.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type Path (the .bai index path)
  OUTPUT: boolean

  LET uri = input.toUri()
  LET scheme = uri.getScheme()
  
  RETURN scheme != "file"
         OR NOT input.getFileSystem().equals(FileSystems.getDefault())
END FUNCTION
```

### Examples

- **S3 Path**: `Path.of(URI.create("s3://bucket/sample.bam.bai"))` → throws `IOException` or `UnsupportedOperationException` from `FileChannel.open()` because S3 paths cannot be memory-mapped
- **GCS Path**: `Path.of(URI.create("gs://bucket/sample.bam.bai"))` → same failure as above; `FileChannel.map()` is not supported for GCS NIO provider
- **HDFS Path**: `Path.of(URI.create("hdfs://namenode/data/sample.bai"))` → `FileChannel.open()` fails because HDFS doesn't support local `FileChannel` semantics
- **Local Path (no bug)**: `Path.of("/data/sample.bam.bai")` → works correctly with `MemoryMappedFileBuffer`

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Local `Path` with memory mapping enabled must continue to use `MemoryMappedFileBuffer` for efficient random access
- Local `Path` with memory mapping disabled must continue to use `RandomAccessFileBuffer` for random access without memory mapping
- `SeekableStream`-based construction must continue to use `IndexStreamBuffer` directly
- All index parsing logic (magic number verification, bin/chunk reading, linear index) must remain unchanged regardless of buffer implementation
- `CSIIndex` behavior must remain unchanged (it already handles non-local paths correctly)

**Scope:**
All inputs where the path is a local file (scheme == "file" on the default file system) should be completely unaffected by this fix. This includes:
- All existing local `.bai` file access patterns
- Memory-mapped vs non-memory-mapped mode selection for local files
- Direct `SeekableStream` construction path
- Compressed index (CSI) handling

## Hypothesized Root Cause

Based on the bug description, the root cause is:

1. **Missing Path Locality Check in `AbstractBAMFileIndex`**: The `AbstractBAMFileIndex(Path, SAMSequenceDictionary)` constructor unconditionally creates a `MemoryMappedFileBuffer(path)`. `MemoryMappedFileBuffer` calls `FileChannel.open(path)` followed by `fileChannel.map()`, which requires the path to be on a local file system that supports memory-mapped I/O.

2. **Missing Path Locality Check in `AbstractBAMFileIndex(Path, SAMSequenceDictionary, boolean)`**: Even when `useMemoryMapping` is `false`, this constructor falls back to `RandomAccessFileBuffer(path)`, which also uses `FileChannel.open(path)`. Non-local NIO providers may not support `FileChannel` at all, or may not support positional random access via it.

3. **Missing Fallback in `IndexFileBufferFactory.getBuffer(Path, boolean)`**: The factory method makes the same assumption — it only chooses between `MemoryMappedFileBuffer`, `RandomAccessFileBuffer`, and `CompressedIndexFileBuffer`, all of which require local path semantics.

4. **Existing Solution Pattern Not Applied**: `CSIIndex(Path, SAMSequenceDictionary)` already solves this by wrapping the `Path` in a `SeekablePathStream` and delegating to the stream-based constructor. `SeekablePathStream` uses `Files.newByteChannel(path)`, which is supported by all NIO file system providers. This same pattern was not applied to the BAI constructors.

## Correctness Properties

Property 1: Bug Condition - Non-Local Path Opens Successfully

_For any_ BAM index `Path` where `isBugCondition(path)` is true (the path's URI scheme is not "file" or the path is not on the default file system), the fixed `AbstractBAMFileIndex` constructors SHALL successfully open the index using a stream-based buffer (`IndexStreamBuffer` backed by `SeekablePathStream`) and produce valid index query results identical to what would be produced if the same index content were read from a local path.

**Validates: Requirements 2.1, 2.2**

Property 2: Preservation - Local Path Behavior Unchanged

_For any_ BAM index `Path` where `isBugCondition(path)` is false (the path is a local file on the default file system), the fixed `AbstractBAMFileIndex` constructors SHALL use the same buffer implementation as before (`MemoryMappedFileBuffer` when memory mapping is enabled, `RandomAccessFileBuffer` when disabled), producing identical index query results and maintaining the same performance characteristics.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `src/main/java/htsjdk/samtools/AbstractBAMFileIndex.java`

**Constructors**: `AbstractBAMFileIndex(Path, SAMSequenceDictionary)` and `AbstractBAMFileIndex(Path, SAMSequenceDictionary, boolean)`

**Specific Changes**:

1. **Add Non-Local Path Detection**: Introduce a helper method (or inline check) that determines whether a `Path` is local. A path is local if its file system is the default file system (`path.getFileSystem().equals(FileSystems.getDefault())`). This is consistent with how `SeekableStreamFactory` distinguishes local from non-local paths.

2. **Modify `AbstractBAMFileIndex(Path, SAMSequenceDictionary)` Constructor**: When the path is non-local, create a `SeekablePathStream` from the path and delegate to the `IndexStreamBuffer`-based constructor path (mirroring what `CSIIndex(Path, SAMSequenceDictionary)` does). When local, preserve the existing `MemoryMappedFileBuffer` behavior.

3. **Modify `AbstractBAMFileIndex(Path, SAMSequenceDictionary, boolean)` Constructor**: When the path is non-local, ignore the `useMemoryMapping` flag (since neither memory mapping nor `RandomAccessFileBuffer` can work) and fall back to `IndexStreamBuffer` via `SeekablePathStream`. When local, preserve the existing behavior of choosing between `MemoryMappedFileBuffer` and `RandomAccessFileBuffer`.

4. **Update `IndexFileBufferFactory.getBuffer(Path, boolean)`**: Add the same non-local detection so that callers using the factory (e.g., `CSIIndex(Path, boolean, SAMSequenceDictionary)`) also benefit from the fallback. When the path is non-local, open a `SeekablePathStream` and delegate to `IndexFileBufferFactory.getBuffer(SeekableStream)`.

5. **Add Required Import**: Import `SeekablePathStream` and `FileSystems` in `AbstractBAMFileIndex` if the check is done there, or keep the logic centralized in `IndexFileBufferFactory`.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Write tests that create a BAM index on a non-local NIO file system (using Google Jimfs, which is already a test dependency) and attempt to open it via `AbstractBAMFileIndex` subclasses. Run these tests on the UNFIXED code to observe failures and understand the root cause.

**Test Cases**:
1. **Jimfs Memory-Mapped Test**: Copy a valid `.bai` file into a Jimfs file system and open via `CachingBAMFileIndex(path, dictionary)` (will fail on unfixed code with IOException from FileChannel.map)
2. **Jimfs Non-Memory-Mapped Test**: Same as above but with `useMemoryMapping=false` via `CachingBAMFileIndex(path, dictionary, false)` (will fail on unfixed code because Jimfs doesn't support FileChannel random access as expected by `RandomAccessFileBuffer`)
3. **Jimfs DiskBasedBAMFileIndex Test**: Open via `DiskBasedBAMFileIndex(path, dictionary)` on Jimfs (will fail on unfixed code)
4. **Jimfs IndexFileBufferFactory Test**: Call `IndexFileBufferFactory.getBuffer(jimfsPath, true)` directly (will fail on unfixed code)

**Expected Counterexamples**:
- `IOException` or `UnsupportedOperationException` thrown from `FileChannel.open()` or `FileChannel.map()` when given a Jimfs path
- Possible causes: `MemoryMappedFileBuffer` unconditionally calls `FileChannel.open(path)` which may not be supported by non-default file system providers

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL path WHERE isBugCondition(path) DO
  index := new CachingBAMFileIndex(path, dictionary)
  result := index.getSpanOverlapping(refIndex, start, end)
  ASSERT result equals expectedResultFromLocalCopy
  index.close()
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL path WHERE NOT isBugCondition(path) DO
  ASSERT AbstractBAMFileIndex_original(path) = AbstractBAMFileIndex_fixed(path)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain (various reference indices, start/end positions)
- It catches edge cases that manual unit tests might miss (boundary positions, empty regions)
- It provides strong guarantees that behavior is unchanged for all local-path inputs

**Test Plan**: Observe behavior on UNFIXED code first for local `.bai` files (memory-mapped and non-memory-mapped modes), then write property-based tests capturing that behavior.

**Test Cases**:
1. **Local Memory-Mapped Preservation**: Verify that local `.bai` files opened with memory mapping produce identical query results before and after the fix
2. **Local Non-Memory-Mapped Preservation**: Verify that local `.bai` files opened without memory mapping produce identical query results
3. **SeekableStream Path Preservation**: Verify that direct `SeekableStream` construction continues to work identically
4. **Index Content Equivalence**: For a given `.bai` file, verify that the stream-based fallback produces byte-for-byte equivalent index content to the memory-mapped path

### Unit Tests

- Test `CachingBAMFileIndex(jimfsPath, dictionary)` successfully opens and queries a BAM index on Jimfs
- Test `DiskBasedBAMFileIndex(jimfsPath, dictionary)` works on Jimfs
- Test `CachingBAMFileIndex(jimfsPath, dictionary, false)` works on Jimfs (non-memory-mapped mode)
- Test `CachingBAMFileIndex(jimfsPath, dictionary, true)` works on Jimfs (memory-mapped requested but falls back to stream)
- Test that local paths still use `MemoryMappedFileBuffer` (verify via behavior, not reflection)
- Test edge case: empty `.bai` file on non-local path (should fail gracefully with appropriate error)

### Property-Based Tests

- Generate random query regions (reference index, start, end) and verify that results from Jimfs-backed index match results from local-file-backed index for the same `.bai` content
- Generate random BAM index paths (local vs Jimfs) and verify construction succeeds for all valid `.bai` files
- Test that `getSpanOverlapping`, `getMetaData`, and `getStartOfLastLinearBin` produce identical results regardless of whether the buffer is memory-mapped or stream-based

### Integration Tests

- Test full read workflow: open BAM on Jimfs with index on Jimfs, query by region, verify records returned
- Test mixed paths: BAM on local disk with index on Jimfs (if supported by `SamReaderFactory`)
- Test `CSIIndex` continues to work on Jimfs (regression guard — it already works but ensure no interference)
