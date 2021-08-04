/**
 * 
 */
package com.project.minio.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Fikri
 *
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FileResponseDTO extends GeneralResponseDTO {
	
	private String fileName;
    private String fileType;
    private byte[] data;
    private long size;

}
