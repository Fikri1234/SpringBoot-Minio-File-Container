/**
 * 
 */
package com.project.minio.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jlefebure.spring.boot.minio.MinioException;
import com.project.minio.Constant.MessageConstant;
import com.project.minio.DTO.FileResponseDTO;
import com.project.minio.DTO.GeneralResponseDTO;
import com.project.minio.ServiceImpl.MinioServiceImpl;

import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Fikri
 *
 */

@RestController
@RequestMapping("/files")
@Slf4j
public class MinioResource {
	
	@Autowired
    private MinioServiceImpl minioService;

    @GetMapping
    public List<Item> testMinio() throws MinioException {
    	log.info("+++++++++++++++++++++++++");
    	List<Item> it = minioService.list();
    	for (Item d :it) {
    		log.info("objectName: {}",d.objectName());
    		log.info("etag: {}",d.etag());
    		log.info("displayName: {}",d.owner().displayName());
    		log.info("size: {}",d.size());
    		log.info("storageClass: {}",d.storageClass());
    		log.info("versionId: {}",d.versionId());
    	}
    	log.info("========================");
    	List<Item> fls = minioService.fullList();
    	for (Item d :fls) {
    		log.info("objectName: {}",d.objectName());
    		log.info("etag: {}",d.etag());
    		log.info("displayName: {}",d.owner().displayName());
    		log.info("displayName id: {}",d.owner().id());
    		log.info("size: {}",d.size());
    		log.info("storageClass: {}",d.storageClass());
    		log.info("versionId: {}",d.versionId());
    	}
        return minioService.fullList();
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<FileResponseDTO> getObject(@PathVariable("fileName") String fileName) {
        
    	FileResponseDTO dto = new FileResponseDTO();
    	byte[] content = null;
    	InputStream inputStream = null;
    	try {
	    	inputStream = minioService.get(Path.of(fileName));
	        content = IOUtils.toByteArray(inputStream);
	        
	        dto.setResponseStatus(MessageConstant.STATUS_SUCCESS);
	        dto.setResponseMessage(MessageConstant.STATUS_SUCCESS);
	        dto.setData(content);
	        dto.setFileName(fileName);
	        dto.setFileType(fileName);
	        dto.setSize(content.length);
	        
	        inputStream.close();
	        
	        return ResponseEntity
			        .ok()
			        .contentLength(content.length)
			        .header("Content-type", URLConnection.guessContentTypeFromStream(inputStream))
			        .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
			        .body(dto);
	        
    	} catch (MinioException e) {
			// TODO: handle exception
    		e.printStackTrace();
    		dto.setResponseStatus(MessageConstant.STATUS_ERROR);
    		dto.setResponseMessage(e.getMessage());
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			dto.setResponseStatus(MessageConstant.STATUS_ERROR);
			dto.setResponseMessage(e.getMessage());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			dto.setResponseStatus(MessageConstant.STATUS_ERROR);
			dto.setResponseMessage(e.getMessage());
		}
    	
		return ResponseEntity
                .ok()
                .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
                .body(dto);
    }

    @PostMapping
    public void addAttachement(@RequestParam("file") MultipartFile file) {
        Path path = Path.of(file.getOriginalFilename());
        try {
            minioService.upload(path, file.getInputStream(), file.getContentType());
        } catch (MinioException e) {
            throw new IllegalStateException("The file cannot be upload on the internal storage. Please retry later", e);
        } catch (IOException e) {
            throw new IllegalStateException("The file cannot be read", e);
        }
    }
    
    @DeleteMapping("/{fileName}")
    public ResponseEntity<GeneralResponseDTO> deleteObject(@PathVariable("fileName") String fileName) {
        
    	GeneralResponseDTO dto = new GeneralResponseDTO();
    	
    	try {
			minioService.get(Path.of(fileName));
		} catch (MinioException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			dto.setResponseStatus(MessageConstant.STATUS_ERROR);
			dto.setResponseMessage("Unable to delete. Data " +fileName+ " not found");
			
			return ResponseEntity.ok().body(dto);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			dto.setResponseStatus(MessageConstant.STATUS_ERROR);
			dto.setResponseMessage(e.getMessage());
			
			return ResponseEntity.ok().body(dto);
		}
    	
    	try {
    	    	
	    	minioService.remove(Path.of(fileName));
	        
	        dto.setResponseStatus(MessageConstant.STATUS_SUCCESS);
	        dto.setResponseMessage(MessageConstant.STATUS_SUCCESS);
	        
    	} catch (MinioException e) {
			// TODO: handle exception
    		e.printStackTrace();
    		dto.setResponseStatus(MessageConstant.STATUS_ERROR);
    		dto.setResponseMessage(e.getMessage());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			dto.setResponseStatus(MessageConstant.STATUS_ERROR);
			dto.setResponseMessage(e.getMessage());
		}
        
        return ResponseEntity.ok().body(dto);
    }

}
