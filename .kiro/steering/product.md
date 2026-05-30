# HTSJDK Product Overview

HTSJDK is a Java library for accessing common high-throughput sequencing (HTS) file formats used in genomics and bioinformatics.

## Core Functionality

- **File Format Support**: SAM, BAM, CRAM, VCF, BCF, and related index formats (BAI, CRAI, CSI, Tabix)
- **Reference Sequences**: FASTA file handling with indexing
- **Genomic Features**: BED, GFF, interval lists via the Tribble framework
- **Utilities**: File validation, sorting, merging, indexing, and format conversion

## Key Use Cases

- Reading and writing alignment files (SAM/BAM/CRAM)
- Parsing and generating variant call files (VCF/BCF)
- Working with genomic intervals and annotations
- Building bioinformatics pipelines and tools

## Licensing

- Most code: MIT License
- CRAM code: Apache License 2.0
- Tribble (VCF support): LGPL
- SRA support: Public domain

Check individual source files for specific license information.
