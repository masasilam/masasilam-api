// Buat file baru: src/main/java/com/naskah/demo/util/file/ByteArrayMultipartFile.java

package com.naskah.demo.util.file;

import org.springframework.web.multipart.MultipartFile;
import java.io.*;

public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String filename;
    private final String contentType;

    public ByteArrayMultipartFile(String filename, String contentType, byte[] content) {
        this.filename    = filename;
        this.contentType = contentType;
        this.content     = content;
    }

    @Override public String getName()            { return filename; }
    @Override public String getOriginalFilename(){ return filename; }
    @Override public String getContentType()     { return contentType; }
    @Override public boolean isEmpty()           { return content == null || content.length == 0; }
    @Override public long getSize()              { return content.length; }
    @Override public byte[] getBytes()           { return content; }
    @Override public InputStream getInputStream(){ return new ByteArrayInputStream(content); }
    @Override public void transferTo(File dest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }
}