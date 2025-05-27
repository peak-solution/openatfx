package com.peaksolution.openatfx;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import com.peaksolution.openatfx.api.corba.InstanceElementImplTest;


public class UnitTestFileHandler extends LocalFileHandler {
    
    private Path root;
    
    public UnitTestFileHandler() {
        URL url = InstanceElementImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        File file = new File(url.getFile());
        root = file.toPath().getParent().toAbsolutePath();
    }
    
    public UnitTestFileHandler(File rootPath) {
        root = rootPath.toPath();
    }
    
    @Override
    public String getFileRoot(Path path) throws IOException {
        return root.toString();
    }

    @Override
    public String getFileName(Path path) throws IOException {
        return root.resolve(path).getFileName().toString();
    }

    @Override
    public InputStream getFileStream(Path path) throws IOException {
        File filePath = root.resolve(path).toFile();
        if (!filePath.exists()) {
            throw new IOException("File '" + path + "' not found!");
        }
        if (!filePath.canRead()) {
            throw new IOException("Unable to open file: " + path);
        }
        return new BufferedInputStream(new FileInputStream(filePath));
    }

}
