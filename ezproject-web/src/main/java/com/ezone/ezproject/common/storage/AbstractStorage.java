package com.ezone.ezproject.common.storage;

import com.ezone.ezproject.common.storage.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
public abstract class AbstractStorage implements IStorage {

    @Override
    public void save(String key, InputStream content) throws StorageException {
        save(key, content, UNKNOWN_CONTENT_LENGTH);
    }

    @Override
    public void save(String key, byte[] content) throws StorageException {
        save(key, new ByteArrayInputStream(content), content.length);
    }

    @Override
    public InputStream open(String key) throws StorageException {
        OpenResult result = openDetail(key);
        if (null == result) {
            return null;
        }
        return result.getContent();
    }

    protected String join(String... paths) {
        return StringUtils.joinWith("/", paths);
    }

}
