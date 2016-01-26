package bg.nijel.aGrep.utils;

import org.apache.commons.codec.DecoderException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.apache.commons.codec.binary.Hex.decodeHex;

/**
 * Created by Nick on 1/19/2016.
 */
public class EncodingHelper {

    private File mFile;
    private String mHexInput;
    private BufferedReader mBufferedReader;
    private InputStreamReader mInputStreamReader;
    private ByteArrayInputStream mByteArrayInputStream;


    public EncodingHelper(File file) {
        this.mFile = file;
    }

    public EncodingHelper(String hexInput) {
        this.mHexInput = hexInput;
    }

    public void reset() {
        this.mFile = null;
        this.mHexInput = null;
        try {
            mByteArrayInputStream.close();
            mInputStreamReader.close();
            mBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] toByteArray(String s) {
        try {
            return decodeHex(s.toCharArray());
        } catch (DecoderException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BufferedReader getReader() throws IOException {
        int size = 0;
        byte[] bytes = null;
        if (mFile != null && mFile.exists() && mFile.length() > 0) {
            size = (int) mFile.length();
            bytes = new byte[size];
            byte[] tmpBuff = new byte[size];
            FileInputStream fis = new FileInputStream(mFile);
            try {
                int read = fis.read(bytes, 0, size);
                if (read < size) {
                    int remain = size - read;
                    while (remain > 0) {
                        read = fis.read(tmpBuff, 0, remain);
                        System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                        remain -= read;
                    }
                }
            } catch (IOException e) {
                bytes = null;
                throw e;
            } finally {
                fis.close();
            }
        } else if (mHexInput != null && mHexInput.length() > 0) {
            size = mHexInput.length() / 2;
            bytes = toByteArray(mHexInput);
        }
        if (bytes != null && size > 0) {
            //RootShell.log(mFile.getAbsolutePath() + ":" + bytes.length);
            mByteArrayInputStream = new ByteArrayInputStream(bytes, 0, size);
            mInputStreamReader = new InputStreamReader(mByteArrayInputStream);
            mBufferedReader = new BufferedReader(mInputStreamReader, 8192);
            return mBufferedReader;
        }
        return null;
    }

}
