package htsjdk.samtools;

import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.nio.file.Path;

class IndexFileBufferFactory {

    static IndexFileBuffer getBuffer(Path path, boolean enableMemoryMapping) {
        boolean isCompressed;
        try {
            isCompressed = IOUtil.isBlockCompressed(path);
        } catch (IOException ioe) {
            throw (new RuntimeIOException(ioe));
        }

        return isCompressed ? new CompressedIndexFileBuffer(path) : (enableMemoryMapping ? new MemoryMappedFileBuffer(path) : new RandomAccessFileBuffer(path));
    }

    static IndexFileBuffer getBuffer(SeekableStream seekableStream) {
        boolean isCompressed;
        isCompressed = IOUtil.isGZIPInputStream(seekableStream);

        return isCompressed ?
                new CompressedIndexFileBuffer(seekableStream) :
                new IndexStreamBuffer(seekableStream);
    }
}
