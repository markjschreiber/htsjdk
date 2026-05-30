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

import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.variant.vcf.VCFFileReader;
import net.jqwik.api.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Property-based tests for verifying error messages include path information.
 * 
 * Feature: file-to-path-migration
 * Property 9: Error Messages Include Path Information
 * Validates: Requirements 15.1, 15.5
 * 
 * For any I/O error that occurs when accessing a Path, the exception message 
 * should contain the string representation of the Path that caused the error.
 */
public class ErrorMessagesIncludePathPropertyTest {

    /**
     * Property 9: Error Messages Include Path Information - BAM Files
     * 
     * For any non-existent BAM file Path, attempting to open it should throw
     * an exception whose message contains the path string.
     * 
     * Validates: Requirements 15.1, 15.5
     */
    @Property(tries = 100)
    void bamFileErrorMessageContainsPath(
            @ForAll("nonExistentBamPaths") Path nonExistentPath) {
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            // If we get here, the file somehow exists or was opened - skip this case
            throw new AssertionError("Expected exception for non-existent file: " + nonExistentPath);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            
            // The error message should contain some identifiable part of the path
            String pathString = nonExistentPath.toString();
            String fileName = nonExistentPath.getFileName().toString();
            
            boolean containsPathInfo = fullMessage.contains(pathString) ||
                    fullMessage.contains(fileName) ||
                    containsAnyPathComponent(fullMessage, nonExistentPath);
            
            assert containsPathInfo :
                    String.format("Error message should contain path information. " +
                            "Path: %s, Message: %s", pathString, fullMessage);
        }
    }

    /**
     * Property 9: Error Messages Include Path Information - SAM Files
     * 
     * For any non-existent SAM file Path, attempting to open it should throw
     * an exception whose message contains the path string.
     * 
     * Validates: Requirements 15.1, 15.5
     */
    @Property(tries = 100)
    void samFileErrorMessageContainsPath(
            @ForAll("nonExistentSamPaths") Path nonExistentPath) {
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            throw new AssertionError("Expected exception for non-existent file: " + nonExistentPath);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            String pathString = nonExistentPath.toString();
            String fileName = nonExistentPath.getFileName().toString();
            
            boolean containsPathInfo = fullMessage.contains(pathString) ||
                    fullMessage.contains(fileName) ||
                    containsAnyPathComponent(fullMessage, nonExistentPath);
            
            assert containsPathInfo :
                    String.format("Error message should contain path information. " +
                            "Path: %s, Message: %s", pathString, fullMessage);
        }
    }

    /**
     * Property 9: Error Messages Include Path Information - CRAM Files
     * 
     * For any non-existent CRAM file Path, attempting to open it should throw
     * an exception whose message contains the path string.
     * 
     * Validates: Requirements 15.1, 15.5
     */
    @Property(tries = 100)
    void cramFileErrorMessageContainsPath(
            @ForAll("nonExistentCramPaths") Path nonExistentPath) {
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            throw new AssertionError("Expected exception for non-existent file: " + nonExistentPath);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            String pathString = nonExistentPath.toString();
            String fileName = nonExistentPath.getFileName().toString();
            
            boolean containsPathInfo = fullMessage.contains(pathString) ||
                    fullMessage.contains(fileName) ||
                    containsAnyPathComponent(fullMessage, nonExistentPath);
            
            assert containsPathInfo :
                    String.format("Error message should contain path information. " +
                            "Path: %s, Message: %s", pathString, fullMessage);
        }
    }

    /**
     * Property 9: Error Messages Include Path Information - VCF Files
     * 
     * For any non-existent VCF file Path, attempting to open it should throw
     * an exception whose message contains the path string.
     * 
     * Validates: Requirements 15.1, 15.5
     */
    @Property(tries = 100)
    void vcfFileErrorMessageContainsPath(
            @ForAll("nonExistentVcfPaths") Path nonExistentPath) {
        
        try (VCFFileReader reader = new VCFFileReader(nonExistentPath, false)) {
            throw new AssertionError("Expected exception for non-existent file: " + nonExistentPath);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            String pathString = nonExistentPath.toString();
            String fileName = nonExistentPath.getFileName().toString();
            
            boolean containsPathInfo = fullMessage.contains(pathString) ||
                    fullMessage.contains(fileName) ||
                    containsAnyPathComponent(fullMessage, nonExistentPath);
            
            assert containsPathInfo :
                    String.format("Error message should contain path information. " +
                            "Path: %s, Message: %s", pathString, fullMessage);
        }
    }

    /**
     * Property 9: Error Messages Include Path Information - Reference FASTA Files
     * 
     * For any non-existent FASTA reference file Path, attempting to open it should throw
     * an exception whose message contains the path string.
     * 
     * Validates: Requirements 15.1, 15.5
     */
    @Property(tries = 100)
    void fastaFileErrorMessageContainsPath(
            @ForAll("nonExistentFastaPaths") Path nonExistentPath) {
        
        try {
            ReferenceSequenceFileFactory.getReferenceSequenceFile(nonExistentPath);
            throw new AssertionError("Expected exception for non-existent file: " + nonExistentPath);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            String pathString = nonExistentPath.toString();
            String fileName = nonExistentPath.getFileName().toString();
            
            boolean containsPathInfo = fullMessage.contains(pathString) ||
                    fullMessage.contains(fileName) ||
                    containsAnyPathComponent(fullMessage, nonExistentPath);
            
            assert containsPathInfo :
                    String.format("Error message should contain path information. " +
                            "Path: %s, Message: %s", pathString, fullMessage);
        }
    }

    /**
     * Property 9: Error Messages Include Path Information - Generic Paths
     * 
     * For any non-existent file Path with various extensions, attempting to open it 
     * should throw an exception whose message contains the path string.
     * 
     * Validates: Requirements 15.1, 15.5
     */
    @Property(tries = 100)
    void genericFileErrorMessageContainsPath(
            @ForAll("nonExistentGenericPaths") Path nonExistentPath) {
        
        try {
            SamReaderFactory.makeDefault().open(nonExistentPath);
            throw new AssertionError("Expected exception for non-existent file: " + nonExistentPath);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            String pathString = nonExistentPath.toString();
            String fileName = nonExistentPath.getFileName().toString();
            
            boolean containsPathInfo = fullMessage.contains(pathString) ||
                    fullMessage.contains(fileName) ||
                    containsAnyPathComponent(fullMessage, nonExistentPath);
            
            assert containsPathInfo :
                    String.format("Error message should contain path information. " +
                            "Path: %s, Message: %s", pathString, fullMessage);
        }
    }

    // ==================== Providers ====================

    /**
     * Provides non-existent BAM file paths for testing.
     */
    @Provide
    Arbitrary<Path> nonExistentBamPaths() {
        return nonExistentPaths(".bam");
    }

    /**
     * Provides non-existent SAM file paths for testing.
     */
    @Provide
    Arbitrary<Path> nonExistentSamPaths() {
        return nonExistentPaths(".sam");
    }

    /**
     * Provides non-existent CRAM file paths for testing.
     */
    @Provide
    Arbitrary<Path> nonExistentCramPaths() {
        return nonExistentPaths(".cram");
    }

    /**
     * Provides non-existent VCF file paths for testing.
     */
    @Provide
    Arbitrary<Path> nonExistentVcfPaths() {
        return nonExistentPaths(".vcf");
    }

    /**
     * Provides non-existent FASTA file paths for testing.
     */
    @Provide
    Arbitrary<Path> nonExistentFastaPaths() {
        return nonExistentPaths(".fasta");
    }

    /**
     * Provides non-existent paths with various extensions for testing.
     */
    @Provide
    Arbitrary<Path> nonExistentGenericPaths() {
        return Arbitraries.of(".bam", ".sam", ".cram", ".vcf", ".bcf")
                .flatMap(this::nonExistentPaths);
    }

    /**
     * Creates an arbitrary for non-existent paths with the given extension.
     * Uses a base path that definitely doesn't exist.
     */
    private Arbitrary<Path> nonExistentPaths(String extension) {
        return Combinators.combine(
                directoryNames().list().ofMinSize(2).ofMaxSize(4),
                fileBaseNames()
        ).as((dirs, baseName) -> {
            // Build a path that definitely doesn't exist
            // Use /nonexistent_htsjdk_test_dir as base to ensure it doesn't exist
            StringBuilder pathStr = new StringBuilder("/nonexistent_htsjdk_test_dir_");
            pathStr.append(System.currentTimeMillis() % 10000);
            for (String dir : dirs) {
                pathStr.append("/").append(dir);
            }
            pathStr.append("/").append(baseName).append(extension);
            return Path.of(pathStr.toString());
        });
    }

    /**
     * Provides directory names for path construction.
     */
    @Provide
    Arbitrary<String> directoryNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(12);
    }

    /**
     * Provides file base names (without extension).
     */
    @Provide
    Arbitrary<String> fileBaseNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(15);
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

    /**
     * Check if the message contains any component of the path.
     */
    private boolean containsAnyPathComponent(String message, Path path) {
        // Check if message contains the filename
        if (path.getFileName() != null && 
                message.contains(path.getFileName().toString())) {
            return true;
        }
        
        // Check if message contains any directory component
        for (int i = 0; i < path.getNameCount(); i++) {
            String component = path.getName(i).toString();
            if (component.length() > 3 && message.contains(component)) {
                return true;
            }
        }
        
        return false;
    }
}
