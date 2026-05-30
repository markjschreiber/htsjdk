# Implementation Plan: File to Path Migration

## Overview

This implementation plan breaks down the File to Path migration into discrete, actionable tasks. Each task builds on previous work and includes specific requirements references. The migration follows a phased approach to minimize risk and enable incremental validation.

The implementation uses **Java** as the programming language, following HTSJDK's existing patterns and conventions.

**Note on Test Tasks**: All test-writing tasks have been moved to Phase 11 (after task 14.2) to ensure existing test files are migrated to Path first, avoiding compilation errors when adding new tests.

## Tasks

- [x] 1. Phase 1: Core I/O Utilities Migration
  - Migrate the foundational I/O utilities that all other components depend on
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 1.1 Update IOUtil class to use Path exclusively
  - Remove all File-based utility methods from `htsjdk.samtools.util.IOUtil`
  - Ensure Path-based equivalents exist for all operations (exists, isDirectory, isReadable, isWritable, size)
  - Add URI-based Path construction helper methods
  - Update JavaDoc to document Path-based API
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 1.2 Update HtsPath to operate exclusively with Path
  - Remove any File dependencies from `htsjdk.io.HtsPath`
  - Ensure constructors accept String, URI, and Path
  - Verify toPath() and getURI() methods work correctly
  - Update internal implementation to use Path operations
  - _Requirements: 1.2, 1.4_

- [x] 1.3 Update IOPath interface and implementations
  - Ensure `htsjdk.io.IOPath` interface uses Path
  - Update all implementations to use Path internally
  - Remove any File-based methods or fields
  - _Requirements: 1.3_

- [-] 2. Phase 2: Factory Classes Migration
  - Migrate factory classes that create readers and writers
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 2.1 Migrate SamReaderFactory to Path/URI
  - Remove `open(File)` method from `htsjdk.samtools.SamReaderFactory`
  - Keep existing `open(Path)` method
  - Add `open(URI)` convenience method that delegates to `open(Path.of(URI))`
  - Remove `referenceSequence(File)` method
  - Keep `referenceSequence(Path)` method
  - Add `referenceSequence(URI)` convenience method
  - Remove `getFileHeader(File)` method
  - Keep `getFileHeader(Path)` method
  - Add `getFileHeader(URI)` convenience method
  - Update JavaDoc with migration guidance
  - _Requirements: 2.1, 9.5_

- [x] 2.2 Migrate SAMFileWriterFactory to Path
  - Replace all File parameters with Path in `htsjdk.samtools.SAMFileWriterFactory`
  - Update `makeWriter()`, `makeBAMWriter()`, `makeSAMWriter()`, `makeCRAMWriter()` methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 2.2_

- [x] 2.3 Migrate ReferenceSequenceFileFactory to Path/URI
  - Replace File parameters with Path in `htsjdk.samtools.reference.ReferenceSequenceFileFactory`
  - Update `getReferenceSequenceFile(File)` to `getReferenceSequenceFile(Path)`
  - Add `getReferenceSequenceFile(URI)` convenience method
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 2.3, 9.5_

- [x] 2.4 Migrate VariantContextWriterBuilder to Path
  - Replace `setOutputFile(File)` with `setOutputPath(Path)` in `htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder`
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 2.4_

- [x] 3. Checkpoint - Verify factories work with Path
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Phase 3: Reader Classes Migration
  - Migrate reader classes for all file formats
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4.1 Migrate SAMTextReader to Path
  - Update constructors in `htsjdk.samtools.SAMTextReader` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path (use Files.newInputStream, etc.)
  - Update JavaDoc
  - _Requirements: 3.1_

- [x] 4.2 Migrate BAMFileReader to Path
  - Update constructors in `htsjdk.samtools.BAMFileReader` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Ensure index Path parameter works correctly
  - Update JavaDoc
  - _Requirements: 3.2_

- [x] 4.3 Migrate CRAMFileReader to Path
  - Update constructors in `htsjdk.samtools.CRAMFileReader` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Ensure reference Path and index Path parameters work correctly
  - Update JavaDoc
  - _Requirements: 3.3_

- [x] 4.4 Migrate VCFFileReader to Path
  - Update constructors in `htsjdk.variant.vcf.VCFFileReader` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 3.4_

- [x] 4.5 Migrate FastqReader to Path
  - Update constructors in `htsjdk.samtools.fastq.FastqReader` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 3.5_

- [x] 5. Phase 4: Writer Classes Migration
  - Migrate writer classes for all file formats
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

- [x] 5.1 Migrate SAMTextWriter to Path
  - Update constructors in `htsjdk.samtools.SAMTextWriter` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path (use Files.newOutputStream, etc.)
  - Update JavaDoc
  - _Requirements: 4.1_

- [x] 5.2 Migrate BAMFileWriter to Path
  - Update constructors in `htsjdk.samtools.BAMFileWriter` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 4.2_

- [x] 5.3 Migrate CRAMFileWriter to Path
  - Update constructors in `htsjdk.samtools.CRAMFileWriter` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 4.3_

- [x] 5.4 Migrate VCF/BCF writers to Path
  - Update constructors in `htsjdk.variant.vcf.VCFWriter` to accept Path instead of File
  - Update constructors in `htsjdk.variant.bcf2.BCF2Writer` to accept Path instead of File
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 4.4, 4.5_

- [x] 5.5 Migrate FastqWriter to Path
  - Update constructors in `htsjdk.samtools.fastq.BasicFastqWriter` to accept Path instead of File
  - Update `FastqWriterFactory` to use Path
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 4.6_

- [x] 6. Checkpoint - Verify readers and writers work with Path
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Phase 5: Index Reader Classes Migration
  - Migrate index reader classes
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 7.1 Migrate AbstractBAMFileIndex to Path
  - Update `htsjdk.samtools.AbstractBAMFileIndex` to accept Path parameters
  - Update `openIndex(Path dataPath, Path indexPath)` method
  - Add `findIndexPath(Path dataPath)` static method for index discovery
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 5.1, 14.1, 14.4_

- [x] 7.2 Migrate DiskBasedBAMFileIndex to Path
  - Update constructors in `htsjdk.samtools.DiskBasedBAMFileIndex` to accept Path
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 5.2_

- [x] 7.3 Migrate CSIIndex and SBIIndex to Path
  - Update `htsjdk.samtools.CSIIndex` to accept Path parameters
  - Update `htsjdk.samtools.SBIIndex` to accept Path parameters
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 5.3, 5.4_

- [x] 7.4 Migrate Tribble index classes to Path
  - Update `htsjdk.tribble.index.AbstractIndex` to use Path
  - Update `htsjdk.tribble.index.IndexFactory` to accept Path parameters
  - Update `htsjdk.tribble.index.DynamicIndexCreator` to use Path
  - Update `htsjdk.tribble.index.LinearIndex` to use Path
  - Remove File-based methods
  - Update JavaDoc
  - _Requirements: 5.5_

- [x] 8. Phase 6: Index Writer Classes Migration
  - Migrate index writer classes
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 8.1 Migrate BAMIndexer to Path
  - Update constructors in `htsjdk.samtools.BAMIndexer` to accept Path
  - Update `createIndex(Path bamPath, Path indexPath)` method
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 6.1_

- [x] 8.2 Migrate BinaryBAMIndexWriter to Path
  - Update `htsjdk.samtools.BinaryBAMIndexWriter` to accept Path parameters
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 6.2_

- [x] 8.3 Migrate CRAM indexers to Path
  - Update `htsjdk.samtools.CRAMBAIIndexer` to accept Path parameters
  - Update `htsjdk.samtools.CRAMCRAIIndexer` to accept Path parameters
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 6.3, 6.4_

- [x] 8.4 Migrate index factory classes to Path
  - Update all index factory classes to accept Path parameters
  - Remove File-based methods
  - Update JavaDoc
  - _Requirements: 6.5_

- [x] 9. Phase 7: Reference Sequence Classes Migration
  - Migrate reference sequence handling classes
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 9.1 Migrate FastaSequenceFile to Path
  - Update constructors in `htsjdk.samtools.reference.FastaSequenceFile` to accept Path
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 7.1_

- [x] 9.2 Migrate IndexedFastaSequenceFile to Path
  - Update constructors in `htsjdk.samtools.reference.IndexedFastaSequenceFile` to accept Path
  - Update constructor accepting index Path parameter
  - Remove File-based constructors
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 7.2_

- [x] 9.3 Migrate FastaSequenceIndex to Path
  - Update `htsjdk.samtools.reference.FastaSequenceIndex` to accept Path parameters
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 7.3_

- [x] 9.4 Migrate ReferenceSequenceFileWalker to Path
  - Update `htsjdk.samtools.reference.ReferenceSequenceFileWalker` to accept Path parameters
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 7.4_

- [x] 10. Checkpoint - Verify indexes and references work with Path
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Phase 8: Utility Classes Migration
  - Migrate utility classes
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 11.1 Migrate SamFiles utility class to Path
  - Update `htsjdk.samtools.SamFiles` to accept and return Path
  - Update `findIndex(Path dataPath)` method
  - Update `isBAMFile(Path)`, `isCRAMFile(Path)`, `isSAMFile(Path)` methods
  - Remove File-based methods
  - Update JavaDoc
  - _Requirements: 8.1_

- [x] 11.2 Migrate BamFileIoUtils to Path
  - Update `htsjdk.samtools.BamFileIoUtils` to accept Path parameters
  - Remove File-based methods
  - Update internal File usage to Path
  - Update JavaDoc
  - _Requirements: 8.2_

- [x] 11.3 Migrate Tribble utility class to Path
  - Update `htsjdk.tribble.Tribble` to return Path instead of File
  - Update `indexPath(Path)` method (was `indexFile(File)`)
  - Update `tabixIndexPath(Path)` method (was `tabixIndexFile(File)`)
  - Use Path operations for name construction
  - Remove File-based methods
  - Update JavaDoc
  - _Requirements: 8.3_

- [x] 11.4 Migrate filter classes to Path
  - Update `htsjdk.samtools.filter.JavascriptSamRecordFilter` to accept Path for script files
  - Update `htsjdk.variant.variantcontext.filter.JavascriptVariantFilter` to accept Path
  - Update `htsjdk.samtools.filter.ReadNameFilter` to accept Path
  - Remove File-based methods
  - Update JavaDoc
  - _Requirements: 8.4_

- [x] 12. Phase 9: URI Support and Error Handling
  - Add comprehensive URI support and error handling
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 15.1, 15.2, 15.3, 15.4, 15.5_

- [x] 12.1 Add URI convenience methods to all factories
  - Ensure all factory classes have URI-accepting methods
  - Verify URI methods delegate to Path.of(URI)
  - Add error handling for malformed URIs
  - Update JavaDoc with URI examples
  - _Requirements: 9.4, 9.5_

- [x] 12.2 Implement Path not found error handling
  - Add Files.exists() checks before opening files
  - Throw IOException with full path information (use toAbsolutePath())
  - Update all reader/writer classes
  - _Requirements: 15.1_

- [x] 12.3 Implement permission error handling
  - Add Files.isReadable() and Files.isWritable() checks
  - Throw IOException with permission details and path
  - Update all reader/writer classes
  - _Requirements: 15.2_

- [x] 12.4 Implement URI parsing error handling
  - Catch IllegalArgumentException from Path.of(URI)
  - Wrap in IOException with URI details
  - Provide guidance on correct URI format
  - Update all URI-accepting methods
  - _Requirements: 15.3_

- [x] 12.5 Implement missing filesystem provider error handling
  - Catch FileSystemNotFoundException
  - Throw IOException indicating missing provider
  - Suggest installing appropriate NIO SPI provider
  - Update all URI-accepting methods
  - _Requirements: 15.4_

- [x] 13. Phase 10: Test Utility Migration
  - Migrate test utility classes to use Path
  - _Requirements: 10.1_

- [x] 13.1 Migrate test utility classes to Path
  - Update `htsjdk.samtools.SAMRecordSetBuilder` to use Path
  - Update test utility classes in `htsjdk.samtools` package
  - Update test utility classes in `htsjdk.tribble` package
  - Update test utility classes in `htsjdk.variant` package
  - Remove File-based test utilities
  - _Requirements: 10.1_

- [-] 14. Phase 11: Test Migration and New Test Creation
  - Migrate all existing tests to use Path, then add new tests
  - _Requirements: 10.1, 10.2, 10.5, 12.1, 12.2, 12.3, 12.4_

- [x] 14.1 Migrate SAM/BAM/CRAM test files to use Path
  - Update all tests in `src/test/java/htsjdk/samtools` to use Path
  - Replace `new File()` with `Path.of()`
  - Use `Path.of(URI)` for test resources where appropriate
  - Ensure tests pass with Path-based APIs
  - _Requirements: 10.1, 10.2_

- [x] 14.2 Migrate VCF/BCF test files to use Path
  - Update all tests in `src/test/java/htsjdk/variant` to use Path
  - Replace `new File()` with `Path.of()`
  - Use `Path.of(URI)` for test resources where appropriate
  - Ensure tests pass with Path-based APIs
  - _Requirements: 10.1, 10.2_

- [x] 14.3 Migrate Tribble test files to use Path
  - Update all tests in `src/test/java/htsjdk/tribble` to use Path
  - Replace `new File()` with `Path.of()`
  - Use `Path.of(URI)` for test resources where appropriate
  - Ensure tests pass with Path-based APIs
  - _Requirements: 10.1, 10.2_

- [x] 14.4 Checkpoint - Verify existing tests pass
  - Run full test suite with `./gradlew test`
  - Ensure all migrated tests pass, ask the user if questions arise.


- [x] 14.5 Write unit tests for IOUtil Path methods
  - Test path existence checking with Files.exists()
  - Test directory checking with Files.isDirectory()
  - Test readability and writability checks
  - Test URI to Path construction
  - Verify no File-based methods exist using reflection
  - _Requirements: 1.1, 1.5_

- [x] 14.6 Write unit tests for SamReaderFactory Path/URI methods
  - Test opening BAM file with Path
  - Test opening BAM file with file:// URI
  - Test setting reference sequence with Path
  - Test getting file header with Path
  - Verify no File-based methods exist
  - _Requirements: 2.1, 2.5_

- [x] 14.7 Write unit tests for SAMFileWriterFactory Path methods
  - Test creating BAM writer with Path
  - Test creating SAM writer with Path
  - Test creating CRAM writer with Path and reference Path
  - Verify no File-based methods exist
  - _Requirements: 2.2, 2.5_

- [x] 14.8 Write unit tests for reference and variant writer factories
  - Test ReferenceSequenceFileFactory with Path and URI
  - Test VariantContextWriterBuilder with Path
  - Verify no File-based methods exist
  - _Requirements: 2.3, 2.4, 2.5_

- [x] 14.9 Write unit tests for SAM/BAM/CRAM readers with Path
  - Test SAMTextReader with Path to test SAM file
  - Test BAMFileReader with Path to test BAM file
  - Test CRAMFileReader with Path to test CRAM file and reference
  - Verify readers can read records correctly
  - Verify no File-based constructors exist
  - _Requirements: 3.1, 3.2, 3.3, 3.6_

- [x] 14.10 Write unit tests for VCF and FASTQ readers with Path
  - Test VCFFileReader with Path to test VCF file
  - Test FastqReader with Path to test FASTQ file
  - Verify readers can read records correctly
  - Verify no File-based constructors exist
  - _Requirements: 3.4, 3.5, 3.6_

- [x] 14.11 Write unit tests for SAM/BAM/CRAM writers with Path
  - Test SAMTextWriter writing to Path
  - Test BAMFileWriter writing to Path
  - Test CRAMFileWriter writing to Path
  - Verify written files can be read back correctly
  - Verify no File-based constructors exist
  - _Requirements: 4.1, 4.2, 4.3, 4.7_

- [x] 14.12 Write unit tests for VCF/BCF/FASTQ writers with Path
  - Test VCFWriter writing to Path
  - Test BCF2Writer writing to Path
  - Test BasicFastqWriter writing to Path
  - Verify written files can be read back correctly
  - Verify no File-based constructors exist
  - _Requirements: 4.4, 4.5, 4.6, 4.7_

- [x] 14.13 Write unit tests for index readers with explicit Path
  - Test opening BAM index with explicit index Path
  - Test opening CRAM index with explicit index Path
  - Test opening VCF index with explicit index Path
  - Verify indexes work correctly when not in standard location
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 14.4_

- [x] 14.14 Write unit tests for index writers with Path
  - Test BAMIndexer creating index at Path
  - Test CRAMBAIIndexer creating index at Path
  - Test CRAMCRAIIndexer creating index at Path
  - Verify created indexes can be read back correctly
  - Verify no File-based methods exist
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 14.15 Write unit tests for reference sequence classes with Path
  - Test FastaSequenceFile with Path to test FASTA file
  - Test IndexedFastaSequenceFile with Path to test FASTA and index
  - Test FastaSequenceIndex with Path
  - Test ReferenceSequenceFileWalker with Path
  - Verify sequences can be read correctly
  - Verify no File-based methods exist
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 14.16 Write unit tests for utility classes with Path
  - Test SamFiles.findIndex() with Path
  - Test SamFiles file type detection with Path
  - Test BamFileIoUtils operations with Path
  - Test Tribble index path construction
  - Test filter classes with Path to script files
  - Verify no File-based methods exist
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 14.17 Write unit tests for file:// URI support
  - Test opening BAM file with file:// URI
  - Test opening CRAM file with file:// URI
  - Test opening VCF file with file:// URI
  - Test opening FASTQ file with file:// URI
  - Verify files can be read correctly via URI
  - _Requirements: 9.1_

- [x] 14.18 Write unit tests for all error conditions
  - Test non-existent file error
  - Test permission denied error
  - Test malformed URI error
  - Test missing filesystem provider error
  - Test missing index file error
  - Verify all error messages are clear and actionable
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [x] 14.19 Write property test for URI to Path round trip
  - **Property 1: URI to Path Round Trip**
  - **Validates: Requirements 1.4, 9.4**
  - Generate random file:// URIs
  - Verify Path.of(URI) → toUri() preserves scheme and path
  - Test with 100+ iterations
  - _Requirements: 1.4, 9.4_

- [x] 14.20 Write property test for path resolution consistency
  - **Property 2: Path Resolution Consistency**
  - **Validates: Requirements 13.2**
  - Generate random base Paths and relative path strings
  - Resolve relative paths using Path.resolve()
  - Verify resolved Path contains both base and relative components
  - Test with 100+ iterations
  - _Requirements: 13.2_

- [x] 14.21 Write property test for index path construction
  - **Property 3: Index Path Construction**
  - **Validates: Requirements 14.5**
  - Generate random data file Paths
  - Construct index Paths by appending extensions
  - Verify index Path is in same filesystem and parent directory
  - Test with 100+ iterations
  - _Requirements: 14.5_

- [x] 14.22 Write property test for BAM index auto-discovery
  - **Property 4: Index Auto-Discovery for BAM Files**
  - **Validates: Requirements 14.1**
  - Create temporary BAM files with corresponding .bai files
  - Verify index discovery finds the .bai file
  - Test with 100+ iterations
  - _Requirements: 14.1_

- [x] 14.23 Write property tests for CRAM and VCF index auto-discovery
  - **Property 5: Index Auto-Discovery for CRAM Files**
  - **Property 6: Index Auto-Discovery for VCF Files**
  - **Validates: Requirements 14.2, 14.3**
  - Create temporary CRAM files with .crai files
  - Create temporary VCF files with .idx or .tbi files
  - Verify index discovery finds the index files
  - Test with 100+ iterations each
  - _Requirements: 14.2, 14.3_

- [x] 14.24 Write property test for NIO SPI compatibility with jimfs
  - **Property 7: NIO SPI Compatibility with Jimfs**
  - **Validates: Requirements 9.3, 10.3**
  - Create in-memory filesystem with jimfs
  - Copy test BAM/CRAM/VCF files to jimfs
  - Open files using HTSJDK with Path from jimfs
  - Verify reading and writing works correctly
  - Test with 100+ iterations
  - _Requirements: 9.3, 10.3_

- [x] 14.25 Write property test for custom filesystem compatibility
  - **Property 8: NIO SPI Compatibility with Custom Filesystems**
  - **Validates: Requirements 9.3, 10.4, 14.6**
  - Test with jimfs as custom filesystem
  - Verify index discovery works in custom filesystem
  - Verify all reader/writer operations work
  - Test with 100+ iterations
  - _Requirements: 9.3, 10.4, 14.6_

- [x] 14.26 Write property test for error messages include path
  - **Property 9: Error Messages Include Path Information**
  - **Validates: Requirements 15.1, 15.5**
  - Generate random non-existent Paths
  - Attempt to open files
  - Verify exception messages contain the Path string
  - Test with 100+ iterations
  - _Requirements: 15.1, 15.5_

- [x] 14.27 Write property test for error messages include URI
  - **Property 10: Error Messages Include URI Information**
  - **Validates: Requirements 15.3, 15.5**
  - Generate random malformed URIs
  - Attempt to construct Paths
  - Verify exception messages contain the URI string
  - Test with 100+ iterations
  - _Requirements: 15.3, 15.5_

- [x] 15. Phase 12: Final Validation
  - Verify migration is complete
  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 15.1 Verify no File imports remain in main source
  - Search for `import java.io.File` in `src/main/java`
  - Remove any remaining File imports
  - Verify compilation succeeds without File
  - _Requirements: 12.1_

- [x] 15.2 Verify no File-based public APIs remain
  - Use reflection to scan all public methods
  - Verify no public methods accept or return File
  - Document any exceptions (if absolutely necessary)
  - _Requirements: 12.2, 12.3, 12.4_

- [x] 15.3 Checkpoint - Verify all tests pass
  - Run full test suite with `./gradlew test`
  - Ensure all tests pass, ask the user if questions arise.
  - **Result**: 33,631/33,659 tests pass. 3 failures are pre-existing environment issues (require samtools binary or htsget server).

- [x] 16. Phase 13: Documentation and Migration Guide
  - Create comprehensive documentation for the migration
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 16.1 Update JavaDoc for all migrated classes
  - Add @deprecated tags to removed methods (if any remain temporarily)
  - Document Path-based replacements
  - Add examples of Path and URI usage
  - Document NIO SPI compatibility
  - _Requirements: 11.4_

- [x] 16.2 Create migration guide document
  - Document all removed File-based methods
  - Document all changed method signatures
  - Provide before/after code examples for common patterns
  - Include examples for URI usage
  - Include examples for custom filesystem usage
  - _Requirements: 11.1, 11.2, 11.3_

- [x] 16.3 Update CHANGELOG with breaking changes
  - List all removed File-based methods by class
  - List all changed method signatures
  - Note behavioral changes (if any)
  - Provide link to migration guide
  - _Requirements: 11.1, 11.2_

- [x] 16.4 Update README with version information
  - Note that version 4.0.0 is a breaking change
  - Link to migration guide
  - Update version compatibility matrix
  - Document NIO SPI support
  - _Requirements: 11.5_

- [x] 17. Final Checkpoint - Complete migration validation
  - Run full test suite including property tests
  - Verify all documentation is complete
  - Verify no File imports in main source
  - Verify all public APIs use Path
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All tasks are required for comprehensive migration
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Checkpoints ensure incremental validation
- The migration is designed to be done incrementally with validation at each phase
- All property tests should run with minimum 100 iterations
- Use jqwik for property-based testing in Java
- **Test tasks have been moved to Phase 11 (tasks 14.5-14.27) to ensure existing tests are migrated first**
