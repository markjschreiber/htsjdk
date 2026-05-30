/*
 * The MIT License
 *
 * Copyright (c) 2015 Pierre Lindenbaum @yokofakun Institut du Thorax - Nantes - France
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
package htsjdk.samtools.filter;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * JavaScript-based SAM record filter.
 * 
 * <p>The script puts the following variables in the script context:
 * <ul>
 *   <li>'record' - a {@link SAMRecord}</li>
 *   <li>'header' - a {@link SAMFileHeader}</li>
 * </ul>
 * 
 * <p>All constructors use {@link Path} for filesystem operations, enabling compatibility
 * with Java NIO Service Provider Interface (SPI) and custom filesystems.
 * 
 * @author Pierre Lindenbaum PhD Institut du Thorax - INSERM - Nantes - France
 */
public class JavascriptSamRecordFilter extends AbstractJavascriptFilter<SAMFileHeader, SAMRecord>
        implements SamRecordFilter {
    /**
     * Constructor using a JavaScript file path.
     * 
     * @param scriptPath the path to the JavaScript file to be compiled
     * @param header the SAM file header
     * @throws IOException if the script file cannot be read
     */
    public JavascriptSamRecordFilter(final Path scriptPath, final SAMFileHeader header) throws IOException {
        super(scriptPath, header);
    }

    /**
     * Constructor using a JavaScript expression.
     * 
     * @param scriptExpression the JavaScript expression to be compiled
     * @param header the SAM file header
     */
    public JavascriptSamRecordFilter(final String scriptExpression, final SAMFileHeader header) {
        super(scriptExpression, header);
    }

    /**
     * Constructor using a Reader.
     * 
     * @param scriptReader the JavaScript reader to be compiled. will be closed
     * @param header the SAM file header
     */
    public JavascriptSamRecordFilter(final Reader scriptReader, final SAMFileHeader header) {
        super(scriptReader, header);
    }

    /** return true of both records are filteredOut (AND) */
    @Override
    public boolean filterOut(final SAMRecord first, final SAMRecord second) {
        return filterOut(first) && filterOut(second);
    }

    /** read is filtered out if the javascript program returns false */
    @Override
    public boolean filterOut(final SAMRecord record) {
        return !accept(record);
    }

    @Override
    public String getRecordKey() {
        return "record";
    }
}
