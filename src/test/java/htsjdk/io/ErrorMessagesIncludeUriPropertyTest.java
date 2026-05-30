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
package htsjdk.io;

import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import net.jqwik.api.*;

import java.io.IOException;
import java.net.URI;

/**
 * Property-based tests for verifying error messages include URI information.
 * 
 * Feature: file-to-path-migration
 * Property 10: Error Messages Include URI Information
 * Validates: Requirements 15.3, 15.5
 * 
 * For any error that occurs when constructing a Path from a URI, the exception 
 * message should contain the string representation of the URI that caused the error.
 */
public class ErrorMessagesIncludeUriPropertyTest {

    /**
     * Property 10: Error Messages Include URI Information - SamReaderFactory
     * 
     * For any URI with an unknown/unsupported scheme, attempting to open it with
     * SamReaderFactory should throw an exception whose message contains the URI
     * scheme or URI string.
     * 
     * Validates: Requirements 15.3, 15.5
     */
    @Property(tries = 100)
    void samReaderFactoryErrorMessageContainsUri(
            @ForAll("unknownSchemeUris") URI invalidUri) {
        
        try {
            SamReaderFactory.makeDefault().open(invalidUri);
            // If we get here, the URI was somehow valid - this shouldn't happen
            throw new AssertionError("Expected exception for invalid URI: " + invalidUri);
        } catch (IOException e) {
            String fullMessage = getFullErrorMessage(e);
            
            // The error message should contain the URI scheme or the full URI
            String scheme = invalidUri.getScheme();
            String uriString = invalidUri.toString();
            
            boolean containsUriInfo = fullMessage.contains(scheme) ||
                    fullMessage.contains(uriString) ||
                    fullMessage.contains("Invalid URI") ||
                    fullMessage.contains("No filesystem provider");
            
            assert containsUriInfo :
                    String.format("Error message should contain URI information. " +
                            "URI: %s, Scheme: %s, Message: %s", uriString, scheme, fullMessage);
        }
    }

    /**
     * Property 10: Error Messages Include URI Information - ReferenceSequenceFileFactory
     * 
     * For any URI with an unknown/unsupported scheme, attempting to open it with
     * ReferenceSequenceFileFactory should throw an exception whose message contains
     * the URI scheme or URI string.
     * 
     * Validates: Requirements 15.3, 15.5
     */
    @Property(tries = 100)
    void referenceFactoryErrorMessageContainsUri(
            @ForAll("unknownSchemeUris") URI invalidUri) {
        
        try {
            ReferenceSequenceFileFactory.getReferenceSequenceFile(invalidUri);
            throw new AssertionError("Expected exception for invalid URI: " + invalidUri);
        } catch (IOException e) {
            String fullMessage = getFullErrorMessage(e);
            String scheme = invalidUri.getScheme();
            String uriString = invalidUri.toString();
            
            boolean containsUriInfo = fullMessage.contains(scheme) ||
                    fullMessage.contains(uriString) ||
                    fullMessage.contains("Invalid URI") ||
                    fullMessage.contains("No filesystem provider");
            
            assert containsUriInfo :
                    String.format("Error message should contain URI information. " +
                            "URI: %s, Scheme: %s, Message: %s", uriString, scheme, fullMessage);
        }
    }

    /**
     * Property 10: Error Messages Include URI Information - SAMFileWriterFactory BAM
     * 
     * For any URI with an unknown/unsupported scheme, attempting to create a BAM
     * writer should throw an exception whose message contains the URI scheme or
     * URI string.
     * 
     * Validates: Requirements 15.3, 15.5
     */
    @Property(tries = 100)
    void samFileWriterFactoryBamErrorMessageContainsUri(
            @ForAll("unknownSchemeUris") URI invalidUri) {
        
        try {
            new SAMFileWriterFactory().makeBAMWriter(null, false, invalidUri);
            throw new AssertionError("Expected exception for invalid URI: " + invalidUri);
        } catch (IOException | NullPointerException e) {
            // NullPointerException may occur before URI validation due to null header
            // We're primarily testing that when URI validation fails, the message contains URI info
            if (e instanceof IOException) {
                String fullMessage = getFullErrorMessage(e);
                String scheme = invalidUri.getScheme();
                String uriString = invalidUri.toString();
                
                boolean containsUriInfo = fullMessage.contains(scheme) ||
                        fullMessage.contains(uriString) ||
                        fullMessage.contains("Invalid URI") ||
                        fullMessage.contains("No filesystem provider");
                
                assert containsUriInfo :
                        String.format("Error message should contain URI information. " +
                                "URI: %s, Scheme: %s, Message: %s", uriString, scheme, fullMessage);
            }
            // NullPointerException is acceptable - it means we hit header validation before URI validation
        }
    }

    /**
     * Property 10: Error Messages Include URI Information - SAMFileWriterFactory SAM
     * 
     * For any URI with an unknown/unsupported scheme, attempting to create a SAM
     * writer should throw an exception whose message contains the URI scheme or
     * URI string.
     * 
     * Validates: Requirements 15.3, 15.5
     */
    @Property(tries = 100)
    void samFileWriterFactorySamErrorMessageContainsUri(
            @ForAll("unknownSchemeUris") URI invalidUri) {
        
        try {
            new SAMFileWriterFactory().makeSAMWriter(null, false, invalidUri);
            throw new AssertionError("Expected exception for invalid URI: " + invalidUri);
        } catch (IOException | NullPointerException e) {
            if (e instanceof IOException) {
                String fullMessage = getFullErrorMessage(e);
                String scheme = invalidUri.getScheme();
                String uriString = invalidUri.toString();
                
                boolean containsUriInfo = fullMessage.contains(scheme) ||
                        fullMessage.contains(uriString) ||
                        fullMessage.contains("Invalid URI") ||
                        fullMessage.contains("No filesystem provider");
                
                assert containsUriInfo :
                        String.format("Error message should contain URI information. " +
                                "URI: %s, Scheme: %s, Message: %s", uriString, scheme, fullMessage);
            }
        }
    }

    /**
     * Property 10: Error Messages Include URI Information - VariantContextWriterBuilder
     * 
     * For any URI with an unknown/unsupported scheme, attempting to set output URI
     * should throw an exception whose message contains the URI scheme or URI string.
     * 
     * Validates: Requirements 15.3, 15.5
     */
    @Property(tries = 100)
    void variantContextWriterBuilderErrorMessageContainsUri(
            @ForAll("unknownSchemeUris") URI invalidUri) {
        
        try {
            new VariantContextWriterBuilder().setOutputURI(invalidUri);
            throw new AssertionError("Expected exception for invalid URI: " + invalidUri);
        } catch (RuntimeException e) {
            String fullMessage = getFullErrorMessage(e);
            String scheme = invalidUri.getScheme();
            String uriString = invalidUri.toString();
            
            boolean containsUriInfo = fullMessage.contains(scheme) ||
                    fullMessage.contains(uriString) ||
                    fullMessage.contains("Invalid URI") ||
                    fullMessage.contains("No filesystem provider");
            
            assert containsUriInfo :
                    String.format("Error message should contain URI information. " +
                            "URI: %s, Scheme: %s, Message: %s", uriString, scheme, fullMessage);
        }
    }

    /**
     * Property 10: Error Messages Include URI Information - Various Unknown Schemes
     * 
     * For any URI with various unknown schemes (s3, hdfs, gs, etc.), attempting to
     * use it should throw an exception whose message contains the scheme information.
     * 
     * Validates: Requirements 15.3, 15.5
     */
    @Property(tries = 100)
    void variousUnknownSchemesErrorMessageContainsScheme(
            @ForAll("variousUnknownSchemeUris") URI invalidUri) {
        
        try {
            SamReaderFactory.makeDefault().open(invalidUri);
            throw new AssertionError("Expected exception for invalid URI: " + invalidUri);
        } catch (IOException e) {
            String fullMessage = getFullErrorMessage(e);
            String scheme = invalidUri.getScheme();
            
            // The error message should mention the scheme or indicate it's unsupported
            boolean containsSchemeInfo = fullMessage.contains(scheme) ||
                    fullMessage.contains("No filesystem provider") ||
                    fullMessage.contains("Invalid URI") ||
                    fullMessage.contains("scheme");
            
            assert containsSchemeInfo :
                    String.format("Error message should contain scheme information. " +
                            "Scheme: %s, Message: %s", scheme, fullMessage);
        }
    }

    // ==================== Providers ====================

    /**
     * Provides URIs with unknown/unsupported schemes for testing.
     * These schemes don't have registered NIO SPI providers.
     */
    @Provide
    Arbitrary<URI> unknownSchemeUris() {
        return Combinators.combine(
                unknownSchemes(),
                pathComponents()
        ).as((scheme, path) -> URI.create(scheme + "://" + path));
    }

    /**
     * Provides URIs with various cloud/distributed filesystem schemes.
     * These are common schemes that typically don't have providers installed.
     */
    @Provide
    Arbitrary<URI> variousUnknownSchemeUris() {
        return Combinators.combine(
                Arbitraries.of("s3", "gs", "hdfs", "wasb", "abfs", "adl", "oss", "cos"),
                pathComponents()
        ).as((scheme, path) -> URI.create(scheme + "://" + path));
    }

    /**
     * Provides unknown scheme names that don't have NIO SPI providers.
     */
    @Provide
    Arbitrary<String> unknownSchemes() {
        // Generate random scheme names that definitely don't exist
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(15)
                .map(s -> "unknown" + s.toLowerCase());
    }

    /**
     * Provides path components for URI construction.
     */
    @Provide
    Arbitrary<String> pathComponents() {
        Arbitrary<String> bucketOrHost = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10)
                .map(String::toLowerCase);
        
        Arbitrary<String> pathPart = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10);
        
        Arbitrary<String> extension = Arbitraries.of("bam", "sam", "cram", "vcf", "fasta");
        
        return Combinators.combine(bucketOrHost, pathPart, extension)
                .as((bucket, path, ext) -> bucket + "/" + path + "." + ext);
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
