package com.ezone.ezproject.common.storage;

import com.ezone.ezproject.common.storage.exception.StorageException;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface IStorage {
    long UNKNOWN_CONTENT_LENGTH = -1L;

    /**
     * save and close content
     * Deprecated: 如果是ks3存储，ks3-sdk有上传内容过大导致OOM的问题，需指定contentLength可解决；
     * 故在能获取到contentLength的情况下，更推荐使用：save(String key, InputStream content, long contentLength)
     */
    @Deprecated
    void save(String key, InputStream content) throws StorageException;

    /**
     * save and close content
     */
    void save(String key, InputStream content, long contentLength) throws StorageException;

    void save(String key, byte[] content) throws StorageException;

    OpenResult openDetail(String key) throws StorageException;

    InputStream open(String key) throws StorageException;

    void delete(String key);

    void deleteFolder(String key);

    /**
     * 异常情况返回-1
     * @param key
     * @return
     */
    long sizeof(String key);

    long sizeofFolder(String key);

    void traverseFolder(String key, Consumer<TraverseData> action);

    @Data
    @SuperBuilder
    class MetaData {
        private long lastModifiedMs;
        private long size;

        private static final int DAY_MS = 24 * 3600 * 1000;
        public boolean isExpired(int expireDays) {
            if (lastModifiedMs <= 0) {
                return false;
            }
            return System.currentTimeMillis() - lastModifiedMs > expireDays * DAY_MS;
        }
    }

    @Data
    @SuperBuilder
    class OpenResult extends MetaData {
        private InputStream content;

        public void close() throws IOException {
            if (null == content) {
                return;
            }
            content.close();
        }
    }

    @Data
    @SuperBuilder
    class TraverseData extends MetaData {
        /**
         * 遍历到的子key, 需要是相对于storage的path
         */
        private String key;
    }
}
