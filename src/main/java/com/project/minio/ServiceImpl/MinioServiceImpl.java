/**
 * 
 */
package com.project.minio.ServiceImpl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jlefebure.spring.boot.minio.MinioConfigurationProperties;
import com.jlefebure.spring.boot.minio.MinioException;
import com.jlefebure.spring.boot.minio.MinioFetchException;

import io.minio.DownloadObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import io.minio.messages.Item;

/**
 * @author Fikri
 *
 */

@Service
public class MinioServiceImpl {
	
	@Autowired
	private MinioClient minioClient;
	@Autowired
	private MinioConfigurationProperties configurationProperties;

    /**
     * List all objects at root of the bucket
     *
     * @return List of items
     */
    public List<Item> list() {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .prefix("")
                .recursive(false)
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * List all objects at root of the bucket
     *
     * @return List of items
     */
    public List<Item> fullList() {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * List all objects with the prefix given in parameter for the bucket.
     * Simulate a folder hierarchy. Objects within folders (i.e. all objects which match the pattern {@code {prefix}/{objectName}/...}) are not returned
     *
     * @param path Prefix of seeked list of object
     * @return List of items
     */
    public List<Item> list(Path path) {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .prefix(path.toString())
                .recursive(false)
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * List all objects with the prefix given in parameter for the bucket
     * <p>
     * All objects, even those which are in a folder are returned.
     *
     * @param path Prefix of seeked list of object
     * @return List of items
     */
    public List<Item> getFullList(Path path) {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .prefix(path.toString())
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * Utility method which map results to items and return a list
     *
     * @param myObjects Iterable of results
     * @return List of items
     */
    private List<Item> getItems(Iterable<Result<Item>> myObjects) {
        return StreamSupport
            .stream(myObjects.spliterator(), true)
            .map(itemResult -> {
                try {
                    return itemResult.get();
                } catch (Exception e) {
                    throw new MinioFetchException("Error while parsing list of objects", e);
                } 
            })
            .collect(Collectors.toList());
    }

    /**
     * Get an object from Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @return The object as an InputStream
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while fetch object
     */
    public InputStream get(Path path) throws com.jlefebure.spring.boot.minio.MinioException {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(path.toString())
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Get metadata of an object from Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @return Metadata of the  object
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while fetching object metadatas
     */
    public StatObjectResponse getMetadata(Path path) throws com.jlefebure.spring.boot.minio.MinioException {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(path.toString())
                    .build();
            return minioClient.statObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Get metadata for multiples objects from Minio
     *
     * @param paths Paths of all objects with prefix. Objects names must be included.
     * @return A map where all paths are keys and metadatas are values
     */
    public Map<Path, StatObjectResponse> getMetadata(Iterable<Path> paths) {
        return StreamSupport.stream(paths.spliterator(), false)
            .map(path -> {
                try {
                    StatObjectArgs args = StatObjectArgs.builder()
                            .bucket(configurationProperties.getBucket())
                            .object(path.toString())
                            .build();
                    return new HashMap.SimpleEntry<>(path, minioClient.statObject(args));
                } catch (Exception e) {
                    throw new MinioFetchException("Error while parsing list of objects", e);
                }
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a file from Minio, and save it in the {@code fileName} file
     *
     * @param source   Path with prefix to the object. Object name must be included.
     * @param fileName Filename
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while fetch object
     */
    public void getAndSave(Path source, String fileName) throws com.jlefebure.spring.boot.minio.MinioException {
        try {
            DownloadObjectArgs args = DownloadObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .filename(fileName)
                    .build();
            minioClient.downloadObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param headers     Additional headers to put on the file. The map MUST be mutable. All custom headers will start with 'x-amz-meta-' prefix when fetched with {@code getMetadata()} method.
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, Map<String, String> headers) throws
        com.jlefebure.spring.boot.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .headers(headers)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file) throws
        com.jlefebure.spring.boot.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @param headers     Additional headers to put on the file. The map MUST be mutable
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, String contentType, Map<String, String> headers) throws
        com.jlefebure.spring.boot.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .headers(headers)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, String contentType) throws
        com.jlefebure.spring.boot.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     * upload file bigger than Xmx size
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an Filename
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path source, File file) throws
            com.jlefebure.spring.boot.minio.MinioException {
        try {
            UploadObjectArgs args = UploadObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .filename(file.getAbsolutePath())
                    .build();
            minioClient.uploadObject(args);
        } catch (Exception e) {
            throw new com.jlefebure.spring.boot.minio.MinioException("Error while fetching files in Minio", e);
        }
    }


    /**
     * Remove a file to Minio
     *
     * @param source Path with prefix to the object. Object name must be included.
     * @throws com.jlefebure.spring.boot.minio.MinioException if an error occur while removing object
     */
    public void remove(Path source) throws com.jlefebure.spring.boot.minio.MinioException {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(source.toString())
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

}
