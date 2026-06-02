# Bugfix Requirements Document

## Introduction

When opening a BAM index (`.bai`) file from a non-local `Path` (e.g., an S3 path provided via an NIO SPI such as s3-nio-spi), `AbstractBAMFileIndex` unconditionally attempts to memory-map the file via `MemoryMappedFileBuffer`. Memory-mapping is fundamentally incompatible with remote file systems — you cannot memory-map a remote object. This causes a failure when the BAM index resides on S3 or any other non-local NIO file system. The fix should fall back to a stream-based reader (`IndexStreamBuffer`) for non-local paths, mirroring the approach already used for tabix indexes.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a BAM index is opened via a non-local `Path` (e.g., S3) with memory mapping enabled THEN the system throws an exception because `MemoryMappedFileBuffer` cannot memory-map a remote file

1.2 WHEN a BAM index is opened via a non-local `Path` with memory mapping disabled THEN the system throws an exception because `RandomAccessFileBuffer` also requires local file semantics (random access via `FileChannel`)

### Expected Behavior (Correct)

2.1 WHEN a BAM index is opened via a non-local `Path` with memory mapping enabled THEN the system SHALL fall back to a stream-based buffer (e.g., `IndexStreamBuffer` backed by a `SeekableStream` from the `Path`) and read the index successfully

2.2 WHEN a BAM index is opened via a non-local `Path` with memory mapping disabled THEN the system SHALL use a stream-based buffer (e.g., `IndexStreamBuffer` backed by a `SeekableStream` from the `Path`) and read the index successfully

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a BAM index is opened via a local file `Path` with memory mapping enabled THEN the system SHALL CONTINUE TO use `MemoryMappedFileBuffer` for efficient random access

3.2 WHEN a BAM index is opened via a local file `Path` with memory mapping disabled THEN the system SHALL CONTINUE TO use `RandomAccessFileBuffer` for random access without memory mapping

3.3 WHEN a BAM index is opened via a `SeekableStream` directly THEN the system SHALL CONTINUE TO use `IndexStreamBuffer` as it does today

3.4 WHEN a BAM index is opened via a local `Path` THEN the system SHALL CONTINUE TO correctly parse the index and return valid query results
