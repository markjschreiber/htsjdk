# Implementation Plan

## Overview

This task list follows the exploratory bugfix workflow for fixing the non-local Path BAM index stream fallback bug. The fix adds non-local path detection and falls back to `IndexStreamBuffer` via `SeekablePathStream` when a BAM index is opened from a non-default file system (S3, GCS, Jimfs, etc.).

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Non-Local Path Throws on BAM Index Open
  - **IMPORTANT**: Write this property-based test BEFORE implementing the fix
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Use Jimfs (in-memory NIO file system) to create non-local paths. Scope the property to concrete failing cases: copy a valid `.bai` file into Jimfs, then attempt to open it via `CachingBAMFileIndex(path, dictionary)` and `CachingBAMFileIndex(path, dictionary, false)`
  - **Test Setup**: Copy `src/test/resources/htsjdk/samtools/BAMFileIndexTest/index_test.bam.bai` (or similar valid `.bai` from test resources) into a Jimfs file system
  - **Test Cases**:
    - `CachingBAMFileIndex(jimfsPath, dictionary)` — memory-mapped mode (default)
    - `CachingBAMFileIndex(jimfsPath, dictionary, false)` — non-memory-mapped mode
    - `DiskBasedBAMFileIndex(jimfsPath, dictionary)` — alternate subclass
    - `IndexFileBufferFactory.getBuffer(jimfsPath, true)` — factory method directly
  - **Bug Condition**: `isBugCondition(path)` where `!path.getFileSystem().equals(FileSystems.getDefault())`
  - **Expected Behavior (post-fix)**: Index opens successfully via stream-based buffer and produces valid query results matching local-path results
  - Run test on UNFIXED code - expect FAILURE (IOException or UnsupportedOperationException from FileChannel.open/map)
  - Document counterexamples found (e.g., "CachingBAMFileIndex(jimfsPath, dict) throws IOException because MemoryMappedFileBuffer cannot map a Jimfs path")
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 2.1, 2.2_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Local Path Buffer Selection and Query Results Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - **Observe behavior on UNFIXED code for local paths (non-buggy inputs)**:
    - Observe: `CachingBAMFileIndex(localPath, dictionary)` uses `MemoryMappedFileBuffer` and returns correct query results
    - Observe: `CachingBAMFileIndex(localPath, dictionary, false)` uses `RandomAccessFileBuffer` and returns correct query results
    - Observe: `CachingBAMFileIndex(seekableStream, dictionary)` uses `IndexStreamBuffer` and returns correct query results
    - Observe: Query results from memory-mapped and non-memory-mapped modes are identical for the same `.bai` file
  - **Write property-based tests capturing observed behavior**:
    - For all valid reference indices and query regions (start, end) within the test `.bai` file, `getSpanOverlapping(ref, start, end)` returns the same result in memory-mapped mode as in non-memory-mapped mode
    - For all valid reference indices, `getMetaData(ref)` returns the same result regardless of buffer mode
    - `getStartOfLastLinearBin()` returns the same value for both modes
    - Local path construction always succeeds without exception
  - **Property scope**: `NOT isBugCondition(path)` — paths on the default file system (local `.bai` files)
  - Verify tests PASS on UNFIXED code (confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Fix for non-local Path BAM index stream fallback

  - [x] 3.1 Add non-local path detection helper
    - Add a private static helper method `isNonLocalPath(Path path)` that returns `true` when `!path.getFileSystem().equals(FileSystems.getDefault())`
    - Add import for `java.nio.file.FileSystems`
    - Place in `AbstractBAMFileIndex` (or in `IndexFileBufferFactory` if centralizing)
    - _Bug_Condition: isBugCondition(path) where !path.getFileSystem().equals(FileSystems.getDefault())_
    - _Requirements: 2.1, 2.2_

  - [x] 3.2 Modify AbstractBAMFileIndex(Path, SAMSequenceDictionary) constructor
    - When `isNonLocalPath(path)` is true, create `new IndexStreamBuffer(new SeekablePathStream(path))` instead of `new MemoryMappedFileBuffer(path)`
    - When local, preserve existing `MemoryMappedFileBuffer(path)` behavior
    - Add import for `htsjdk.samtools.seekablestream.SeekablePathStream`
    - _Bug_Condition: isBugCondition(path) where !path.getFileSystem().equals(FileSystems.getDefault())_
    - _Expected_Behavior: Non-local paths open via IndexStreamBuffer(SeekablePathStream(path))_
    - _Preservation: Local paths continue to use MemoryMappedFileBuffer_
    - _Requirements: 2.1, 3.1_

  - [x] 3.3 Modify AbstractBAMFileIndex(Path, SAMSequenceDictionary, boolean) constructor
    - When `isNonLocalPath(path)` is true, ignore `useMemoryMapping` flag and create `new IndexStreamBuffer(new SeekablePathStream(path))`
    - When local, preserve existing choice between `MemoryMappedFileBuffer` and `RandomAccessFileBuffer`
    - _Bug_Condition: isBugCondition(path) where !path.getFileSystem().equals(FileSystems.getDefault())_
    - _Expected_Behavior: Non-local paths open via IndexStreamBuffer(SeekablePathStream(path)) regardless of useMemoryMapping flag_
    - _Preservation: Local paths continue to use MemoryMappedFileBuffer (true) or RandomAccessFileBuffer (false)_
    - _Requirements: 2.2, 3.2_

  - [x] 3.4 Update IndexFileBufferFactory.getBuffer(Path, boolean) with same fallback
    - When path is non-local, open a `SeekablePathStream` and delegate to `IndexFileBufferFactory.getBuffer(SeekableStream)` (returning an `IndexStreamBuffer`)
    - When local, preserve existing behavior (MemoryMappedFileBuffer, RandomAccessFileBuffer, or CompressedIndexFileBuffer)
    - _Bug_Condition: isBugCondition(path) where !path.getFileSystem().equals(FileSystems.getDefault())_
    - _Expected_Behavior: Non-local paths routed through stream-based buffer via factory_
    - _Preservation: Local paths routed through existing buffer selection logic_
    - _Requirements: 2.1, 2.2, 3.1, 3.2_

  - [x] 3.5 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Non-Local Path Opens Successfully
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior (non-local paths open via stream fallback)
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2_

  - [x] 3.6 Verify preservation tests still pass
    - **Property 2: Preservation** - Local Path Behavior Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all local-path tests still pass after fix (no regressions in buffer selection or query results)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 4. Checkpoint - Ensure all tests pass
  - Run full test suite: `./gradlew test`
  - Ensure all exploration tests (Property 1) pass — confirms bug is fixed
  - Ensure all preservation tests (Property 2) pass — confirms no regressions
  - Ensure no pre-existing tests are broken by the change
  - Ask the user if questions arise

## Task Dependencies

- 3.1 depends on [1, 2]
- 3.2 depends on [3.1]
- 3.3 depends on [3.1]
- 3.4 depends on [3.1]
- 3.5 depends on [3.2, 3.3, 3.4]
- 3.6 depends on [3.2, 3.3, 3.4]
- 4 depends on [3.5, 3.6]

## Notes

- Tests use **Jimfs** (Google in-memory file system, already a test dependency) to simulate non-local paths
- Tests use **TestNG** (not JUnit) per project conventions
- The exploration test (task 1) is expected to FAIL on unfixed code — this confirms the bug exists
- The preservation test (task 2) is expected to PASS on unfixed code — this confirms baseline behavior
- After the fix (task 3), both test sets should PASS
- The fix mirrors the pattern already used by `CSIIndex(Path, SAMSequenceDictionary)` which wraps non-local paths in `SeekablePathStream`
