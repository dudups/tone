package com.ezone.ezproject.common.storage;

import com.ezone.ezproject.common.storage.exception.StorageException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class FileStorage extends AbstractStorage {
    private String rootPath;

    @Override
    public void save(String key, InputStream content, long contentLength) throws StorageException {
        try (InputStream autoCloseContent = new AutoCloseInputStream(content)) {
            FileUtils.copyToFile(autoCloseContent, file(key));
        } catch (IOException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    @Override
    public OpenResult openDetail(String key) throws StorageException {
        File file = file(key);
        if (file.exists() && file.isFile()) {
            try {
                return OpenResult.builder()
                        .content(FileUtils.openInputStream(file))
                        .lastModifiedMs(file.lastModified())
                        .size(file.length())
                        .build();
            } catch (IOException e) {
                throw new StorageException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public InputStream open(String key) throws StorageException {
        File file = file(key);
        if (file.exists() && file.isFile()) {
            try {
                return FileUtils.openInputStream(file);
            } catch (IOException e) {
                throw new StorageException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public void delete(String key) {
        File file = file(key);
        try {
            FileUtils.forceDelete(file);
        } catch (FileNotFoundException e) {
            // do nothing
        } catch (Exception e) {
            log.error(String.format("delete file:[%s] exception!", file.getAbsolutePath()), e);
        }
    }

    @Override
    public void deleteFolder(String key) {
        delete(key);
    }

    @Override
    public long sizeof(String key) {
        File file = file(key);
        if (!file.exists()) {
            return 0L;
        }
        try {
            return FileUtils.sizeOf(file);
        } catch (Exception e) {
            log.warn(String.format("Size of file:[%s] exception!", file.getAbsolutePath()), e);
            return -1L;
        }
    }

    @Override
    public long sizeofFolder(String key) {
        return sizeof(key);
    }

    @Override
    public void traverseFolder(String key, Consumer<TraverseData> action) {
        File file = file(key);
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            return;
        }
        traverse(key, action);
    }

    private void traverse(String key, Consumer<TraverseData> action) {
        File file = file(key);
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            action.accept(TraverseData.builder()
                    .key(key)
                    .lastModifiedMs(file.lastModified())
                    .size(file.length())
                    .build());
            return;
        }
        String[] fileNames = file.list();
        if (null == fileNames || fileNames.length == 0) {
            return;
        }
        Arrays.stream(fileNames).forEach(fileName -> {
            traverse(StoragePathUtil.join(key, fileName), action);
        });
    }

    private File file(String key) {
        // StoragePathUtil.SEP="/" 不考虑windows系统
        return new File(StoragePathUtil.join(rootPath, key));
    }
}
