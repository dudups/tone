package com.ezone.ezproject.common.storage;

import com.ezone.ezproject.common.storage.exception.StorageException;
import com.ksyun.ks3.dto.GetObjectResult;
import com.ksyun.ks3.dto.HeadBucketResult;
import com.ksyun.ks3.dto.HeadObjectResult;
import com.ksyun.ks3.dto.Ks3Object;
import com.ksyun.ks3.dto.Ks3ObjectSummary;
import com.ksyun.ks3.dto.ObjectListing;
import com.ksyun.ks3.dto.ObjectMetadata;
import com.ksyun.ks3.exception.Ks3ClientException;
import com.ksyun.ks3.exception.serviceside.NoSuchKeyException;
import com.ksyun.ks3.exception.serviceside.NotFoundException;
import com.ksyun.ks3.service.Ks3Client;
import com.ksyun.ks3.service.request.GetObjectRequest;
import com.ksyun.ks3.service.request.HeadObjectRequest;
import com.ksyun.ks3.service.request.ListObjectsRequest;
import com.ksyun.ks3.service.request.PutObjectRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.util.function.Consumer;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class Ks3Storage extends AbstractStorage {
    private Ks3Client client;

    private String bucket;

    @Getter(lazy = true)
    private final boolean bucketEnsured = ensureBucket();

    private String rootPath;

    @Override
    public void save(String key, InputStream content, long contentLength) throws StorageException {
        if (!isBucketEnsured()) {
            throw new StorageException("Bucket is not created!");
        }
        saveWithBucketEnsured(key, content, contentLength);
    }

    private void saveWithBucketEnsured(String key, InputStream content, long contentLength) throws StorageException {
        String fullKey = fullKey(key);
        ObjectMetadata meta = new ObjectMetadata();
        if (contentLength >= 0) {
            meta.setContentLength(contentLength);
        }
        PutObjectRequest request = new PutObjectRequest(bucket, fullKey, content, meta);
        try {
            client.putObject(request);
        } catch (Ks3ClientException e) { // Ks3ServiceException is subclass of Ks3ClientException
            throw new StorageException(e.getMessage(), e);
        } finally {
            if (null != content) {
                try {
                    content.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    @Override
    public OpenResult openDetail(String key) throws StorageException {
        GetObjectRequest request = new GetObjectRequest(bucket, fullKey(key));

        try {
            GetObjectResult result = client.getObject(request);
            Ks3Object object = result.getObject();
            ObjectMetadata meta = object.getObjectMetadata();
            return OpenResult.builder()
                    .content(object.getObjectContent())
                    .lastModifiedMs(meta.getLastModified().getTime())
                    .size(meta.getContentLength())
                    .build();
        } catch (NoSuchKeyException e) {
            // do nothing
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void delete(String key) {
        String fullKey = fullKey(key);
        try {
            client.deleteObject(bucket, fullKey);
        } catch (Exception e) {
            log.error(String.format("delete key:[%s] exception!", fullKey), e);
        }
    }

    @Override
    public void deleteFolder(String key) {
        traverseByFolderPrefix(key, list -> {
            list.getObjectSummaries().forEach(ks3ObjectSummary -> {
                try {
                    client.deleteObject(bucket, ks3ObjectSummary.getKey());
                } catch (Exception e) {
                    log.error(String.format("delete key:[%s] exception!", ks3ObjectSummary.getKey()), e);
                }
            });
        });
    }

    @Override
    public long sizeof(String key) {
        try {
            ObjectMetadata metadata = metadata(key);
            if (null == metadata) {
                return 0L;
            }
            return metadata.getContentLength();
        } catch (Exception e) {
            log.warn(String.format("Size of key:[%s] exception!", key), e);
            return -1;
        }
    }

    @Override
    public long sizeofFolder(String key) {
        long[] size = new long[] { 0 };
        try {
            traverseByFolderPrefix(key, list -> {
                size[0] += list.getObjectSummaries().parallelStream().mapToLong(Ks3ObjectSummary::getSize).sum();
            });
        } catch (Exception e) {
            log.error(String.format("Sizeof folder:[%s] exception!", key), e);
            return -1L;
        }
        return size[0];
    }

    @Override
    public void traverseFolder(String key, Consumer<TraverseData> action) {
        try {
            traverseByFolderPrefix(key, list -> list.getObjectSummaries().forEach(summary -> {
                action.accept(TraverseData.builder()
                        .key(StringUtils.stripStart(
                                StringUtils.substringAfter(summary.getKey(), rootPath),
                                StoragePathUtil.SEP))
                        .lastModifiedMs(summary.getLastModified().getTime())
                        .size(summary.getSize())
                        .build());
            }));
        } catch (Exception e) {
            log.error(String.format("Traverse folder:[%s] exception!", key), e);
        }
    }

    private void traverseByFolderPrefix(String folderPrefix, Consumer<ObjectListing> action) {
        String fullFolderKey = fullFolderKey(folderPrefix);
        long size = 0;
        ObjectListing list = null;
        //初始化一个请求
        ListObjectsRequest request =
                new ListObjectsRequest(bucket, fullFolderKey);
        try {
            do {
                //isTruncated为true时表示之后还有object，所以应该继续循环
                if (list != null && list.isTruncated()) {
                    //在ObjectListing中将返回下次请求的marker
                    //如果请求的时候没有设置delimiter，则不会返回nextMarker,
                    // 需要使用上一次list的最后一个key做为nextMarker
                    request.setMarker(list.getObjectSummaries()
                            .get(list.getObjectSummaries().size() - 1).getKey());
                }
                list = client.listObjects(request);
                action.accept(list);
            } while (list.isTruncated());
        } catch (Exception e) {
            log.error(String.format("traverse folder:[%s] exception!", fullFolderKey), e);
        }
    }

    private ObjectMetadata metadata(String key) {
        String fullKey = fullKey(key);

        try {
            HeadObjectRequest request = new HeadObjectRequest(bucket, fullKey);
            HeadObjectResult result = client.headObject(request);
            return result.getObjectMetadata();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private String fullKey(String key) {
        return StringUtils.joinWith("/", rootPath, key);
    }

    /**
     * ks3匹配前缀，如p可以匹配p1和p/1等；故作为folder需要确保结尾为/
     */
    private String fullFolderKey(String key) {
        String fullFolderKey = StringUtils.joinWith("/", rootPath, key);
        if (fullFolderKey.endsWith("/")) {
            return fullFolderKey;
        }
        return fullFolderKey + "/";
    }

    private boolean ensureBucket() {
        try {
            HeadBucketResult result = client.headBucket(bucket);
            if (HttpStatus.OK.value() != result.getStatueCode()) {
                client.createBucket(bucket);
            }
        } catch (Exception e) {
            log.error(String.format("Ensure bucket:[%s] exception!", bucket), e);
            return false;
        }
        return true;
    }
}
