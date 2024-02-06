package de.rechner.openatfx;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;


public class UnitTestFileHandler extends LocalFileHandler {
    
    private Path root;
    
    public UnitTestFileHandler() {
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        File file = new File(url.getFile());
        root = file.toPath().getParent().toAbsolutePath();
    }
    
    public UnitTestFileHandler(File rootPath) {
        root = rootPath.toPath();
    }
    
    @Override
    public String getFileRoot(String path) throws IOException {
        return root.toString();
    }

    @Override
    public String getFileName(String path) throws IOException {
        return root.resolve(path).getFileName().toString();
    }

    @Override
    public InputStream getFileStream(String path) throws IOException {
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
