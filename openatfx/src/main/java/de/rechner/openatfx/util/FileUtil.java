package de.rechner.openatfx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


/**
 * Utility class for file handling.
 * 
 * @author Christian Rechner
 */
public class FileUtil {

    /**
     * Copies a file from source to target.
     * 
     * @param sourceFile The source file.
     * @param destFile The destination file.
     * @throws IOException Error copying file.
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Strips the file extension (e.g. '.txt').
     * 
     * @param s The file name.
     * @return File name without extension.
     */
    public static String stripExtension(final String s) {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
    }

}
