package com.ezone.ezproject.common.storage;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Component
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class EzbaseStorageHelper {
    @Value("${relativeEzbaseStorage.rootPath}")
    private String rootPath;

    public String path(String path) {
        return StoragePathUtil.join(rootPath, path);
    }

    public String encodedPath(String path) {
        String result = path(path);
        try {
            // URLEncoder按w3c要求，空格转+，而RFC则包括空格等字符统一编码%XX格式(和浏览器编码规范一致)，故需修正处理一下
            return URLEncoder.encode(result, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return result;
        }
    }
}
