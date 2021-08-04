/**
 * 
 */
package com.project.minio.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Fikri
 *
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponseDTO {

	private String responseStatus;
	private String responseMessage;

}
