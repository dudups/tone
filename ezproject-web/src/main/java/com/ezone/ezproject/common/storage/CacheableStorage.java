package com.ezone.ezproject.common.storage;

import com.ezone.ezproject.common.storage.exception.StorageException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class CacheableStorage extends AbstractStorage {
    private IStorage cache;
    private IStorage storage;
    private long cacheMaxContentLength = DEFAULT_CACHE_SIZE;

    private static final long DEFAULT_CACHE_SIZE = 1000 * 1000;

    @Override
    public void save(String key, InputStream content, long contentLength) throws StorageException {
        cache.delete(key);
        storage.save(key, content, contentLength);
    }

    /**
     * 注意：返回的lastModifiedMs等元数据信息，可能是缓存的而不是持久storage的
     * @param key
     * @return
     * @throws StorageException
     */
    @Override
    public OpenResult openDetail(String key) throws StorageException {
        try {
            OpenResult result = cache.openDetail(key);
            if (null != result) {
                return result;
            }
        } catch (Exception e) {
            log.error(String.format("Open key=[%s] from cache exception!", key), e);
        }
        OpenResult result = storage.openDetail(key);
        if (null == result) {
            return null;
        }
        if (result.getSize() > cacheMaxContentLength || result.getSize() < 0) {
            return result;
        }
        try (InputStream content = result.getContent()) {
            cache.save(key, content, result.getSize());
            return cache.openDetail(key);
        } catch (IOException e) {
            log.error(String.format("Update cache key=[%s] exception!", key), e);
        }
        return storage.openDetail(key);
    }

    @Override
    public void delete(String key) {
        storage.delete(key);
        cache.delete(key);
    }

    @Override
    public void deleteFolder(String key) {
        storage.deleteFolder(key);
        cache.deleteFolder(key);
    }

    @Override
    public long sizeof(String key) {
        return storage.sizeof(key);
    }

    @Override
    public long sizeofFolder(String key) {
        return storage.sizeofFolder(key);
    }

    @Override
    public void traverseFolder(String key, Consumer<TraverseData> action) {
        storage.traverseFolder(key, action);
    }
}
