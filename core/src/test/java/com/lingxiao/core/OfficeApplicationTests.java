package com.lingxiao.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

@SpringBootTest
@Slf4j
class OfficeApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testCopyFile(){
        long decode = decode("XIRM3");
        log.info("解码后的pid： {}",decode);
        log.info("编码后的pid： {}",encode(decode));
    }

    private void copyFileNio(String srcPath,String targetPath){
        File file = new File(targetPath);
        if (!file.exists()){
            try {
                Files.createFile(file.toPath());
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
       try ( FileChannel inChannel = new FileInputStream(srcPath).getChannel();
             FileChannel outChannel = new FileInputStream(targetPath).getChannel();){
           MappedByteBuffer mappedBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, new File(srcPath).length());
           outChannel.write(mappedBuffer);
       }catch (IOException e){
           e.printStackTrace();
       }
    }

    public static final String charArrayStr = "VBa08tXmAbQC2y1EJG94PZ7SxsFpUeMdHfgOKhYiRjcDTkIl6Nno5qrWu3vwLz";
    public static long decode(String encodedStr){
        long result = 0;
        int index;
        if (StringUtils.isNotBlank(encodedStr)) {
            for (int i = 0; i < encodedStr.length(); i++) {
                index = charArrayStr.indexOf(encodedStr.charAt(i));
                result += (long) (index * (Math.pow(62, encodedStr.length() - i - 1)));
            }
        }

        return result;
    }

    public static String encode(Long number){
        Long rest = number;
        StringBuilder result = new StringBuilder();
        Long remainder = 0L;
        do {
            remainder = rest % 62;
            result.append(charArrayStr.charAt(remainder.intValue()));
            rest = rest / 62;
        } while(rest > 61);

        result.append(charArrayStr.charAt(rest.intValue()));

        return StringUtils.reverse(result.toString());
    }
}
