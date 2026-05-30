# Requirements Document

## Introduction

This specification defines the requirements for migrating HTSJDK from `java.io.File` to `java.nio.file.Path` with URI constructor support. This migration enables full Java NIO Service Provider Interface (SPI) compatibility, allowing HTSJDK to work seamlessly with custom filesystems such as S3, HDFS, in-memory filesystems, and other cloud storage providers. This is a breaking change suitable for a major version release (4.0.0).

## Glossary

- **Path**: `java.nio.file.Path` - Modern Java NIO file system abstraction
- **File**: `java.io.File` - Legacy Java I/O file abstraction (to be removed)
- **URI**: Uniform Resource Identifier for locating resources
- **NIO SPI**: Java NIO Service Provider Interface for custom filesystem implementations
- **Factory**: Factory classes that create reader/writer instances
- **Reader**: Classes that read genomic data files (SAM/BAM/CRAM/VCF/BCF/FASTQ)
- **Writer**: Classes that write genomic data files
- **Index**: Index files that enable efficient random access to genomic data
- **HTSJDK**: High-Throughput Sequencing Java Development Kit
- **SAM/BAM/CRAM**: Sequence Alignment/Map formats
- **VCF/BCF**: Variant Call Format
- **FASTQ**: Sequence read format
- **Tribble**: HTSJDK framework for genomic feature files

## Requirements

### Requirement 1: Core I/O Path Migration

**User Story:** As a library maintainer, I want to migrate core I/O utilities to use Path instead of File, so that all higher-level components can build on a modern foundation.

#### Acceptance Criteria

1. WHEN accessing `htsjdk.samtools.util.IOUtil` THEN the system SHALL provide only Path-based utility methods
2. WHEN using `htsjdk.io.HtsPath` THEN the system SHALL operate exclusively with Path objects
3. WHEN using `htsjdk.io.IOPath` THEN the system SHALL operate exclusively with Path objects
4. WHEN constructing paths from URIs THEN the system SHALL use `Path.of(URI)` for full NIO SPI compatibility
5. THE IOUtil class SHALL NOT contain any File-based method signatures

### Requirement 2: Factory Class Path Migration

**User Story:** As a developer, I want factory classes to accept Path and URI parameters, so that I can create readers and writers for files in any filesystem.

#### Acceptance Criteria

1. WHEN opening a file with `SamReaderFactory` THEN the system SHALL accept Path or URI parameters
2. WHEN creating a writer with `SAMFileWriterFactory` THEN the system SHALL accept Path parameters
3. WHEN creating a reference sequence with `ReferenceSequenceFileFactory` THEN the system SHALL accept Path or URI parameters
4. WHEN building a variant writer with `VariantContextWriterBuilder` THEN the system SHALL accept Path parameters
5. THE factory classes SHALL NOT provide File-based method overloads

### Requirement 3: Reader Class Path Migration

**User Story:** As a developer, I want reader classes to work with Path objects, so that I can read genomic data from any filesystem.

#### Acceptance Criteria

1. WHEN constructing `SAMTextReader` THEN the system SHALL accept Path parameters
2. WHEN constructing `BAMFileReader` THEN the system SHALL accept Path parameters
3. WHEN constructing `CRAMFileReader` THEN the system SHALL accept Path parameters
4. WHEN constructing `VCFFileReader` THEN the system SHALL accept Path parameters
5. WHEN constructing `FastqReader` THEN the system SHALL accept Path parameters
6. THE reader constructors SHALL NOT accept File parameters

### Requirement 4: Writer Class Path Migration

**User Story:** As a developer, I want writer classes to work with Path objects, so that I can write genomic data to any filesystem.

#### Acceptance Criteria

1. WHEN constructing `SAMTextWriter` THEN the system SHALL accept Path parameters
2. WHEN constructing `BAMFileWriter` THEN the system SHALL accept Path parameters
3. WHEN constructing `CRAMFileWriter` THEN the system SHALL accept Path parameters
4. WHEN constructing `VCFWriter` THEN the system SHALL accept Path parameters
5. WHEN constructing `BCF2Writer` THEN the system SHALL accept Path parameters
6. WHEN constructing `BasicFastqWriter` THEN the system SHALL accept Path parameters
7. THE writer constructors SHALL NOT accept File parameters

### Requirement 5: Index Reader Path Migration

**User Story:** As a developer, I want index readers to work with Path objects, so that I can access indexed genomic data from any filesystem.

#### Acceptance Criteria

1. WHEN reading BAM indexes with `AbstractBAMFileIndex` THEN the system SHALL accept Path parameters
2. WHEN reading disk-based indexes with `DiskBasedBAMFileIndex` THEN the system SHALL accept Path parameters
3. WHEN reading CSI indexes with `CSIIndex` THEN the system SHALL accept Path parameters
4. WHEN reading SBI indexes with `SBIIndex` THEN the system SHALL accept Path parameters
5. WHEN reading Tribble indexes THEN the system SHALL accept Path parameters
6. THE index readers SHALL NOT accept File parameters

### Requirement 6: Index Writer Path Migration

**User Story:** As a developer, I want index writers to work with Path objects, so that I can create indexes for genomic data in any filesystem.

#### Acceptance Criteria

1. WHEN creating BAM indexes with `BAMIndexer` THEN the system SHALL accept Path parameters
2. WHEN writing binary BAM indexes with `BinaryBAMIndexWriter` THEN the system SHALL accept Path parameters
3. WHEN creating CRAM BAI indexes with `CRAMBAIIndexer` THEN the system SHALL accept Path parameters
4. WHEN creating CRAM CRAI indexes with `CRAMCRAIIndexer` THEN the system SHALL accept Path parameters
5. WHEN creating indexes with factory classes THEN the system SHALL accept Path parameters
6. THE index writers SHALL NOT accept File parameters

### Requirement 7: Reference Sequence Path Migration

**User Story:** As a developer, I want reference sequence classes to work with Path objects, so that I can access reference genomes from any filesystem.

#### Acceptance Criteria

1. WHEN opening reference sequences with `FastaSequenceFile` THEN the system SHALL accept Path parameters
2. WHEN opening indexed references with `IndexedFastaSequenceFile` THEN the system SHALL accept Path parameters
3. WHEN reading FASTA indexes with `FastaSequenceIndex` THEN the system SHALL accept Path parameters
4. WHEN walking reference sequences with `ReferenceSequenceFileWalker` THEN the system SHALL accept Path parameters
5. THE reference sequence classes SHALL NOT accept File parameters

### Requirement 8: Utility Class Path Migration

**User Story:** As a developer, I want utility classes to work with Path objects, so that I can perform file operations on any filesystem.

#### Acceptance Criteria

1. WHEN using `SamFiles` utilities THEN the system SHALL accept Path parameters
2. WHEN using `BamFileIoUtils` utilities THEN the system SHALL accept Path parameters
3. WHEN using `Tribble` index naming utilities THEN the system SHALL return Path objects
4. WHEN using filter classes THEN the system SHALL accept Path parameters for script files
5. THE utility classes SHALL NOT accept or return File objects

### Requirement 9: URI Support for Remote Files

**User Story:** As a developer, I want to open files using URIs, so that I can access genomic data from remote locations and cloud storage.

#### Acceptance Criteria

1. WHEN opening a file with a file:// URI THEN the system SHALL successfully open the local file
2. WHEN opening a file with an http:// or https:// URI THEN the system SHALL successfully open the remote file
3. WHEN opening a file with a custom filesystem URI THEN the system SHALL use the appropriate NIO SPI provider
4. WHEN constructing a Path from a URI THEN the system SHALL use `Path.of(URI)` for compatibility
5. THE system SHALL support URIs for all reader and writer factory methods

### Requirement 10: Test Migration and NIO SPI Validation

**User Story:** As a library maintainer, I want comprehensive tests using Path and custom filesystems, so that I can ensure the migration is correct and NIO SPI compatibility works.

#### Acceptance Criteria

1. WHEN running unit tests THEN the system SHALL use Path objects instead of File objects
2. WHEN accessing test resources THEN the system SHALL use URI-based Path construction
3. WHEN testing with jimfs THEN the system SHALL successfully read and write to in-memory filesystems
4. WHEN testing with custom NIO SPI implementations THEN the system SHALL successfully operate on custom filesystems
5. THE test suite SHALL NOT contain any File-based test code in main test paths

### Requirement 11: Breaking Change Documentation

**User Story:** As a library user, I want clear documentation of breaking changes and migration paths, so that I can update my code to use the new API.

#### Acceptance Criteria

1. WHEN reviewing the changelog THEN the system SHALL list all removed File-based methods
2. WHEN reviewing the changelog THEN the system SHALL list all changed method signatures
3. WHEN reviewing migration documentation THEN the system SHALL provide before/after code examples
4. WHEN reviewing JavaDoc THEN the system SHALL document the Path-based replacements for removed methods
5. THE documentation SHALL include a comprehensive migration guide

### Requirement 12: Backward Compatibility Removal

**User Story:** As a library maintainer, I want to cleanly remove File-based APIs, so that the codebase is consistent and maintainable.

#### Acceptance Criteria

1. WHEN compiling the library THEN the system SHALL NOT import `java.io.File` in main source code
2. WHEN reviewing public APIs THEN the system SHALL NOT expose File parameters or return types
3. WHEN reviewing deprecated methods THEN the system SHALL have removed all File-based deprecated methods
4. THE codebase SHALL use Path consistently throughout all packages
5. THE system SHALL provide clear compilation errors when users attempt to use removed File-based APIs

### Requirement 13: Path Utility Methods

**User Story:** As a developer, I want convenient utility methods for common Path operations, so that I can easily work with paths in different contexts.

#### Acceptance Criteria

1. WHEN converting a string to a Path THEN the system SHALL provide convenience methods
2. WHEN resolving relative paths THEN the system SHALL use Path.resolve() operations
3. WHEN checking file existence THEN the system SHALL use Files.exists() with Path
4. WHEN checking if a path is a directory THEN the system SHALL use Files.isDirectory() with Path
5. WHEN getting file names THEN the system SHALL use Path.getFileName() operations

### Requirement 14: Index File Path Resolution

**User Story:** As a developer, I want index files to be automatically located using Path-based logic, so that I don't need to manually specify index locations when they follow standard naming conventions.

#### Acceptance Criteria

1. WHEN opening a BAM file THEN the system SHALL automatically locate the .bai index using Path operations
2. WHEN opening a CRAM file THEN the system SHALL automatically locate the .crai index using Path operations
3. WHEN opening a VCF file THEN the system SHALL automatically locate the .idx or .tbi index using Path operations
4. WHEN an index file is not in the standard location THEN the system SHALL accept an explicit Path parameter for the index location
5. WHEN constructing index paths THEN the system SHALL use Path string concatenation or resolution
6. THE index resolution logic SHALL work with custom filesystems via NIO SPI

### Requirement 15: Error Handling for Path Operations

**User Story:** As a developer, I want clear error messages when Path operations fail, so that I can diagnose and fix issues quickly.

#### Acceptance Criteria

1. WHEN a Path does not exist THEN the system SHALL throw an exception with the full path information
2. WHEN a Path cannot be accessed THEN the system SHALL throw an exception with permission details
3. WHEN a URI is malformed THEN the system SHALL throw an exception with the invalid URI
4. WHEN a custom filesystem is not available THEN the system SHALL throw an exception indicating the missing provider
5. THE error messages SHALL include the Path or URI that caused the failure
