package it.ecubit.elabora.lul.tools.model;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(of = "filename")
public class PdfDocumentInfo implements Serializable {

	private static final long serialVersionUID = -735636449622339700L;
	
	private String filename;
	private int numPages;
	private float[] writingAreaRect = new float[4];
	private int rotationAngle;
	private long filesize;
	private boolean rebuilt;
	private boolean encrypted;

}
